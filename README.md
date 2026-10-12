# AWESUM Asset Management

A professional Android application designed to manage installation jobs, track assets (tools, vehicles, and equipment), and document completed work through site photos. The app features a robust synchronization system between website orders, Firebase Authentication, and a custom PHP/MySQL backend.

---

## 🚀 Key Features

*   **Website Orders & Jobs Connection**: Connects website orders directly with installation jobs.
*   **Secure Authentication**: Dual-layer auth using Firebase (Email/Password and Google Single Sign-On).
*   **MySQL Sync**: Automatic user synchronization to a custom MySQL database for long-term data ownership.
*   **Job Tracking & Summary**: View active installation jobs, addresses, schedules, and full editable job summaries.
*   **Asset Management**: Log and filter tools, equipment, and vehicles used on-site.
*   **Photo Documentation**: Capture and upload site photos with captions to a central server.
*   **Post-Job Reflection**: Draft and submit post-installation reviews and return-checks.

---

## 🛠️ Technology Stack

*   **Mobile**: Android (Kotlin, ViewPager2, Retrofit, Coroutines, Glide, Material 3).
*   **Auth**: Firebase Authentication (Google Identity).
*   **Backend**: PHP 8.x (REST API).
*   **Database**: MySQL / phpMyAdmin.
*   **Hosting**: Production (Afrihost/cPanel) or Local (XAMPP/WAMP).

---

## 🌍 Complete Guide: Deploying & Connecting to Afrihost

This section provides a step-by-step guide to hosting your database and PHP backend on **Afrihost cPanel** and connecting the Android app to it.

### Step 1: Create Database & User in Afrihost cPanel
1. Log in to your **Afrihost ClientZone** -> Go to **Hosting** -> Select your Domain -> Click **cPanel**.
2. In cPanel, search for **MySQL® Databases**:
   * **Create New Database**: e.g., `yourusername_awesum_db`.
   * **Create New User**: e.g., `yourusername_dbuser` and set a secure password.
   * **Add User to Database**: Assign `yourusername_dbuser` to `yourusername_awesum_db` and select **ALL PRIVILEGES**.

### Step 2: Import Database Schema & Seed Data
1. In cPanel, open **phpMyAdmin**.
2. Select your newly created database (`yourusername_awesum_db`) from the left menu.
3. Click the **Import** tab at the top.
4. Click **Choose File** and select `awesum_combined_full.sql` (or `backend/schema.sql`).
5. Scroll to the bottom and click **Import** (or **Go**).

### Step 3: Upload Backend Files to Afrihost
1. In cPanel, open **File Manager** and navigate to `public_html/`.
2. Create a new folder named `awesum_api` (Full path: `public_html/awesum_api/`).
3. Upload all PHP files from the `backend/` folder into `public_html/awesum_api/`:
   * `db_config.php`
   * `get_orders.php`
   * `get_or_create_job_for_order.php`
   * `get_job.php`
   * `get_assets.php`
   * `get_team.php`
   * `get_photos.php`
   * `get_reflection.php`
   * `save_reflection.php`
   * `add_asset.php`
   * `add_member.php`
   * `upload_photo.php`
   * `submit_job_log.php`
   * `sync_user.php`
4. Create an `uploads/` directory inside `public_html/awesum_api/uploads/` and grant write permissions (`0755` or `0777`).

### Step 4: Configure `db_config.php` on Afrihost
In cPanel File Manager, edit `public_html/awesum_api/db_config.php` with your database credentials:

```php
<?php
define('DB_HOST', 'localhost'); // On Afrihost, MySQL host remains 'localhost'
define('DB_USER', 'yourusername_dbuser'); // Your cPanel MySQL username
define('DB_PASS', 'YourStrongPassword123!'); // Your cPanel MySQL password
define('DB_NAME', 'yourusername_awesum_db'); // Your cPanel MySQL database name

$conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'DB connection failed: ' . $conn->connect_error]);
    exit;
}
$conn->set_charset('utf8mb4');

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Content-Type: application/json; charset=UTF-8');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

function respond($success, $data = null, $message = '') {
    echo json_encode(['success' => $success, 'message' => $message, 'data' => $data]);
    exit;
}
```

### Step 5: Update Android App `ApiClient.kt`
1. Open `app/src/main/java/com/awesum/assetmanagement/api/ApiClient.kt` in Android Studio.
2. Change `BASE_URL` to point to your live Afrihost domain:

```kotlin
package com.awesum.assetmanagement.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Live Afrihost Web Hosting Endpoint
    private const val BASE_URL = "https://yourdomain.co.za/awesum_api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```

### Step 6: Configure Firebase for Production
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Under **Authentication** -> **Settings** -> **Authorized Domains**, add your domain: `yourdomain.co.za`.
3. Under **Project Settings** -> **General** -> **Your Apps**, ensure your SHA-1 fingerprint is added so Google Sign-In operates over SSL.

---

## ⚙️ Local Development (XAMPP / Android Emulator)

For local testing on the Android Emulator:
1. Copy `backend/` files to `C:/xampp/htdocs/awesum_api/`.
2. In phpMyAdmin, import `awesum_combined_full.sql`.
3. In `ApiClient.kt`, set `BASE_URL = "http://10.0.2.2/awesum_api/"`.

---

## 📂 Project Structure

```text
├── app/                  # Android mobile application source
│   ├── src/main/java/    # Kotlin source code
│   ├── src/main/res/     # Layouts, themes, and drawables
│   └── google-services.json # Firebase configuration
├── backend/              # PHP REST API & SQL scripts
│   ├── db_config.php     # MySQL credentials
│   ├── get_orders.php    # Website orders endpoint
│   ├── get_or_create_job_for_order.php # Job provisioning
│   └── *.php             # Asset, photo, team & reflection endpoints
└── awesum_combined_full.sql # Combined database import script
```

## 📄 License
This application is designed for field installation tracking, asset management, and website order synchronization.
