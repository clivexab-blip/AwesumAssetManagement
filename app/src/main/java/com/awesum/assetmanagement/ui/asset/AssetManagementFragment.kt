package com.awesum.assetmanagement.ui.asset

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.awesum.assetmanagement.Constants
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.api.ApiClient
import com.awesum.assetmanagement.model.Asset
import com.awesum.assetmanagement.model.Job
import com.awesum.assetmanagement.model.SitePhoto
import com.awesum.assetmanagement.model.TeamMember
import com.awesum.assetmanagement.ui.add.AddAssetActivity
import com.awesum.assetmanagement.ui.add.AddMemberActivity
import com.awesum.assetmanagement.ui.add.UploadPhotoActivity
import com.awesum.assetmanagement.ui.photo.PhotoAdapter
import com.awesum.assetmanagement.ui.team.TeamAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AssetManagementFragment : Fragment(R.layout.fragment_asset_management) {

    private val jobId: Int
        get() = arguments?.getInt(Constants.EXTRA_JOB_ID) ?: Constants.CURRENT_JOB_ID

    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var tvJobCode: TextView
    private lateinit var tvJobStatus: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var cardEndDate: View
    private lateinit var tvAssetCount: TextView
    private lateinit var btnAddAsset: TextView
    private lateinit var btnAddMember: TextView
    private lateinit var btnUploadPhoto: TextView
    private lateinit var btnSaveDraft: TextView
    private lateinit var btnSubmitJobLog: TextView

    private lateinit var rvAssets: androidx.recyclerview.widget.RecyclerView
    private lateinit var rvTeam: androidx.recyclerview.widget.RecyclerView
    private lateinit var rvPhotos: androidx.recyclerview.widget.RecyclerView

    private val assetAdapter = AssetAdapter()
    private val teamAdapter = TeamAdapter()
    private val photoAdapter = PhotoAdapter()

    private var allAssets: List<Asset> = emptyList()
    private var activeFilter: String = "All"
    private var selectedEndTime: String? = null

    private lateinit var chipAll: TextView
    private lateinit var chipDispatch: TextView
    private lateinit var chipEquipment: TextView
    private lateinit var chipVehicle: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        tvJobCode = view.findViewById(R.id.tvJobCode)
        tvJobStatus = view.findViewById(R.id.tvJobStatus)
        tvAddress = view.findViewById(R.id.tvAddress)
        tvStart = view.findViewById(R.id.tvStart)
        tvEnd = view.findViewById(R.id.tvEnd)
        cardEndDate = view.findViewById(R.id.cardEndDate)
        tvAssetCount = view.findViewById(R.id.tvAssetCount)
        btnAddAsset = view.findViewById(R.id.btnAddAsset)
        btnAddMember = view.findViewById(R.id.btnAddMember)
        btnUploadPhoto = view.findViewById(R.id.btnUploadPhoto)
        btnSaveDraft = view.findViewById(R.id.btnSaveDraft)
        btnSubmitJobLog = view.findViewById(R.id.btnSubmitJobLog)

        rvAssets = view.findViewById(R.id.rvAssets)
        rvTeam = view.findViewById(R.id.rvTeam)
        rvPhotos = view.findViewById(R.id.rvPhotos)

        chipAll = view.findViewById(R.id.chipAll)
        chipDispatch = view.findViewById(R.id.chipDispatch)
        chipEquipment = view.findViewById(R.id.chipEquipment)
        chipVehicle = view.findViewById(R.id.chipVehicle)

        rvAssets.layoutManager = LinearLayoutManager(requireContext())
        rvAssets.adapter = assetAdapter

        rvTeam.layoutManager = LinearLayoutManager(requireContext())
        rvTeam.adapter = teamAdapter

        rvPhotos.layoutManager = GridLayoutManager(requireContext(), 3)
        rvPhotos.adapter = photoAdapter

        cardEndDate.setOnClickListener { pickEndDateAndTime() }
        tvEnd.setOnClickListener { pickEndDateAndTime() }

        chipAll.setOnClickListener { setFilter("All") }
        chipDispatch.setOnClickListener { setFilter("Dispatch") }
        chipEquipment.setOnClickListener { setFilter("Equipment") }
        chipVehicle.setOnClickListener { setFilter("Vehicle") }

        btnAddAsset.setOnClickListener {
            val intent = Intent(requireContext(), AddAssetActivity::class.java).apply {
                putExtra(Constants.EXTRA_JOB_ID, jobId)
            }
            startActivity(intent)
        }
        btnAddMember.setOnClickListener {
            val intent = Intent(requireContext(), AddMemberActivity::class.java).apply {
                putExtra(Constants.EXTRA_JOB_ID, jobId)
            }
            startActivity(intent)
        }
        btnUploadPhoto.setOnClickListener {
            val intent = Intent(requireContext(), UploadPhotoActivity::class.java).apply {
                putExtra(Constants.EXTRA_JOB_ID, jobId)
            }
            startActivity(intent)
        }
        btnSaveDraft.setOnClickListener { submitJobLog(draft = true) }
        btnSubmitJobLog.setOnClickListener { submitJobLog(draft = false) }

        swipeRefresh.setOnRefreshListener { loadAll() }

        loadAll()
    }

    override fun onResume() {
        super.onResume()
        // Refresh whenever we come back from Add Asset / Add Member / Upload Photo
        loadAll()
    }

    private fun pickEndDateAndTime() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        val formatted = String.format(
                            Locale.US,
                            "%04d-%02d-%02d %02d:%02d:00",
                            year, month + 1, dayOfMonth, hourOfDay, minute
                        )
                        selectedEndTime = formatted
                        tvEnd.text = formatDate(formatted)
                        toast("End date set to $formatted")
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setFilter(filter: String) {
        activeFilter = filter
        val chips = listOf(chipAll to "All", chipDispatch to "Dispatch", chipEquipment to "Equipment", chipVehicle to "Vehicle")
        for ((chip, name) in chips) {
            if (name == filter) {
                chip.setBackgroundResource(R.drawable.bg_tab_selected)
                chip.setTextColor("#0B0F19".toColorInt())
            } else {
                chip.setBackgroundResource(R.drawable.bg_tab_unselected)
                chip.setTextColor("#8892A6".toColorInt())
            }
        }
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (activeFilter == "All") allAssets else allAssets.filter { it.category == activeFilter }
        assetAdapter.submitList(filtered)
    }

    private fun loadAll() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val jobDef = async { loadJob() }
            val assetsDef = async { loadAssets() }
            val teamDef = async { loadTeam() }
            val photosDef = async { loadPhotos() }
            awaitAll(jobDef, assetsDef, teamDef, photosDef)
            swipeRefresh.isRefreshing = false
        }
    }

    private suspend fun loadJob() {
        try {
            val res = ApiClient.instance.getJob(jobId)
            val job: Job? = res.body()?.data
            if (res.isSuccessful && (job != null)) {
                tvJobCode.text = job.job_code
                tvJobStatus.text = job.status
                tvAddress.text = job.installation_address
                tvStart.text = formatDate(job.start_time)
                if (selectedEndTime == null) {
                    tvEnd.text = job.end_time?.let { formatDate(it) } ?: "— (Tap to set)"
                }
            }
        } catch (e: Exception) {
            toast("Could not load job: ${e.message}")
        }
    }

    private suspend fun loadAssets() {
        try {
            val res = ApiClient.instance.getAssets(jobId)
            allAssets = res.body()?.data ?: emptyList()
            applyFilter()
            tvAssetCount.text = getString(R.string.asset_count_format, allAssets.size)
        } catch (e: Exception) {
            toast("Could not load assets: ${e.message}")
        }
    }

    private suspend fun loadTeam() {
        try {
            val res = ApiClient.instance.getTeam(jobId)
            val members: List<TeamMember> = res.body()?.data ?: emptyList()
            teamAdapter.submitList(members)
        } catch (e: Exception) {
            toast("Could not load team: ${e.message}")
        }
    }

    private suspend fun loadPhotos() {
        try {
            val res = ApiClient.instance.getPhotos(jobId)
            val photos: List<SitePhoto> = res.body()?.data ?: emptyList()
            photoAdapter.submitList(photos)
        } catch (e: Exception) {
            toast("Could not load photos: ${e.message}")
        }
    }

    private fun submitJobLog(draft: Boolean) {
        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.submitJobLog(jobId, if (draft) 1 else 0, selectedEndTime)
                if (res.isSuccessful && res.body()?.success == true) {
                    toast(if (draft) "Draft saved" else "Job log submitted")
                    if (!draft) loadJob()
                } else {
                    toast(res.body()?.message ?: "Failed to save")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            }
        }
    }

    private fun formatDate(raw: String): String {
        return try {
            val parts = raw.split(" ")
            val datePart = parts[0]
            val timePart = parts.getOrNull(1)?.substring(0, 5) ?: ""
            "$datePart · $timePart"
        } catch (e: Exception) {
            raw
        }
    }

    private fun toast(msg: String) {
        if (isAdded) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
