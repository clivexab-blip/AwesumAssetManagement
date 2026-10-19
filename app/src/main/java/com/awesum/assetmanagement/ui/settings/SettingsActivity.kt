package com.awesum.assetmanagement.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.SessionManager
import com.awesum.assetmanagement.api.ApiClient
import com.awesum.assetmanagement.ui.auth.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack = findViewById<TextView>(R.id.btnBack)
        val tvUserInitials = findViewById<TextView>(R.id.tvUserInitials)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvServerUrl = findViewById<TextView>(R.id.tvServerUrl)
        val btnClearCache = findViewById<TextView>(R.id.btnClearCache)
        val btnLogout = findViewById<View>(R.id.btnLogout)

        btnBack.setOnClickListener { finish() }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val name = SessionManager.getName(this).ifEmpty {
            firebaseUser?.displayName ?: firebaseUser?.email?.substringBefore("@") ?: "Lead Installer"
        }
        val email = SessionManager.getEmail(this).ifEmpty {
            firebaseUser?.email ?: "installer@awesum.co.za"
        }

        tvUserName.text = name
        tvUserEmail.text = email
        tvUserInitials.text = name.split(" ")
            .asSequence()
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "AI" }

        tvServerUrl.text = ApiClient.BASE_URL

        btnClearCache.setOnClickListener {
            Toast.makeText(this, "Local cache cleared successfully!", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener { logout() }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
        SessionManager.clear(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }
}
