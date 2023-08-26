<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);

$response = array();
$md5password = md5($password);
$query = "SELECT ID, Name, Gender, Organisation, Status, State FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}';";
if ($result = $mysqli->query($query)) {
	if($row = $result->fetch_assoc()){
	    $response["Response"] = 'Login successful';
	    $response["ID"] = $row["ID"];
	    $response["Name"] = $row["Name"];
	    $response["Gender"] = $row["Gender"];
	    $response["Organisation"] = $row["Organisation"];
	    $response["Status"] = $row["Status"];
	    $response["State"] = $row["State"];
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
	}else{
		$response["Response"] = 'Login unsuccessful';
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
