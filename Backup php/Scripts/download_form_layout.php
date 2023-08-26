<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$form = $mysqli->real_escape_string($array["form"]);

$response = array();
$p = 'Forms/'.strval($form).'.json';
$content = file_get_contents($p);
$response["Response"] = 'Success';
$response['Json'] = $content;
$json = $serverEncrypt->encrypt($response);
echo json_encode($json, JSON_FORCE_OBJECT);
?>
