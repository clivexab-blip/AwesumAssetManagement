package com.awesum.assetmanagement.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseTest {

    @Test
    fun testApiResponseSuccess() {
        val response = ApiResponse(
            success = true,
            data = "Test Data",
            message = "Operation successful"
        )

        assertTrue(response.success)
        assertEquals("Test Data", response.data)
        assertEquals("Operation successful", response.message)
    }

    @Test
    fun testApiResponseError() {
        val response = ApiResponse<String>(
            success = false,
            data = null,
            message = "Order not found"
        )

        assertFalse(response.success)
        assertEquals(null, response.data)
        assertEquals("Order not found", response.message)
    }
}
