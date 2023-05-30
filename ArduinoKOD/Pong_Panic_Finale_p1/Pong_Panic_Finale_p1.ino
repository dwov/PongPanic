#include <SPI.h>
#include <Ethernet.h> // Ethernet bibliotek för att hantera anslutningen, ethernet-controllern mm.
#include <FastLED.h>  // Färdigt bibliotek för att kontrollera diverse led-controllers, bl.a. WS2112b

/* Skriven 2023 av David Permlid
 * 
 * Det här är den slutgiltiga koden för det inbyggda systemet. I denna finns det funktioner som läser av 
 * och tolkar meddelanden som i främsta form är koordinater mottagna från det servern. Den läser samtidigt 
 * kontinuerligt av knapparna och berättar samt registrerar om en knapp detekteras som nedtryckt.
 * 
 * I filen har jag tagit användning av två färdiga bibliotek för att hjälpa mig kontrollera Ethernet_Shield
 * samt ledslingan som används i kretsen.
 * 
 * Programmet sätter en hårdkodad IP- och MAC-adress som sedan skapar variabler och dylikt som behövs under
 * körning. Sedan körs huvudprogrammet som kontinuerligt bekräftar att systemet är uppkopplat och vid brist
 * på uppkoppling försöker systemet kontinuerligt återansluta. Om ansluten kollar den om knapparna är nedtryckta
 * och om vi fått något nytt meddelande från servern.
 * 
 * Om vi mottager en koordinat så tänds lampan som motsvarar koordinaten på spelplanen. Vid mottaget "reset"
 * så släcks lamporna. Knappar skickas till servern om den är i samma koloumn som "bollen" och startar en 
 * timeout för alla knapparna på 500ms som hindrar spelaren från att klicka igen på den avsatta tiden.
 */

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
unsigned long buttonTimer1 = 0;  // Timer to send to server (in milliseconds)
unsigned long buttonTimeout = 500; // Timeout duration for button presses (in milliseconds)

int x = 0;
int y = 0;
int buttonState[NUM_BUTTONS];
int lastButtonState;

void setup() {
  FastLED.addLeds<NEOPIXEL, 8>(leds, NUM_LEDS);

  Ethernet.begin(mac, ip);
  Serial.begin(9600);

  for (int i = 0; i < NUM_BUTTONS; i++) {
    pinMode(BUTTON_PIN[i], INPUT_PULLUP);
    buttonState[i] = LOW;
  }
  lastButtonState = LOW;

  delay(2000);
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

/* 
 * Turns of all the lights (set all lights to black)
 */
void resetLights() {
  for(int i = 0; i < NUM_LEDS; i++) {
    leds[i] = CRGB::Black;
  }
  FastLED.show();
  Serial.println("reset_lights");
}

/*
 * This function sends a "heartbeat" to the server if 3 seconds has passed.
 */
void handleHeartbeat() {
  if(millis() - lastHeartbeatTime >= 3000) {
    client.println("heartbeat");
    lastHeartbeatTime = millis();
  }
}

/*
 * Reads the current state of all buttons. If a button has been pressed it will start a timeout,
 * and block the user from pressing any buttons for a short period of time. If the button is in 
 * the same coloum as the x coordinate, it will be sent to the server as a timer from when the 
 * y coordinate last was 0.
 */
void readButtonState() {
  for(int i = 0; i < NUM_BUTTONS; i++) {
    if(digitalRead(BUTTON_PIN[i]) == HIGH && lastButtonState == LOW) {
      Serial.println("Button " + String(i) + " pressed");
      lastButtonState = HIGH;
      buttonTimer = millis();
      if ((4 - x) == i) {
        buttonTimer1 = millis() - buttonTimer1;
        client.println("timer:" + String(buttonTimer1));
      }
    }
  }

  if (millis() - buttonTimer >= buttonTimeout) {
    for(int i = 0; i < NUM_BUTTONS; i++) {
      lastButtonState = LOW;
    }
  }
}

/*
 * Checks if anything has been sent from the server and handles it appropriately, turning on lights,
 * turn off lights.
 */
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
      leds[4 - x + (y * 5)] = CRGB::Magenta;
      FastLED.show();
      if (y == 0) {
        buttonTimer1 = millis();
      }
    } else {
      Serial.println("Invalid y value: " + String(y));
    }
  }
}

/*
 * Tries to connect to the specified server ip, and port.
 */
boolean connectToServer() {
  Serial.println("Connecting to server...");
  resetLights();
  client.stop();
  delay(100);  // Wait for a second before attempting to reconnect
  
  if (client.connect(serverIP, serverPort)) {
    lastHeartbeatTime = millis();  // Reset the heartbeat timer
    return true;
  }
  
  return false;
}