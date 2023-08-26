<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$id = $mysqli->real_escape_string($array["id"]);

$response = array();
$query = "SELECT Mobile FROM login_table WHERE ID!=${id} AND Mobile='${mobile}';";
if ($result = $mysqli->query($query)) {
	if ($row = $result->fetch_assoc()) {
        $response["Response"] = 'Mobile is in use';
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
	}else{
		$response["Response"] = 'Mobile is not in use';
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
