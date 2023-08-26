<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$code = $mysqli->real_escape_string($array["code"]);
$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);

//May replace with email verification (more secured)
if($code != "some_random_code"){
    $response["Response"] = 'Something went wrong';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
    return;
}

$response = array();
$md5password = md5($password);
$query = "SELECT Open FROM login_table WHERE Mobile='${mobile}';";
if ($result = $mysqli->query($query)){
    if ($row = $result->fetch_assoc()) {
        $now = new DateTime();
        $open = date_create_from_format('Y-m-d H:i:s', $row["Open"]);
        $difference = $open->format('U') - $now->format('U');
        if ($difference <= 180) {
            $query = "UPDATE login_table SET Password='${md5password}' AND Open='0000-00-00 00:00:00' WHERE Mobile='${mobile}';";
            if ($result2 = $mysqli->query($query)) {
                $query = "SELECT ID, Name, Gender, Organisation, Status, State FROM login_table WHERE Mobile='${mobile}';";
                if ($result3 = $mysqli->query($query)) {
                    $num_fields = mysqli_num_fields($result3);
                    if ($row = $result3->fetch_assoc()) {
                        for ($i = 0; $i < $num_fields; $i++) {
                            $fld = $result3 -> fetch_field_direct($i) -> name;
                            $response[$fld] = $row[$fld];
                        }
                    }
                    $response["Response"] = 'Success';
                    $json = $serverEncrypt->encrypt($response);
                    echo json_encode($json, JSON_FORCE_OBJECT);
                    $result3->free();
                }else{
                    //Account should exist
                    $response["Response"] = 'Something went wrong';
                    $json = $serverEncrypt->encrypt($response);
                    echo json_encode($json, JSON_FORCE_OBJECT);
                }
            }else{
                $response["Response"] = 'Failed to process request';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
            }
        }else{
            //Password is not open (illegal)
            $response["Response"] = 'Something went wrong';
            $json = $serverEncrypt->encrypt($response);
            echo json_encode($json, JSON_FORCE_OBJECT);
        }
    }else{
        //Account should exist
        $response["Response"] = 'Something went wrong';
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
    }
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>