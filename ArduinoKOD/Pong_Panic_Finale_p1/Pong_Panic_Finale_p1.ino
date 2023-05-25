#include <SPI.h>
#include <Ethernet.h> // Ethernet bibliotek för att hantera anslutningen, ethernet-controllern mm.
#include <FastLED.h>  // Färdigt bibliotek för att kontrollera diverse led-controllers, bl.a. WS2112b

#define NUM_LEDS 25
CRGB leds[NUM_LEDS];

byte mac[] = {0xA8, 0x61, 0x0A, 0xAD, 0x5E, 0x8D};  // MAC address for Ethernet shield

IPAddress serverIP(192, 168, 1, 41);  // IP address of the server
unsigned int serverPort = 4567;

IPAddress ip(192, 168, 1, 11);  // IP address of the Arduino
EthernetClient client;

const int BUTTON_PIN[] = {A0, A1, A2, A3, A4};
const int NUM_BUTTONS = sizeof(BUTTON_PIN) / sizeof(BUTTON_PIN[0]);

unsigned long lastHeartbeatTime = 0;  // Timer since last heartbeat (in milliseconds)
unsigned long buttonTimer = 0;  // Timer for button presses (in milliseconds)
unsigned long buttonTimer1 = 0;
unsigned long buttonTimeout = 500; // Timeout duration for button presses (in milliseconds)

int x = 0;
int y = 0;
int buttonState[NUM_BUTTONS];
int lastButtonState[NUM_BUTTONS];

void setup() {
  FastLED.addLeds<NEOPIXEL, 8>(leds, NUM_LEDS);

  Ethernet.begin(mac, ip);
  Serial.begin(9600);

  for (int i = 0; i < NUM_BUTTONS; i++) {
    pinMode(BUTTON_PIN[i], INPUT_PULLUP);
    buttonState[i] = LOW;
    lastButtonState[i] = LOW;
  }

  delay(2500);
  resetLights();
}

void loop() {
  if (!client.connected()) {
    if (connectToServer()) {
      Serial.println("Connected to server");
    } else {
      Serial.println("Connection failed");
      delay(2000);  // Wait before trying again
      return;
    }
  }
  
  handleHeartbeat();
  readButtonState();

  if(client.available()) {
    processServerResponse();
  }
}

void resetLights() {
  for(int i = 0; i < NUM_LEDS; i++) {
    leds[i] = CRGB::Black;
  }
  FastLED.show();
  Serial.println("reset_lights");
}

boolean checkButton(int index) {
  if(digitalRead(BUTTON_PIN[index]) == HIGH && buttonState[index] == LOW) {
    return true;
  }
  return false;
}

void handleHeartbeat() {
  if(millis() - lastHeartbeatTime >= 3000) {
    client.println("heartbeat");
    lastHeartbeatTime = millis();
  }
}

void readButtonState() {
  for(int i = 0; i < NUM_BUTTONS; i++) {
    if(digitalRead(BUTTON_PIN[i]) == HIGH && buttonState[i] == LOW) {
      Serial.println("Button " + String(i) + " pressed");
      buttonState[i] = HIGH;
      buttonTimer = millis();
      if ((4 - x) == i) {
        buttonTimer1 = millis() - buttonTimer1;
        client.println("timer:" + String(buttonTimer1));
      }
    }
  }

  if (millis() - buttonTimer >= buttonTimeout) {
    for(int i = 0; i < NUM_BUTTONS; i++) {
      buttonState[i] = LOW;
    }
  }
}

void processServerResponse() {
  String response = client.readStringUntil('\n');
  if (response.startsWith("reset")) {
    resetLights();
  } else {
    int comma = response.indexOf(',');
    String p1 = response.substring(0, comma);
    x = p1.toInt();
    String p2 = response.substring(comma + 1);
    y = p2.toInt();
    Serial.println("Received data from server: " + p1 + ", " + p2);
    
    if (y >= 0 && y <= 4) {
      leds[4 - x + (y * 5)] = CRGB::Aquamarine;
      FastLED.show();
      if (y == 0) {
        buttonTimer1 = millis();
      }
    } else {
      Serial.println("Invalid y value: " + String(y));
    }
  }
}

boolean connectToServer() {
  Serial.println("Connecting to server...");
  resetLights();
  client.stop();
  delay(1000);  // Wait for a second before attempting to reconnect
  
  if (client.connect(serverIP, serverPort)) {
    lastHeartbeatTime = millis();  // Reset the heartbeat timer
    return true;
  }
  
  return false;
}