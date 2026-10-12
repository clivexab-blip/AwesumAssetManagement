package com.awesum.assetmanagement

import org.junit.Assert.assertEquals
import org.junit.Test

class ConstantsTest {

    @Test
    fun testConstantsDefaultsAndMutability() {
        assertEquals("extra_job_id", Constants.EXTRA_JOB_ID)

        val initialJobId = Constants.CURRENT_JOB_ID
        Constants.CURRENT_JOB_ID = 42
        assertEquals(42, Constants.CURRENT_JOB_ID)

        // Reset to initial
        Constants.CURRENT_JOB_ID = initialJobId
    }
}
