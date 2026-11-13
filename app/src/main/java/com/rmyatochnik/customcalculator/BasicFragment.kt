package com.rmyatochnik.customcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class BasicFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_basic, container, false)
        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnDot, R.id.btnAdd, R.id.btnSub, R.id.btnMul, R.id.btnDiv,
            R.id.btnAC, R.id.btnParens, R.id.btnPercent, R.id.btnEqual, R.id.btnDel
        )

        for (id in buttons) {
            view.findViewById<Button>(id)?.setOnClickListener {
                (activity as? MainActivity)?.onButtonClick((it as Button).text.toString())
            }
        }

        // Long press on backspace to clear all
        view.findViewById<Button>(R.id.btnDel)?.setOnLongClickListener {
            (activity as? MainActivity)?.onButtonClick("AC")
            true
        }

        return view
    }
}
