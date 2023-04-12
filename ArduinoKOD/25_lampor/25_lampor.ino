#include <SPI.h>         // needed for Arduino versions later than 0018
#include <Ethernet.h>

// Enter a MAC address and IP address for your controller below.
// The IP address will be dependent on your local network:
/*byte mac[] = { 0xA8, 0x61, 0x0A, 0xAE, 0x6E, 0x9D };
IPAddress serverIP(192, 168, 1, 49);
IPAddress ip(192, 168, 1, 44);
unsigned int serverPort = 5656;
EthernetClient client;*/

//const int BUTTON_PIN = 7;
int ledState1 = LOW;
int ledState2 = HIGH;
int lastButtonState;
int currentButtonState;

int cols[5] = {2, 3, 4, 5, 6};
int rows[5] = {8, 10, 11, 9, 13};

void setup() {
  delay(1000);
  // start the Ethernet:
  //Ethernet.begin(mac, ip);
  Serial.begin(9600);

  //pinMode(BUTTON_PIN, INPUT_PULLUP);
  //currentButtonState = digitalRead(BUTTON_PIN);
  int i;

  for (i = 0; i < 5; i++) {
    pinMode(cols[i], OUTPUT);
    pinMode(rows[i], OUTPUT);
  }

  for (i = 0; i < 5; i++) {
    digitalWrite(cols[i], HIGH);
    digitalWrite(rows[i], LOW);
  }
  delay(2000);
}

void loop() {
  for(int i = 0; i < 5; i++) {
    for(int j = 0; j < 5; j++) {
      digitalWrite(rows[i], HIGH);
      digitalWrite(cols[j], LOW);
      delay(100);
      Serial.println("X");
      Serial.println(i);
      Serial.println("Y");
      Serial.println(j);
      delay(100);
      resetLights();
      delay(100);
    }
  }
}

void resetLights() {
  for(int i = 0; i < 5; i++) {
    for(int j = 0; j < 5; j++) {
      digitalWrite(rows[i], LOW);
      digitalWrite(cols[j], HIGH);
    }
  }
  Serial.println("Reset lights");
}
