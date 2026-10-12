package com.awesum.assetmanagement.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.model.Order
import com.bumptech.glide.Glide
import org.json.JSONArray

class OrdersAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onSummaryClick: (Order) -> Unit
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
        private val ivOrderPicture: ImageView = itemView.findViewById(R.id.ivOrderPicture)
        private val tvCustomerInfo: TextView = itemView.findViewById(R.id.tvCustomerInfo)
        private val tvItemsSummary: TextView = itemView.findViewById(R.id.tvItemsSummary)
        private val tvOrderRef: TextView = itemView.findViewById(R.id.tvOrderRef)
        private val badgeOrderStatus: TextView = itemView.findViewById(R.id.badgeOrderStatus)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        private val tvJobCode: TextView = itemView.findViewById(R.id.tvJobCode)
        private val btnManageJob: TextView = itemView.findViewById(R.id.btnManageJob)
        private val btnViewSummary: TextView = itemView.findViewById(R.id.btnViewSummary)

        fun bind(order: Order) {
            tvOrderRef.text = order.reference_code.ifEmpty { "Order #${order.order_id}" }
            badgeOrderStatus.text = order.order_status
            tvAddress.text = order.address.ifEmpty { "No address specified" }
            tvOrderDate.text = formatDate(order.created_at)
            tvOrderTotal.text = String.format("R %.2f", order.total)

            val customerName = order.customer_name?.ifEmpty { "Guest Customer" } ?: "Guest Customer"
            val customerEmail = order.customer_email?.ifEmpty { "" } ?: ""
            tvCustomerInfo.text = if (customerEmail.isNotEmpty()) "$customerName ($customerEmail)" else customerName
            tvItemsSummary.text = formatItemsSummary(order.items)

            if (!order.order_image.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(order.order_image)
                    .placeholder(R.drawable.bg_input)
                    .error(R.drawable.bg_input)
                    .centerCrop()
                    .into(ivOrderPicture)
            } else {
                ivOrderPicture.setImageResource(R.drawable.ic_launcher_foreground)
            }

            if (!order.job_code.isNullOrEmpty()) {
                tvJobCode.text = "Job: ${order.job_code}"
                tvJobCode.setTextColor("#3ECF7A".toColorInt())
                btnManageJob.text = "Manage Job →"
                btnViewSummary.visibility = View.VISIBLE
            } else {
                tvJobCode.text = "Job: Not Started"
                tvJobCode.setTextColor("#8892A6".toColorInt())
                btnManageJob.text = "Start Job →"
                btnViewSummary.visibility = View.GONE
            }

            itemView.setOnClickListener { onOrderClick(order) }
            btnManageJob.setOnClickListener { onOrderClick(order) }
            btnViewSummary.setOnClickListener { onSummaryClick(order) }
        }

        private fun formatItemsSummary(itemsJson: String?): String {
            if (itemsJson.isNullOrBlank()) return "Items: Website Product Order"
            return try {
                val jsonArray = JSONArray(itemsJson)
                val summaryList = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.optString("name", obj.optString("title", "Product"))
                    val qty = obj.optInt("quantity", obj.optInt("qty", 1))
                    summaryList.add("$name (x$qty)")
                }
                if (summaryList.isNotEmpty()) "Items: " + summaryList.joinToString(", ") else "Items: Website Product Order"
            } catch (e: Exception) {
                "Items: Website Product Order"
            }
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
