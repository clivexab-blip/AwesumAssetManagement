package com.awesum.assetmanagement.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JobTest {

    @Test
    fun testJobCreation() {
        val job = Job(
            job_id = 1,
            job_code = "JOB-2026-0083",
            user_id = 1,
            order_id = 101,
            status = "Active",
            installation_address = "14 Rivonia Rd, Sandton, 2196",
            start_time = "2026-08-29 07:30:00",
            end_time = "2026-08-29 15:45:00"
        )

        assertEquals(1, job.job_id)
        assertEquals("JOB-2026-0083", job.job_code)
        assertEquals(1, job.user_id)
        assertEquals(101, job.order_id)
        assertEquals("Active", job.status)
        assertEquals("14 Rivonia Rd, Sandton, 2196", job.installation_address)
        assertEquals("2026-08-29 07:30:00", job.start_time)
        assertEquals("2026-08-29 15:45:00", job.end_time)
    }

    @Test
    fun testJobWithNullOrderIdAndEndTime() {
        val job = Job(
            job_id = 2,
            job_code = "JOB-2026-0084",
            user_id = 2,
            order_id = null,
            status = "Active",
            installation_address = "Cape Town CBD",
            start_time = "2026-10-09 08:00:00",
            end_time = null
        )

        assertEquals(2, job.job_id)
        assertNull(job.order_id)
        assertNull(job.end_time)
    }

    @Test
    fun testJobDefaultOrderId() {
        val job = Job(
            job_id = 3,
            job_code = "JOB-2026-0085",
            user_id = 1,
            status = "Completed",
            installation_address = "Durban Central",
            start_time = "2026-10-08 09:00:00",
            end_time = "2026-10-08 17:00:00"
        )

        assertNull(job.order_id)
        assertEquals("Completed", job.status)
    }
}
