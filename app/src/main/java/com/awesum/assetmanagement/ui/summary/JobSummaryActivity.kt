package com.awesum.assetmanagement.ui.summary

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
import com.awesum.assetmanagement.ui.asset.AssetAdapter
import com.awesum.assetmanagement.ui.photo.PhotoAdapter
import com.awesum.assetmanagement.ui.team.TeamAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class JobSummaryActivity : AppCompatActivity() {

    private val jobId: Int
        get() = intent.getIntExtra(Constants.EXTRA_JOB_ID, Constants.CURRENT_JOB_ID)

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvJobCode: TextView
    private lateinit var tvJobStatus: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var btnToggleJobStatus: TextView

    private lateinit var tvAssetCount: TextView
    private lateinit var btnAddAsset: TextView
    private lateinit var btnAddMember: TextView
    private lateinit var btnUploadPhoto: TextView

    private lateinit var rvAssets: RecyclerView
    private lateinit var rvTeam: RecyclerView
    private lateinit var rvPhotos: RecyclerView

    private lateinit var tvSavedReflectionNotes: TextView
    private lateinit var tvReflectionBadge: TextView
    private lateinit var tvTeamReturnBadge: TextView
    private lateinit var etReflectionNotes: EditText
    private lateinit var btnReturnYes: TextView
    private lateinit var btnReturnNo: TextView
    private lateinit var btnSaveReflection: TextView
    private lateinit var btnDone: TextView
    private lateinit var btnBack: TextView

    private val assetAdapter = AssetAdapter()
    private val teamAdapter = TeamAdapter()
    private val photoAdapter = PhotoAdapter()

    private var currentJobStatus: String = "Active"
    private var teamMustReturn: Boolean? = null
    private var selectedEndTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_summary)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnBack = findViewById(R.id.btnBack)
        tvJobCode = findViewById(R.id.tvJobCode)
        tvJobStatus = findViewById(R.id.tvJobStatus)
        tvAddress = findViewById(R.id.tvAddress)
        tvStart = findViewById(R.id.tvStart)
        tvEnd = findViewById(R.id.tvEnd)
        btnToggleJobStatus = findViewById(R.id.btnToggleJobStatus)

        tvAssetCount = findViewById(R.id.tvAssetCount)
        btnAddAsset = findViewById(R.id.btnAddAsset)
        btnAddMember = findViewById(R.id.btnAddMember)
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto)

        rvAssets = findViewById(R.id.rvAssets)
        rvTeam = findViewById(R.id.rvTeam)
        rvPhotos = findViewById(R.id.rvPhotos)

        tvSavedReflectionNotes = findViewById(R.id.tvSavedReflectionNotes)
        tvReflectionBadge = findViewById(R.id.tvReflectionBadge)
        tvTeamReturnBadge = findViewById(R.id.tvTeamReturnBadge)
        etReflectionNotes = findViewById(R.id.etReflectionNotes)
        btnReturnYes = findViewById(R.id.btnReturnYes)
        btnReturnNo = findViewById(R.id.btnReturnNo)
        btnSaveReflection = findViewById(R.id.btnSaveReflection)
        btnDone = findViewById(R.id.btnDone)

        rvAssets.layoutManager = LinearLayoutManager(this)
        rvAssets.adapter = assetAdapter

        rvTeam.layoutManager = LinearLayoutManager(this)
        rvTeam.adapter = teamAdapter

        rvPhotos.layoutManager = GridLayoutManager(this, 3)
        rvPhotos.adapter = photoAdapter

        btnBack.setOnClickListener { finish() }
        btnDone.setOnClickListener { finish() }

        tvEnd.setOnClickListener { pickEndDateAndTime() }

        btnReturnYes.setOnClickListener { selectReturn(true) }
        btnReturnNo.setOnClickListener { selectReturn(false) }

        btnAddAsset.setOnClickListener {
            startActivity(Intent(this, AddAssetActivity::class.java).apply {
                putExtra(Constants.EXTRA_JOB_ID, jobId)
            })
        }
        btnAddMember.setOnClickListener {
            startActivity(Intent(this, AddMemberActivity::class.java).apply {
                putExtra(Constants.EXTRA_JOB_ID, jobId)
            })
        }
        btnUploadPhoto.setOnClickListener {
            startActivity(Intent(this, UploadPhotoActivity::class.java).apply {
                putExtra(Constants.EXTRA_JOB_ID, jobId)
            })
        }

        btnToggleJobStatus.setOnClickListener { toggleJobStatus() }
        btnSaveReflection.setOnClickListener { saveReflection() }

        swipeRefresh.setOnRefreshListener { loadAll() }

        loadAll()
    }

    override fun onResume() {
        super.onResume()
        loadAll()
    }

    private fun pickEndDateAndTime() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                TimePickerDialog(
                    this,
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

    private fun loadAll() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val jDef = async { loadJob() }
            val aDef = async { loadAssets() }
            val tDef = async { loadTeam() }
            val pDef = async { loadPhotos() }
            val rDef = async { loadReflection() }
            awaitAll(jDef, aDef, tDef, pDef, rDef)
            swipeRefresh.isRefreshing = false
        }
    }

    private suspend fun loadJob() {
        try {
            val res = ApiClient.instance.getJob(jobId)
            val job: Job? = res.body()?.data
            if (res.isSuccessful && job != null) {
                currentJobStatus = job.status
                tvJobCode.text = job.job_code
                tvJobStatus.text = job.status
                tvAddress.text = job.installation_address
                tvStart.text = formatDate(job.start_time)
                if (selectedEndTime == null) {
                    tvEnd.text = job.end_time?.let { formatDate(it) } ?: "— (Tap to set)"
                }

                if (job.status == "Completed") {
                    btnToggleJobStatus.text = "Job Status: Completed (Click to Re-Open)"
                    tvJobStatus.setTextColor("#3ECF7A".toColorInt())
                } else {
                    btnToggleJobStatus.text = "Mark Job as Completed"
                    tvJobStatus.setTextColor("#F2A93B".toColorInt())
                }
            }
        } catch (e: Exception) {
            toast("Could not load job details: ${e.message}")
        }
    }

    private suspend fun loadAssets() {
        try {
            val res = ApiClient.instance.getAssets(jobId)
            val assets: List<Asset> = res.body()?.data ?: emptyList()
            assetAdapter.submitList(assets)
            tvAssetCount.text = "${assets.size} assets"
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
            toast("Could not load site photos: ${e.message}")
        }
    }

    private suspend fun loadReflection() {
        try {
            val res = ApiClient.instance.getReflection(jobId)
            val reflection = res.body()?.data
            if (res.isSuccessful && reflection != null) {
                val notesText = reflection.notes?.ifEmpty { null } ?: "No reflection notes recorded yet."
                tvSavedReflectionNotes.text = notesText
                etReflectionNotes.setText(reflection.notes ?: "")

                val isSubmitted = reflection.submitted == 1
                tvReflectionBadge.text = if (isSubmitted) "Submitted" else "Draft"
                if (isSubmitted) {
                    tvReflectionBadge.setTextColor("#3ECF7A".toColorInt())
                } else {
                    tvReflectionBadge.setTextColor("#F2A93B".toColorInt())
                }

                val mustReturn = reflection.team_must_return == 1
                tvTeamReturnBadge.text = if (mustReturn) "Team Return Required: YES" else "Team Return Required: NO"
                selectReturn(mustReturn)
            } else {
                tvSavedReflectionNotes.text = "No reflection notes recorded yet."
                tvReflectionBadge.text = "None"
                tvTeamReturnBadge.text = "Team Return Required: NO"
            }
        } catch (e: Exception) {
            tvSavedReflectionNotes.text = "No reflection notes recorded yet."
        }
    }

    private fun selectReturn(mustReturn: Boolean) {
        teamMustReturn = mustReturn
        if (mustReturn) {
            btnReturnYes.setBackgroundResource(R.drawable.bg_btn_primary)
            btnReturnYes.setTextColor("#0B0F19".toColorInt())
            btnReturnNo.setBackgroundResource(R.drawable.bg_btn_outline)
            btnReturnNo.setTextColor("#FFFFFF".toColorInt())
        } else {
            btnReturnNo.setBackgroundResource(R.drawable.bg_btn_primary)
            btnReturnNo.setTextColor("#0B0F19".toColorInt())
            btnReturnYes.setBackgroundResource(R.drawable.bg_btn_outline)
            btnReturnYes.setTextColor("#FFFFFF".toColorInt())
        }
    }

    private fun toggleJobStatus() {
        val draft = if (currentJobStatus == "Completed") 1 else 0
        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.submitJobLog(jobId, draft, selectedEndTime)
                if (res.isSuccessful && res.body()?.success == true) {
                    toast("Job status updated")
                    loadJob()
                } else {
                    toast(res.body()?.message ?: "Failed to update job status")
                }
            } catch (e: Exception) {
                toast("Error updating status: ${e.message}")
            }
        }
    }

    private fun saveReflection() {
        val notes = etReflectionNotes.text.toString().trim()
        val returnFlag = if (teamMustReturn == true) 1 else 0

        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.saveReflection(jobId, notes, returnFlag, submitted = 1)
                if (res.isSuccessful && res.body()?.success == true) {
                    toast("Reflection saved and updated")
                    loadReflection()
                } else {
                    toast(res.body()?.message ?: "Failed to save reflection")
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
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
