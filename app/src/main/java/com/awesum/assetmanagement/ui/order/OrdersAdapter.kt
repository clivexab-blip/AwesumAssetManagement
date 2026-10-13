package com.awesum.assetmanagement.ui.order

import android.util.Base64
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
        private val tvProductDetails: TextView = itemView.findViewById(R.id.tvProductDetails)
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
            tvCustomerInfo.text = "Customer: $customerName"
            tvItemsSummary.text = formatItemsSummary(order)

            val prodDetails = formatProductDetails(order)
            if (prodDetails.isNotEmpty()) {
                tvProductDetails.visibility = View.VISIBLE
                tvProductDetails.text = prodDetails
            } else {
                tvProductDetails.visibility = View.GONE
            }

            val imageUrl = order.order_image
            if (!imageUrl.isNullOrBlank()) {
                ivOrderPicture.setPadding(0, 0, 0, 0)
                if (imageUrl.startsWith("data:image")) {
                    try {
                        val base64Data = imageUrl.substringAfter(",")
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        Glide.with(itemView.context)
                            .load(imageBytes)
                            .placeholder(R.drawable.ic_order_placeholder)
                            .error(R.drawable.ic_order_placeholder)
                            .centerCrop()
                            .into(ivOrderPicture)
                    } catch (e: Exception) {
                        ivOrderPicture.setImageResource(R.drawable.ic_order_placeholder)
                    }
                } else {
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_order_placeholder)
                        .error(R.drawable.ic_order_placeholder)
                        .centerCrop()
                        .into(ivOrderPicture)
                }
            } else {
                val pad = (12 * itemView.resources.displayMetrics.density).toInt()
                ivOrderPicture.setPadding(pad, pad, pad, pad)
                ivOrderPicture.setImageResource(R.drawable.ic_order_placeholder)
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

        private fun formatProductDetails(order: Order): String {
            val details = order.items_details
            if (!details.isNullOrEmpty()) {
                val detailsList = details.mapNotNull { item ->
                    val cat = item.category?.ifEmpty { null }
                    val price = item.price
                    val desc = item.description?.ifEmpty { null }

                    val parts = mutableListOf<String>()
                    if (cat != null) parts.add("Category: $cat")
                    if (price != null && price > 0) parts.add(String.format("Unit Price: R %.2f", price))
                    if (desc != null) parts.add("Info: $desc")

                    if (parts.isNotEmpty()) parts.joinToString(" • ") else null
                }
                if (detailsList.isNotEmpty()) return detailsList.joinToString("\n")
            }
            return ""
        }

        private fun formatItemsSummary(order: Order): String {
            val details = order.items_details
            if (!details.isNullOrEmpty()) {
                val list = details.map { "${it.name ?: "Product"} (x${it.quantity})" }
                return "Items: " + list.joinToString(", ")
            }

            val itemsJson = order.items
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
