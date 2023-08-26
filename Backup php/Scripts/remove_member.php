<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);
$opp_id = $mysqli->real_escape_string($array["opp_id"]);

$response = array();
$md5password = md5($password);
$query = "SELECT ID, Name, Friends FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}' AND Status=2;";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $id = $row["ID"];
        $name = $row["Name"];
        $res = $row["Friends"];
        $members = explode (",", $res);
        $pos = array_search("${opp_id}", $members);
        if(!$pos && $pos!==0){
            //opp_id should be in the Friends column
            $response["Response"] = 'Something went wrong';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }else{
            unset($members[$pos]);
            $new_members = implode(",",$members);
            $query = "UPDATE login_table SET Friends='{$new_members}' WHERE ID=${id};";
            if ($result2 = $mysqli->query($query)) {
                $query = "UPDATE login_table SET Organisation='', Status=0 WHERE ID=${opp_id} AND Organisation='${name}';";
                if ($result3 = $mysqli->query($query)) {
                    $file = 'Forms/'.strval($name).'/'.strval($opp_id).'.png';
                    if (!unlink($file)) {  
                        $response["Response"] = 'Something went wrong';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT); 
                    }  
                    else {  
                        $response["Response"] = 'Success';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT);
                    }
                }else{
                    $response["Response"] = 'Failed to process request';
                    $json = $serverEncrypt->encrypt($response);
                    echo json_encode($json, JSON_FORCE_OBJECT);
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
    echo json_encode($response, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
