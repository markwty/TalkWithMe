//Sensors & BLE
#include "SPI.h"
#include "Wire.h"
#include "Adafruit_GFX.h"
#include "Adafruit_SSD1306.h"
#include "MAX30100_PulseOximeter.h"
#include "Adafruit_MLX90614.h"
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

//ulp
#include "esp_sleep.h"
#include "driver/rtc_io.h"
#include "soc/sens_reg.h"
#include "esp32/ulp.h"
#include "ulp_main.h"
#include "ulptool.h"

struct tm timeinfo;
const int buzzer = 22, gsrSensor = 35, switchPin = 33, enterPin = 27, restartPin = 32, batteryPin = 39, groundPin = 26;
const int threshold = 20;
int destination = 0, state = 0;
Adafruit_SSD1306 display(128, 64, &Wire, -1);//-1 = Not used
PulseOximeter pox;
Adafruit_MLX90614 mlx = Adafruit_MLX90614();
boolean displaying = false, timeInit = false, needUpdate = false, ended = false, poxStarted = false, beatDetected = false;
int bpm;
float temperature = 0, gsr = 0, blood_oxygen = 0;
const unsigned char battery [] PROGMEM = {
  0xff, 0xff, 0x00, 0xc0, 0x01, 0x00, 0xbb, 0x6d, 0x80, 0xbb, 0x6d, 0xc0, 0xbb, 0x6d, 0xc0, 0xbb, 
  0x6d, 0xc0, 0xbb, 0x6d, 0xc0, 0xbb, 0x6d, 0x80, 0xc0, 0x01, 0x00, 0xff, 0xff, 0x00
};

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"
std::string deviceName = "Connect Together";//don't use "reserved"
BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false;
String txValue = "";
char txBuff[20];
uint8_t ctxValue[20];
std::string rxValue = "", accumrxValue = "";

boolean appended = false, update = false;
int numLines = 0;
#define credentials_length 10
std::string credentials[credentials_length] = "";
std::string ssid = "", password = "";

WiFiMulti wifiMulti;

extern const uint8_t ulp_main_bin_start[] asm("_binary_ulp_main_bin_start");
extern const uint8_t ulp_main_bin_end[]   asm("_binary_ulp_main_bin_end");
static void init_ulp_program();
static RTC_NOINIT_ATTR int total_steps;

boolean debug = false;

//19(SDA), 23(SCL) reserve for screen & infared-red thermometer
//33(SDA), 34(SCL) reservce for pulse-oximeter
//2(SDA), 15(SCL) reserve for accelerometer (need rtc for ulp)
//12 (need to pull low during boot), 2 cannot be attached during flash, pull 0 to ground to force esp32 into flash mode
//ADC2 pins may be unusable as analog pins (but still try not to use, may have other wifi functions) when wifi is running
//32, 27 used for touch
//35 used for gsr sensor
//21 used for buzzer

static void init_ulp_program(){
  ulp_set_wakeup_period(0, 10000 * 1000);
  esp_err_t err = ulptool_load_binary(0, ulp_main_bin_start, (ulp_main_bin_end - ulp_main_bin_start) / sizeof(uint32_t));
  ESP_ERROR_CHECK(err);
    
  ESP_ERROR_CHECK(rtc_gpio_init(GPIO_NUM_15));
  ESP_ERROR_CHECK(rtc_gpio_set_direction(GPIO_NUM_15, RTC_GPIO_MODE_INPUT_ONLY));
  ESP_ERROR_CHECK(rtc_gpio_init(GPIO_NUM_2));
  ESP_ERROR_CHECK(rtc_gpio_set_direction(GPIO_NUM_2, RTC_GPIO_MODE_INPUT_ONLY));

  //esp_sleep_pd_config(ESP_PD_DOMAIN_RTC_SLOW_MEM, ESP_PD_OPTION_ON);
  //esp_deep_sleep_pd_config(ESP_PD_DOMAIN_RTC_PERIPH, ESP_PD_OPTION_AUTO);
  //REG_SET_FIELD(SENS_ULP_CP_SLEEP_CYC0_REG, SENS_SLEEP_CYCLES_S0, 1500000);
}

int readFile(fs::FS &fs, const char* path){
  if (debug) {
    Serial.printf("Reading file: %s\r\n", path);
  }
  File file = fs.open(path);
  if (!file || file.isDirectory()) {
    if (debug) {
      Serial.println("Failed to open file for reading");
    }
    return 0;
  }
  int i = 0;
  char buffer[200];//max length of line
  while (file.available()) {
    int l = file.readBytesUntil('\n', buffer, sizeof(buffer));
    buffer[l] = 0;
    credentials[i++] = buffer;
    if(i == credentials_length - 1){
      break;
    }
  }
  file.close();
  return i + 1;
}

void appendFile(fs::FS &fs, const char* path, const char* message){
  if (debug) {
    Serial.printf("Appending to file: %s\r\n", path);
  }
  File file = fs.open(path, FILE_APPEND);
  if (!file) {
    if (debug) {
      Serial.println("Failed to open file for appending");
    }
    return;
  }
  if (file.print(message)) {
    if (debug) {
      Serial.println("File appended");
    }
  }else{
    if (debug) {
      Serial.println("Append failed");
    }
  }
  file.close();
}

void writeFile(fs::FS &fs, const char* path, const char* message){
  if (debug) {
    Serial.printf("Writing to file: %s\r\n", path);
  }
  File file = fs.open(path, FILE_WRITE);
  if (!file) {
    if (debug) {
      Serial.println("Failed to open file for writing");
    }
    return;
  }
  if (file.print(message)) {
    if (debug) {
      Serial.println("File written");
    }
  }else{
    if (debug) {
      Serial.println("Write failed");
    }
  }
  file.close();
}

void* syncTime(void* ptr) {
  if (numLines == 0) {
    return ptr;
  }
  WiFi.mode(WIFI_STA);
  if (wifiMulti.run() != WL_CONNECTED || appended || update){
    appended = false;
    for (int i = 0; i < numLines - 1; i+=2) {
      //Last line is '\n'
      wifiMulti.addAP(credentials[i].c_str(), credentials[i + 1].c_str());
    }
    int i = 2;
    while (wifiMulti.run() != WL_CONNECTED) {
      delay(500);
      i--;
      if (i == 0) {
        return ptr;
      }
    }
  }
  configTime(0, 28800, "pool.ntp.org");

  if(!getLocalTime(&timeinfo)){
    Serial.println("Failed to obtain time");
    return ptr;
  }
  timeInit = true;
  return ptr;
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
    if (debug) {
      for (int i = 0; i < rxValue.length(); i++) {
        Serial.print(rxValue[i]);
      }
      Serial.println();
    }
    if (rxValue.length() > 0) {
      if (rxValue.at(0) == 195 && rxValue.at(1) == 164) {//4294967295
        if (rxValue.at(rxValue.length() - 1) == 188 && rxValue.at(rxValue.length() - 2) == 195) {
          accumrxValue = rxValue.substr(2, rxValue.length() - 4);
          process(accumrxValue);
        }else{
          accumrxValue = rxValue.substr(2);
        }
      }else if (rxValue.at(rxValue.length() - 1) == 188 && rxValue.at(rxValue.length() - 2) == 195) {
        accumrxValue += rxValue.substr(0, rxValue.length() - 2);
        process(accumrxValue);
      }else{
        accumrxValue += rxValue;
      }
    }
  }

  void process(std::string accumrxValue){
    if (debug) {
      for (int i = 0; i < accumrxValue.length(); i++) {
        Serial.print(accumrxValue[i]);
      }
      Serial.println();
    }
    std::size_t pos = accumrxValue.find("::");
    if (pos != -1) {
      std::string field = accumrxValue.substr(0, pos);
      std::string value = accumrxValue.substr(pos + 2);
      if (field.compare("ssid") == 0) {
        ssid = value;
      }else if (field.compare("password") == 0) {
        password = value;
        if(ssid.compare("") != 0){
          boolean through = false;
          for (int i = 0; i < numLines - 1; i+=2) {
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
              numLines+=2;
              appended = true;
            }else{
              //numLines = credentials_length + 1
              credentials[credentials_length - 3] = ssid;
              credentials[credentials_length - 2] = password;
              update = true;
            }
          }
        }
        ssid = "";
        password = "";
      }else if (field.compare("buggy") == 0) {
        if (deviceName.compare(value) == 0) {
          if (deviceConnected) {
            txValue = "";
            txValue.concat(deviceName.c_str());
            txValue.concat("::");
            txValue.concat(destination);
            txValue.toCharArray(txBuff, 20);
            memcpy(ctxValue, txBuff, 20);
            pTxCharacteristic->setValue(ctxValue, 20);
            pTxCharacteristic->notify();
            if (debug) {
              Serial.println("notifying"); 
            }
          }
        }
      }
    }
  }
};

void restart(){
  ended = true;
  display.clearDisplay();
  display.display();
  if (poxStarted) {
    pox.off();
    pox.shutdown();
  }
  esp_sleep_enable_timer_wakeup(1 * 1000000);
  esp_deep_sleep_start();
}

void setup() {
  Serial.begin(115200);
  esp_sleep_wakeup_cause_t cause = esp_sleep_get_wakeup_cause();
  if (cause == 0) {
    total_steps = 0;
    init_ulp_program();
  }else if (cause == ESP_SLEEP_WAKEUP_TOUCHPAD) {
    total_steps += (uint16_t)ulp_steps;
    ulp_steps = 0;
    init_ulp_program();
  }
  ESP_ERROR_CHECK(ulp_run((&ulp_entry - RTC_SLOW_MEM) / sizeof(uint32_t)));
  
  adc_power_on();
  if(!SPIFFS.begin(true)){
    Serial.println("SPIFFS Mount Failed");
    delay(1000);
    goToDeepSleep();
  }

  //RTC memory cannot be retain after power restart/pressing reset button, so SPIFFS is used instead but there is limited write cycles but should be sufficient
  //RTC_DATA_ATTR, RTC_NOINIT_ATTR
  //writeFile(SPIFFS, "/wifi_credentials.txt", "");
  numLines = readFile(SPIFFS, "/wifi_credentials.txt");
  if (debug) {
    Serial.print("Number of lines:");
    Serial.println(numLines);
  }

  pthread_t timeThread;
  pthread_create(&timeThread, NULL, syncTime, NULL);
  
  if (!Wire.begin(19, 23, 100000)){
    Serial.println("Failed to initialize Wire");
    delay(1000);
    goToDeepSleep();
  }
  if (!Wire1.begin(16, 17, 100000)){
    Serial.println("Failed to initialize Wire1");
    delay(1000);
    goToDeepSleep();
  }
  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C, true, false)) {
    Serial.println(F("SSD1306 initialisation failed"));
    delay(1000);
    goToDeepSleep();
  }else{
    if (debug) {
      Serial.println("SSD1306 initialisation success");
    }
    display.clearDisplay();
    display.display();
  }
  pinMode(buzzer, OUTPUT);
  pinMode(batteryPin, INPUT);
  pinMode(groundPin, OUTPUT);
  digitalWrite(groundPin, LOW);

  touchAttachInterrupt(restartPin, restart, threshold);

  BLEDevice::init(deviceName);
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new CustomServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  pTxCharacteristic = pService->createCharacteristic(
                    CHARACTERISTIC_UUID_TX,
                    BLECharacteristic::PROPERTY_NOTIFY
                  );
  pTxCharacteristic->addDescriptor(new BLE2902());

  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID_RX,
                      BLECharacteristic::PROPERTY_WRITE
                    );
  pRxCharacteristic->setCallbacks(new CustomCharacteristicCallbacks());
  pService->start();
  pServer->getAdvertising()->start();
}

void init_screen(){//don't use init()
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  display.fillRect(0, 0, 128, 64, SSD1306_BLACK);
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  if (timeInit && getLocalTime(&timeinfo)) {
    display.setCursor(0,0);
    char timeStringBuff[50];
    strftime(timeStringBuff, sizeof(timeStringBuff), "%a, %d %b, %H:%M:%S", &timeinfo);
    display.print(timeStringBuff);
  }
  display.setCursor(0,11);
  display.print("Steps:");
  display.print(total_steps);
  float voltage = analogRead(batteryPin)/4095.0 * 7.06;
  int percentage;
  if (voltage > 4.19){
    percentage = 100;
  } else if (voltage <= 3.50) {
    percentage = 0;
  } else {
    percentage = 2808.3808 * pow(voltage, 4) - 43560.9157 * pow(voltage, 3) + 252848.5888 * pow(voltage, 2) - 650767.4615 * voltage + 626532.5703;
  }
  display.drawBitmap(80, 10, battery, 18, 10, WHITE);
  if (percentage == 0 or percentage == 100) {
    percentage = 100;
  } else {
    if (percentage > 75) {
      //Do nothing (indicate as full bar)
      //display.fillRect(80 + 1, 12, 13, 6, BLACK);//empty battery level
    }else if (percentage > 50) {
      display.fillRect(80 + 12, 12, 2, 6, BLACK);
    }else if (percentage > 25) {
      display.fillRect(80 + 9, 12, 5, 6, BLACK);
    }else {
      display.fillRect(80 + 5, 12, 9, 6, BLACK);
    }
  }
  //display.print(" Battery:");
  display.setCursor(100, 11);
  if (percentage == 100) {
    display.print(" -");
  } else {
    display.print(percentage);
    display.print("%");
  }

  display.setTextSize(2);
  display.setCursor(0,22);
  display.print("Sensors");
  display.setCursor(0,43);
  display.print("Move");
  display.fillRect(0, 22, 128, 21, SSD1306_INVERSE);
  display.display();
  displaying = false;
}

void toggle(){
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  display.fillRect(0, 22, 128, 64, SSD1306_INVERSE);
  display.display();
  displaying = false;
}

void updateSteps(){
  if((uint16_t)ulp_steps == 0){
    return;
  }
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  display.fillRect(0, 11, 128, 11, SSD1306_BLACK);
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  total_steps += (uint16_t)ulp_steps;
  ulp_steps = 0;
  display.setCursor(0,11);
  display.print("Steps:");
  display.print(total_steps);
  float voltage = analogRead(batteryPin)/4095.0 * 7.06;
  int percentage;
  if (voltage > 4.19){
    percentage = 100;
  } else if (voltage <= 3.50) {
    percentage = 0;
  } else {
    percentage = 2808.3808 * pow(voltage, 4) - 43560.9157 * pow(voltage, 3) + 252848.5888 * pow(voltage, 2) - 650767.4615 * voltage + 626532.5703;
  }
  display.drawBitmap(80, 10, battery, 18, 10, WHITE);
  if (percentage == 0 or percentage == 100) {
    percentage = 100;
  } else {
    if (percentage > 75) {
      //Do nothing (indicate as full bar)
    } else if (percentage > 50) {
      display.fillRect(80 + 12, 12, 2, 6, BLACK);
    } else if (percentage > 25) {
      display.fillRect(80 + 9, 12, 5, 6, BLACK);
    } else {
      display.fillRect(80 + 5, 12, 9, 6, BLACK);
    }
  }
  display.setCursor(100, 11);
  if (percentage == 100) {
    display.print("-");
  } else {
    display.print(percentage);
    display.print("%");
  }
  display.display();
  displaying = false;
}

void updateTime(){
  if (displaying || !timeInit) {
    return;
  }
  displaying = true;
  if (getLocalTime(&timeinfo)) {
    display.fillRect(0, 0, 128, 10, SSD1306_BLACK);
    display.setCursor(0,0);
    display.setTextSize(1);
    char timeStringBuff[50];
    strftime(timeStringBuff, sizeof(timeStringBuff), "%a, %d %b, %H:%M:%S", &timeinfo);
    display.print(timeStringBuff);
    display.display();
  }
  displaying = false;
}

void init_screen_move1(){//don't use init()
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  display.fillRect(0, 0, 128, 64, SSD1306_BLACK);
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0,0);
  display.print("Your current location");
  display.setCursor(0,22);
  display.print("location 1");
  display.setCursor(0,32);
  display.print("location 2");
  display.setCursor(0,42);
  display.print("location 3");
  display.setCursor(0,52);
  display.print("location 4");
  display.fillRect(0, 22, 128, 10, SSD1306_INVERSE);
  display.display();
  displaying = false;
}

void init_screen_move2(int option){//don't use init()
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  display.fillRect(0, 0, 128, 64, SSD1306_BLACK);
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(1);
  display.setCursor(0,0);
  display.print("Your destination");
  int y_start = 22;
  for (int i = 0; i < 4; i++) {
    if (i != option) {
      display.setCursor(0, y_start);
      display.print("location ");
      display.print(i + 1);
      y_start += 14;
    }
  }
  display.fillRect(0, 22, 128, 14, SSD1306_INVERSE);
  display.display();
  displaying = false;
}

void print_message(const char* message){
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  display.fillRect(0, 0, 128, 64, SSD1306_BLACK);
  display.setTextColor(SSD1306_WHITE);
  display.setTextSize(2);
  display.setCursor(0,16);
  display.print(message);
  display.display();
  displaying = false;
}

int toggle_move(int option, int numOptions){
  //option starts from 0
  while (displaying) {
    delay(1000);
  }
  displaying = true;
  int stepSize = (64 - 22)/numOptions;
  int index = 22 + option * stepSize;
  if (option == numOptions - 1) {
    display.fillRect(0, index, 128, stepSize, SSD1306_INVERSE);
    display.fillRect(0, 22, 128, stepSize, SSD1306_INVERSE);
    display.display();
    displaying = false;
    return 0;
  }else{
    display.fillRect(0, index, 128, stepSize * 2, SSD1306_INVERSE);
    display.display();
    displaying = false;
    return option + 1;
  }
}

boolean filteredTouchRead(int pin_no) {
  int positiveTouchCounter = 0;
  while (touchRead(pin_no) < threshold) {
    positiveTouchCounter++;
    if (positiveTouchCounter == 20) {
      return true;
    }
  }
  return false;
}

void loop(){
  int option = 0, cycle = 5;
  long start;
  boolean touched = false, filteredTouch;

  init_screen();
  start = millis();
  while (state == 0) {
    filteredTouch = filteredTouchRead(switchPin);
    if (!touched && filteredTouch) {
      touched = true;
      option = 1 - option;
      toggle();
    }else if (touched && !filteredTouch) {
      touched = false;
    }
    filteredTouch = filteredTouchRead(enterPin);
    if (filteredTouch) {
      state = option + 1;
    }
    delay(200);
    updateSteps();
    cycle--;
    if (cycle == 0) {
      cycle = 5;
      updateTime();
    }
    if (millis() > start + 20000) {
      goToDeepSleep();
    }
  }

  if (state == 1) {
    int sub_state = 0, counter = 0;
    pthread_t pulseThread;
    start = millis();
    refresh(0);
    pthread_create(&pulseThread, NULL, updatePulseOximeter, NULL);
    while (true) {
      int milli = millis();
      updateValues();
      counter++;
      if(counter == 30000){
        counter = 0;
        gsr = analogRead(gsrSensor)/4095.0 * 100;
        refresh(2);
      }
      if (beatDetected) {
        refresh(5);
        beatDetected = false;
      }
            
      if (milli > start + 5000) {
        if (sub_state == 0) {
          mlx.begin(&Wire);
          temperature = mlx.readObjectTempC();
          sub_state = 1;
          for (int i=0;i<5;i++) {
            digitalWrite(buzzer, HIGH);
            delay(1);
            digitalWrite(buzzer, LOW);
            delay(1);
          }
          refresh(3);
        }
      }
      if (milli > start + 20000) {
        refresh(4);
        StaticJsonDocument<200> dict_data;
        StaticJsonDocument<200> array;
        dict_data["device"] = deviceName;
        dict_data["bpm"] = floor(10*bpm)/10;
        dict_data["temperature"] = floor(10*temperature)/10;
        dict_data["moisture"] = floor(10*gsr)/10;
        //serializeJsonPretty(dict_data, Serial);
        unsigned char* array_str = sendHttpRequest("update_sensors.php", dict_data);
        if (array_str && !array_str[0]) {
          Serial.println("Check your connection (local)");
        }else{
          deserializeJson(array, array_str);
          const char* response = array["Response"];
          Serial.println(response);
        }
        delay(5000);
        goToDeepSleep();
      }
    }
  }else if (state == 2) {
    int call_location;
    option = 0;
    init_screen_move1();
    while (state == 2) {
      filteredTouch = filteredTouchRead(switchPin);
      if (!touched && filteredTouch) {
        touched = true;
        option = toggle_move(option, 4);
      }else if (touched && !filteredTouch) {
        touched = false;
      }
      filteredTouch = filteredTouchRead(enterPin);
      if (filteredTouch) {
        call_location = option + 1;
        state = 3;
      }
      delay(200);
    }
    init_screen_move2(option);
    int prev_option = option;
    option = 0;
    while (state == 3) {
      filteredTouch = filteredTouchRead(switchPin);
      if (!touched && filteredTouch) {
        touched = true;
        option = toggle_move(option, 3);
      }else if (touched && !filteredTouch) {
        touched = false;
      }
      filteredTouch = filteredTouchRead(enterPin);
      if (filteredTouch) {
        if (option >= prev_option) {
          destination = option + 2;
        } else {
          destination = option + 1;
        }
        state = 4;
      }
      delay(200);
    }

    print_message("Waiting\nfor reply");
    
    bool through = false;
    StaticJsonDocument<200> dict_data;
    StaticJsonDocument<200> array;
    unsigned char* array_str;
    while (!through) {
      dict_data["device"] = deviceName;
      dict_data["location"] = call_location;
      array_str = sendHttpRequest("simple_buggy_call.php", dict_data);
      if (array_str && !array_str[0]) {
        Serial.println("Check your connection (local)");
      }else{
        deserializeJson(array, array_str);
        const char* response = array["Response"];
        Serial.println(response);
        if (strcmp(response, "Success") == 0) {
          char result[40];
          strcpy(result, "Buggy ");
          strcat(result, (const char*)array["ID"]);
          strcat(result, "\nis on \nthe way");
          print_message(result);
          through = true;
        }
      }
    }

    through = false;
    while (!through) {
      array_str = sendHttpRequest("get_buggy_location.php", dict_data);
      if (array_str && !array_str[0]) {
        Serial.println("Check your connection (local)");
      }else{
        deserializeJson(array, array_str);
        const char* response = array["Response"];
        Serial.println(response);
        if (strcmp(response, "Success") == 0) {
          const char* vehicle_location = array["Location"];
          if (atoi(vehicle_location) == call_location) {
            print_message("Buggy\nis here");
            while(true){
              array_str = sendHttpRequest("check_buggy_left.php", dict_data);
              if (array_str && !array_str[0]) {
                Serial.println("Check your connection (local)");
              }else{
                deserializeJson(array, array_str);
                const char* response = array["Response"];
                if (strcmp(response, "Buggy left") == 0) {
                  print_message("Buggy left");
                  delay(20000);//Wait 20 seconds
                  goToDeepSleep();
                }
              }
            }
          }else{
            char result[40];
            strcpy(result, "Buggy\nis at\nlocation ");
            strcat(result,(const char*)vehicle_location);
            print_message(result);
          }
        }
      }
    }
  }
}

void beatDetectedCallback() {
  beatDetected = true;
}

void* updatePulseOximeter(void* ptr) {
  if(!pox.begin(&Wire1)) {
    Serial.println(F("Pulse-oximeter initialisation failed"));
    return ptr;
  }else{
    poxStarted = true;
    if (debug) {
      Serial.println("Pulse-oximeter initialisation success");
    }
  }
  pox.setOnBeatDetectedCallback(beatDetectedCallback);
  int tsLastReport = 0;
  while(true){
    pox.update();
    if (ended) {
      break;
    }
    if (millis() - tsLastReport > 1000) {
      int heartRate = pox.getHeartRate();
      float oxi = pox.getSpO2();
      if (heartRate != 0) {
        bpm = heartRate;
        needUpdate = true;
      }
      if (oxi != 0) {
        blood_oxygen = oxi;
        needUpdate = true;
      }
      tsLastReport = millis();
    }
  }
}

void updateValues(){
  if (needUpdate) {
    refresh(1);
    needUpdate = false;
  }
}

void refresh(int update_type){
  while(displaying){
    if (update_type < 3) {
      return;
    }else{
      delay(1000);
    }
  }
  displaying = true;
  if (update_type == 0) {
    display.fillRect(0, 0, 128, 64, SSD1306_BLACK);
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(0,0);
    display.println("BPM: 0");
    display.print("Blood oxygen: 0.0");
    display.println("%");
    display.setCursor(0,18);
    display.print("Temperature: 0.0");
    display.print((char)223);
    display.println("C");
    display.setCursor(0,27);
    display.print("Skin moisture: 0.0");
    display.println("%");
    display.setCursor(0,40);
    display.println("Put the device 1cm");
    display.println("from your forehead");
    display.println("until a beep is heard");
  }else if (update_type == 1) {
    display.fillRect(0, 0, 50, 8, SSD1306_BLACK);
    display.fillRect(0, 7, 128, 11, SSD1306_BLACK);
    display.setCursor(0,0);
    display.print("BPM: ");
    display.println(bpm);
    display.print("Blood oxygen: ");
    display.print(blood_oxygen, 1);
    display.println("%");
  }else if (update_type == 2) {
    display.fillRect(0, 27, 128, 9, SSD1306_BLACK);
    display.setCursor(0,27);
    display.print("Skin moisture: ");
    display.print(gsr, 1);
    display.println("%");
  }else if (update_type == 3) {
    display.fillRect(0, 18, 128, 9, SSD1306_BLACK);
    display.fillRect(0, 40, 128, 24, SSD1306_BLACK);
    display.setCursor(0,18);
    display.print("Temperature: ");
    display.print(temperature);
    display.print((char)223);
    display.println("C");
  }else if (update_type == 4) {
    ended = true;
    display.fillRect(0, 27, 128, 9, SSD1306_BLACK);
    display.setCursor(0,27);
    display.print("Skin moisture: ");
    display.print(gsr, 1);
    display.println("%");
    display.setCursor(0,40);
    display.println("Data posted");
  }else if (update_type == 5) {
    display.setCursor(50, 0);
    display.write(3);
    display.display();
    delay(200);
    display.fillRect(50, 0, 78, 8, SSD1306_BLACK);
  }
  display.display();
  displaying = false;
}

class ClientEncrypt{
  private:
    char* key;

    void pad(unsigned char *s, int n, int c) {
      unsigned char *p = s + n - strlen((char*)s);
      strcpy((char*)p, (char*)s);
      p--;
      while (p >= s) { p[0] = c; p--; }
    }
  public:
    ClientEncrypt(){
      char* keystr = "<insert key here>";
      size_t len = 16;
      key = reinterpret_cast<char*>(base64_decode((const unsigned char*)keystr, strlen(keystr), &len));
    }
    String encrypt(StaticJsonDocument<200>& dict_data){
      char iv[17] = {0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x00};
      char ivf[17] = {0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x01,0x00};
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
      mbedtls_aes_setkey_enc(&aes, (const unsigned char*)key, strlen(key)*8);
      char data_bytes[200];
      serializeJson(dict_data, data_bytes);
      int leng = strlen(data_bytes);
      int num = 16 - (leng % 16);
      int total_length = leng + num;
      for (int i = leng; i <= total_length; i++){
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
    
    unsigned char* decrypt(String text){
      StaticJsonDocument<300> array;
      deserializeJson(array, text);
      int leng = atoi(array["length"]);
      int upleng = leng + 16 - (leng % 16);
      const char* iv = array["iv"];
      const char* body = array["body"];

      mbedtls_aes_context aes;
      mbedtls_aes_init(&aes);
      mbedtls_aes_setkey_enc(&aes, (const unsigned char*)key, strlen(key)*8);
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

unsigned char* sendHttpRequest(String scriptName, StaticJsonDocument<200>& dict_data){
  if (numLines == 0) {
    return (unsigned char *)"";
  }
  WiFi.mode(WIFI_STA);
  if (wifiMulti.run() != WL_CONNECTED || appended || update){
    appended = false;
    std::string concat_credentials = "";
    for (int i = 0; i < numLines - 1; i+=2) {
      //Last line is '\n'
      wifiMulti.addAP(credentials[i].c_str(), credentials[i + 1].c_str());
      concat_credentials += credentials[i] + "\n" + credentials[i + 1] + "\n";
    }
    if (update) {
      writeFile(SPIFFS, "/wifi_credentials.txt", concat_credentials.c_str());
      update = false;
    }
    int i = 10;
    while (wifiMulti.run() != WL_CONNECTED) {
      delay(500);
      if (debug) {
        Serial.print(".");
      }
      i--;
      if (i == 0) {
        return (unsigned char *)"";
      }
    }
  }
  
  HTTPClient http;
  http.begin("http://markelili.000webhostapp.com/Connect Together/" + scriptName);
  http.addHeader("Content-Type", "application/x-www-form-urlencoded");
  ClientEncrypt clientEncrypt;
  String prepend = "data=";
  String result = clientEncrypt.encrypt(dict_data);
  prepend.concat(result);
  int httpResponseCode = http.POST(prepend);

  String content = http.getString();
  if (httpResponseCode > 0) {
    unsigned char* array_str = clientEncrypt.decrypt(content);
    return array_str;
  }else{
    return (unsigned char *)"";
  }
  http.end();
}

void callback(){
  //placeholder callback function
}

void goToDeepSleep(){
  ended = true;
  display.clearDisplay();
  display.display();
  if (poxStarted) {
    pox.off();
    pox.shutdown();
  }
  
  adc_power_off();

  WiFi.disconnect(true);
  esp_wifi_stop;
  WiFi.mode(WIFI_OFF);
  //esp_wifi_deinit();
  
  btStop();
  BLEDevice::deinit(true);

  ESP_ERROR_CHECK(rtc_gpio_init(GPIO_NUM_26));
  ESP_ERROR_CHECK(rtc_gpio_set_direction(GPIO_NUM_26, RTC_GPIO_MODE_OUTPUT_ONLY));
  ESP_ERROR_CHECK(rtc_gpio_set_level(GPIO_NUM_26, HIGH));
  
  touchAttachInterrupt(restartPin, callback, threshold);
  esp_sleep_enable_touchpad_wakeup();
  
  ESP_ERROR_CHECK(esp_sleep_enable_ulp_wakeup());
  esp_deep_sleep_start();
}
