package com.rmyatochnik.customcalculator

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class SettingsFragment : Fragment() {

    private var previewPlayer: MediaPlayer? = null

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val contentResolver = requireContext().contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("custom_sound_path", it.toString())
                .putBoolean("is_asset_sound", false)
                .apply()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Дизайн
        val etBg = view.findViewById<TextInputEditText>(R.id.etColorBg)
        val etText = view.findViewById<TextInputEditText>(R.id.etColorText)
        val etBtn = view.findViewById<TextInputEditText>(R.id.etColorBtn)
        val btnApply = view.findViewById<Button>(R.id.btnApplyColors)

        etBg.setText(prefs.getString("custom_color_bg", "#0F172A"))
        etText.setText(prefs.getString("custom_color_text", "#F8FAFC"))
        etBtn.setText(prefs.getString("custom_color_btn", "#2563EB"))

        fun applyTheme(bg: String, text: String, btn: String) {
            etBg.setText(bg)
            etText.setText(text)
            etBtn.setText(btn)
            prefs.edit()
                .putString("custom_color_bg", bg)
                .putString("custom_color_text", text)
                .putString("custom_color_btn", btn)
                .apply()
            activity?.recreate()
        }

        // Пресеты
        view.findViewById<Button>(R.id.btnPresetClassic)?.setOnClickListener { applyTheme("#0F172A", "#F8FAFC", "#2563EB") }
        view.findViewById<Button>(R.id.btnPresetCyberpunk)?.setOnClickListener { applyTheme("#120024", "#00FFCC", "#2A004E") }
        view.findViewById<Button>(R.id.btnPresetEmerald)?.setOnClickListener { applyTheme("#064E3B", "#ECFDF5", "#047857") }
        view.findViewById<Button>(R.id.btnPresetDracula)?.setOnClickListener { applyTheme("#282A36", "#F8F8F2", "#44475A") }
        view.findViewById<Button>(R.id.btnPresetSunset)?.setOnClickListener { applyTheme("#2D1B2D", "#FFE5EC", "#5C2A4A") }

        btnApply.setOnClickListener {
            prefs.edit()
                .putString("custom_color_bg", etBg.text.toString())
                .putString("custom_color_text", etText.text.toString())
                .putString("custom_color_btn", etBtn.text.toString())
                .apply()
            activity?.recreate()
        }

        // Язык
        view.findViewById<Button>(R.id.btnLangRu).setOnClickListener {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("ru")
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
        view.findViewById<Button>(R.id.btnLangEn).setOnClickListener {
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags("en")
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

        // Звуки
        val switchSound = view.findViewById<SwitchMaterial>(R.id.switchSound)
        switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        view.findViewById<Button>(R.id.btnSelectSound).setOnClickListener {
            pickAudio.launch(arrayOf("audio/*"))
        }

        view.findViewById<Button>(R.id.btnSoundLibrary).setOnClickListener {
            showSoundLibraryDialog()
        }

        // Вибрация
        val switchVibration = view.findViewById<SwitchMaterial>(R.id.switchVibration)
        switchVibration.isChecked = prefs.getBoolean("vibration_enabled", true)
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        val sliderStrength = view.findViewById<Slider>(R.id.sliderVibStrength)
        sliderStrength.value = prefs.getFloat("vib_strength", 128f)
        sliderStrength.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat("vib_strength", value).apply()
        }

        val sliderDuration = view.findViewById<Slider>(R.id.sliderVibDuration)
        sliderDuration.value = prefs.getFloat("vib_duration", 50f)
        sliderDuration.addOnChangeListener { _, value, _ ->
            prefs.edit().putFloat("vib_duration", value).apply()
        }

        // Интерфейс
        val switchDisplayTop = view.findViewById<SwitchMaterial>(R.id.switchDisplayTop)
        switchDisplayTop.isChecked = prefs.getBoolean("display_top", true)
        switchDisplayTop.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("display_top", isChecked).apply()
            activity?.recreate()
        }

        // Кастомные ответы
        val etExample = view.findViewById<TextInputEditText>(R.id.etCustomExample)
        val etAnswer = view.findViewById<TextInputEditText>(R.id.etCustomAnswer)
        val btnAddRule = view.findViewById<Button>(R.id.btnAddCustomAnswer)
        val btnClearRules = view.findViewById<Button>(R.id.btnClearCustomAnswers)
        val layoutRules = view.findViewById<LinearLayout>(R.id.layoutCustomRules)

        fun renderRules() {
            layoutRules.removeAllViews()
            val currentRules = prefs.getStringSet("custom_answers", setOf()) ?: setOf()
            for (rule in currentRules) {
                val parts = rule.split("|")
                if (parts.size == 2) {
                    val rowView = LayoutInflater.from(context).inflate(R.layout.item_history, layoutRules, false)
                    rowView.findViewById<TextView>(R.id.tvHistoryTimestamp).text = "Правило:"
                    rowView.findViewById<TextView>(R.id.tvHistoryExpression).text = parts[0]
                    rowView.findViewById<TextView>(R.id.tvHistoryResult).text = "= ${parts[1]}"
                    rowView.findViewById<ImageButton>(R.id.btnDeleteHistory).setOnClickListener {
                        val updated = (prefs.getStringSet("custom_answers", setOf()) ?: setOf()).toMutableSet()
                        updated.remove(rule)
                        prefs.edit().putStringSet("custom_answers", updated).apply()
                        renderRules()
                    }
                    layoutRules.addView(rowView)
                }
            }
        }

        renderRules()

        btnAddRule.setOnClickListener {
            val ex = etExample.text.toString().trim()
            val ans = etAnswer.text.toString().trim()
            if (ex.isNotEmpty() && ans.isNotEmpty()) {
                val currentRules = prefs.getStringSet("custom_answers", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                currentRules.add("$ex|$ans")
                prefs.edit().putStringSet("custom_answers", currentRules).apply()
                etExample.text?.clear()
                etAnswer.text?.clear()
                Toast.makeText(context, "Правило добавлено!", Toast.LENGTH_SHORT).show()
                renderRules()
            }
        }

        btnClearRules.setOnClickListener {
            prefs.edit().remove("custom_answers").apply()
            Toast.makeText(context, "Все правила удалены", Toast.LENGTH_SHORT).show()
            renderRules()
        }

        return view
    }

    private fun showSoundLibraryDialog() {
        try {
            val assetManager = requireContext().assets
            val allFiles = assetManager.list("") ?: arrayOf()
            val sounds = allFiles.filter { it.endsWith(".mp3") || it.endsWith(".wav") || it.endsWith(".ogg") }.toTypedArray()

            if (sounds.isEmpty()) {
                Toast.makeText(context, "Библиотека пуста. Звуки не найдены.", Toast.LENGTH_SHORT).show()
                return
            }

            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Выберите звук (нажмите для выбора)")
            builder.setItems(sounds) { _, which ->
                val selectedSound = sounds[which]
                playSoundPreview(selectedSound)
                val prefs = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("custom_sound_path", selectedSound)
                    .putBoolean("is_asset_sound", true)
                    .apply()
                Toast.makeText(context, "Выбрано: $selectedSound", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSoundPreview(assetName: String) {
        try {
            previewPlayer?.release()
            previewPlayer = MediaPlayer().apply {
                val afd = requireContext().assets.openFd(assetName)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        previewPlayer?.release()
        previewPlayer = null
    }
}
