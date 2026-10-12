package com.awesum.assetmanagement

import com.awesum.assetmanagement.model.ApiResponse
import com.awesum.assetmanagement.model.Asset
import com.awesum.assetmanagement.model.Job
import com.awesum.assetmanagement.model.Order
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceTest {

    private val gson = Gson()

    @Test
    fun testJsonDeserializationThroughputBenchmark() {
        val jsonPayload = """
            {
                "success": true,
                "message": "Orders retrieved",
                "data": [
                    {
                        "order_id": 101,
                        "reference_code": "REF-2026-999",
                        "user_id": 5,
                        "items": "[{\"product_id\":1,\"quantity\":3}]",
                        "total": 850.50,
                        "address": "100 Rosebank Rd, Johannesburg",
                        "order_status": "Paid",
                        "created_at": "2026-10-09 14:00:00",
                        "job_id": 42,
                        "job_code": "JOB-2026-0042",
                        "job_status": "Active"
                    }
                ]
            }
        """.trimIndent()

        val iterations = 10_000
        val type = object : TypeToken<ApiResponse<List<Order>>>() {}.type

        val elapsed = measureTimeMillis {
            repeat(iterations) {
                val parsed: ApiResponse<List<Order>> = gson.fromJson(jsonPayload, type)
                assertTrue(parsed.success)
                assertEquals(101, parsed.data?.first()?.order_id)
            }
        }

        val opsPerSec = (iterations.toDouble() / elapsed) * 1000
        println("PERFORMANCE BENCHMARK: Deserialized $iterations JSON payloads in $elapsed ms (${opsPerSec.toInt()} ops/sec)")
        assertTrue("JSON parsing throughput should exceed 1,000 ops/sec", opsPerSec > 1000)
    }

    @Test
    fun testParallelCoroutineNetworkSimulationThroughput() = runBlocking {
        val iterations = 1_000

        val elapsed = measureTimeMillis {
            repeat(iterations) {
                val jobDef = async(Dispatchers.Default) {
                    Job(1, "JOB-1", 1, 10, "Active", "Address 1", "2026-10-09", null)
                }
                val assetsDef = async(Dispatchers.Default) {
                    listOf(Asset(1, 1, "Tool A", "Equipment", "In Use", 1, null))
                }
                val resultJob = jobDef.await()
                val resultAssets = assetsDef.await()
                assertEquals(1, resultJob.job_id)
                assertEquals(1, resultAssets.size)
            }
        }

        val opsPerSec = (iterations.toDouble() / elapsed) * 1000
        println("PERFORMANCE BENCHMARK: Executed $iterations parallel coroutine simulations in $elapsed ms (${opsPerSec.toInt()} ops/sec)")
        assertTrue("Parallel coroutines throughput should be fast", elapsed < 1000)
    }

    @Test
    fun testMemoryAndListTransformationBenchmark() {
        val listSize = 20_000
        val assets = List(listSize) { i ->
            Asset(i, 1, "Asset $i", if (i % 2 == 0) "Dispatch" else "Equipment", "In Use", 1, null)
        }

        val elapsed = measureTimeMillis {
            val transformed = assets.asSequence()
                .filter { it.category == "Dispatch" }
                .map { it.name.uppercase() }
                .toList()

            assertEquals(10_000, transformed.size)
        }

        println("PERFORMANCE BENCHMARK: Sequence transformation of $listSize items completed in $elapsed ms")
        assertTrue("Sequence transformation should finish in under 100ms", elapsed < 100)
    }
}
