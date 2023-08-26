<?php
error_reporting(0);
define('MyConst', TRUE);
require "init.php";

require_once('Classes/ServerEncrypt.php');

$serverEncrypt = new ServerEncrypt();
$array = $serverEncrypt->decrypt($_POST["data"]);

$mobile = $mysqli->real_escape_string($array["mobile"]);
$password = $mysqli->real_escape_string($array["password"]);
$local_list_str = $mysqli->real_escape_string($array["local_list_str"]);
$modified = $mysqli->real_escape_string($array["modified"]);

$response = array();
$md5password = md5($password);
$query = "SELECT Pending FROM login_table WHERE Mobile='${mobile}' AND Password='${md5password}';";
if ($result = $mysqli->query($query)) {
    if ($row = $result->fetch_assoc()) {
        $local_members = explode (",", $local_list_str);
        $members = explode (",", $row["Pending"]);
        $members = array_filter($members);
        
        $deleted_members = array_diff($local_members, $members);
        $new_members = array_diff($members, $local_members);
        $existing_members = array_intersect($members, $local_members);
        
        $existing_list = implode(",", $existing_members);
        $new_list = implode(",", $new_members);
        $deleted_list = implode(",", $deleted_members);
        $res=array();
        $cnt=0;
        if (!empty($existing_list)) {
            $query = "SELECT ID, Name, Gender, Organisation, Status FROM login_table WHERE ID in (${existing_list}) AND Modified > '${modified}';";
            if ($result2 = $mysqli->query($query)) {
                $num_fields = mysqli_num_fields($result2);
                while ($row = $result2->fetch_assoc()) {
                    $array = array();
                    for ($i = 0; $i < $num_fields; $i++) {
                        $fld = $result2 -> fetch_field_direct($i) -> name;
                        $array[$fld] = $row[$fld];
                    }
                    $p = 'Images/'.strval($row["ID"]).'.png';
                    $content = file_get_contents($p);
                    $array['Image'] = base64_encode($content);
                    $res[strval($cnt)] = $array;
                    $cnt++;                       
                }
                $result2->free();
            }else{
                $response["Response"] = 'Failed to process request';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
                $result->free();
                $mysqli->close();
                return;
            }
        }
        if (!empty($new_list)) {
            $query = "SELECT ID, Name, Gender, Organisation, Status FROM login_table WHERE ID in (${new_list});";
            if ($result3 = $mysqli->query($query)) {
                $num_fields = mysqli_num_fields($result3);
                while ($row = $result3->fetch_assoc()) {
                    $array = array();
                    for ($i = 0; $i < $num_fields; $i++) {
                        $fld = $result3 -> fetch_field_direct($i) -> name;
                        $array[$fld] = $row[$fld];
                    }
                    $p = 'Images/'.strval($row["ID"]).'.png';
                    $content = file_get_contents($p);
                    $array['Image'] = base64_encode($content);
                    $res[strval($cnt)] = $array;
                    $cnt++;                       
                }
                $result3->free();
            }else{
                $response["Response"] = 'Failed to process request';
                $json = $serverEncrypt->encrypt($response);
                echo json_encode($json, JSON_FORCE_OBJECT);
                $result->free();
                $mysqli->close();
                return;
            }
        }
        
        $res["length"] = strval($cnt);
        $response["Response"] = 'Success';
        $response["List"] = $res;
        $response["Deleted"] = $deleted_list;
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
    }else{
        //Acccount should exist
        $response["Response"] = 'Something went wrong';
        $json = $serverEncrypt->encrypt($response);
        echo json_encode($json, JSON_FORCE_OBJECT);
    }
    $result->free();
}else{
    $response["Response"] = 'Failed to process request';
    $json = $serverEncrypt->encrypt($response);
    echo json_encode($json, JSON_FORCE_OBJECT);
}
$mysqli->close();
?>
