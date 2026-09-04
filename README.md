# AWESUM Asset Management

A professional Android application designed to manage installation jobs, track assets (tools, vehicles, and equipment), and document completed work through site photos. The app features a robust synchronization system between Firebase Authentication and a custom PHP/MySQL backend.

## 🚀 Key Features

*   **Secure Authentication**: Dual-layer auth using Firebase (Email/Password and Google Single Sign-On).
*   **MySQL Sync**: Automatic user synchronization to a custom MySQL database for long-term data ownership.
*   **Job Tracking**: View active installation jobs, addresses, and schedules.
*   **Asset Management**: Log and filter tools, equipment, and vehicles used on-site.
*   **Photo Documentation**: Capture and upload site photos with captions to a central server.
*   **Post-Job Reflection**: Draft and submit post-installation reviews and return-checks.

## 🛠️ Technology Stack

*   **Mobile**: Android (Kotlin, Jetpack ViewPager2, Retrofit, Glide, Material 3).
*   **Auth**: Firebase Authentication (Google Identity).
*   **Backend**: PHP 8.x (REST API).
*   **Database**: MySQL / phpMyAdmin.
*   **Hosting**: Local (XAMPP/WAMP) or Production (Afrihost/cPanel).

## 📋 Prerequisites

*   **Android Studio** (Hedgehog or newer recommended).
*   **Java 17+** (Required for Gradle 9.7.1).
*   **XAMPP / WAMP** for local database testing.
*   **Firebase Project** with Android app registered (using package `com.awesum.assetmanagement`).

## ⚙️ Setup Instructions

### 1. Backend (PHP & MySQL)
1.  Copy the `backend/` folder contents to your server's web root (e.g., `C:/xampp/htdocs/awesum_api/`).
2.  Open **phpMyAdmin**, create a database named `awesum_db`.
3.  Import the `backend/schema.sql` file into your database.
4.  Edit `db_config.php` with your database credentials.

### 2. Firebase Configuration
1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Enable **Email/Password** and **Google** sign-in providers.
3.  Add your **SHA-1 fingerprint** (run `signingReport` in Android Studio) to Project Settings.
4.  Download `google-services.json` and place it in the `app/` directory of this project.

### 3. Android App
1.  Open [ApiClient.kt](app/src/main/java/com/awesum/assetmanagement/api/ApiClient.kt).
2.  Update `BASE_URL` to point to your local IP or live domain.
3.  Build and run the project!

## 🌍 Production Deployment (Afrihost)

To move from local testing to a live server:
1.  Upload the `backend/` files to your Afrihost `public_html` directory.
2.  Use cPanel to create a production MySQL database and user.
3.  Update the app's `BASE_URL` to use `https://yourdomain.co.za/...`.
4.  Add your **Release SHA-1** fingerprint to Firebase to keep Google SSO working.

## 📂 Project Structure

```text
├── app/                  # Android mobile application source
│   ├── src/main/java/    # Kotlin source code (MVVM-lite)
│   ├── src/main/res/     # Layouts, themes, and resources
│   └── google-services.json # Firebase config (User must provide)
├── backend/              # PHP API and SQL scripts
│   ├── schema.sql        # Database structure
│   ├── db_config.php     # Connection settings
│   └── *.php             # API endpoints
└── build.gradle          # Project-level build configuration
```

## 📄 License
This project is for professional asset management and field service tracking.
