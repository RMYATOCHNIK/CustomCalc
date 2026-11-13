package com.rmyatochnik.customcalculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    private lateinit var tvExpression: TextView
    private lateinit var tvResult: TextView
    private lateinit var tvMemory: TextView
    private var expression = ""
    private var memoryValue = 0.0
    private var customPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDisplayTop = prefs.getBoolean("display_top", true)
        setContentView(R.layout.activity_main)

        // Loading animation fade out
        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        loadingOverlay?.alpha = 1f
        loadingOverlay?.animate()
            ?.alpha(0f)
            ?.setDuration(800)
            ?.setStartDelay(400)
            ?.withEndAction {
                loadingOverlay?.visibility = View.GONE
            }
            ?.start()

        // Apply Custom Colors
        val bgColor = android.graphics.Color.parseColor(prefs.getString("custom_color_bg", "#0F172A"))
        val textColor = android.graphics.Color.parseColor(prefs.getString("custom_color_text", "#F8FAFC"))
        val btnColorStr = prefs.getString("custom_color_btn", "#2563EB")
        val btnColor = try { android.graphics.Color.parseColor(btnColorStr) } catch (e: Exception) { android.graphics.Color.parseColor("#2563EB") }

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main).setBackgroundColor(bgColor)
        findViewById<TextView>(R.id.tvResult)?.setTextColor(textColor)
        findViewById<TextView>(R.id.tvExpression)?.setTextColor(textColor)
        findViewById<TextView>(R.id.tvExpression)?.alpha = 0.7f

        applyButtonColors(findViewById<View>(R.id.main), btnColor)

        if (!isDisplayTop) {
            val display = findViewById<android.widget.LinearLayout>(R.id.displayContainer)
            val keyboard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.keyboardCard)
            
            val layoutParamsDisplay = display.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            val layoutParamsKeyboard = keyboard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            
            layoutParamsDisplay.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParamsDisplay.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            
            layoutParamsKeyboard.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParamsKeyboard.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            
            display.layoutParams = layoutParamsDisplay
            keyboard.layoutParams = layoutParamsKeyboard
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvExpression = findViewById(R.id.tvExpression)
        tvResult = findViewById(R.id.tvResult)
        tvMemory = findViewById(R.id.tvMemory)

        findViewById<ImageButton>(R.id.btnCopy)?.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Calculator Result", tvResult.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copy_result) + ": " + tvResult.text, Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnHistory)?.setOnClickListener {
            val dialog = HistoryDialogFragment()
            dialog.onItemClick = { item ->
                expression = item.expression
                tvExpression.text = expression
                tvResult.text = item.result
            }
            dialog.show(supportFragmentManager, "HistoryDialog")
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 6
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> BasicFragment()
                    1 -> ScientificFragment()
                    2 -> AdvancedFragment()
                    3 -> ColumnFragment()
                    4 -> ConverterFragment()
                    else -> SettingsFragment()
                }
            }
        }

        val tabTitles = listOf(
            getString(R.string.tab_basic),
            getString(R.string.tab_scientific),
            getString(R.string.tab_advanced),
            getString(R.string.tab_column),
            getString(R.string.tab_converter),
            getString(R.string.tab_settings)
        )
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    fun onButtonClick(text: String) {
        playSound()
        vibrate()
        when (text) {
            "AC" -> {
                expression = ""
                tvResult.text = "0"
            }
            "⌫" -> {
                removeLastToken()
            }
            "=" -> {
                evaluate()
            }
            "MC" -> {
                memoryValue = 0.0
                updateMemoryDisplay()
            }
            "MR" -> {
                val memStr = if (memoryValue % 1.0 == 0.0) memoryValue.toLong().toString() else memoryValue.toString()
                expression += memStr
            }
            "M+" -> {
                val currentVal = getCurrentDisplayValue()
                memoryValue += currentVal
                updateMemoryDisplay()
            }
            "M-" -> {
                val currentVal = getCurrentDisplayValue()
                memoryValue -= currentVal
                updateMemoryDisplay()
            }
            "()" -> {
                val openCount = expression.count { it == '(' }
                val closeCount = expression.count { it == ')' }
                expression += if (openCount > closeCount) ")" else "("
            }
            "π" -> expression += "π"
            "e" -> expression += "e"
            "√" -> expression += "sqrt("
            "^" -> expression += "^"
            "!" -> expression += "!"
            "mod" -> expression += "%"
            "sin", "cos", "tan", "log", "ln", "abs", "floor", "ceil", "exp" -> expression += "$text("
            else -> expression += text
        }
        tvExpression.text = expression
    }

    private fun removeLastToken() {
        if (expression.isEmpty()) return
        val functions = listOf("sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(", "floor(", "ceil(", "exp(")
        for (func in functions) {
            if (expression.endsWith(func)) {
                expression = expression.substring(0, expression.length - func.length)
                return
            }
        }
        expression = expression.substring(0, expression.length - 1)
    }

    private fun getCurrentDisplayValue(): Double {
        val resStr = tvResult.text.toString()
        val num = resStr.toDoubleOrNull()
        if (num != null) return num
        return try {
            simpleEvaluate(expression)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun updateMemoryDisplay() {
        if (memoryValue != 0.0) {
            tvMemory.visibility = View.VISIBLE
            val memStr = if (memoryValue % 1.0 == 0.0) memoryValue.toLong().toString() else memoryValue.toString()
            tvMemory.text = "M: $memStr"
        } else {
            tvMemory.visibility = View.GONE
        }
    }

    private fun playSound() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("sound_enabled", true)) {
            val customSoundPath = prefs.getString("custom_sound_path", null)
            val isAsset = prefs.getBoolean("is_asset_sound", false)
            
            if (customSoundPath != null) {
                try {
                    if (customPlayer == null) {
                        customPlayer = MediaPlayer()
                    } else {
                        customPlayer?.reset()
                    }
                    
                    if (isAsset) {
                        val afd = assets.openFd(customSoundPath)
                        customPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } else {
                        customPlayer?.setDataSource(this, Uri.parse(customSoundPath))
                    }

                    customPlayer?.prepare()
                    customPlayer?.start()
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        customPlayer?.release()
        customPlayer = null
    }

    private fun vibrate() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("vibration_enabled", true)) {
            val strength = prefs.getFloat("vib_strength", 128f).toInt()
            val duration = prefs.getFloat("vib_duration", 50f).toLong()
            
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, strength.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    private fun evaluate() {
        if (expression.isEmpty()) return
        
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val customRules = prefs.getStringSet("custom_answers", setOf())
        
        for (rule in customRules!!) {
            val parts = rule.split("|")
            if (parts.size == 2 && expression == parts[0]) {
                tvResult.text = parts[1]
                HistoryManager.addHistory(this, expression, parts[1])
                return
            }
        }

        try {
            val result = simpleEvaluate(expression)
            val resultStr = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
            tvResult.text = resultStr
            HistoryManager.addHistory(this, expression, resultStr)
        } catch (e: ArithmeticException) {
            tvResult.text = "На 0 нельзя делить"
        } catch (e: Exception) {
            tvResult.text = "Error"
        }
    }

    private fun simpleEvaluate(expr: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) {
                        val denominator = parseFactor()
                        if (denominator == 0.0) throw ArithmeticException("На 0 нельзя делить")
                        x /= denominator
                    }
                    else if (eat('%'.code)) x %= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code || ch == 'π'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code || ch == '√'.code) nextChar()
                    val func = expr.substring(startPos, pos)
                    if (func == "π") x = PI
                    else if (func == "e") x = E
                    else {
                        x = parseFactor()
                        if (func == "sqrt" && x < 0) throw RuntimeException("На 0 нельзя делить")
                        x = when (func) {
                            "sqrt", "√" -> sqrt(x)
                            "sin" -> sin(Math.toRadians(x))
                            "cos" -> cos(Math.toRadians(x))
                            "tan" -> tan(Math.toRadians(x))
                            "log" -> log10(x)
                            "ln" -> ln(x)
                            "abs" -> abs(x)
                            "floor" -> floor(x)
                            "ceil" -> ceil(x)
                            "exp" -> exp(x)
                            else -> throw RuntimeException("Unknown function: $func")
                        }
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor())
                if (eat('!'.code)) {
                    var res = 1.0
                    for (i in 1..x.toInt()) res *= i
                    x = res
                }

                return x
            }
        }.parse()
    }

    fun applyButtonColors(view: View?, color: Int) {
        if (view == null) return
        if (view is android.widget.Button) {
            view.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyButtonColors(view.getChildAt(i), color)
            }
        }
    }
}
