#include <stdint.h>
#include <avr/interrupt.h>
#include <avr/io.h>
#include <avr/pgmspace.h>

#include <Wire.h>
#include <ByteBuffer.h>
#include <EE24CCxxx.h>
#include <TMRpcm.h>

#define EEPROM1 0x50

#include "cadencepart2.h"

void setup()
{

  //initalized i2c and set to 400Hz
  Wire.begin();
  TWBR = 12;

  pinMode(13,OUTPUT);
  digitalWrite(13,HIGH);
  writeEEPROM(cadence_start, &cadence[0], sizeof(cadence));
  digitalWrite(13,LOW);
}

void loop(){
  digitalWrite(13,HIGH);
  delay(250);
  digitalWrite(13,LOW);
  delay(250);

}

void writeEEPROM(const unsigned int address, const byte* data, int length) 
{

  Wire.beginTransmission(EEPROM1);
  Wire.write( address >> 8); 
  Wire.write( address & 0xFF);

  for (int i=0; i<length;i++){
    Wire.write(pgm_read_byte(data+i));

    /*
     * EEPROM memory page 64 but Arduino buffer is 
     * only 32 plus address bytes, using 16 because
     * evenly devides into 64
     */
    if (i>0 && (i+1)%16==0){
      Wire.endTransmission();
      delay(6);  // needs 5ms for page write

      Wire.beginTransmission(EEPROM1);
      Wire.write( address+i+1 >> 8); 
      Wire.write( address+i+1 & 0xFF);
    }
  }

  Wire.endTransmission();
  delay(6);  // needs 5ms for page write

}

void readEEPROM(unsigned int address, unsigned int length ) 
{
  Wire.beginTransmission(EEPROM1);
  Wire.write( (address >> 8) & 0xFF); 
  Wire.write( (address >> 0) & 0xFF);
  Wire.endTransmission();

  Wire.requestFrom(EEPROM1, length);
  while (Wire.available()) 
  {
    byte data = Wire.read();
  }
}




