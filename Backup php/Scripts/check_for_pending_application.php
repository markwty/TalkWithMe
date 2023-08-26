<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$id = $mysqli->real_escape_string($array["id"]);

$response = array();
$query = "SELECT Organisation FROM login_table WHERE ID=${id};";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $organisation = $row["Organisation"];
        if ($organisation == '') {
            $response["Response"] = 'No pending request/Request has been rejected';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);          
        }else{
            $response["Response"] = 'Pending request';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);    
        }
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
