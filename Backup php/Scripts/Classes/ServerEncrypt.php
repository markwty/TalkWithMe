<?php
class ServerEncrypt{
    private $privKey;
    private $publicKey;
    function __construct(){
        //$this->privKey = file_get_contents(dirname(__FILE__)."/private.txt");
        //$this->publicKey = file_get_contents(dirname(__FILE__)."/public.txt");
        $this->privKey = <<<EOD
-----BEGIN PRIVATE KEY----- 
<insert private key here>
-----END PRIVATE KEY-----
EOD;
        $this->publicKey = <<<EOD
-----BEGIN PUBLIC KEY----- 
<insert public key here>
-----END PUBLIC KEY-----
EOD;
    }
    function generateKeys(){
        $res = openssl_pkey_new();
        openssl_pkey_export($res, $privKey);
        $publicKey = openssl_pkey_get_details($res);
        $publicKey=$publicKey["key"];
        echo $privKey."<br>";
        echo $publicKey;
    }
    function encrypt($response){
        $cipher = 'AES-128-CBC';
        $secretKey = openssl_random_pseudo_bytes(16);//keysize=128
        $ivlen = openssl_cipher_iv_length($cipher);
        $iv = openssl_random_pseudo_bytes($ivlen);
        $text = openssl_encrypt(json_encode($response, JSON_FORCE_OBJECT), $cipher, $secretKey, OPENSSL_RAW_DATA, $iv);
        openssl_public_encrypt($secretKey, $key, openssl_get_publickey($this->publicKey));
        $json = array();
        $json["key"] = base64_encode($key);
        $json["iv"] = base64_encode($iv);
        $json["body"] = base64_encode($text);
        return $json;
    }
    function decrypt($text){
        $array = json_decode($text, true);
        $key = $array["key"];
        $iv = $array["iv"];
        $body = $array["body"];
        //$key = str_replace(' ', '+', $key);
        //$iv = str_replace(' ', '+', $iv);
        //$body = str_replace(' ', '+', $body);
        
        
        openssl_private_decrypt(base64_decode($key), $secretKey, openssl_get_privatekey($this->privKey));
        $cipher = 'AES-128-CBC';
        $content = openssl_decrypt(base64_decode($body), $cipher, $secretKey, OPENSSL_RAW_DATA, base64_decode($iv));
        //$jsonData = rtrim(utf8_decode($content), "\0");
        $array = json_decode($content, true);
        return $array;
    }
}
?>