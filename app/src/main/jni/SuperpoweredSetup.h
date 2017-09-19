#ifndef Header_SuperpoweredSetup
#define Header_SuperpoweredSetup

#include <math.h>
#include <pthread.h>

#include "SuperpoweredSetup.h"
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredFilter.h>
#include <SuperpoweredRoll.h>
#include <SuperpoweredFlanger.h>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>

#define HEADROOM_DECIBEL 3.0f

class SuperpoweredSetup{
public:

	SuperpoweredSetup(unsigned int samplerate, unsigned int buffersize, const char *path, int postition);
	~SuperpoweredSetup();

	bool process(short int *output, unsigned int numberOfSamples);
	void onTestPlay(int value);
	void onSuperPause();
	void onCrossfader(int value);
	void onFxSelect(int value);
	void onFxOff();
	void onFxValue(int value);
    void onEQBand(unsigned int index, int gain);

private:
    SuperpoweredAndroidAudioIO *audioSystem2;
    SuperpoweredAdvancedAudioPlayer *playerA, *playerB;
    SuperpoweredAdvancedAudioPlayer *mPlayers[9];
    SuperpoweredRoll *roll;
    SuperpoweredFilter *filter;
    SuperpoweredFlanger *flanger;
    float *stereoBuffer;
    unsigned char activeFx;
    float crossValue, volA, volB;
};

#endif
