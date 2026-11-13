package com.rmyatochnik.customcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class ColumnFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_column, container, false)
        
        val etNum1 = view.findViewById<TextInputEditText>(R.id.etNum1)
        val etNum2 = view.findViewById<TextInputEditText>(R.id.etNum2)
        val spinnerOp = view.findViewById<Spinner>(R.id.spinnerOp)
        val btnCalculate = view.findViewById<Button>(R.id.btnCalculateColumn)
        val tvResult = view.findViewById<TextView>(R.id.tvColumnResult)

        val ops = arrayOf("+", "-", "*", "/")
        spinnerOp.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ops)

        btnCalculate.setOnClickListener {
            val n1 = etNum1.text.toString()
            val n2 = etNum2.text.toString()
            val op = spinnerOp.selectedItem.toString()
            
            if (n1.isNotEmpty() && n2.isNotEmpty()) {
                tvResult.text = generateDetailedColumn(n1, n2, op)
            }
        }
        
        return view
    }

    private fun generateDetailedColumn(n1: String, n2: String, op: String): String {
        val v1 = n1.toLongOrNull() ?: return "Только целые числа"
        val v2 = n2.toLongOrNull() ?: return "Только целые числа"
        
        return when (op) {
            "*" -> formatMultiplication(v1, v2)
            "/" -> formatDivision(v1, v2)
            else -> formatSimple(v1, v2, op)
        }
    }

    private fun formatSimple(v1: Long, v2: Long, op: String): String {
        val res = if (op == "+") v1 + v2 else v1 - v2
        val s1 = v1.toString()
        val s2 = v2.toString()
        val sRes = res.toString()
        val maxLen = maxOf(s1.length, s2.length, sRes.length) + 1
        
        return StringBuilder().apply {
            append(s1.padStart(maxLen)).append("\n")
            append(op).append(s2.padStart(maxLen - 1)).append("\n")
            append("—".repeat(maxLen)).append("\n")
            append(sRes.padStart(maxLen))
        }.toString()
    }

    private fun formatMultiplication(v1: Long, v2: Long): String {
        val s1 = v1.toString()
        val s2 = v2.toString()
        val sRes = (v1 * v2).toString()
        val steps = mutableListOf<String>()
        
        if (s2.length > 1) {
            s2.reversed().forEachIndexed { index, char ->
                val digit = char.toString().toLong()
                if (digit > 0) {
                    val stepVal = v1 * digit
                    steps.add(stepVal.toString() + " ".repeat(index))
                }
            }
        }

        val maxLen = maxOf(s1.length, s2.length, sRes.length, steps.maxOfOrNull { it.trim().length + (it.length - it.trim().length) } ?: 0) + 1
        
        return StringBuilder().apply {
            append(s1.padStart(maxLen)).append("\n")
            append("×").append(s2.padStart(maxLen - 1)).append("\n")
            append("—".repeat(maxLen)).append("\n")
            if (steps.size > 1) {
                steps.forEach { append(it.padStart(maxLen)).append("\n") }
                append("—".repeat(maxLen)).append("\n")
            }
            append(sRes.padStart(maxLen))
        }.toString()
    }

    private fun formatDivision(v1: Long, v2: Long): String {
        if (v2 == 0L) return "На 0 нельзя делить"
        if (v1 < v2) {
            val s1 = v1.toString()
            val s2 = v2.toString()
            return "$s1 | $s2\n" + " ".repeat(s1.length) + " | ———\n" + " ".repeat(s1.length) + " | 0"
        }

        val s1 = v1.toString()
        val s2 = v2.toString()
        val res = v1 / v2
        val sRes = res.toString()
        
        val sb = StringBuilder()
        val digits = s1.map { it - '0' }
        var current = 0L
        var firstStep = true
        
        // Header line
        sb.append(s1).append(" | ").append(s2).append("\n")
        
        for (i in digits.indices) {
            current = current * 10 + digits[i]
            if (current >= v2) {
                val q = current / v2
                val prod = q * v2
                val prodStr = prod.toString()
                
                val currentPadding = i - prodStr.length + 1
                val pad = " ".repeat(currentPadding.coerceAtLeast(0))
                
                // Subtract line
                sb.append(pad).append("-").append(prod)
                if (firstStep) {
                    val rightPadCount = (s1.length - (currentPadding + prodStr.length + 1)).coerceAtLeast(0)
                    val rightPad = " ".repeat(rightPadCount)
                    sb.append(rightPad).append(" | ").append("—".repeat(maxOf(s2.length, sRes.length)))
                }
                sb.append("\n")
                
                // Horizontal line
                sb.append(pad).append(" ").append("—".repeat(prodStr.length))
                if (firstStep) {
                    val rightPadCount = (s1.length - (currentPadding + prodStr.length + 1)).coerceAtLeast(0)
                    val rightPad = " ".repeat(rightPadCount)
                    sb.append(rightPad).append(" | ").append(sRes)
                    firstStep = false
                }
                sb.append("\n")
                
                current %= v2
            }
        }
        
        // Final remainder
        val lastRemainder = current.toString()
        val lastPadCount = (s1.length - lastRemainder.length).coerceAtLeast(0)
        sb.append(" ".repeat(lastPadCount)).append(lastRemainder)
        
        return sb.toString()
    }
}