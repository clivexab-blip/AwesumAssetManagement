<?php
require_once 'db_config.php';

// Retrieve orders from website, reading product images from `products` table and quote images from `quotes` table
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

$getQuoteImg = function($q_row) {
    if (!$q_row) return null;
    if (!empty($q_row['design_image'])) return $q_row['design_image'];
    if (!empty($q_row['draft_image'])) return $q_row['draft_image'];
    if (!empty($q_row['client_idea_image'])) return $q_row['client_idea_image'];
    return null;
};

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

                    // 1. Query `products` table by ID or Name
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
                        $p_row2 = $p_stmt2->get_result()->fetch_assoc();
                    }

                    // 2. Query `quotes` table for Custom Quote image
                    $quote_img_raw = null;
                    if (!empty($item['design_image'])) $quote_img_raw = $item['design_image'];
                    elseif (!empty($item['draft_image'])) $quote_img_raw = $item['draft_image'];
                    elseif (!empty($item['client_idea_image'])) $quote_img_raw = $item['client_idea_image'];

                    $quote_id = !empty($item['quote_id']) ? intval($item['quote_id']) : 0;
                    if ($quote_id == 0 && !empty($item['id'])) {
                        $q_chk_id = intval($item['id']);
                        $q_stmt_chk = $conn->prepare("SELECT design_image, draft_image, client_idea_image FROM quotes WHERE id = ? LIMIT 1");
                        $q_stmt_chk->bind_param('i', $q_chk_id);
                        $q_stmt_chk->execute();
                        $q_row_chk = $q_stmt_chk->get_result()->fetch_assoc();
                        if ($q_row_chk) {
                            $quote_img_raw = $getQuoteImg($q_row_chk);
                        }
                    }

                    $quote_ref = !empty($item['quote_ref']) ? trim($item['quote_ref']) : (!empty($item['reference_code']) ? trim($item['reference_code']) : '');

                    if (!$quote_img_raw && $quote_id > 0) {
                        $q_stmt = $conn->prepare("SELECT design_image, draft_image, client_idea_image FROM quotes WHERE id = ? LIMIT 1");
                        $q_stmt->bind_param('i', $quote_id);
                        $q_stmt->execute();
                        $q_row = $q_stmt->get_result()->fetch_assoc();
                        $quote_img_raw = $getQuoteImg($q_row);
                    }

                    if (!$quote_img_raw && !empty($quote_ref)) {
                        $q_stmt = $conn->prepare("SELECT design_image, draft_image, client_idea_image FROM quotes WHERE reference_code = ? LIMIT 1");
                        $q_stmt->bind_param('s', $quote_ref);
                        $q_stmt->execute();
                        $q_row = $q_stmt->get_result()->fetch_assoc();
                        $quote_img_raw = $getQuoteImg($q_row);
                    }

                    // Query latest quote for user if item indicates a Quote or no product row found
                    $is_quote_item = (stripos($prod_name, 'quote') !== false) || (stripos($prod_name, 'custom') !== false);
                    if (!$quote_img_raw && ($is_quote_item || !$p_row) && !empty($row['user_id'])) {
                        $uid = intval($row['user_id']);
                        $q_stmt2 = $conn->prepare("SELECT design_image, draft_image, client_idea_image FROM quotes WHERE user_id = ? ORDER BY id DESC LIMIT 1");
                        $q_stmt2->bind_param('i', $uid);
                        $q_stmt2->execute();
                        $q_row2 = $q_stmt2->get_result()->fetch_assoc();
                        $quote_img_raw = $getQuoteImg($q_row2);
                    }

                    // Item image priority: Product Image -> Quote Image -> Item JSON Image
                    $itemImgUrl = null;
                    if ($p_row && !empty($p_row['image'])) {
                        $itemImgUrl = formatImageUrl($p_row['image'], $baseUrl);
                    } elseif (!empty($quote_img_raw)) {
                        $itemImgUrl = formatImageUrl($quote_img_raw, $baseUrl);
                    } elseif (!empty($item['image'])) {
                        $itemImgUrl = formatImageUrl($item['image'], $baseUrl);
                    }

                    if ($first_image === null && !empty($itemImgUrl)) {
                        $first_image = $itemImgUrl;
                    }

                    if ($p_row) {
                        $items_details[] = [
                            'product_id'  => intval($p_row['id']),
                            'name'        => $p_row['name'],
                            'category'    => $p_row['category'],
                            'description' => $p_row['description'],
                            'price'       => floatval($p_row['price']),
                            'image'       => $itemImgUrl,
                            'quantity'    => $qty
                        ];
                    } else {
                        $items_details[] = [
                            'product_id'  => $prod_id > 0 ? $prod_id : null,
                            'name'        => !empty($prod_name) ? $prod_name : 'Custom Quote',
                            'category'    => 'Custom Quote',
                            'description' => null,
                            'price'       => isset($item['price']) ? floatval($item['price']) : 0.0,
                            'image'       => $itemImgUrl,
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
