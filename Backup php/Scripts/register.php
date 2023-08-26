<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);
$name = $mysqli->real_escape_string($array["name"]);
$gender = $mysqli->real_escape_string($array["gender"]);
$state = $mysqli->real_escape_string($array["state"]);

$response = array();
$query = "SELECT ID FROM login_table WHERE Mobile='${mobile}';";
if ($result = $mysqli->query($query)) {
	if ($row = $result->fetch_assoc()) {
        $response["Response"] = 'Mobile number is in use';
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
	}else{
		$md5password = md5($password);
	    $query = "INSERT INTO login_table (Mobile, Password, Name, Gender, State) VALUES ('${mobile}','${md5password}','${name}',${gender},${state});";
	    if ($result2 = $mysqli->query($query)) {
	        $query = "SELECT ID FROM login_table WHERE Mobile='${mobile}';";
	        if($result3 = $mysqli->query($query)){
	            if ($row = $result3->fetch_assoc()) {
                    $id = $row["ID"];
                    $image = $array["image"];
                    if(!empty($image)){
                        $binary = base64_decode($image);
                        header('Content-Type: bitmap; charset=utf-8');
                        $file = fopen('Images/'.strval($id).'.png', 'wb');
                        fwrite($file, $binary);
                        fclose($file);
                    }
                    $response["Response"] = 'Registration successful';
                    $response["ID"] = $id;
                    $json = $serverEncrypt->encrypt($response);
                    echo json_encode($json, JSON_FORCE_OBJECT);
	            }else{
	                //Entry with mobile number is just inserted
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
    $result->free();
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
