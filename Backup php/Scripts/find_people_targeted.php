<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');
require_once('Classes/Generator.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);

$response = array();
$md5password = md5($password);
$query = "SELECT ID, Name FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}';";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $id = $row["ID"];
        $name = $row["Name"];
        $query = "SELECT ID, Name FROM online_table WHERE State=0 AND Target=${id};";
        if ($result2 = $mysqli->query($query)) {
            if ($row = $result2->fetch_assoc()) {
                $opp_id = $row['ID'];
                $opp_name = $row['Name'];
                $query = "UPDATE online_table SET OppID=${id}, OppName='${name}', State=1 WHERE ID=${opp_id};";
                if ($result3 = $mysqli->query($query)) {
                    $generator = new TokenGenerator();
                    $token = $generator->generateToken($id);
                    $response["Response"] = 'Success';
                    $response["Opp_id"] = strval($opp_id);
                    $response["Opp_name"] = strval($opp_name);
                    $response["Token"] = $token;
                    $json = $serverEncrypt->encrypt($response);
                    echo json_encode($json, JSON_FORCE_OBJECT);
                    $result3->free();
                }else{
                    $response["Response"] = 'Failed to process request';
                    $json = $serverEncrypt->encrypt($response);
                    echo json_encode($json, JSON_FORCE_OBJECT);
                }
            }else{
                $response["Response"] = 'Waiting...';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
            }
            $result2->free();
        }
        else{
            $response["Response"] = 'Failed to process request';
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
}
else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
