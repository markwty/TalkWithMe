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
$query = "SELECT ID, Name, Gender, Status FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}';";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $id = $row["ID"];
        $name = $row["Name"];
        $gender = $row["Gender"];
        $status = $row["Status"];
        if ($status != 1) {
            $response["Response"] = 'Something went wrong. Please try logging in again.';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }else{
            $query="SELECT ID FROM online_table WHERE ID=${id};";
            if ($result2 = $mysqli->query($query)) {
                if ($row = $result2->fetch_assoc()) {
                    $query="UPDATE online_table SET Name='${name}', Gender=${gender}, OppID=0, OppName='', State=0, Target=0 WHERE ID=${id};";
                    if ($result3 = $mysqli->query($query)) {
                        $response["Response"] = 'Success';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT);
                        $result3->free();
                    }else{
                        $response["Response"] = 'Failed to process request';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT);
                    }
                }else{
                    $query="INSERT INTO online_table (ID, Name, Gender) VALUES (${id},'${name}',${gender});";
                    if ($result3 = $mysqli->query($query)) {
                        $response["Response"] = 'Success';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT);
                        $result3->free();
                    }else{
                        $response["Response"] = 'Failed to process request';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT);
                    }
                }
                $result2->free();
            }else{
                $response["Response"] = 'Failed to process request';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
            }
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
