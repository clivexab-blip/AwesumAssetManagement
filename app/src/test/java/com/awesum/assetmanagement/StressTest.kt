package com.awesum.assetmanagement

import com.awesum.assetmanagement.model.ApiResponse
import com.awesum.assetmanagement.model.Asset
import com.awesum.assetmanagement.model.Job
import com.awesum.assetmanagement.model.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class StressTest {

    @Test
    fun testAssetFilteringPerformanceUnderStress() {
        // Generate 100,000 asset items
        val categories = listOf("Dispatch", "Equipment", "Vehicle")
        val statuses = listOf("In Use", "Out for Service")

        val largeAssetList = List(100_000) { index ->
            Asset(
                asset_id = index + 1,
                job_id = (index % 50) + 1,
                name = "Asset Item #$index",
                category = categories[index % categories.size],
                status = statuses[index % statuses.size],
                quantity = (index % 5) + 1,
                notes = "Note $index"
            )
        }

        // Measure filtering time over 100,000 items
        val elapsed = measureTimeMillis {
            val dispatch = largeAssetList.filter { it.category == "Dispatch" }
            val equipment = largeAssetList.filter { it.category == "Equipment" }
            val vehicle = largeAssetList.filter { it.category == "Vehicle" }

            assertEquals(33334, dispatch.size)
            assertEquals(33333, equipment.size)
            assertEquals(33333, vehicle.size)
        }

        println("STRESS TEST PASS: Filtered 100,000 items into 3 categories in $elapsed ms")
        assertTrue("Filtering 100k items should be under 500ms", elapsed < 500)
    }

    @Test
    fun testHighConcurrentCoroutinesJobSwitchingStress() = runBlocking {
        val jobsCount = 500
        val concurrentTasks = List(jobsCount) { index ->
            async(Dispatchers.Default) {
                val jobId = index + 1
                val mockJob = Job(
                    job_id = jobId,
                    job_code = "JOB-2026-$jobId",
                    user_id = 1,
                    order_id = jobId,
                    status = if (jobId % 2 == 0) "Completed" else "Active",
                    installation_address = "$jobId Main St",
                    start_time = "2026-10-09 10:00:00",
                    end_time = null
                )
                mockJob.job_id
            }
        }

        val elapsed = measureTimeMillis {
            val results = concurrentTasks.awaitAll()
            assertEquals(jobsCount, results.size)
        }

        println("STRESS TEST PASS: Executed 500 concurrent coroutine job tasks in $elapsed ms")
        assertTrue("500 concurrent coroutines should finish under 1000ms", elapsed < 1000)
    }

    @Test
    fun testLargeOrderListMappingStress() {
        val orderCount = 50_000
        val elapsed = measureTimeMillis {
            val orders = List(orderCount) { index ->
                Order(
                    order_id = index + 1,
                    reference_code = "REF-$index",
                    user_id = (index % 10) + 1,
                    items = "[{\"item\": $index}]",
                    total = 100.0 + index,
                    address = "Street #$index",
                    order_status = "Payment Pending",
                    created_at = "2026-10-09",
                    job_id = if (index % 2 == 0) index else null,
                    job_code = if (index % 2 == 0) "JOB-$index" else null,
                    job_status = if (index % 2 == 0) "Active" else null
                )
            }

            val wrappedResponse = ApiResponse(
                success = true,
                data = orders,
                message = "Orders fetched"
            )

            val linkedOrders = wrappedResponse.data?.filter { it.job_id != null } ?: emptyList()
            assertEquals(25_000, linkedOrders.size)
        }

        println("STRESS TEST PASS: Processed 50,000 orders and filtered linked jobs in $elapsed ms")
        assertTrue("50,000 order list processing should be under 500ms", elapsed < 500)
    }
}
