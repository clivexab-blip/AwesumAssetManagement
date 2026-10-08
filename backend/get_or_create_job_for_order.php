<?php
require_once 'db_config.php';

$order_id = isset($_POST['order_id']) ? intval($_POST['order_id']) : 0;
$user_id  = isset($_POST['user_id']) ? intval($_POST['user_id']) : 1;

if ($order_id <= 0) respond(false, null, 'order_id is required');

// 1. Check if a job already exists for this order_id
$stmt = $conn->prepare("SELECT job_id, job_code, user_id, status, installation_address, start_time, end_time FROM job WHERE order_id = ? LIMIT 1");
$stmt->bind_param('i', $order_id);
$stmt->execute();
$res = $stmt->get_result();

if ($row = $res->fetch_assoc()) {
    respond(true, $row, 'Existing job retrieved');
}

// 2. Fetch order details to populate new job
$stmt_order = $conn->prepare("SELECT reference_code, address FROM orders WHERE id = ? LIMIT 1");
$stmt_order->bind_param('i', $order_id);
$stmt_order->execute();
$order_res = $stmt_order->get_result()->fetch_assoc();

if (!$order_res) {
    respond(false, null, 'Order not found');
}

$ref_code = $order_res['reference_code'];
$job_code = 'JOB-' . ($ref_code ? $ref_code : rand(1000, 9999));
$address  = $order_res['address'] ? $order_res['address'] : 'No installation address specified';
$now      = date('Y-m-d H:i:s');

// Ensure valid user_id
if ($user_id <= 0) $user_id = 1;
$u_check = $conn->prepare("SELECT user_id FROM user WHERE user_id = ? LIMIT 1");
$u_check->bind_param('i', $user_id);
$u_check->execute();
if (!$u_check->get_result()->fetch_assoc()) {
    $user_id = 1;
}

$stmt_insert = $conn->prepare("INSERT INTO job (job_code, user_id, order_id, status, installation_address, start_time) VALUES (?, ?, ?, 'Active', ?, ?)");
$stmt_insert->bind_param('siiss', $job_code, $user_id, $order_id, $address, $now);

if ($stmt_insert->execute()) {
    $new_job_id = $stmt_insert->insert_id;

    // Fetch newly created job
    $stmt_new = $conn->prepare("SELECT job_id, job_code, user_id, status, installation_address, start_time, end_time FROM job WHERE job_id = ? LIMIT 1");
    $stmt_new->bind_param('i', $new_job_id);
    $stmt_new->execute();
    $new_job = $stmt_new->get_result()->fetch_assoc();

    respond(true, $new_job, 'New job created for order');
} else {
    respond(false, null, 'Failed to create job: ' . $conn->error);
}
