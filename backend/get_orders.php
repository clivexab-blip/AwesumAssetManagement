<?php
require_once 'db_config.php';

// Retrieve website orders with completed/received payment
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
        WHERE LOWER(o.status) NOT LIKE '%pending%'
          AND LOWER(o.status) NOT LIKE '%awaiting%'
          AND LOWER(o.status) NOT LIKE '%draft%'
          AND LOWER(o.status) NOT LIKE '%cancelled%'
          AND LOWER(o.status) NOT LIKE '%failed%'
        ORDER BY o.id DESC
        LIMIT 500";

$result = $conn->query($sql);

$scheme = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https://' : 'http://');
$host = $_SERVER['HTTP_HOST'];
$scriptDir = rtrim(dirname($_SERVER['SCRIPT_NAME']), '/\\');
$apiBaseUrl = $scheme . $host . ($scriptDir === '' || $scriptDir === '/' ? '' : $scriptDir) . '/';
$rootBaseUrl = $scheme . $host . '/';

function formatImageUrl($rawImage, $apiBaseUrl, $rootBaseUrl) {
    if (empty($rawImage)) return null;
    if (preg_match('~^https?://~i', $rawImage) || preg_match('~^data:image/~i', $rawImage)) {
        return $rawImage;
    }
    $clean = ltrim($rawImage, '/');
    $filename = basename($clean);

    // 1. Check physical existence inside API directory
    if (file_exists(__DIR__ . '/uploads/images/' . $filename)) {
        return $apiBaseUrl . 'uploads/images/' . $filename;
    }
    if (file_exists(__DIR__ . '/uploads/' . $filename)) {
        return $apiBaseUrl . 'uploads/' . $filename;
    }

    // 2. Check physical existence inside Document Root
    if (isset($_SERVER['DOCUMENT_ROOT'])) {
        if (file_exists($_SERVER['DOCUMENT_ROOT'] . '/uploads/images/' . $filename)) {
            return $rootBaseUrl . 'uploads/images/' . $filename;
        }
        if (file_exists($_SERVER['DOCUMENT_ROOT'] . '/images/' . $filename)) {
            return $rootBaseUrl . 'images/' . $filename;
        }
        if (file_exists($_SERVER['DOCUMENT_ROOT'] . '/' . $clean)) {
            return $rootBaseUrl . $clean;
        }
    }

    // 3. Fallback constructions
    if (strpos($clean, 'uploads/') === 0 || strpos($clean, 'images/') === 0) {
        return $rootBaseUrl . $clean;
    }
    return $rootBaseUrl . 'uploads/images/' . $clean;
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

                    // A. Marketplace Product lookup in `products` table
                    $p_row = null;
                    $prod_id = intval($item['product_id'] ?? $item['id'] ?? $item['item_id'] ?? 0);
                    $prod_name = trim($item['name'] ?? $item['title'] ?? '');

                    if ($prod_id > 0) {
                        $p_stmt = $conn->prepare("SELECT id, name, category, description, price, image, active FROM products WHERE id = ? LIMIT 1");
                        $p_stmt->bind_param('i', $prod_id);
                        $p_stmt->execute();
                        $p_row = $p_stmt->get_result()->fetch_assoc();
                    }

                    if (!$p_row && !empty($prod_name)) {
                        $p_stmt2 = $conn->prepare("SELECT id, name, category, description, price, image, active FROM products WHERE LOWER(name) = LOWER(?) LIMIT 1");
                        $p_stmt2->bind_param('s', $prod_name);
                        $p_stmt2->execute();
                        $p_row = $p_stmt2->get_result()->fetch_assoc();

                        if (!$p_row) {
                            $p_stmt3 = $conn->prepare("SELECT id, name, category, description, price, image, active FROM products WHERE LOWER(name) LIKE LOWER(?) LIMIT 1");
                            $like_name = '%' . $prod_name . '%';
                            $p_stmt3->bind_param('s', $like_name);
                            $p_stmt3->execute();
                            $p_row = $p_stmt3->get_result()->fetch_assoc();
                        }
                    }

                    // B. Custom Quote lookup in `quotes` table
                    $q_row = null;
                    $quote_id = intval($item['quote_id'] ?? 0);
                    $quote_ref = trim($item['quote_ref'] ?? $item['reference_code'] ?? '');

                    if ($quote_id > 0) {
                        $q_stmt = $conn->prepare("SELECT id, reference_code, description, category, design_image, draft_image, client_idea_image FROM quotes WHERE id = ? LIMIT 1");
                        $q_stmt->bind_param('i', $quote_id);
                        $q_stmt->execute();
                        $q_row = $q_stmt->get_result()->fetch_assoc();
                    }

                    if (!$q_row && !empty($quote_ref)) {
                        $q_stmt2 = $conn->prepare("SELECT id, reference_code, description, category, design_image, draft_image, client_idea_image FROM quotes WHERE reference_code = ? LIMIT 1");
                        $q_stmt2->bind_param('s', $quote_ref);
                        $q_stmt2->execute();
                        $q_row = $q_stmt2->get_result()->fetch_assoc();
                    }

                    if (!$q_row && !empty($row['reference_code'])) {
                        $order_ref = $row['reference_code'];
                        $q_stmt3 = $conn->prepare("SELECT id, reference_code, description, category, design_image, draft_image, client_idea_image FROM quotes WHERE reference_code = ? LIMIT 1");
                        $q_stmt3->bind_param('s', $order_ref);
                        $q_stmt3->execute();
                        $q_row = $q_stmt3->get_result()->fetch_assoc();
                    }

                    $is_quote_type = (stripos($prod_name, 'quote') !== false) || (stripos($prod_name, 'custom') !== false);
                    if (!$q_row && $is_quote_type && !empty($row['user_id'])) {
                        $uid = intval($row['user_id']);
                        $order_created = $row['created_at'];
                        $q_stmt4 = $conn->prepare("SELECT id, reference_code, description, category, design_image, draft_image, client_idea_image FROM quotes WHERE user_id = ? AND (design_image IS NOT NULL OR draft_image IS NOT NULL OR client_idea_image IS NOT NULL) ORDER BY ABS(TIMESTAMPDIFF(SECOND, created_at, ?)) ASC LIMIT 1");
                        $q_stmt4->bind_param('is', $uid, $order_created);
                        $q_stmt4->execute();
                        $q_row = $q_stmt4->get_result()->fetch_assoc();
                    }

                    $quote_img_raw = null;
                    if ($q_row) {
                        if (!empty($q_row['design_image'])) $quote_img_raw = $q_row['design_image'];
                        elseif (!empty($q_row['draft_image'])) $quote_img_raw = $q_row['draft_image'];
                        elseif (!empty($q_row['client_idea_image'])) $quote_img_raw = $q_row['client_idea_image'];
                    }

                    // C. Extract the exact image for this item
                    $itemImgUrl = null;
                    if ($p_row && !empty($p_row['image'])) {
                        $itemImgUrl = formatImageUrl($p_row['image'], $apiBaseUrl, $rootBaseUrl);
                    } elseif (!empty($quote_img_raw)) {
                        $itemImgUrl = formatImageUrl($quote_img_raw, $apiBaseUrl, $rootBaseUrl);
                    } elseif (!empty($item['image'])) {
                        $itemImgUrl = formatImageUrl($item['image'], $apiBaseUrl, $rootBaseUrl);
                    } elseif (!empty($item['design_image'])) {
                        $itemImgUrl = formatImageUrl($item['design_image'], $apiBaseUrl, $rootBaseUrl);
                    } elseif (!empty($item['draft_image'])) {
                        $itemImgUrl = formatImageUrl($item['draft_image'], $apiBaseUrl, $rootBaseUrl);
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
                            'name'        => !empty($prod_name) ? $prod_name : ($q_row['category'] ?? 'Custom Quote'),
                            'category'    => $q_row['category'] ?? 'Custom Quote',
                            'description' => $q_row['description'] ?? null,
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
