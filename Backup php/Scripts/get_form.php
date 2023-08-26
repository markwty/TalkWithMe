<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$opp_id = $mysqli->real_escape_string($array["opp_id"]);
$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);

$response = array();
$md5password = md5($password);
$query = "SELECT Name FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}' AND Status=2;";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $name = $row["Name"];
        $p = 'Forms/'.strval($name).'/'.strval($opp_id).'.png';
        $content = file_get_contents($p);
        $response["Response"] = 'Success';
        $response["Image"] = base64_encode($content);
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
    }else{
        //Account should exist
        $response["Response"] = 'Something went wrong';
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
    }
    $result->free();
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
