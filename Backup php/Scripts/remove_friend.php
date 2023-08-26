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
$query = "SELECT ID, Friends FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}';";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $id = $row["ID"];
        $friends = $row["Friends"];
        $members = explode (",", $friends);
        $pos = array_search("${opp_id}", $members);
        if (!$pos && $pos!==0) {
            //opp_id should be in the Friends column
            $response["Response"] = 'Something went wrong';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }else{
            unset($members[$pos]);
            $new_members = implode(",", $members);
            $query = "UPDATE login_table SET Friends='{$new_members}' WHERE ID=${id};";
            if ($result2 = $mysqli->query($query)) {
                $query = "SELECT Friends FROM login_table WHERE ID=${opp_id};";
                if ($result3 = $mysqli->query($query)) {
                    if ($row = $result3->fetch_assoc()) {
                        $friends = $row["Friends"];
                        $members = explode (",", $friends);
                        $pos = array_search("${id}", $members);
                        if (!$pos && $pos!==0) {
                            //id should be found in Friends of the other person
                            $response["Response"] = 'Something went wrong';
                            $json = $serverEncrypt->encrypt($response);
                            echo json_encode($json, JSON_FORCE_OBJECT);
                        }else{
                            unset($members[$pos]);
                            $new_members = implode(",",$members);
                            $query = "UPDATE login_table SET Friends='{$new_members}' WHERE ID=${opp_id};";
                            if ($result4 = $mysqli->query($query)) {
                                $response["Response"] = 'Success';
                                $json = $serverEncrypt->encrypt($response);
                                echo json_encode($json, JSON_FORCE_OBJECT);
                                $result4->free();
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
    echo json_encode($response, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
