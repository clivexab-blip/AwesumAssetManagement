package com.awesum.assetmanagement.ui.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.awesum.assetmanagement.Constants
import com.awesum.assetmanagement.MainActivity
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.SessionManager
import com.awesum.assetmanagement.api.ApiClient
import com.awesum.assetmanagement.model.Order
import com.awesum.assetmanagement.ui.auth.LoginActivity
import com.awesum.assetmanagement.ui.settings.SettingsActivity
import com.awesum.assetmanagement.ui.summary.JobSummaryActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class OrdersActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvOrderCount: TextView
    private lateinit var btnSettings: View
    private lateinit var btnLogout: View

    private val ordersAdapter = OrdersAdapter(
        onOrderClick = { order -> openJobForOrder(order) },
        onSummaryClick = { order -> openJobSummaryForOrder(order) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_orders)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty = findViewById(R.id.tvEmpty)
        progressBar = findViewById(R.id.progressBar)
        tvOrderCount = findViewById(R.id.tvOrderCount)
        btnSettings = findViewById(R.id.btnSettings)
        btnLogout = findViewById(R.id.btnLogout)

        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = ordersAdapter

        swipeRefresh.setOnRefreshListener { loadOrders() }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnLogout.setOnClickListener { logout() }

        loadOrders()
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    private fun loadOrders() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.getOrders()
                val orders = res.body()?.data ?: emptyList()
                if (res.isSuccessful) {
                    ordersAdapter.submitList(orders)
                    if (orders.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvOrderCount.text = "0 website orders found"
                    } else {
                        tvEmpty.visibility = View.GONE
                        tvOrderCount.text = "${orders.size} website orders available"
                    }
                } else {
                    toast(res.body()?.message ?: "Failed to load orders")
                }
            } catch (e: Exception) {
                toast("Could not load orders: ${e.message}")
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun openJobForOrder(order: Order) {
        progressBar.visibility = View.VISIBLE
        val userId = SessionManager.getUserId(this)

        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.getOrCreateJobForOrder(order.order_id, if (userId > 0) userId else 1)
                val job = res.body()?.data
                if (res.isSuccessful && job != null) {
                    Constants.CURRENT_JOB_ID = job.job_id
                    val intent = Intent(this@OrdersActivity, MainActivity::class.java).apply {
                        putExtra(Constants.EXTRA_JOB_ID, job.job_id)
                    }
                    startActivity(intent)
                } else {
                    toast(res.body()?.message ?: "Could not open job for order")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun openJobSummaryForOrder(order: Order) {
        progressBar.visibility = View.VISIBLE
        val userId = SessionManager.getUserId(this)

        lifecycleScope.launch {
            try {
                val res = ApiClient.instance.getOrCreateJobForOrder(order.order_id, if (userId > 0) userId else 1)
                val job = res.body()?.data
                if (res.isSuccessful && job != null) {
                    Constants.CURRENT_JOB_ID = job.job_id
                    val intent = Intent(this@OrdersActivity, JobSummaryActivity::class.java).apply {
                        putExtra(Constants.EXTRA_JOB_ID, job.job_id)
                    }
                    startActivity(intent)
                } else {
                    toast(res.body()?.message ?: "Could not open summary for order")
                }
            } catch (e: Exception) {
                toast("Error: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        SessionManager.clear(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
