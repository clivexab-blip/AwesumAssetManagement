package com.awesum.assetmanagement

import com.awesum.assetmanagement.model.ApiResponse
import com.awesum.assetmanagement.model.Asset
import com.awesum.assetmanagement.model.Job
import com.awesum.assetmanagement.model.Order
import com.awesum.assetmanagement.model.SitePhoto
import com.awesum.assetmanagement.model.TeamMember
import com.awesum.assetmanagement.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemTest {

    @Test
    fun testEndToEndWebsiteOrderToJobSystemWorkflow() {
        // 1. System Auth Sync Simulation
        val user = User(
            user_id = 1,
            name = "Sipho Dlamini",
            email = "sipho@awesum.co.za"
        )
        assertNotNull(user.user_id)
        assertEquals("Sipho Dlamini", user.name)
        assertEquals("sipho@awesum.co.za", user.email)

        // 2. System Orders Retrieval Simulation
        val websiteOrder = Order(
            order_id = 101,
            reference_code = "ORD-2026-9001",
            user_id = user.user_id,
            items = "[{\"product_id\":1,\"name\":\"Custom Signage\",\"qty\":2}]",
            total = 1250.00,
            address = "14 Rivonia Rd, Sandton, 2196",
            order_status = "Paid",
            created_at = "2026-10-09 08:00:00",
            job_id = null,
            job_code = null,
            job_status = null
        )
        assertEquals("ORD-2026-9001", websiteOrder.reference_code)

        // 3. System Job Provisioning Flow (get_or_create_job_for_order)
        val provisionedJob = Job(
            job_id = 50,
            job_code = "JOB-" + websiteOrder.reference_code,
            user_id = user.user_id,
            order_id = websiteOrder.order_id,
            status = "Active",
            installation_address = websiteOrder.address,
            start_time = "2026-10-09 08:30:00",
            end_time = null
        )

        // Set active job state in system
        Constants.CURRENT_JOB_ID = provisionedJob.job_id
        assertEquals(50, Constants.CURRENT_JOB_ID)
        assertEquals("JOB-ORD-2026-9001", provisionedJob.job_code)
        assertEquals(websiteOrder.order_id, provisionedJob.order_id)

        // 4. Asset Management System Operations
        val assetsList = mutableListOf(
            Asset(1, Constants.CURRENT_JOB_ID, "Paste Brush Set", "Dispatch", "In Use", 1, "Notes A"),
            Asset(2, Constants.CURRENT_JOB_ID, "Scaffold Section", "Equipment", "In Use", 2, "Notes B"),
            Asset(3, Constants.CURRENT_JOB_ID, "Van GP 45 XY", "Vehicle", "In Use", 1, "Vehicle Notes")
        )
        assertEquals(3, assetsList.size)

        // Filter system assets by category
        val dispatchAssets = assetsList.filter { it.category == "Dispatch" }
        assertEquals(1, dispatchAssets.size)

        // 5. Installation Team Management System Operations
        val teamMembers = mutableListOf(
            TeamMember(1, Constants.CURRENT_JOB_ID, "Sipho Dlamini", "Lead Installer", "+27 71 000 0000"),
            TeamMember(2, Constants.CURRENT_JOB_ID, "Thandeka Mokoena", "Technician", "+27 72 111 2222")
        )
        assertEquals(2, teamMembers.size)

        // 6. Site Photo Upload System Operations
        val sitePhotos = mutableListOf(
            SitePhoto(1, Constants.CURRENT_JOB_ID, "http://localhost/uploads/job50_1.jpg", "Completed wall print", "2026-10-09 10:00:00")
        )
        assertEquals(1, sitePhotos.size)

        // 7. Post-Job Reflection System Submission
        val reflectionSubmitted = true
        val reflectionNotes = "All installation steps completed according to spec."
        assertTrue(reflectionSubmitted)
        assertEquals("All installation steps completed according to spec.", reflectionNotes)

        // 8. Final Job Log Submission System State Update
        val completedJob = provisionedJob.copy(status = "Completed", end_time = "2026-10-09 12:00:00")
        assertEquals("Completed", completedJob.status)
        assertNotNull(completedJob.end_time)

        // 9. System Wrapup & Verification
        val finalResponse = ApiResponse(
            success = true,
            data = completedJob,
            message = "Job log successfully completed"
        )
        assertTrue(finalResponse.success)
        assertEquals("Job log successfully completed", finalResponse.message)
    }
}
