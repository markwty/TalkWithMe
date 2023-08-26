<?php
require_once('ServerEncrypt.php');
$serverEncrypt = new ServerEncrypt();
$serverEncrypt->generateKeys();

#require_once('ServerEncryptNoRSA.php');
#$serverEncryptNoRSA = new ServerEncryptNoRSA();
#$serverEncryptNoRSA->generateKeys();
?>