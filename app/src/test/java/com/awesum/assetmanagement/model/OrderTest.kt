package com.awesum.assetmanagement.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OrderTest {

    @Test
    fun testOrderCreationWithLinkedJobAndCustomerDetails() {
        val order = Order(
            order_id = 10,
            reference_code = "REF-2026-001",
            user_id = 2,
            customer_name = "Mpilonhle Memela",
            customer_email = "mpilo@gmail.com",
            items = "[{\"product_id\": 1, \"quantity\": 2}]",
            total = 450.00,
            address = "10 Main St, Johannesburg",
            order_status = "Payment Pending",
            created_at = "2026-10-09 10:00:00",
            job_id = 5,
            job_code = "JOB-2026-0005",
            job_status = "Active",
            order_image = "http://localhost/uploads/product1.jpg"
        )

        assertEquals(10, order.order_id)
        assertEquals("REF-2026-001", order.reference_code)
        assertEquals("Mpilonhle Memela", order.customer_name)
        assertEquals("mpilo@gmail.com", order.customer_email)
        assertEquals(2, order.user_id)
        assertEquals(450.00, order.total, 0.001)
        assertEquals("10 Main St, Johannesburg", order.address)
        assertEquals("Payment Pending", order.order_status)
        assertNotNull(order.job_id)
        assertEquals(5, order.job_id)
        assertEquals("JOB-2026-0005", order.job_code)
        assertEquals("Active", order.job_status)
        assertEquals("http://localhost/uploads/product1.jpg", order.order_image)
    }

    @Test
    fun testOrderCreationWithoutLinkedJob() {
        val order = Order(
            order_id = 11,
            reference_code = "REF-2026-002",
            user_id = 3,
            items = null,
            total = 1200.50,
            address = "25 Sandton Dr, Sandton",
            order_status = "Processing",
            created_at = "2026-10-09 12:00:00",
            job_id = null,
            job_code = null,
            job_status = null
        )

        assertEquals(11, order.order_id)
        assertNull(order.job_id)
        assertNull(order.job_code)
        assertNull(order.job_status)
        assertNull(order.items)
    }

    @Test
    fun testOrderCopyAndEquality() {
        val order1 = Order(
            order_id = 1,
            reference_code = "REF-001",
            user_id = 1,
            items = "{}",
            total = 100.00,
            address = "Address 1",
            order_status = "Paid",
            created_at = "2026-10-09",
            job_id = 1,
            job_code = "JOB-001",
            job_status = "Active"
        )

        val order2 = order1.copy(order_status = "Completed")

        assertEquals(order1.order_id, order2.order_id)
        assertEquals(order1.reference_code, order2.reference_code)
        assertEquals("Completed", order2.order_status)
    }
}
