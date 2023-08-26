<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');
require_once('Classes/Firebase.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);
$target = $mysqli->real_escape_string($array["target"]);

$response = array();
$md5password = md5($password);
$query = "SELECT ID, Name FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}';";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $id = $row["ID"];
        $name = $row["Name"];
        $query = "SELECT Name FROM login_table WHERE ID=${target};";
        if ($result2 = $mysqli->query($query)) {
            if ($row = $result2->fetch_assoc()) {
                $opp_name = $row["Name"];
                $query="SELECT ID FROM online_table WHERE ID=${id};";
                if ($result3 = $mysqli->query($query)){
                    if($row = $result3->fetch_assoc()) {
                        $query="UPDATE online_table SET Name='${name}', Target=${target} WHERE ID=${id};";
                        if ($result4 = $mysqli->query($query)) {
                            $res = array();
                            $res['data']['Action'] = "INCOMING";
                            $res['data']['ID'] = $target;
                            $res['data']['OppID'] = $id;
                            $res['data']['OppName'] = $name;
                            $res['data']['Name'] = $opp_name;
                    		$query = "SELECT Token FROM login_table WHERE ID=${target};";
                            if ($result5 = $mysqli->query($query)) {
                                if ($row = $result5->fetch_assoc()) {
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
                                $result5->free();
                            }else{
                                $response["Response"] = 'Failed to process request';
                                $json = $serverEncrypt->encrypt($response);
                                echo json_encode($json, JSON_FORCE_OBJECT);
                            }
                            $result4->free();
                        }else{
                            $response["Response"] = 'Failed to process request';
                            $json = $serverEncrypt->encrypt($response);
                            echo json_encode($json, JSON_FORCE_OBJECT);
                        }
                    }else{
                        $query="INSERT INTO online_table (ID, Name, Target) VALUES (${id},'${name}',${target});";
                        if ($result4 = $mysqli->query($query)) {
                            $res = array();
                            $res['data']['Action'] = "INCOMING";
                            $res['data']['ID'] = $target;
                            $res['data']['OppID'] = $id;
                            $res['data']['OppName'] = $name;
                            $res['data']['Name'] = $opp_name;
                    		$query = "SELECT Token FROM login_table WHERE ID=${target};";
                            if ($result5 = $mysqli->query($query)) {
                                if ($row = $result5->fetch_assoc()) {
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
                                $result5->free();
                            }else{
                                $response["Response"] = 'Failed to process request';
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
            }else{
                $response["Response"] = 'Something is wrong';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
            }
            $result2->free();
        }else{
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
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
