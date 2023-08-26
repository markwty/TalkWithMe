<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$id = $mysqli->real_escape_string($array["id"]);
$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);
$name = $mysqli->real_escape_string($array["name"]);
$gender = $mysqli->real_escape_string($array["gender"]);
$modified = $mysqli->real_escape_string($array["modified"]);
$state = $mysqli->real_escape_string($array["state"]);
$old_password = $mysqli->real_escape_string($array["old_password"]);

$response = array();
$md5password = md5($password);
$md5old_password = md5($old_password);
$query = "UPDATE login_table SET Mobile='${mobile}', Password='${md5password}', Name='${name}', Gender=${gender}, Modified='${modified}', State=${state} WHERE ID=${id} AND Password='${md5old_password}';";
if ($result = $mysqli->query($query)) {
    $image = $array["image"];
    if (!empty($image)) {
        $binary = base64_decode($image);
        header('Content-Type: bitmap; charset=utf-8');
        $file = fopen('Images/'.strval($id).".png", 'wb');
        fwrite($file, $binary);
        fclose($file);
    }
    $response["Response"] = 'Success';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
    $result->free();
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
