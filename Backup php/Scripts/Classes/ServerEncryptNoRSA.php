<?php
class ServerEncryptNoRSA{
    private $key = "UiJFzKBvOeHbnjM2U8qc+g==";
    function __construct(){
        $this->key = base64_decode($this->key);
    }
    function generateKeys(){
        echo base64_encode(openssl_random_pseudo_bytes(16));
    }
    function encrypt($response){
        $cipher = 'AES-128-CBC';
        $iv = openssl_random_pseudo_bytes(16);
        //$response = utf8_encode(json_encode($response, JSON_FORCE_OBJECT));
        $response = json_encode($response, JSON_FORCE_OBJECT);
        $text = openssl_encrypt($response, $cipher, $this->key, OPENSSL_RAW_DATA, $iv);
        $json = array();
        $json["length"] = strval(strlen($response));
        $json["iv"] = base64_encode($iv);
        $json["body"] = base64_encode($text);
        return $json;
    }
    function decrypt($text){
        $array = json_decode($text, true);
        $iv = $array["iv"];
        $body = $array["body"];
        $iv = str_replace(' ', '+', $iv);
        $body = str_replace(' ', '+', $body);
        
        $cipher = 'AES-128-CBC';
        $content = openssl_decrypt(base64_decode($body), $cipher, $this->key, OPENSSL_RAW_DATA|OPENSSL_ZERO_PADDING, base64_decode($iv));
        //$jsonData = rtrim(utf8_decode($content), "\0");
        $jsonData = rtrim($content, "\0");
        $array = json_decode($jsonData, true);
        return $array;
    }
}
?>