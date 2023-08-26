package com.example.talkwithme;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class ClientEncrypt {
    private String classname = getClass().getName();
    //Get from server (first time only)
    private final String publicKeyString = "";
    //Generated from application (first time only)
    private final String privateKeyString = "";
    private PublicKey publicKey;
    private static ClientEncrypt clientEncryptInstance = null;

    private ClientEncrypt(){
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.decode(publicKeyString, Base64.DEFAULT));
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(publicSpec);
        }catch(NoSuchAlgorithmException | InvalidKeySpecException e){
            Log.e(classname, e.toString());
        }
    }

    static ClientEncrypt getInstance() {
        if (clientEncryptInstance == null) {
            clientEncryptInstance = new ClientEncrypt();
        }
        return clientEncryptInstance;
    }

    @SuppressWarnings("unused")
    void generateRSAKeyPair() {
        //Only generated once, public key should be known to server
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            String privateKeyString = Base64.encodeToString(keyPair.getPrivate().getEncoded(), Base64.DEFAULT);
            String publicKeyString = Base64.encodeToString(keyPair.getPublic().getEncoded(), Base64.DEFAULT);

            Log.v(classname, "privateKey\n" + privateKeyString + "\n");
            Log.v(classname, "publicKey\n" + publicKeyString + "\n");
        } catch (NoSuchAlgorithmException e) {
            Log.e(classname, e.toString());
        }
    }

    String encrypt(String text){
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey secretKey = keyGenerator.generateKey();

            byte[] raw = secretKey.getEncoded();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            SecureRandom r = new SecureRandom();
            byte[] iv = new byte[16];
            r.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(iv));
            String cipherTextString = Base64.encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);

            Cipher cipher2 = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher2.init(Cipher.ENCRYPT_MODE, publicKey);
            String encryptedSecretKey = Base64.encodeToString(cipher2.doFinal(secretKey.getEncoded()), Base64.DEFAULT);

            JSONObject encryptedJSON = new JSONObject();
            encryptedJSON.put("key", encryptedSecretKey);
            encryptedJSON.put("iv", Base64.encodeToString(iv, Base64.DEFAULT));
            encryptedJSON.put("body", cipherTextString);
            /*
            {"key":-------{encrypted (by RSA public key) AES key + base64 encoded},
                "iv":-------{base64 encoded iv}, "body":-------{encryted (by AES) data + base64 encoded}}
            data={"mobile":----, "password":----, "name":----, "gender":----, "state":----}
             */
            return encryptedJSON.toString();
        }catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                BadPaddingException | InvalidAlgorithmParameterException | JSONException e) {
            Log.e(classname, e.toString());
            return "";
        }
    }

    String decrypt(String text){
        try {
            JSONObject collection = new JSONObject(text);

            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyString, Base64.DEFAULT));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(privateSpec);

            Cipher cipher1 = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher1.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] secretKeyBytes = cipher1.doFinal(Base64.decode(collection.getString("key"), Base64.DEFAULT));
            SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES");

            byte[] raw = secretKey.getEncoded();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            byte[] iv = Base64.decode(collection.getString("iv"), Base64.DEFAULT);
            String textBody = collection.getString("body");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv));
            byte[] original = cipher.doFinal(Base64.decode(textBody, Base64.DEFAULT));
            return new String(original, StandardCharsets.UTF_8);

        }catch(NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | JSONException e) {
            Log.e(classname, e.toString());
            e.printStackTrace();
            return "";
        }
    }
}