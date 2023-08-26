<?php
if(!defined('MyConst')) {
   die('Direct access not permitted');
}
$host = "localhost";
$dbuser = "<insert db user here>";
$dbpassword = "<insert db password here>";
$db = "<insert db name here>";
// domain name: markelili.000webhostapp.com
$mysqli = mysqli_connect($host, $dbuser, $dbpassword, $db);
?>
