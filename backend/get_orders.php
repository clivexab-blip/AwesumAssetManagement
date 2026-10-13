<?php
require_once 'db_config.php';

// Retrieve orders from website, dynamically reading full product details from the `products` table
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

$scheme = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https://' : 'http://');
$host = $_SERVER['HTTP_HOST'];
$baseUrl = $scheme . $host . '/';

function formatImageUrl($rawImage, $baseUrl) {
    if (empty($rawImage)) return null;
    if (preg_match('~^https?://~i', $rawImage) || preg_match('~^data:image/~i', $rawImage)) {
        return $rawImage;
    }
    $clean = ltrim($rawImage, '/');
    if (strpos($clean, 'uploads/') === 0 || strpos($clean, 'images/') === 0) {
        return $baseUrl . $clean;
    }
    return $baseUrl . 'uploads/images/' . $clean;
}

$orders = [];
if ($result) {
    while ($row = $result->fetch_assoc()) {
        $first_image = null;
        $items_details = [];

        if (!empty($row['items'])) {
            $items = json_decode($row['items'], true);
            if (is_array($items)) {
                foreach ($items as $item) {
                    $qty = isset($item['quantity']) ? intval($item['quantity']) : (isset($item['qty']) ? intval($item['qty']) : 1);
                    $prod_id = 0;
                    if (!empty($item['product_id'])) { $prod_id = intval($item['product_id']); }
                    elseif (!empty($item['id'])) { $prod_id = intval($item['id']); }
                    elseif (!empty($item['item_id'])) { $prod_id = intval($item['item_id']); }

                    $prod_name = trim($item['name'] ?? $item['title'] ?? '');

                    $p_row = null;

                    // Query `products` table dynamically by ID or Name
                    if ($prod_id > 0) {
                        $p_stmt = $conn->prepare("SELECT id, name, category, description, price, image, active FROM products WHERE id = ? LIMIT 1");
                        $p_stmt->bind_param('i', $prod_id);
                        $p_stmt->execute();
                        $p_row = $p_stmt->get_result()->fetch_assoc();
                    }

                    if (!$p_row && !empty($prod_name)) {
                        $p_stmt2 = $conn->prepare("SELECT id, name, category, description, price, image, active FROM products WHERE LOWER(name) = LOWER(?) OR LOWER(name) LIKE LOWER(?) LIMIT 1");
                        $like_name = '%' . $prod_name . '%';
                        $p_stmt2->bind_param('ss', $prod_name, $like_name);
                        $p_stmt2->execute();
                        $p_row = $p_stmt2->get_result()->fetch_assoc();
                    }

                    if ($p_row) {
                        $imgUrl = formatImageUrl($p_row['image'], $baseUrl);
                        if ($first_image === null && !empty($imgUrl)) {
                            $first_image = $imgUrl;
                        }
                        $items_details[] = [
                            'product_id'  => intval($p_row['id']),
                            'name'        => $p_row['name'],
                            'category'    => $p_row['category'],
                            'description' => $p_row['description'],
                            'price'       => floatval($p_row['price']),
                            'image'       => $imgUrl,
                            'quantity'    => $qty
                        ];
                    } else {
                        // Fallback item details if product row not found in products table
                        $imgUrl = !empty($item['image']) ? formatImageUrl($item['image'], $baseUrl) : null;
                        if ($first_image === null && !empty($imgUrl)) {
                            $first_image = $imgUrl;
                        }
                        $items_details[] = [
                            'product_id'  => $prod_id > 0 ? $prod_id : null,
                            'name'        => !empty($prod_name) ? $prod_name : 'Custom Product',
                            'category'    => 'General',
                            'description' => null,
                            'price'       => isset($item['price']) ? floatval($item['price']) : 0.0,
                            'image'       => $imgUrl,
                            'quantity'    => $qty
                        ];
                    }
                }
            }
        }

        $row['order_image']   = $first_image;
        $row['items_details'] = $items_details;
        $orders[] = $row;
    }
}

respond(true, $orders);
