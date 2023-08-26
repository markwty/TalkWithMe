<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

$code = $mysqli->real_escape_string($_POST["code"]);
$mobile = $mysqli->real_escape_string($array["mobile"]);
$mobile = substr($mobile, 3);

if(code != "some_random_code2"){
    $response["Response"] = 'Something went wrong';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
    return;
}

$query = "SELECT Mobile FROM login_table WHERE Mobile='${mobile}';";
if ($result = $mysqli->query($query)) {
    if($row = $result->fetch_assoc()) {
        $datetime = date('Y-m-d H:i:s');
        $query = "UPDATE login_table SET Open='${datetime}' WHERE Mobile='${mobile}';";
        if ($result2 = $mysqli->query($query)) {
            $response["Response"] = 'Success';
            echo json_encode($response, JSON_FORCE_OBJECT);
            $result2->free();
        }else{
            $response["Response"] = 'Failed to process request';
            echo json_encode($response, JSON_FORCE_OBJECT);
        }
    }else{
        $response["Response"] = 'Failed to process request';
        echo json_encode($response, JSON_FORCE_OBJECT);
    }
    $result->free();
}else{
    $response["Response"] = 'Failed to process request';
    echo json_encode($response, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>