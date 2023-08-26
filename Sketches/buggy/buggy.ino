//Buggy no. 1
#include "Arduino.h"
#include "Adafruit_TCS34725.h"
#include "Adafruit_NeoPixel.h"
//BLE
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

//File system
#include "FS.h"
#include "SPIFFS.h"

//Wifi
#include <WiFi.h>
#include <WiFiMulti.h>
#include <esp_wifi.h>
#include <HTTPClient.h>

//Encrytion
#include <ArduinoJson.h>
#include "mbedtls/aes.h"
extern "C" {
#include "crypto/base64.h"
}

//Deep sleep
#include "driver/adc.h"
#include <esp_bt.h>

//Disable brownout
#include "soc/soc.h"
#include "soc/rtc_cntl_reg.h"

Adafruit_TCS34725 tcs = Adafruit_TCS34725(TCS34725_INTEGRATIONTIME_50MS, TCS34725_GAIN_4X);
Adafruit_NeoPixel strip = Adafruit_NeoPixel(1, 5, NEO_GRB + NEO_KHZ800);
int vSpeed = 120;        // MAX 255,200//-60
int vSpeedB = 70;
int turn_speed = 130;    // MAX 255,220
long duration;
float distance;
int left_sensor_state, right_sensor_state;
int destination = 0, location = 0;
bool pendingRequest = false, reachedCheckpoint = false, released = false;

bool debug = false;

//L298N Connection
// motor one
const int enA = 18;
const int in1 = 17;
const int in2 = 16;
// motor two
const int enB = 13;
const int in3 = 23;
const int in4 = 19;
//Sensor Connection
const int leftSensorPin = 36, rightSensorPin = 34;
const int trigPin = 12, echoPin = 35;
const int threshold = 2000;

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
std::string deviceName = "Buggy1";
BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false, deviceFound = false;
std::string rxValue = "", accumrxValue = "";
std::string clientDevice = "";
static BLEAdvertisedDevice* clientBLEDevice;
pthread_t client_ble_thread;
BLEClient* pClient;

boolean appended = false, update = false;
int numLines = 0;
#define credentials_length 10
std::string credentials[credentials_length] = "";
std::string ssid = "", password = "";

WiFiMulti wifiMulti;
IPAddress local_IP(192, 168, 1, 184);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 0, 0);
IPAddress primaryDNS(8, 8, 8, 8);
IPAddress secondaryDNS(8, 8, 4, 4);

StaticJsonDocument<200> dict_data;
StaticJsonDocument<200> array;
unsigned char* array_str;

int readFile(fs::FS &fs, const char* path) {
  Serial.printf("Reading file: %s\r\n", path);
  File file = fs.open(path);
  if (!file || file.isDirectory()) {
    Serial.println("Failed to open file for reading");
    return 0;
  }
  int i = 0;
  char buffer[200];//max length of line
  while (file.available()) {
    int l = file.readBytesUntil('\n', buffer, sizeof(buffer));
    buffer[l] = 0;
    credentials[i++] = buffer;
    if (i == credentials_length - 1) {
      break;
    }
  }
  file.close();
  return i + 1;
}

void appendFile(fs::FS &fs, const char* path, const char* message) {
  Serial.printf("Appending to file: %s\r\n", path);
  File file = fs.open(path, FILE_APPEND);
  if (!file) {
    Serial.println("Failed to open file for appending");
    return;
  }
  if (file.print(message)) {
    Serial.println("File appended");
  } else {
    Serial.println("Append failed");
  }
  file.close();
}

void writeFile(fs::FS &fs, const char* path, const char* message) {
  Serial.printf("Writing to file: %s\r\n", path);
  File file = fs.open(path, FILE_WRITE);
  if (!file) {
    Serial.println("Failed to open file for writing");
    return;
  }
  if (file.print(message)) {
    Serial.println("File written");
  } else {
    Serial.println("Write failed");
  }
  file.close();
}

class CustomServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

class CustomCharacteristicCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      rxValue = pCharacteristic->getValue();
      if (rxValue.length() > 0) {
        if (rxValue.at(0) == 195 && rxValue.at(1) == 164) {//4294967295
          if (rxValue.at(rxValue.length() - 1) == 188 && rxValue.at(rxValue.length() - 2) == 195) {
            accumrxValue = rxValue.substr(2, rxValue.length() - 4);
            process(accumrxValue);
          } else {
            accumrxValue = rxValue.substr(2);
          }
        } else if (rxValue.at(rxValue.length() - 1) == 188 && rxValue.at(rxValue.length() - 2) == 195) {
          accumrxValue += rxValue.substr(0, rxValue.length() - 2);
          process(accumrxValue);
        } else {
          accumrxValue += rxValue;
        }
      }
    }

    void process(std::string accumrxValue) {
      for (int i = 0; i < accumrxValue.length(); i++) {
        Serial.print(accumrxValue[i]);
      }
      Serial.println();
      std::size_t pos = accumrxValue.find("::");
      if (pos != -1) {
        std::string field = accumrxValue.substr(0, pos);
        std::string value = accumrxValue.substr(pos + 2);
        if (field.compare("ssid") == 0) {
          ssid = value;
        } else if (field.compare("password") == 0) {
          password = value;
          if (ssid.compare("") != 0) {
            boolean through = false;
            for (int i = 0; i < numLines - 1; i += 2) {
              if (credentials[i].compare(ssid) == 0 && credentials[i + 1].compare(password) != 0) {
                credentials[i + 1] = password;
                update = true;
                through = true;
                break;
              }
            }
            if (!through) {
              if (numLines < credentials_length + 1) {
                credentials[numLines - 1] = ssid;
                credentials[numLines] = password;
                appendFile(SPIFFS, "/wifi_credentials.txt", (ssid + "\n" + password + "\n").c_str());
                numLines += 2;
                appended = true;
              } else {
                //numLines = credentials_length + 1
                credentials[credentials_length - 3] = ssid;
                credentials[credentials_length - 2] = password;
                update = true;
              }
            }
          }
          ssid = "";
          password = "";
        }
      }
    }
};

class CustomClientCallback : public BLEClientCallbacks {
    void onConnect(BLEClient* pclient) {
      deviceConnected = true;
      Serial.println("Connected");
    }

    void onDisconnect(BLEClient* pclient) {
      deviceConnected = false;
      Serial.println("Disconnected");
    }
};

static void ClientNotifyCallback(BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* pData, size_t length, bool isNotify) {
  std::string pdata(pData, pData + length);
  std::size_t pos = pdata.find("::");
  Serial.println(pdata.c_str());
  if (pos != -1) {
    std::string field = pdata.substr(0, pos);
    std::string value = pdata.substr(pos + 2);
    if (field.compare(clientDevice) == 0) {
      destination = atoi(value.c_str());
      if (destination > 0) {
        released = true;
        //led light on to indicate unlocked?
        pClient->disconnect();
      }
    }
  }
}

void connectToServer() {
  pClient = BLEDevice::createClient();
  pClient->setClientCallbacks(new CustomClientCallback());
  pClient->connect(clientBLEDevice);

  BLERemoteService* pRemoteService = pClient->getService(SERVICE_UUID);
  if (pRemoteService == nullptr) {
    Serial.println("Failed to connect to UART");
    pClient->disconnect();
  }

  BLERemoteCharacteristic* pRemoteCharacteristic = pRemoteService->getCharacteristic(CHARACTERISTIC_UUID_TX);
  if (pRemoteCharacteristic == nullptr) {
    pClient->disconnect();
  }
  if (pRemoteCharacteristic->canNotify()) {
    Serial.println("Registering callback");
    pRemoteCharacteristic->registerForNotify(ClientNotifyCallback);
  }
  pRemoteCharacteristic = pRemoteService->getCharacteristic(CHARACTERISTIC_UUID_RX);
  if (pRemoteCharacteristic->canWrite()) {
    Serial.println("Writing");
    pRemoteCharacteristic->writeValue("ä" + deviceName + "::" + clientDevice + "ü");
  }
}

class CustomAdvertisedDeviceCallback: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {
      if (advertisedDevice.haveName() && advertisedDevice.getName().compare(clientDevice) == 0) {
        BLEDevice::getScan()->stop();
        clientBLEDevice = new BLEAdvertisedDevice(advertisedDevice);
        deviceFound = true;
      }
    }
};

void* scanForServer(void* ptr) {
  BLEDevice::init(deviceName);
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new CustomAdvertisedDeviceCallback());
  pBLEScan->setInterval(1349);
  pBLEScan->setWindow(449);
  pBLEScan->setActiveScan(true);
  pBLEScan->start(60, false);
}

int getLocation() {
  uint16_t red, green, blue, cls;
  int maxi = 0, midi = 0, mini = 0, loc = 0;
  tcs.setInterrupt(false);
  delay(60);
  tcs.getRawData(&red, &green, &blue, &cls);
  tcs.setInterrupt(true);
  uint32_t sum = cls;
  float r, g, b;
  r = red; r /= sum; r *= 256;
  g = green; g /= sum; g *= 256;
  b = blue; b /= sum; b *= 256;
  Serial.printf("Red: %f, Green: %f, Blue: %f\n", r, g, b);

    int colors[] = {r, g, b};
  if (r < g) {
    if (r < b) {
      mini = 0;
      if (b < g) {
        midi = 2;
        maxi = 1;
      }else{
        midi = 1;
        maxi = 2;
      }
    }else{
      mini = 2;
      midi = 0;
      maxi = 1;
    }
  }else{
    if (r < b) {
      mini = 1;
      midi = 0;
      maxi = 2;
    }else{
      maxi = 0;
      if (b < g) {
        midi = 1;
        mini = 2;
      }else{
        midi = 2;
        mini = 1;
      }
    }
  }
  //Not perfect dependant on unknown colours
  if (colors[maxi] - colors[midi] > 10) {
    if (maxi == 0){
      //Red
      r = 255; g = 0; b = 0;
      loc = 1;
    }else if (maxi == 1) {
      //Green
      r = 0; g = 255; b = 0;
      loc = 2;
    }else{
      //Blue
      r = 0; g = 0; b = 255;
      loc = 4;
    }
  }else{
    //White
    r = 100; g = 240; b = 240;
    loc = 3;
  }
  strip.setPixelColor(0, r, g, b);
  strip.show();
  return loc;
}

void setup() {
  Serial.begin(115200);
  delay(3000);
  
  adc_power_on();
  //WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0);
  
  if (!SPIFFS.begin(true)) {
    Serial.println("SPIFFS Mount Failed");
    delay(1000);
    goToDeepSleep();
  }

  //writeFile(SPIFFS, "/wifi_credentials.txt", "");
  numLines = readFile(SPIFFS, "/wifi_credentials.txt");
  Serial.printf("Number of lines: %d\n", numLines);
  /*
  if (!WiFi.config(local_IP, gateway, subnet, primaryDNS, secondaryDNS)) {
    Serial.println("STA Failed to configure");
    delay(1000);
    goToDeepSleep();
  }
  */

  BLEDevice::init(deviceName);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new CustomServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new CustomCharacteristicCallbacks());
  pService->start();
  pServer->getAdvertising()->start();
  //delay(10000);//10 seconds to update wifi details
  delay(1000);
  pServer->getAdvertising()->stop();
  pService->stop();
  BLEDevice::deinit(false);
  
  pinMode(enA, OUTPUT);
  pinMode(enB, OUTPUT);
  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);
  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);

  ledcSetup(0, 8000, 8);
  ledcAttachPin(enA, 0);
  ledcSetup(1, 8000, 8);
  ledcAttachPin(enB, 1);

  digitalWrite(in1, LOW);
  digitalWrite(in2, LOW);
  ledcWrite(0, 0);//left
  digitalWrite(in3, LOW);
  digitalWrite(in4, LOW);
  ledcWrite(1, 0);//right

  if (!tcs.begin()) {
    Serial.println(F("TCS34725 initialisation failed"));
    delay(1000);
    goToDeepSleep();
  }
  strip.begin();
  strip.show();
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);

  dict_data["id"] = 1;//buggy 1

  location = getLocation();
  if (location == 0) {
    goToDeepSleep();
  } else {
    dict_data["location"] = location;
  }
}

void onMotor(bool on = true) {
  if (on) {
    digitalWrite(in1, HIGH);
    digitalWrite(in2, LOW);
    digitalWrite(in3, LOW);
    digitalWrite(in4, HIGH);
  } else {
    digitalWrite(in1, LOW);
    digitalWrite(in2, LOW);
    ledcWrite(0, 160);//left
    digitalWrite(in3, LOW);
    digitalWrite(in4, LOW);
    ledcWrite(1, 160);//right
  }
}

void loop() {
  if (!pendingRequest) {
    onMotor(false);
    //serializeJsonPretty(dict_data, Serial);
    array_str = sendHttpRequest("start_buggy_update_buggy_location.php", dict_data);
    if (array_str && !array_str[0]) {
      Serial.println("Check your connection (local)");
    } else {
      deserializeJson(array, array_str);
      const char* response = array["Response"];
      Serial.println(response);
      if (strcmp(response, "No request") == 0) {
        delay(5000);//Wait for 5 seconds & check again
      } else if (strcmp(response, "New/Pending request") == 0) {
        destination = atoi(array["Destination"]);
        clientDevice = (const char*)array["Device"];
        if (destination != location) {
          pendingRequest = true;
        } else {
          if (clientDevice.compare("reserved") != 0) {
            released = false;
            deviceFound = false;
            pthread_create(&client_ble_thread, NULL, scanForServer, NULL);
            for (int i = 0; i < 30; i++){
              if (deviceFound) {
                deviceFound = false;
                connectToServer();
              }
              if (released) {
                dict_data["destination"] = destination;
                array_str = sendHttpRequest("update_buggy_destination.php", dict_data);
                if (array_str && !array_str[0]) {
                  Serial.println("Check your connection (local)");
                } else {
                  deserializeJson(array, array_str);
                  const char* response = array["Response"];
                  Serial.println(response);
                }
                goToDeepSleep();
              }
              delay(1000);
            }
            if (released) {
              goToDeepSleep();
            }
          }
          //Timeout
          array_str = sendHttpRequest("reset_buggy.php", dict_data);
          if (array_str && !array_str[0]) {
            Serial.println("Check your connection (local)");
          } else {
            deserializeJson(array, array_str);
            const char* response = array["Response"];
            Serial.println(response);
            delay(5000);//Wait for 5 seconds & check again
          }
        }
      }
    }
  } else {
    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);
    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);
    duration = pulseIn(echoPin, HIGH, 600);
    distance = duration / 58.2;
    if (distance < 10 && distance != 0) {// or distance > 1000
      onMotor(false);
      Serial.print("Obstructed: ");
      Serial.println(distance);
      if (debug) {
        delay(1000);
      } else {
        delay(100);
      }
      return;
    }

    left_sensor_state = analogRead(leftSensorPin);
    right_sensor_state = analogRead(rightSensorPin);
    if (debug) {
      Serial.print("Left: ");
      Serial.println(left_sensor_state);
      Serial.print("Right: ");
      Serial.println(right_sensor_state);
    }

    if (right_sensor_state > threshold && left_sensor_state < threshold) {
      Serial.println("Turning right");
      onMotor();
      ledcWrite(0, turn_speed);
      ledcWrite(1, vSpeedB);
      //digitalWrite(in3, LOW);
      //digitalWrite(in4, LOW);
      reachedCheckpoint = false;
    } else if (right_sensor_state < threshold && left_sensor_state > threshold) {
      Serial.println("Turning left");
      onMotor();
      //digitalWrite(in1, LOW);
      //digitalWrite(in2, LOW);
      ledcWrite(0, vSpeedB);
      ledcWrite(1, turn_speed);
      reachedCheckpoint = false;
    } else if (right_sensor_state > threshold && left_sensor_state > threshold) {
      Serial.println("Moving forward");
      onMotor();
      ledcWrite(0, vSpeed);
      ledcWrite(1, vSpeed);
      reachedCheckpoint = false;
    } else if (right_sensor_state < threshold && left_sensor_state < threshold) {
      Serial.println("Checkpoint");
      if (!reachedCheckpoint) {
        onMotor(false);
        location = getLocation();
        dict_data["location"] = location;
        Serial.println(location);
        if (!debug) {
          if (location == 0 || location == 5) {
            reachedCheckpoint = true;
            return;
          }
        }
        array_str = sendHttpRequest("start_buggy_update_buggy_location.php", dict_data);
        if (array_str && !array_str[0]) {
          Serial.println("Check your connection (local)");
        } else {
          deserializeJson(array, array_str);
          const char* response = array["Response"];
          Serial.println(response);
          if (strcmp(response, "No request") == 0) {
            pendingRequest = false;
          } else if (strcmp(response, "New/Pending request") == 0) {
            destination = atoi(array["Destination"]);
            clientDevice = (const char*)array["Device"];
            if (destination != location) {
              pendingRequest = true;
              reachedCheckpoint = true;
              delay(2000);
            } else {
              if (clientDevice.compare("reserved") != 0) {
                released = false;
                pthread_create(&client_ble_thread, NULL, scanForServer, NULL);
                for (int i = 0; i < 30; i++){
                  if (deviceFound) {
                    deviceFound = false;
                    connectToServer();
                  }
                  if (released) {
                    dict_data["destination"] = destination;
                    array_str = sendHttpRequest("update_buggy_destination.php", dict_data);
                    if (array_str && !array_str[0]) {
                      Serial.println("Check your connection (local)");
                    } else {
                      deserializeJson(array, array_str);
                      const char* response = array["Response"];
                      Serial.println(response);
                    }
                    goToDeepSleep();
                  }
                  delay(1000);
                }
                if (released) {
                  goToDeepSleep();
                }
              }
              //Timeout
              array_str = sendHttpRequest("reset_buggy.php", dict_data);
              if (array_str && !array_str[0]) {
                Serial.println("Check your connection (local)");
              } else {
                deserializeJson(array, array_str);
                const char* response = array["Response"];
                Serial.println(response);
                if (strcmp(response, "Success") == 0) {
                  pendingRequest = false;
                  delay(5000);//Wait for 5 seconds & check again
                }
              }
            }
          }
        }
      } else {
        onMotor();
        ledcWrite(0, vSpeed);
        ledcWrite(1, vSpeed);
      }
    }
    /*
    if (debug) {
      delay(1000);
    } else {
      delay(25);
    }
    */
  }
}

class ClientEncrypt {
  private:
    char* key;

    void pad(unsigned char *s, int n, int c) {
      unsigned char *p = s + n - strlen((char*)s);
      strcpy((char*)p, (char*)s);
      p--;
      while (p >= s) {
        p[0] = c;
        p--;
      }
    }
  public:
    ClientEncrypt() {
      char* keystr = "UiJFzKBvOeHbnjM2U8qc+g==";
      size_t len = 16;
      key = reinterpret_cast<char*>(base64_decode((const unsigned char*)keystr, strlen(keystr), &len));
    }
    String encrypt(StaticJsonDocument<200>& dict_data) {
      char iv[17] = {0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00};
      char ivf[17] = {0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00};
      //https://twitter.com/wolfeidau/status/692276155722891265
      //Not sure why now gives empty string (because of partition?)
      /*
        for (int i = 0; i < 16; i++) {
        iv[i] = (char)(*(volatile uint8_t*)0x3FF20E44);
        ivf[i] = iv[i];
        }
      */

      mbedtls_aes_context aes;
      mbedtls_aes_init(&aes);
      mbedtls_aes_setkey_enc(&aes, (const unsigned char*)key, strlen(key) * 8);
      char data_bytes[200];
      serializeJson(dict_data, data_bytes);
      int leng = strlen(data_bytes);
      int num = 16 - (leng % 16);
      int total_length = leng + num;
      for (int i = leng; i <= total_length; i++) {
        data_bytes[i] = 0x00;
      }

      unsigned char text[300];//Must be large enough, cannot be total_length
      mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_ENCRYPT, total_length, (unsigned char*)iv, (unsigned char*)data_bytes, text);
      mbedtls_aes_free(&aes);
      text[total_length] = 0x00;

      StaticJsonDocument<300> json;//Must be large enough, cannot be total_length
      size_t len = 16;
      json["iv"] = base64_encode((const unsigned char*)ivf, strlen(ivf), &len);
      size_t len2 = total_length;
      json["body"] = base64_encode((const unsigned char*)text, strlen((char*)text), &len2);
      String result;
      serializeJson(json, result);
      return result;
    }

    unsigned char* decrypt(String text) {
      StaticJsonDocument<300> array;
      deserializeJson(array, text);
      int leng = atoi(array["length"]);
      int upleng = leng + 16 - (leng % 16);
      const char* iv = array["iv"];
      const char* body = array["body"];

      mbedtls_aes_context aes;
      mbedtls_aes_init(&aes);
      mbedtls_aes_setkey_enc(&aes, (const unsigned char*)key, strlen(key) * 8);
      size_t len = 16;
      unsigned char * decoded_iv = base64_decode((const unsigned char *)iv, strlen(iv), &len);
      size_t len2 = upleng;
      unsigned char * decoded_body = base64_decode((const unsigned char *)body, strlen(body), &len2);
      static unsigned char content[200];
      mbedtls_aes_crypt_cbc(&aes, MBEDTLS_AES_DECRYPT, upleng, decoded_iv, decoded_body, content);
      mbedtls_aes_free(&aes);

      content[leng] = '\0';
      return content;
    }
};

unsigned char* sendHttpRequest(String scriptName, StaticJsonDocument<200>& dict_data2) {
  if (numLines == 0) {
    return (unsigned char *)"";
  }

  if (wifiMulti.run() != WL_CONNECTED || appended || update) {
    appended = false;
    wifiMulti.addAP("Redmi", "markwong1997");
    std::string concat_credentials = "";
    for (int i = 0; i < numLines - 1; i += 2) {
      //Last line is '\n'
      wifiMulti.addAP(credentials[i].c_str(), credentials[i + 1].c_str());
      concat_credentials += credentials[i] + "\n" + credentials[i + 1] + "\n";
    }
    if (update) {
      writeFile(SPIFFS, "/wifi_credentials.txt", concat_credentials.c_str());
      update = false;
    }
  }
  int i = 10;
  while (wifiMulti.run() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    i--;
    if (i == 0) {
      return (unsigned char *)"";
    }
  }
  
  HTTPClient http;
  http.begin("http://markelili.000webhostapp.com/Connect Together/" + scriptName);
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");
  ClientEncrypt clientEncrypt;
  String prepend = "data=";
  String result = clientEncrypt.encrypt(dict_data2);
  prepend.concat(result);
  int httpResponseCode = http.POST(prepend);

  String content = http.getString();
  if (httpResponseCode > 0) {
    Serial.println(scriptName);
    unsigned char* array_str = clientEncrypt.decrypt(content);
    return array_str;
  } else {
    return (unsigned char *)"";
  }
  http.end();
}

void goToDeepSleep() {
  strip.setPixelColor(0, 0, 0, 0);
  strip.show();
  tcs.disable();

  adc_power_off();

  WiFi.disconnect(true);
  esp_wifi_stop();
  WiFi.mode(WIFI_OFF);
  //esp_wifi_deinit();

  btStop();
  BLEDevice::deinit(true);

  esp_sleep_enable_timer_wakeup(20 * 1000000);
  esp_deep_sleep_start();
}
