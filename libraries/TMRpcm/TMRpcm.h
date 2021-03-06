/*Library by TMRh20 2012-2013*/

#ifndef TMRpcm_h   // if x.h hasn't been included yet...
#define TMRpcm_h   //   #define this so the compiler knows it has been included

//Comment or Uncomment to en/disable RF streaming of wav files
//Make sure to Comment if not using radio
//#define ENABLE_RF

#include <Arduino.h>
#include "EE24CCxxx.h"

class TMRpcm
{
 public:
	void play(uint16_t startAddress, uint16_t length);
	void stopPlayback();
	void volume(int vol);
	void setVolume(int vol);
	void disable();
	void pause();
	void quality(boolean q);
	int speakerPin;
	boolean wavInfo(uint16_t startAddress, uint16_t length);
	boolean isPlaying();
	boolean rfPlaying;
	unsigned int SAMPLE_RATE;


 private:
	int lastSpeakPin;
	void setPin();
};



#endif