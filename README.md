![image](https://github.com/user-attachments/assets/7009b886-9e7d-432b-80ec-8f67b9733b21)

sample backend code
```php
<?php

header("Content-Type: application/json");

$rawData = file_get_contents("php://input");

$data = json_decode($rawData, true);
if (json_last_error() === JSON_ERROR_NONE) {
    $logFile = 'notification_log.txt';
    $logData = date('Y-m-d H:i:s') . " - " . json_encode($data) . PHP_EOL;
 // do everything in here dude
    echo json_encode(["status" => "success", "message" => "Notification received"]);
} else {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Invalid JSON"]);
}
?>

```

sample notification log
```json
 {"package":"com.example.notifier","title":"TQ Notifier","text":"The app is running in the background."}
```
