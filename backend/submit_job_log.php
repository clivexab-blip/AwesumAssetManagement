<?php
require_once 'db_config.php';

$job_id   = isset($_POST['job_id']) ? intval($_POST['job_id']) : 0;
$draft    = isset($_POST['draft']) ? intval($_POST['draft']) : 0; // 1 = Save Draft, 0 = Submit
$end_time = isset($_POST['end_time']) ? trim($_POST['end_time']) : null;

if ($job_id <= 0) respond(false, null, 'job_id is required');

if ($draft) {
    if (!empty($end_time)) {
        $stmt = $conn->prepare("UPDATE job SET end_time = ? WHERE job_id = ?");
        $stmt->bind_param('si', $end_time, $job_id);
        $stmt->execute();
    }
    respond(true, null, 'Draft saved');
}

if (!empty($end_time)) {
    $stmt = $conn->prepare("UPDATE job SET status = 'Completed', end_time = ? WHERE job_id = ?");
    $stmt->bind_param('si', $end_time, $job_id);
} else {
    $stmt = $conn->prepare("UPDATE job SET status = 'Completed', end_time = NOW() WHERE job_id = ?");
    $stmt->bind_param('i', $job_id);
}

if ($stmt->execute()) {
    respond(true, null, 'Job log submitted');
} else {
    respond(false, null, 'Failed to submit job log: ' . $stmt->error);
}
