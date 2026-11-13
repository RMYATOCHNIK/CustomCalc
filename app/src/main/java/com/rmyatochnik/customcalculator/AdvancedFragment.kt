package com.rmyatochnik.customcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class AdvancedFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_advanced, container, false)
        val buttons = listOf(
            R.id.btnFact, R.id.btnAbs, R.id.btnMod,
            R.id.btnFloor, R.id.btnCeil, R.id.btnExp,
            R.id.btnMC, R.id.btnMR, R.id.btnMPlus, R.id.btnMMinus
        )

        for (id in buttons) {
            view.findViewById<Button>(id)?.setOnClickListener {
                (activity as? MainActivity)?.onButtonClick((it as Button).text.toString())
            }
        }
        return view
    }
}
