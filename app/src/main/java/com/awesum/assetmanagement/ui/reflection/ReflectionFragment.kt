package com.awesum.assetmanagement.ui.reflection

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.awesum.assetmanagement.Constants
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.api.ApiClient
import kotlinx.coroutines.launch

class ReflectionFragment : Fragment(R.layout.fragment_reflection) {

    private val jobId: Int
        get() = arguments?.getInt(Constants.EXTRA_JOB_ID) ?: Constants.CURRENT_JOB_ID

    private var teamMustReturn: Boolean? = null

    private lateinit var etNotes: EditText
    private lateinit var btnYes: TextView
    private lateinit var btnNo: TextView
    private lateinit var btnSaveDraft: TextView
    private lateinit var btnSubmit: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etNotes = view.findViewById(R.id.etReflectionNotes)
        btnYes = view.findViewById(R.id.btnReturnYes)
        btnNo = view.findViewById(R.id.btnReturnNo)
        btnSaveDraft = view.findViewById(R.id.btnReflectionSaveDraft)
        btnSubmit = view.findViewById(R.id.btnReflectionSubmit)

        btnYes.setOnClickListener { selectReturn(true) }
        btnNo.setOnClickListener { selectReturn(false) }

        btnSaveDraft.setOnClickListener { save(submitted = false) }
        btnSubmit.setOnClickListener { save(submitted = true) }

        loadReflection()
    }

    private fun loadReflection() {
        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.getReflection(jobId)
                val reflection = res.body()?.data
                if (res.isSuccessful && reflection != null) {
                    reflection.notes?.let { etNotes.setText(it) }
                    selectReturn(reflection.team_must_return == 1)
                }
            } catch (e: Exception) {
                // Ignore load error for new reflections
            }
        }
    }

    private fun selectReturn(mustReturn: Boolean) {
        teamMustReturn = mustReturn
        if (mustReturn) {
            btnYes.setBackgroundResource(R.drawable.bg_btn_primary)
            btnYes.setTextColor("#0B0F19".toColorInt())
            btnYes.text = getString(R.string.btn_return_yes).uppercase()
            btnNo.setBackgroundResource(R.drawable.bg_btn_outline)
            btnNo.setTextColor("#FFFFFF".toColorInt())
            btnNo.text = getString(R.string.btn_return_no)
        } else {
            btnNo.setBackgroundResource(R.drawable.bg_btn_primary)
            btnNo.setTextColor("#0B0F19".toColorInt())
            btnNo.text = getString(R.string.btn_return_no).uppercase()
            btnYes.setBackgroundResource(R.drawable.bg_btn_outline)
            btnYes.setTextColor("#FFFFFF".toColorInt())
            btnYes.text = getString(R.string.btn_return_yes)
        }
    }

    private fun save(submitted: Boolean) {
        val notes = etNotes.text.toString().trim()
        val returnFlag = if (teamMustReturn == true) 1 else 0

        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.saveReflection(jobId, notes, returnFlag, if (submitted) 1 else 0)
                if (res.isSuccessful && (res.body()?.success == true)) {
                    toast(if (submitted) "Reflection submitted" else "Draft saved")
                } else {
                    toast(res.body()?.message ?: "Failed to save reflection")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    private fun toast(msg: String) {
        if (isAdded) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
