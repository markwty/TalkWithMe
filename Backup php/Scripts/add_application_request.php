<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);
$organisation = $mysqli->real_escape_string($array["organisation"]);

$response = array();
$md5password = md5($password);
$query = "SELECT ID, Organisation FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}' AND Status=0;";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $id = $row["ID"];
        $old_organisation = $row["Organisation"];
        $image = $array["image"];
        if (!empty($image)) {
            $binary = base64_decode($image);
            header('Content-Type: bitmap; charset=utf-8');
            $file = fopen('Forms/'.strval($organisation).'/'.strval($id).'.png', 'wb');
            fwrite($file, $binary);
            fclose($file);
        }
        if ($old_organisation == $organisation) {
            $response["Response"] = 'Success';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }else{
            $query = "UPDATE login_table SET Pending=concat(Pending, ',${id}') WHERE Name='${organisation}' AND Status=2;";
            if ($result2 = $mysqli->query($query)) {
                $query = "UPDATE login_table SET Organisation='${organisation}' WHERE ID=${id};";
                if ($result3 = $mysqli->query($query)) {
                    if($old_organisation == ''){
                        $response["Response"] = 'Success';
                        $json = $serverEncrypt->encrypt($response);
                        echo json_encode($json, JSON_FORCE_OBJECT);
                    }else{
                        $query = "SELECT Pending FROM login_table WHERE Name='${old_organisation}' AND Status=2;";
                        if ($result4 = $mysqli->query($query)) {
                            if ($row = $result4->fetch_assoc()) {
                                $members = explode (",", $row["Pending"]);
                                $pos = array_search("${id}", $members);
                                if (!$pos && $pos!==0) {
                                    //id should be found in Pending of old organisation
                                    $response["Response"] = 'Something went wrong4';
                                    $json = $serverEncrypt->encrypt($response);
                                    echo json_encode($json, JSON_FORCE_OBJECT);
                                }else{
                                    unset($members[$pos]);
                                    $new_members = implode(",", $members);
                                    $query = "UPDATE login_table SET Pending='${new_members}' WHERE Name='${old_organisation}' AND Status=2;";
                                    if ($result5 = $mysqli->query($query)) {
                                        $file = 'Forms/'.strval($old_organisation).'/'.strval($id).'.png';
                                        if (!unlink($file)) {  
                                            $response["Response"] = 'Something went wrong';
                                            $json = $serverEncrypt->encrypt($response);
                                            echo json_encode($json, JSON_FORCE_OBJECT); 
                                        }else{  
                                            $response["Response"] = 'Success';
                                            $json = $serverEncrypt->encrypt($response);
                                            echo json_encode($json, JSON_FORCE_OBJECT);
                                        }
                                        $result5->free();
                                    }else{
                                        $response["Response"] = 'Failed to process request';
                                        $json = $serverEncrypt->encrypt($response);
                                        echo json_encode($json, JSON_FORCE_OBJECT);
                                    }
                                }
                            }else{
                                $response["Response"] = 'Something went wrong';
                                $json = $serverEncrypt->encrypt($response);
                                echo json_encode($json, JSON_FORCE_OBJECT);
                            }
                            $result4->free();
                        }else{
                            $response["Response"] = 'Failed to process request';
                            $json = $serverEncrypt->encrypt($response);
                            echo json_encode($json, JSON_FORCE_OBJECT);
                        }
                    }
                    $result3->free();
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
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
