package com.rmyatochnik.customcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class ScientificFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_scientific, container, false)
        val buttons = listOf(
            R.id.btnSin, R.id.btnCos, R.id.btnTan,
            R.id.btnLog, R.id.btnLn, R.id.btnSqrt,
            R.id.btnPow, R.id.btnPi, R.id.btnE
        )

        for (id in buttons) {
            view.findViewById<Button>(id)?.setOnClickListener {
                (activity as? MainActivity)?.onButtonClick((it as Button).text.toString())
            }
        }
        return view
    }
}