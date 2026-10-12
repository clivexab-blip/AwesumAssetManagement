<?php
require_once 'db_config.php';

// Retrieve orders from website, including customer info, item details, and linked job info
$sql = "SELECT
            o.id AS order_id,
            o.reference_code,
            o.user_id,
            u.name AS customer_name,
            u.email AS customer_email,
            o.items,
            o.total,
            o.address,
            o.status AS order_status,
            o.created_at,
            j.job_id,
            j.job_code,
            j.status AS job_status
        FROM orders o
        LEFT JOIN users u ON u.id = o.user_id
        LEFT JOIN job j ON j.order_id = o.id
        ORDER BY o.id DESC
        LIMIT 500";

$result = $conn->query($sql);

$orders = [];
if ($result) {
    while ($row = $result->fetch_assoc()) {
        $first_image = null;
        if (!empty($row['items'])) {
            $items = json_decode($row['items'], true);
            if (is_array($items)) {
                foreach ($items as $item) {
                    if (!empty($item['image'])) {
                        $first_image = $item['image'];
                        break;
                    } elseif (!empty($item['product_id'])) {
                        $p_stmt = $conn->prepare("SELECT image FROM products WHERE id = ? LIMIT 1");
                        $pid = intval($item['product_id']);
                        $p_stmt->bind_param('i', $pid);
                        $p_stmt->execute();
                        $p_res = $p_stmt->get_result()->fetch_assoc();
                        if ($p_res && !empty($p_res['image'])) {
                            $first_image = $p_res['image'];
                            break;
                        }
                    }
                }
            }
        }
        $row['order_image'] = $first_image;
        $orders[] = $row;
    }
}

respond(true, $orders);
