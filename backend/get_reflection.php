<?php
require_once 'db_config.php';

$job_id = isset($_GET['job_id']) ? intval($_GET['job_id']) : 0;
if ($job_id <= 0) respond(false, null, 'job_id is required');

$stmt = $conn->prepare("SELECT reflection_id, job_id, notes, team_must_return, submitted, created_at FROM reflection WHERE job_id = ? LIMIT 1");
$stmt->bind_param('i', $job_id);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    respond(true, $row);
} else {
    respond(true, null, 'No reflection found');
}
