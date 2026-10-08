package com.awesum.assetmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.viewpager2.widget.ViewPager2
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.ui.auth.LoginActivity
import com.awesum.assetmanagement.ui.order.OrdersActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Guard: if somehow reached without a signed-in Firebase user, bounce to Login.
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val jobId = intent.getIntExtra(Constants.EXTRA_JOB_ID, Constants.CURRENT_JOB_ID)
        Constants.CURRENT_JOB_ID = jobId

        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val btnOrders = findViewById<TextView>(R.id.btnOrders)
        val tabAssetMgmt = findViewById<TextView>(R.id.tabAssetMgmt)
        val tabReflection = findViewById<TextView>(R.id.tabReflection)
        val btnLogout = findViewById<TextView>(R.id.btnLogout)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.isUserInputEnabled = true

        fun highlight(position: Int) {
            if (position == 0) {
                tabAssetMgmt.setTextColor("#F2A93B".toColorInt())
                tabReflection.setTextColor("#8892A6".toColorInt())
            } else {
                tabReflection.setTextColor("#F2A93B".toColorInt())
                tabAssetMgmt.setTextColor("#8892A6".toColorInt())
            }
        }

        btnOrders.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        tabAssetMgmt.setOnClickListener { viewPager.currentItem = 0 }
        tabReflection.setOnClickListener { viewPager.currentItem = 1 }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                highlight(position)
            }
        })

        btnLogout.setOnClickListener { logout() }

        highlight(0)
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        SessionManager.clear(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
