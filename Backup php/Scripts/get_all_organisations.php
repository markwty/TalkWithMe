<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();

$response = array();
$query = "SELECT * FROM organisation_table;";
$res = array();
$cnt = 0;
if ($result = $mysqli->query($query)) {
    $num_fields = mysqli_num_fields($result);
    while ($row = $result->fetch_assoc()) {
        $array = array();
        for ($i = 0; $i < $num_fields; $i++) {
            $fld = $result -> fetch_field_direct($i) -> name;
            $array[$fld] = $row[$fld];
        }
        $res[strval($cnt)] = $array;
        $cnt++;                       
    }
    $res["length"] = strval($cnt);
    $response["Response"] = 'Success';
    $response["List"] = $res;
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
    $result->free();
}else{
    $response["Response"] = 'Something went wrong';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
