#ifndef Header_SuperpoweredExample
#define Header_SuperpoweredExample

#include <math.h>
#include <pthread.h>

#include "SuperpoweredExample.h"
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredRecorder.h>
#include <SuperpoweredFilter.h>
#include <SuperpoweredRoll.h>
#include <SuperpoweredFlanger.h>
#include <AndroidIO/SuperpoweredAndroidAudioIO.h>

#define HEADROOM_DECIBEL 3.0f
static const float headroom = powf(10.0f, -HEADROOM_DECIBEL * 0.025f);

class SuperpoweredExample {
public:

	SuperpoweredExample(unsigned int samplerate, unsigned int buffersize, const char *path, int fileAoffset, int fileAlength, int fileBoffset, int fileBlength, const char *testPath[]);
	~SuperpoweredExample();

	bool process(short int *output, unsigned int numberOfSamples);
	bool processRecording(short int *output, unsigned int numberOfSamples);
	void onSuperPlay();
	void onSuperRecord(bool record, const char *fileTarget,const char *fileTargetWav, const char *tempTarget);
    void onTestPlay(int value);
    void onSuperPause();
	void onCrossfader(int value);
	void onFxSelect(int value);
	void onFxOff();
	void onFxValue(int value);
    void onEQBand(unsigned int index, int gain);

private:
    SuperpoweredAndroidAudioIO *audioSystem;
    SuperpoweredAndroidAudioIO *audioSystemRecording;
    SuperpoweredAdvancedAudioPlayer *playerA, *playerB;
    SuperpoweredAdvancedAudioPlayer *oPlayers[9];
    SuperpoweredRecorder *recorder;
    SuperpoweredRoll *roll;
    SuperpoweredFilter *filter;
    SuperpoweredFlanger *flanger;
    int sampleRate;
    float *stereoBuffer;
    unsigned char activeFx;
    float crossValue, volA, volB;
};

#endif
