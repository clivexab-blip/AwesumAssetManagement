<?php
// =========================================================
// AWESUM Asset Management - Database Connection
// Edit these 4 values to match your phpMyAdmin / MySQL setup
// (XAMPP/WAMP defaults shown below)
// =========================================================
define('DB_HOST', '127.0.0.1');
define('DB_USER', 'awesumj5e6x4_web');
define('DB_PASS', '(m$Xa6l+zZaoO@.c');
define('DB_NAME', 'awesumj5e6x4_awesum');

$conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

if ($conn->connect_error) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'DB connection failed: ' . $conn->connect_error]);
    exit;
}
$conn->set_charset('utf8mb4');

// Common CORS + JSON headers for every endpoint
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
