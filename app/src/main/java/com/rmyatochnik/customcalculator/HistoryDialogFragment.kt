package com.rmyatochnik.customcalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class HistoryDialogFragment : BottomSheetDialogFragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnClear: Button
    private lateinit var adapter: HistoryAdapter

    var onItemClick: ((HistoryItem) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_history, container, false)
        rvHistory = view.findViewById(R.id.rvHistory)
        tvEmpty = view.findViewById(R.id.tvHistoryEmpty)
        btnClear = view.findViewById(R.id.btnClearHistory)

        setupRecyclerView()

        btnClear.setOnClickListener {
            context?.let { ctx ->
                HistoryManager.clearHistory(ctx)
                loadHistory()
            }
        }

        loadHistory()
        return view
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemSelect = { item ->
                onItemClick?.invoke(item)
                dismiss()
            },
            onItemDelete = { item ->
                context?.let { ctx ->
                    HistoryManager.deleteItem(ctx, item.id)
                    loadHistory()
                }
            }
        )
        rvHistory.adapter = adapter
    }

    private fun loadHistory() {
        val list = context?.let { HistoryManager.getHistory(it) } ?: emptyList()
        if (list.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
            adapter.submitList(list)
        }
    }

    private class HistoryAdapter(
        private val onItemSelect: (HistoryItem) -> Unit,
        private val onItemDelete: (HistoryItem) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        private var items = listOf<HistoryItem>()

        fun submitList(newList: List<HistoryItem>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTimestamp.text = item.timestamp
            holder.tvExpression.text = item.expression
            holder.tvResult.text = "= ${item.result}"

            holder.itemView.setOnClickListener {
                onItemSelect(item)
            }

            holder.btnDelete.setOnClickListener {
                onItemDelete(item)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTimestamp: TextView = view.findViewById(R.id.tvHistoryTimestamp)
            val tvExpression: TextView = view.findViewById(R.id.tvHistoryExpression)
            val tvResult: TextView = view.findViewById(R.id.tvResult) ?: view.findViewById(R.id.tvHistoryResult)
            val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteHistory)
        }
    }
}
