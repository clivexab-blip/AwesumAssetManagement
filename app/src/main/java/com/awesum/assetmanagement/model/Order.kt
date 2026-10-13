package com.awesum.assetmanagement.model

data class OrderItemDetail(
    val product_id: Int? = null,
    val name: String? = null,
    val category: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val image: String? = null,
    val quantity: Int = 1
)

data class Order(
    val order_id: Int,
    val reference_code: String,
    val user_id: Int,
    val customer_name: String? = null,
    val customer_email: String? = null,
    val items: String?,
    val items_details: List<OrderItemDetail>? = null,
    val total: Double,
    val address: String,
    val order_status: String,
    val created_at: String,
    val job_id: Int?,
    val job_code: String?,
    val job_status: String?,
    val order_image: String? = null
)
