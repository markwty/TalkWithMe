<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');
require_once('Classes/Firebase.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$id = $mysqli->real_escape_string($array["id"]);
$opp_id = $mysqli->real_escape_string($array["opp_id"]);

$response = array();
$query = "SELECT ID FROM online_table WHERE ID=$opp_id AND Target=$id;";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $res = array();
        $res['data']['Action'] = 'CANCELED';
		$query = "SELECT Token FROM login_table WHERE ID=${opp_id};";
        if ($result2 = $mysqli->query($query)) {
            if ($row = $result2->fetch_assoc()) {
                $token = $row["Token"];
                $firebase = new Firebase(); 
		        $firebase->send($token, $res);
		        $response["Response"] = 'Success';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
            }else{
                $response["Response"] = 'Something is wrong';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
            }
            $result2->free();
        }else{
            $response["Response"] = 'Something went wrong';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }
    }else{
        $response["Response"] = 'The person has already left the call';
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
