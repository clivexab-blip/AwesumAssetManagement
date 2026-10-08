package com.awesum.assetmanagement.ui.add

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.awesum.assetmanagement.Constants
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.api.ApiClient
import kotlinx.coroutines.launch

class AddAssetActivity : AppCompatActivity() {

    private val jobId: Int
        get() = intent.getIntExtra(Constants.EXTRA_JOB_ID, Constants.CURRENT_JOB_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_asset)

        val etAssetName = findViewById<EditText>(R.id.etAssetName)
        val rgCategory = findViewById<RadioGroup>(R.id.rgCategory)
        val rgStatus = findViewById<RadioGroup>(R.id.rgStatus)
        val etQuantity = findViewById<EditText>(R.id.etQuantity)
        val etNotes = findViewById<EditText>(R.id.etNotes)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnSave = findViewById<TextView>(R.id.btnSaveAsset)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val name = etAssetName.text.toString().trim()
            if (name.isEmpty()) {
                etAssetName.error = "Asset name is required"
                return@setOnClickListener
            }

            val category = when (rgCategory.checkedRadioButtonId) {
                R.id.rbEquipment -> "Equipment"
                R.id.rbVehicle -> "Vehicle"
                else -> "Dispatch"
            }
            val status = if (rgStatus.checkedRadioButtonId == R.id.rbOutForService) "Out for Service" else "In Use"
            val quantity = etQuantity.text.toString().toIntOrNull() ?: 1
            val notes = etNotes.text.toString().trim()

            lifecycleScope.launch {
                try {
                    val res = ApiClient.instance.addAsset(jobId, name, category, status, quantity, notes)
                    if (res.isSuccessful && (res.body()?.success == true)) {
                        Toast.makeText(this@AddAssetActivity, "Asset saved", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddAssetActivity, res.body()?.message ?: "Failed to save asset", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AddAssetActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
