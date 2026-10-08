package com.awesum.assetmanagement.ui.add

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.awesum.assetmanagement.Constants
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.api.ApiClient
import kotlinx.coroutines.launch

class AddMemberActivity : AppCompatActivity() {

    private val jobId: Int
        get() = intent.getIntExtra(Constants.EXTRA_JOB_ID, Constants.CURRENT_JOB_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_member)

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etRole = findViewById<EditText>(R.id.etRole)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val tvAvatarPreview = findViewById<TextView>(R.id.tvAvatarPreview)
        val tvNamePreview = findViewById<TextView>(R.id.tvNamePreview)
        val tvRolePreview = findViewById<TextView>(R.id.tvRolePreview)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnAdd = findViewById<TextView>(R.id.btnAddMember)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }

        val livePreview = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = etFullName.text.toString().trim()
                val role = etRole.text.toString().trim()
                tvNamePreview.text = name.ifEmpty { "Full Name" }
                tvRolePreview.text = role.ifEmpty { "Role" }
                tvAvatarPreview.text = name.split(" ")
                    .asSequence()
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "XX" }
            }
        }
        etFullName.addTextChangedListener(livePreview)
        etRole.addTextChangedListener(livePreview)

        btnAdd.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val role = etRole.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (fullName.isEmpty()) {
                etFullName.error = "Full name is required"
                return@setOnClickListener
            }
            if (role.isEmpty()) {
                etRole.error = "Role is required"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val res = ApiClient.instance.addMember(jobId, fullName, role, phone)
                    if (res.isSuccessful && (res.body()?.success == true)) {
                        Toast.makeText(this@AddMemberActivity, "Team member added", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AddMemberActivity, res.body()?.message ?: "Failed to add member", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AddMemberActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
