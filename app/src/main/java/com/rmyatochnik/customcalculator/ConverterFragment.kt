package com.rmyatochnik.customcalculator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class ConverterFragment : Fragment() {

    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var etInputValue: TextInputEditText
    private lateinit var tvConvertedResult: TextView

    private val categories by lazy {
        arrayOf(
            getString(R.string.cat_length),
            getString(R.string.cat_mass),
            getString(R.string.cat_temp),
            getString(R.string.cat_speed),
            getString(R.string.cat_data)
        )
    }

    private val unitsMap by lazy {
        mapOf(
            0 to arrayOf("Метры (м)", "Километры (км)", "Сантиметры (см)", "Миллиметры (мм)", "Мили (mi)", "Футы (ft)", "Дюймы (in)"),
            1 to arrayOf("Килограммы (кг)", "Граммы (г)", "Миллиграммы (мг)", "Фунты (lb)", "Унции (oz)"),
            2 to arrayOf("Цельсий (°C)", "Фаренгейт (°F)", "Кельвин (K)"),
            3 to arrayOf("м/с", "км/ч", "мили/ч (mph)", "Узлы (kn)"),
            4 to arrayOf("Байты (B)", "Килобайты (KB)", "Мегабайты (MB)", "Гигабайты (GB)", "Терабайты (TB)")
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_converter, container, false)

        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        spinnerFrom = view.findViewById(R.id.spinnerFrom)
        spinnerTo = view.findViewById(R.id.spinnerTo)
        etInputValue = view.findViewById(R.id.etInputValue)
        tvConvertedResult = view.findViewById(R.id.tvConvertedResult)

        setupCategorySpinner()

        etInputValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calculateConversion()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerCategory.adapter = adapter

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateUnitSpinners(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val listener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerFrom.onItemSelectedListener = listener
        spinnerTo.onItemSelectedListener = listener
    }

    private fun updateUnitSpinners(categoryIndex: Int) {
        val units = unitsMap[categoryIndex] ?: arrayOf()
        val adapterFrom = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, units)
        val adapterTo = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, units)

        spinnerFrom.adapter = adapterFrom
        spinnerTo.adapter = adapterTo

        if (units.size > 1) {
            spinnerTo.setSelection(1)
        }
        calculateConversion()
    }

    private fun calculateConversion() {
        val inputStr = etInputValue.text.toString()
        if (inputStr.isEmpty() || inputStr == "-") {
            tvConvertedResult.text = "0"
            return
        }

        val inputVal = inputStr.toDoubleOrNull()
        if (inputVal == null) {
            tvConvertedResult.text = getString(R.string.error_msg)
            return
        }

        val categoryPos = spinnerCategory.selectedItemPosition
        val fromPos = spinnerFrom.selectedItemPosition
        val toPos = spinnerTo.selectedItemPosition

        val converted = when (categoryPos) {
            0 -> convertLength(inputVal, fromPos, toPos)
            1 -> convertMass(inputVal, fromPos, toPos)
            2 -> convertTemperature(inputVal, fromPos, toPos)
            3 -> convertSpeed(inputVal, fromPos, toPos)
            4 -> convertData(inputVal, fromPos, toPos)
            else -> inputVal
        }

        val resultStr = if (converted % 1.0 == 0.0) {
            converted.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.6f", converted).trimEnd('0').trimEnd('.')
        }

        tvConvertedResult.text = resultStr
    }

    private fun convertLength(valIn: Double, from: Int, to: Int): Double {
        // Base unit: Meters
        val inMeters = when (from) {
            0 -> valIn           // m
            1 -> valIn * 1000.0  // km
            2 -> valIn / 100.0   // cm
            3 -> valIn / 1000.0  // mm
            4 -> valIn * 1609.344// mi
            5 -> valIn * 0.3048  // ft
            6 -> valIn * 0.0254  // in
            else -> valIn
        }
        return when (to) {
            0 -> inMeters
            1 -> inMeters / 1000.0
            2 -> inMeters * 100.0
            3 -> inMeters * 1000.0
            4 -> inMeters / 1609.344
            5 -> inMeters / 0.3048
            6 -> inMeters / 0.0254
            else -> inMeters
        }
    }

    private fun convertMass(valIn: Double, from: Int, to: Int): Double {
        // Base unit: Kilograms
        val inKg = when (from) {
            0 -> valIn
            1 -> valIn / 1000.0
            2 -> valIn / 1_000_000.0
            3 -> valIn * 0.45359237
            4 -> valIn * 0.028349523125
            else -> valIn
        }
        return when (to) {
            0 -> inKg
            1 -> inKg * 1000.0
            2 -> inKg * 1_000_000.0
            3 -> inKg / 0.45359237
            4 -> inKg / 0.028349523125
            else -> inKg
        }
    }

    private fun convertTemperature(valIn: Double, from: Int, to: Int): Double {
        if (from == to) return valIn
        val celsius = when (from) {
            0 -> valIn
            1 -> (valIn - 32.0) * 5.0 / 9.0
            2 -> valIn - 273.15
            else -> valIn
        }
        return when (to) {
            0 -> celsius
            1 -> (celsius * 9.0 / 5.0) + 32.0
            2 -> celsius + 273.15
            else -> celsius
        }
    }

    private fun convertSpeed(valIn: Double, from: Int, to: Int): Double {
        // Base unit: m/s
        val inMs = when (from) {
            0 -> valIn
            1 -> valIn / 3.6
            2 -> valIn * 0.44704
            3 -> valIn * 0.514444
            else -> valIn
        }
        return when (to) {
            0 -> inMs
            1 -> inMs * 3.6
            2 -> inMs / 0.44704
            3 -> inMs / 0.514444
            else -> inMs
        }
    }

    private fun convertData(valIn: Double, from: Int, to: Int): Double {
        // Base unit: Bytes
        val inBytes = when (from) {
            0 -> valIn
            1 -> valIn * 1024.0
            2 -> valIn * 1024.0 * 1024.0
            3 -> valIn * 1024.0 * 1024.0 * 1024.0
            4 -> valIn * 1024.0 * 1024.0 * 1024.0 * 1024.0
            else -> valIn
        }
        return when (to) {
            0 -> inBytes
            1 -> inBytes / 1024.0
            2 -> inBytes / (1024.0 * 1024.0)
            3 -> inBytes / (1024.0 * 1024.0 * 1024.0)
            4 -> inBytes / (1024.0 * 1024.0 * 1024.0 * 1024.0)
            else -> inBytes
        }
    }
}
