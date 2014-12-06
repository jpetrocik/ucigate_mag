#include <stdint.h>
#include <avr/interrupt.h>
#include <avr/io.h>

#include <ByteBuffer.h>
#include <EE24CCxxx.h>
#include <TMRpcm.h>
#include <Wire.h>

#include "configuration.h"

#define TRUE 1
#define FALSE 0

/**
 * Firmware Version number
 */
#define FIRMWARE_VERSION "1.1.1"

/**
 * Serial input buffer
 */
byte serialInput[100];
int serialSize = 0;

long startTime = 0;
int cadenceStarted = FALSE;
int timerStarted = FALSE;

/**
 * Recalibrated each start, used as a threadhold to indicate timer was triped
 */
int MAT_SENSOR_THRESHOLD = 0;

/**
 * Audio PWM player
 */
TMRpcm trmpcm;

void setup() {
  // initialize serial communication for debug
  SerialPort.begin(9600);

  //Turn on i2c and set to 400Hz
  Wire.begin();
  TWBR = 12;

  //setup pwvm audio
  trmpcm.speakerPin=SPEAKER;
  trmpcm.setVolume(6);

  //set pin modes
  pinMode(GATE, OUTPUT);
  pinMode(LIGHT_RED, OUTPUT);
  pinMode(LIGHT_YELLOW_1, OUTPUT);
  pinMode(LIGHT_YELLOW_2, OUTPUT);
  pinMode(LIGHT_GREEN, OUTPUT);
  pinMode(START, INPUT);

  //disable hum of speaker before first cadence
  pinMode(SPEAKER, OUTPUT);
  digitalWrite(SPEAKER, LOW);

  //turn on magnet
  digitalWrite(GATE, HIGH);

  //performance light show to indicate box is on
  digitalWrite(LIGHT_RED, HIGH);
  delay(250);
  digitalWrite(LIGHT_YELLOW_1, HIGH);
  delay(250);
  digitalWrite(LIGHT_YELLOW_2, HIGH);
  delay(250);
  digitalWrite(LIGHT_GREEN, HIGH);
  delay(250);
  for (int i=0;i<5;i++){
    digitalWrite(LIGHT_RED, HIGH);  
    digitalWrite(LIGHT_YELLOW_1, HIGH);
    digitalWrite(LIGHT_YELLOW_2, HIGH);
    digitalWrite(LIGHT_GREEN, HIGH);
    delay(100);
    digitalWrite(LIGHT_RED, LOW);
    digitalWrite(LIGHT_YELLOW_1, LOW);
    digitalWrite(LIGHT_YELLOW_2, LOW);
    digitalWrite(LIGHT_GREEN, LOW);
    delay(100);
  }

}

/**
 * Sends event via serial, aka blueooth
 *
 * Message Format: BMX99CMD99ARGS
 * BMX = start of message indicator
 * 99 = number of byte of command <- a single byte
 * CMD = the command sent
 * 99 = number of bytes of args <- a single byte 0x00 - 0xFF
 * ARGS = the args for this command
 * 
 */
void sendEvent(String cmd, String args){
  int cmdSize = cmd.length();
  int argSize = args.length();

  SerialPort.print("BMX");
  SerialPort.print((char)cmdSize);
  SerialPort.print(cmd);
  SerialPort.print((char)argSize);
  SerialPort.print(args);
}

/**
 * Returns the software version number
 */
void version() {
  sendEvent("SW_VERSION",FIRMWARE_VERSION);
}

/**
 * Main loop
 */
void loop() {

  //process any serial commands
  processSerialInput();

  long currentTime = millis();

  /**
   * Cadence started from push button, only when cadence not
   * running. 
   */
  if (!digitalRead(START) && !cadenceStarted) {
    cadenceStarted=TRUE;
    sendEvent("EVNT_CADENCE_STARTED","");

    for (int i=0; i<5; i++){
      tone(SPEAKER, 1150, 250);
      delay(500);
    }

    delay(8000);
    startTime = startCadence();
  }

  /*
   * Check Sensor Matt
   */
  if (timerStarted) {
    int timerMat = analogRead(TIMER);
    if ( timerMat > MAT_SENSOR_THRESHOLD ){
      sendEvent("EVNT_TIMER_1", String(currentTime-startTime));
      timerStarted=FALSE;
    }
  }

  /* 
   * 3 seconds after gate drops reset all lights and enable magnet
   * so gate can be lifted for next start
   */
  if ((startTime+3000) < currentTime && cadenceStarted){
    resetGate();
    cadenceStarted = FALSE;
  }

}

/**
 * Start the cadence, blocking until cadence completes
 */
long startCadence(){
  int randNumber;

  calibrateTimer();

  // "OK RIDERS RANDOM START" : 1:50+1:80 sec
  sendEvent("EVNT_OK_RIDERS","");
  trmpcm.play(0,13224);
  digitalWrite(LIGHT_RED, HIGH);
  digitalWrite(LIGHT_YELLOW_1, HIGH);
  digitalWrite(LIGHT_YELLOW_2, HIGH);
  digitalWrite(LIGHT_GREEN, HIGH);
  delay(3300);

  // "RIDERS READY - WATCH THE GATE" : 2.00 sec
  sendEvent("EVNT_RIDERS_READY","");
  trmpcm.play(32000,17535);
  digitalWrite(LIGHT_GREEN, LOW);
  delay(670);
  digitalWrite(LIGHT_YELLOW_2, LOW);
  delay(670);
  digitalWrite(LIGHT_YELLOW_1, LOW);
  delay(670);
  digitalWrite(LIGHT_RED, LOW);
  delay(100);
  trmpcm.disable();

  //Random delay .1 - 2.7 sec
  //offical delay of .1 was to fast, using .250
  randNumber = random(.350, 2700);
  delay(randNumber);        

  //Red Light
  sendEvent("EVNT_RED_LIGHT","");
  lightOn(LIGHT_RED,60);

  //Yellow Light
  sendEvent("EVNT_YELLOW_1_LIGHT","");
  lightOn(LIGHT_YELLOW_1,60);

  //drop gate 120ms early to compensate for magnet field calpse
  digitalWrite(GATE, LOW);

  //Yellow Light
  sendEvent("EVNT_YELLOW_2_LIGHT","");
  lightOn(LIGHT_YELLOW_2,60);

  //Turns green light an return 
  //immediately as buzzer plays
  sendEvent("EVNT_GREEN_LIGHT","");
  digitalWrite(LIGHT_GREEN, HIGH);
  tone(SPEAKER, 632, 2250);

  timerStarted = TRUE;

  return millis();
}

void resetGate(){
  digitalWrite(GATE, HIGH);
  digitalWrite(LIGHT_RED, LOW);
  digitalWrite(LIGHT_YELLOW_1, LOW);
  digitalWrite(LIGHT_YELLOW_2, LOW);
  digitalWrite(LIGHT_GREEN, LOW);
}

void lightOn(int light, int msec){
  digitalWrite(light, HIGH);
  tone(SPEAKER, 632, msec);
  delay(msec+60);
  noTone(SPEAKER);
}


void shiftArray(byte data[], int offset, int length){

  for (int i=offset;i<length;i++){
    data[i-offset]=data[i];
  }

}

String toStr(byte data[], int offset, int length){
  int endOffset = offset+length;
  String value = "";
  value.reserve(length);

  for (int i = offset ; i<endOffset ; i++){
    value += (char)data[i];
  }

  return value;
}

void performCommand(String cmd, String args){
  if (cmd.equals("GET")){
    if (args.equals("SW_VERSION")) {
      version();
    }
  } 
  else if (cmd.equals("START_CADENCE")) {
    if (cadenceStarted==FALSE){
      sendEvent("EVNT_CADENCE_STARTED","");
      cadenceStarted=TRUE;
      delay(500);
      startTime = startCadence();
    }
  } 
}


/**
 * Resets the serial buffer, clears out the input stream and 
 * resets previously read data
 **/
void resetSerial(){
  while (SerialPort.available()) {
    SerialPort.read(); 
  }
  serialSize=0;
}  

void processSerialInput() {

  //read everyting we have recieved adn buffer
  while (SerialPort.available()) {
    serialInput[serialSize++] = SerialPort.read(); 
    if (serialSize>100){
      sendEvent("ERROR","BUFFER_LIMIT");
      resetSerial();
      return;
    }   
  }

  //not a valid cmd, yet
  if(serialSize<5)
    return;

  //look for cmd seq
  for (int i=0; i < serialSize; i++){

    //found cmd seq
    if (serialInput[i] == 'B' && serialInput[i+1] == 'M' && serialInput[i+2] == 'X'){
      i+=3;

      //read cmd
      if (i >= serialSize) return;
      int cmdSize = 0 | serialInput[i++];

      if (i+cmdSize > serialSize) return;
      String cmd = toStr(serialInput, i, cmdSize);
      i+=cmdSize;

      //read args
      if (i >= serialSize) return;
      int argSize = 0 | serialInput[i++];

      if (i+argSize > serialSize) return;
      String args = toStr(serialInput, i, argSize);
      i+=argSize;

      performCommand(cmd, args);

      //reposition unread to start of buffer
      shiftArray(serialInput, i, serialSize);
      serialSize=serialSize-i;

      return;
    } 
  }

  //no valid command found, wiping out data
  serialSize=0;
}

void calibrateTimer(){
  //Get base reading for timing matt
  int reading = 0;
  for (int i=0; i<5; i++){
    reading += analogRead(TIMER);
    delay(20);
  }
  MAT_SENSOR_THRESHOLD=reading/5+300;

}












