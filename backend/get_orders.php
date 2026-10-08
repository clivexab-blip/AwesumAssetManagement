<?php
require_once 'db_config.php';

// Retrieve all orders from website, including linked job info if present
$sql = "SELECT
            o.id AS order_id,
            o.reference_code,
            o.user_id,
            o.items,
            o.total,
            o.address,
            o.status AS order_status,
            o.created_at,
            j.job_id,
            j.job_code,
            j.status AS job_status
        FROM orders o
        LEFT JOIN job j ON j.order_id = o.id
        ORDER BY o.id DESC";

$result = $conn->query($sql);

$orders = [];
if ($result) {
    while ($row = $result->fetch_assoc()) {
        $orders[] = $row;
    }
}

respond(true, $orders);
