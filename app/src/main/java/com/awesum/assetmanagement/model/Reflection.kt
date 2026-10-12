package com.awesum.assetmanagement.model

data class Reflection(
    val reflection_id: Int? = null,
    val job_id: Int,
    val notes: String? = null,
    val team_must_return: Int = 0,
    val submitted: Int = 0,
    val created_at: String? = null
)
