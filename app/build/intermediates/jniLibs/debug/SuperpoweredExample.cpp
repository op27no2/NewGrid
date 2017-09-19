#include "SuperpoweredExample.h"
#include "SuperpoweredSetup.h"
#include <SuperpoweredSimple.h>
#include <SuperpoweredCPU.h>
#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include <stdlib.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <unistd.h>

float *stereoBufferRecording;

static void playerEventCallbackA(void *clientData, SuperpoweredAdvancedAudioPlayerEvent event, void * __unused value) {
    SuperpoweredAdvancedAudioPlayer *playerA = *((SuperpoweredAdvancedAudioPlayer **)clientData);

    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess) {
    	SuperpoweredAdvancedAudioPlayer *playerA = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        playerA->setBpm(126.0f);
        playerA->setFirstBeatMs(353);
        playerA->setPosition(playerA->firstBeatMs, false, false);
    }

    if(event == SuperpoweredAdvancedAudioPlayerEvent_EOF){
        /*bool *stopLooping = (bool *)value;
        *stopLooping = playerA->looping == false;*/
        playerA->pause();
    }
};


static void playerEventCallbackB(void *clientData, SuperpoweredAdvancedAudioPlayerEvent event, void * __unused value) {
    SuperpoweredAdvancedAudioPlayer *playerB = *((SuperpoweredAdvancedAudioPlayer **)clientData);

    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess) {
    	SuperpoweredAdvancedAudioPlayer *playerB = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        playerB->setBpm(123.0f);
        playerB->setFirstBeatMs(40);
        playerB->setPosition(playerB->firstBeatMs, false, false);
    };
    if(event == SuperpoweredAdvancedAudioPlayerEvent_EOF){
        /*bool *stopLooping = (bool *)value;
        *stopLooping = playerB->looping == false;
        playerB->seek(0);
        playerB->playing == false;*/
        //playerB->pause();
    }
}

static void playerEventCallbackTest(void *clientData, SuperpoweredAdvancedAudioPlayerEvent event, void * __unused value) {
    SuperpoweredAdvancedAudioPlayer *playerTest = *((SuperpoweredAdvancedAudioPlayer **)clientData);

    __android_log_print(ANDROID_LOG_DEBUG, "LOG_callback player", "text:%s ", "callback");

    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadSuccess) {
        SuperpoweredAdvancedAudioPlayer *playerTest = *((SuperpoweredAdvancedAudioPlayer **)clientData);
        /*playerB->setBpm(123.0f);
        playerB->setFirstBeatMs(40);
        playerB->setPosition(playerB->firstBeatMs, false, false);*/

        __android_log_print(ANDROID_LOG_DEBUG, "LOG_SETUP1", "text:%s ", "hmmm");

    }

    if (event == SuperpoweredAdvancedAudioPlayerEvent_LoadError ) {

        __android_log_print(ANDROID_LOG_INFO, "LOG error", "Player event: %s, in playerEventCallbackA", value);
    }
    if (event == SuperpoweredAdvancedAudioPlayerEvent_HLSNetworkError ) {
        __android_log_print(ANDROID_LOG_INFO, "LOG error", "Player event: %d, in playerEventCallbackA", event);
    }
    if (event == SuperpoweredAdvancedAudioPlayerEvent_ProgressiveDownloadError ) {
        __android_log_print(ANDROID_LOG_INFO, "LOG error",
                            "Player event: %d, in playerEventCallbackA", event);
    }

        if(event == SuperpoweredAdvancedAudioPlayerEvent_EOF){
        /*bool *stopLooping = (bool *)value;
        *stopLooping = playerB->looping == false;
        playerB->seek(0);
        playerB->playing == false;*/
        playerTest->pause();
        playerTest->seek(0);
    }

}

static void recordCallback(void *clientData) {
    __android_log_print(ANDROID_LOG_DEBUG, "LOG_callback record", "text:%i ", clientData);

};



static bool audioProcessing(void *clientdata, short int *audioIO, int numberOfSamples, int __unused samplerate) {
	return ((SuperpoweredExample *)clientdata)->process(audioIO, (unsigned int)numberOfSamples);
}

static bool audioProcessingRecording(void *clientdata, short int *audioIO, int numberOfSamples, int samplerate) {
    return ((SuperpoweredExample *)clientdata)->processRecording(audioIO, numberOfSamples);
}


SuperpoweredExample::SuperpoweredExample(unsigned int samplerate, unsigned int buffersize, const char *path, int fileAoffset, int fileAlength, int fileBoffset, int fileBlength, const char *testPath[]) : activeFx(0), crossValue(0.0f), volB(0.0f), volA(1.0f * headroom) {
    stereoBuffer = (float *)memalign(16, (buffersize + 16) * sizeof(float) * 2);
    stereoBufferRecording = (float *)memalign(16, (buffersize + 16) * sizeof(float) * 2);
    this->sampleRate = sampleRate;

    playerA = new SuperpoweredAdvancedAudioPlayer(&playerA , playerEventCallbackA, samplerate, 0);
    playerA->open(path, fileAoffset, (fileAlength));
    playerB = new SuperpoweredAdvancedAudioPlayer(&playerB, playerEventCallbackB, samplerate, 0);
    //playerB->open(path, fileBoffset, (fileBlength));

    playerB->open(testPath[1],0,0);

    for (int i=0; i<9; i++) {
        oPlayers[i] = new SuperpoweredAdvancedAudioPlayer(&oPlayers[i], playerEventCallbackTest, samplerate, 0);
        oPlayers[i]->open(testPath[i], 0, 0);
    }

    __android_log_print(ANDROID_LOG_DEBUG, "LOG_TAG00", "text:%s ", testPath[1] );

    playerA->syncMode = playerB->syncMode = SuperpoweredAdvancedAudioPlayerSyncMode_TempoAndBeat;

    roll = new SuperpoweredRoll(samplerate);
    filter = new SuperpoweredFilter(SuperpoweredFilter_Resonant_Lowpass, samplerate);
    flanger = new SuperpoweredFlanger(samplerate);

    const char *temp = testPath[0];


    int what = 5;
    int *testy = &what;

    recorder = new SuperpoweredRecorder(temp, samplerate,1,2,false, recordCallback, __null);

    audioSystem = new SuperpoweredAndroidAudioIO(samplerate, buffersize, false, true, audioProcessing, this, -1, SL_ANDROID_STREAM_MEDIA, buffersize * 2);
    audioSystemRecording = new SuperpoweredAndroidAudioIO(sampleRate, buffersize, true, false, audioProcessingRecording, this, buffersize * 2);
}


SuperpoweredExample::~SuperpoweredExample() {
    delete audioSystem;
    delete audioSystemRecording;
    delete playerA;
    delete playerB;
    delete roll;
    delete filter;
    delete recorder;
    delete flanger;
    free(stereoBuffer);
}

void SuperpoweredExample::onSuperPlay() {
   /* if (!play) {
        playerA->pause();
        playerB->pause();
    } */
    //else {
    if(playerA->playing == true){
        playerA->pause();
        playerA->seek(0);
    }
    if(playerB->playing == true){
        playerB->pause();
        playerB->seek(0);
    }

        bool masterIsA = (crossValue <= 0.5f);
        playerA->play(!masterIsA);
        playerB->play(masterIsA);
   // };
    SuperpoweredCPU::setSustainedPerformanceMode(true); // <-- Important to prevent audio dropouts.
}

void SuperpoweredExample::onSuperRecord(bool record, const char *targetPath,const char *targetPathWav, const char *tempPath) {

    if (!record) {
        recorder = new SuperpoweredRecorder(tempPath, sampleRate);
        recorder->start(targetPath);
        __android_log_print(ANDROID_LOG_DEBUG, "starting record", "text:%s ", targetPath);
    } else {
        //player->pause();
        recorder->stop();
        __android_log_print(ANDROID_LOG_DEBUG, "stoppingrecord", "text:%s ", "record click stop");
       // sleep(.01);

        oPlayers[0]->open(targetPathWav, 0, 0);
        oPlayers[0]->play(false);

        //audioSystem->stop();
    };
}

void SuperpoweredExample::onTestPlay(int value) {

    __android_log_print(ANDROID_LOG_DEBUG, "LOG_clicked ms", "test int = %d", oPlayers[value]->durationMs);
    __android_log_print(ANDROID_LOG_DEBUG, "LOG_clicked ms", "test int = %s", oPlayers[value]->tempFolderPath);

    for(int i=0;i<9;i++) {
        if (oPlayers[i]->playing == true) {
            __android_log_print(ANDROID_LOG_DEBUG, "LOG_SETUP", "text:%s ", "playing clicked");
            oPlayers[i]->pause();
            oPlayers[i]->seek(0);
        }
    }

    oPlayers[value]->play(false);

    SuperpoweredCPU::setSustainedPerformanceMode(true); // <-- Important to prevent audio dropouts.
}

void SuperpoweredExample::onSuperPause() {
    playerA->pause();
    playerB->pause();

}

void SuperpoweredExample::onCrossfader(int value) {
    crossValue = float(value) * 0.01f;
    if (crossValue < 0.01f) {
        volA = 1.0f * headroom;
        volB = 0.0f;
    } else if (crossValue > 0.99f) {
        volA = 0.0f;
        volB = 1.0f * headroom;
    } else { // constant power curve
        volA = cosf(float(M_PI_2) * crossValue) * headroom;
        volB = cosf(float(M_PI_2) * (1.0f - crossValue)) * headroom;
    };
}

void SuperpoweredExample::onFxSelect(int value) {
	__android_log_print(ANDROID_LOG_VERBOSE, "SuperpoweredExample", "FXSEL %i", value);
	activeFx = (unsigned char)value;
}

void SuperpoweredExample::onFxOff() {
    filter->enable(false);
    roll->enable(false);
    flanger->enable(false);
}

#define MINFREQ 60.0f
#define MAXFREQ 20000.0f

static inline float floatToFrequency(float value) {
    if (value > 0.97f) return MAXFREQ;
    if (value < 0.03f) return MINFREQ;
    value = powf(10.0f, (value + ((0.4f - fabsf(value - 0.4f)) * 0.3f)) * log10f(MAXFREQ - MINFREQ)) + MINFREQ;
    return value < MAXFREQ ? value : MAXFREQ;
}

void SuperpoweredExample::onFxValue(int ivalue) {
    float value = float(ivalue) * 0.01f;
    switch (activeFx) {
        case 1:
            filter->setResonantParameters(floatToFrequency(1.0f - value), 0.2f);
            filter->enable(true);
            flanger->enable(false);
            roll->enable(false);
            break;
        case 2:
            if (value > 0.8f) roll->beats = 0.0625f;
            else if (value > 0.6f) roll->beats = 0.125f;
            else if (value > 0.4f) roll->beats = 0.25f;
            else if (value > 0.2f) roll->beats = 0.5f;
            else roll->beats = 1.0f;
            roll->enable(true);
            filter->enable(false);
            flanger->enable(false);
            break;
        default:
            flanger->setWet(value);
            flanger->enable(true);
            filter->enable(false);
            roll->enable(false);
    };
}

bool SuperpoweredExample::process(short int *output, unsigned int numberOfSamples) {
   // __android_log_print(ANDROID_LOG_VERBOSE, "process audio", "input %i", output);

    bool masterIsA = (crossValue <= 0.5f);
    double msElapsedSinceLastBeatA = playerA->msElapsedSinceLastBeat; // When playerB needs it, playerA has already stepped this value, so save it now.
    double masterBpm = masterIsA ? playerA->currentBpm : oPlayers[0]->currentBpm;
    bool silence = !playerA->process(stereoBuffer, false, numberOfSamples, volA, masterBpm, oPlayers[0]->msElapsedSinceLastBeat);

    for (int i=0; i<9; i++) {
        if (oPlayers[i]->process(stereoBuffer, !silence, numberOfSamples, volB, masterBpm, msElapsedSinceLastBeatA)) {
            silence = false;
        }
    }


/*
    double masterBpm = masterIsA ? playerA->currentBpm : playerB->currentBpm;
    double msElapsedSinceLastBeatA = playerA->msElapsedSinceLastBeat; // When playerB needs it, playerA has already stepped this value, so save it now.

    bool silence = !playerA->process(stereoBuffer, false, numberOfSamples, volA, masterBpm, playerB->msElapsedSinceLastBeat);
    if (playerB->process(stereoBuffer, !silence, numberOfSamples, volB, masterBpm, msElapsedSinceLastBeatA)) silence = false;
*/

    roll->bpm = flanger->bpm = (float)masterBpm; // Syncing fx is one line.

    if (roll->process(silence ? NULL : stereoBuffer, stereoBuffer, numberOfSamples) && silence) silence = false;
    if (!silence) {
        filter->process(stereoBuffer, stereoBuffer, numberOfSamples);
        flanger->process(stereoBuffer, stereoBuffer, numberOfSamples);
       // recorder->process(stereoBuffer, stereoBuffer, numberOfSamples);

    };

    // The stereoBuffer is ready now, let's put the finished audio into the requested buffers.
    if (!silence) SuperpoweredFloatToShortInt(stereoBuffer, output, numberOfSamples);
    return !silence;
}

// method to receive recording parts
bool SuperpoweredExample::processRecording(short int *input, unsigned int numberOfSamples) {
    //__android_log_print(ANDROID_LOG_VERBOSE, "process recording", "input %i", input);

        unsigned int data = 0;
        SuperpoweredShortIntToFloat(input, stereoBufferRecording, numberOfSamples);
        data = recorder->process(stereoBufferRecording, NULL, numberOfSamples);
        return true;
}


static SuperpoweredExample *example = NULL;
static SuperpoweredSetup *setupexample = NULL;

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_SuperpoweredExample(JNIEnv *javaEnvironment, jobject __unused obj, jint samplerate, jint buffersize, jstring apkPath, jint fileAoffset, jint fileAlength, jint fileBoffset, jint fileBlength, jobjectArray testPaths) {
    const char *path = javaEnvironment->GetStringUTFChars(apkPath, JNI_FALSE);
    //const char *path2[9] = javaEnvironment->GetStringUTFChars(testPath[], JNI_FALSE);
    const char *path2[9];
    jsize stringCount = (*javaEnvironment).GetArrayLength(testPaths);
    for (int i=0; i<stringCount; i++) {
        jstring string = (jstring) (*javaEnvironment).GetObjectArrayElement(testPaths, i);
        path2[i] = (*javaEnvironment).GetStringUTFChars( string, NULL);
        __android_log_print(ANDROID_LOG_DEBUG, "LOG_TAG2", "text:%s ", path2[i] );
        //javaEnvironment->ReleaseStringUTFChars(string, path2[i]);
        //javaEnvironment->DeleteLocalRef(string);
    }

    example = new SuperpoweredExample((unsigned int)samplerate, (unsigned int)buffersize, path, fileAoffset, fileAlength, fileBoffset, fileBlength, path2);
    javaEnvironment->ReleaseStringUTFChars(apkPath, path);
    for (int i=0; i<stringCount; i++) {
        jstring string = (jstring) (*javaEnvironment).GetObjectArrayElement(testPaths, i);
        javaEnvironment->ReleaseStringUTFChars(string, path2[i]);
        javaEnvironment->DeleteLocalRef(string);
    }
}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onSuperPlay(JNIEnv * __unused javaEnvironment, jobject __unused obj) {
	example->onSuperPlay();
}
extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onSuperRecord(JNIEnv * __unused javaEnvironment, jobject __unused obj, jboolean record, jstring targetPath, jstring targetPathWav, jstring tempPath) {
    const char *target = javaEnvironment->GetStringUTFChars(targetPath, JNI_FALSE);
    const char *target2 = javaEnvironment->GetStringUTFChars(tempPath, JNI_FALSE);
    const char *targetWav = javaEnvironment->GetStringUTFChars(targetPathWav, JNI_FALSE);
    __android_log_print(ANDROID_LOG_DEBUG, "setting record function", "text:%s ", target);
    example->onSuperRecord(record, target,targetWav, target2);
    javaEnvironment->ReleaseStringUTFChars(targetPath, target);
    javaEnvironment->ReleaseStringUTFChars(tempPath, target2);
    javaEnvironment->ReleaseStringUTFChars(targetPathWav, targetWav);

}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onTestPlay(JNIEnv * __unused javaEnvironment, jobject __unused obj, jint value) {
    example->onTestPlay(value);
}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onSuperPause(JNIEnv * __unused javaEnvironment, jobject __unused obj) {
	example->onSuperPause();
}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onCrossfader(JNIEnv * __unused javaEnvironment, jobject __unused obj, jint value) {
	example->onCrossfader(value);
}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onFxSelect(JNIEnv * __unused javaEnvironment, jobject __unused obj, jint value) {
	example->onFxSelect(value);
}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onFxOff(JNIEnv * __unused javaEnvironment, jobject __unused obj) {
	example->onFxOff();
}

extern "C" JNIEXPORT void Java_com_superpowered_crossexample_MainActivity_onFxValue(JNIEnv * __unused javaEnvironment, jobject __unused obj, jint value) {
	example->onFxValue(value);
}
