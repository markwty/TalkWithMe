<?php
class TokenGenerator{
    private $DEV_KEY;
    private $APP_ID;
    private $expiresInSecs;
    function __construct(){
        $this->DEV_KEY = "<insert dev key here>";
        $this->APP_ID = "<insert app id here>.vidyo.io";
        $this->expiresInSecs = 300;
    }
    function generateToken($username){
        $EPOCH_SECONDS = 62167219200;
        $expires = $EPOCH_SECONDS + $this->expiresInSecs + time();
        $jid = $username."@".$this->APP_ID;
        $body = "provision"."\0".$jid."\0".$expires."\0"."";
        $utf8_body = utf8_encode($body);
        $mac_hash = hash_hmac("sha384", $utf8_body, $this->DEV_KEY);
        $serialized = $utf8_body."\0".$mac_hash;
        $b64_encoded = base64_encode($serialized);
        return $b64_encoded;
    }
}
?>