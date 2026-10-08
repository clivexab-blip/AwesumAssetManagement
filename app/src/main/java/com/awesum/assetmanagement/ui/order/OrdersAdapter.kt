package com.awesum.assetmanagement.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.model.Order

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private var items: List<Order> = emptyList()

    fun submitList(list: List<Order>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderRef: TextView = itemView.findViewById(R.id.tvOrderRef)
        private val badgeOrderStatus: TextView = itemView.findViewById(R.id.badgeOrderStatus)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        private val tvJobCode: TextView = itemView.findViewById(R.id.tvJobCode)
        private val btnManageJob: TextView = itemView.findViewById(R.id.btnManageJob)

        fun bind(order: Order) {
            tvOrderRef.text = order.reference_code.ifEmpty { "Order #${order.order_id}" }
            badgeOrderStatus.text = order.order_status
            tvAddress.text = order.address.ifEmpty { "No address specified" }
            tvOrderDate.text = formatDate(order.created_at)
            tvOrderTotal.text = String.format("R %.2f", order.total)

            if (!order.job_code.isNullOrEmpty()) {
                tvJobCode.text = "Job: ${order.job_code}"
                tvJobCode.setTextColor("#3ECF7A".toColorInt())
                btnManageJob.text = "Manage Job →"
            } else {
                tvJobCode.text = "Job: Not Started"
                tvJobCode.setTextColor("#8892A6".toColorInt())
                btnManageJob.text = "Start Job →"
            }

            itemView.setOnClickListener { onOrderClick(order) }
            btnManageJob.setOnClickListener { onOrderClick(order) }
        }

        private fun formatDate(raw: String): String {
            return try {
                raw.split(" ")[0]
            } catch (e: Exception) {
                raw
            }
        }
    }
}
