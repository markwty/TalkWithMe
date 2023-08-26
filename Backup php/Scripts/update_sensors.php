<?php 
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncryptNoRSA.php');

$serverEncryptNoRSA = new ServerEncryptNoRSA();
$array = $serverEncryptNoRSA->decrypt($_POST["data"]);

$device = $mysqli->real_escape_string($array["device"]);
$bpm = $mysqli->real_escape_string($array["bpm"]);
$temperature =$mysqli->real_escape_string($array["temperature"]);
$moisture = $mysqli->real_escape_string($array["moisture"]);

$response = array();
$query = "SELECT Device FROM sensor_table WHERE Device='${device}';";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $query = "UPDATE sensor_table SET BPM='${bpm}', Temperature='${temperature}', Moisture='${moisture}', Time=CURRENT_TIMESTAMP WHERE Device='${device}';";
        if ($result2 = $mysqli->query($query)) {
            $response["Response"] = 'Success';
            $json = $serverEncryptNoRSA->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
            $result2->free();
        }else{
            $response["Response"] = 'Failed to process request';
            $json = $serverEncryptNoRSA->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }
    }else{
        $query = "INSERT INTO sensor_table (BPM, Temperature, Moisture, Device) VALUES ('${bpm}','${temperature}','${moisture}','${device}');";
        if($result2 = $mysqli->query($query)){
            $response["Response"] = 'Success';
            $json = $serverEncryptNoRSA->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
            $result2->free();
        }else{
            $response["Response"] = 'Failed to process request';
            $json = $serverEncryptNoRSA->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }
    }
    $result->free();
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncryptNoRSA->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
