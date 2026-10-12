package com.awesum.assetmanagement.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetTest {

    @Test
    fun testAssetCreationAndCategoryFiltering() {
        val assets = listOf(
            Asset(asset_id = 1, job_id = 1, name = "Paste Brush Set", category = "Dispatch", status = "In Use", quantity = 1, notes = null),
            Asset(asset_id = 2, job_id = 1, name = "Scaffold Section A", category = "Equipment", status = "In Use", quantity = 1, notes = null),
            Asset(asset_id = 3, job_id = 1, name = "Van — GP 45 XY", category = "Vehicle", status = "Out for Service", quantity = 1, notes = null),
            Asset(asset_id = 4, job_id = 1, name = "Lift Platform #3", category = "Equipment", status = "In Use", quantity = 1, notes = null)
        )

        assertEquals(4, assets.size)

        val dispatchAssets = assets.filter { it.category == "Dispatch" }
        assertEquals(1, dispatchAssets.size)
        assertEquals("Paste Brush Set", dispatchAssets[0].name)

        val equipmentAssets = assets.filter { it.category == "Equipment" }
        assertEquals(2, equipmentAssets.size)

        val vehicleAssets = assets.filter { it.category == "Vehicle" }
        assertEquals(1, vehicleAssets.size)
        assertEquals("Out for Service", vehicleAssets[0].status)
    }

    @Test
    fun testAssetStatusFiltering() {
        val assets = listOf(
            Asset(asset_id = 1, job_id = 1, name = "Tool 1", category = "Equipment", status = "In Use", quantity = 1, notes = null),
            Asset(asset_id = 2, job_id = 1, name = "Vehicle 1", category = "Vehicle", status = "Out for Service", quantity = 1, notes = null)
        )

        val inUseList = assets.filter { it.status == "In Use" }
        assertEquals(1, inUseList.size)

        val outForServiceList = assets.filter { it.status == "Out for Service" }
        assertEquals(1, outForServiceList.size)
    }
}
