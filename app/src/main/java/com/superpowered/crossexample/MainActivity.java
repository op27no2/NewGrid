package com.superpowered.crossexample;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import net.bohush.geometricprogressview.GeometricProgressView;

import java.io.File;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    boolean playing = false;
    private TransferUtility transferUtility;
    private Context mContext;
    private File mFiles[] = new File[9];
    private String[] mPaths = new String[9];
    private Button myButtons[] = new Button[12];
    private int complete = 0;
    private GeometricProgressView myLoaders[] = new GeometricProgressView[9];
    private AmazonS3 s3;
    private String samplerateString = null;
    private String buffersizeString = null;;
    private Handler handler = new Handler();
    private RelativeLayout voteLayout;
    private LinearLayout ll1;
    private LinearLayout ll2;
    private LinearLayout ll3;
    private LinearLayout ll4;
    private boolean isBig = true;
    private boolean record = false;

    //looper elements
    private String TAG = "AUDIO_RECORD_PLAYBACK";
    private boolean isRunning = false;
    private Thread m_thread;               /* Thread for running the Loop */

    private AudioRecord recorder = null;
    private AudioTrack track = null;

    int bufferSize = 320;                  /* Buffer for recording data */
    byte buffer[] = new byte[bufferSize];
    private static int[] mSampleRates = new int[] { 8000, 11025, 22050, 44100 };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext=this;
        isStoragePermissionGranted();

        // Get the device's sample rate and buffer size to enable low-latency Android audio output, if available.
        if (Build.VERSION.SDK_INT >= 17) {
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";
        //set up button UI
        for(int i=0; i<12; i++) {
            int resID = getResources().getIdentifier("button"+(i), "id", "com.superpowered.crossexample");
            myButtons[i] = ((Button) findViewById(resID));
            System.out.println(resID+" "+myButtons[i]);
            setOnClick(myButtons[i], i);
        }


        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:0017dc8f-a06d-4d3f-9b21-2f44d4ba7253", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );
        s3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3, mContext);
        downloadSamples();

        ll1 = (LinearLayout) findViewById(R.id.row1);
        ll2 = (LinearLayout) findViewById(R.id.row2);
        ll3 = (LinearLayout) findViewById(R.id.row3);
        ll4 = (LinearLayout) findViewById(R.id.row4);
        voteLayout = (RelativeLayout) findViewById(R.id.vote_panel);

        Button mButton = (Button) findViewById(R.id.playPause);
        mButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    System.out.println("touched");
                    SuperpoweredExample_Play();
                    return true;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    System.out.println("touched2");
                    SuperpoweredExample_Play();
                    return true;
                }
                return false;
            }
        });
        Button mButton2 = (Button) findViewById(R.id.pause);
        mButton2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                    SuperpoweredExample_Pause();

                return false;
            }
        });

        Button mButton3 = (Button) findViewById(R.id.toggle_record);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SuperpoweredExample_Record(record);
                record = !record;

            }
        });

       Button mStartLoopButton = (Button) findViewById(R.id.loop_start);
        mStartLoopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("start loop clicked");

                if(!isRunning) {
                    isRunning = true;
                    do_loopback(isRunning);
                }
            }
        });

       Button mStopLoopButton = (Button) findViewById(R.id.loop_stop);
        mStopLoopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("stop loop clicked");
                if(isRunning) {
                    isRunning = false;
                    do_loopback(isRunning);
                }
            }
        });

        Button voteButton = (Button) findViewById(R.id.voting);
        voteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isBig) {
                    System.out.println("vote clicked: ");
                    int width = ll1.getWidth() / 2;
                    int height = ll1.getHeight() / 2;
                    ll1.getLayoutParams().height = height;
                    ll2.getLayoutParams().height = height;
                    ll3.getLayoutParams().height = height;
                    ll1.getLayoutParams().width = width;
                    ll2.getLayoutParams().width = width;
                    ll3.getLayoutParams().width = width;
                    ll4.setVisibility(View.VISIBLE);
                    voteLayout.setVisibility(View.VISIBLE);

                    ll1.requestLayout();
                    isBig = false;
                }else{
                    System.out.println("vote clicked: ");
                    int width = ll1.getWidth() * 2;
                    int height = ll1.getHeight() * 2;
                    ll1.getLayoutParams().height = height;
                    ll2.getLayoutParams().height = height;
                    ll3.getLayoutParams().height = height;
                    ll1.getLayoutParams().width = width;
                    ll2.getLayoutParams().width = width;
                    ll3.getLayoutParams().width = width;
                    ll4.setVisibility(View.GONE);
                    voteLayout.setVisibility(View.GONE);
                    ll1.requestLayout();
                    isBig = true;
                }

            }
        });


        Runnable mrunnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("button width: " + myButtons[0].getWidth());
                int height = myButtons[0].getWidth();
                ll1.getLayoutParams().height = height;
                ll2.getLayoutParams().height = height;
                ll3.getLayoutParams().height = height;

                ll4.getLayoutParams().height = ll3.getHeight()/2;
                ll4.getLayoutParams().width = ll3.getWidth()/2;
            }
        };
        handler.postDelayed(mrunnable, 50);

        //set up loader UI
        for(int i=0; i<9; i++) {
            int resID = getResources().getIdentifier("progress_view"+(i), "id", "com.superpowered.crossexample");
            myLoaders[i] = ((GeometricProgressView) findViewById(resID));
            System.out.println(resID+" "+myLoaders[i]);
        }

        // crossfader events
        final SeekBar crossfader = (SeekBar)findViewById(R.id.crossFader);
        if (crossfader != null) crossfader.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onCrossfader(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // fx fader events
        final SeekBar fxfader = (SeekBar)findViewById(R.id.fxFader);
        if (fxfader != null) fxfader.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onFxValue(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                onFxValue(seekBar.getProgress());
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                onFxOff();
            }
        });

        // fx select event
        final RadioGroup group = (RadioGroup)findViewById(R.id.radioGroup1);
        if (group != null) group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                RadioButton checkedRadioButton = (RadioButton)radioGroup.findViewById(checkedId);
                onFxSelect(radioGroup.indexOfChild(checkedRadioButton));
            }
        });

    }

    @Override
    protected void onPause(){
        super.onPause();
        System.out.println("ON PAUSE");

        if(recorder !=null) {
            recorder.stop();
            recorder.release();
        }
    }




    private void setOnClick(final Button btn, final int pos) {
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    SuperpoweredExample_TestPlay(pos);
                    v.vibrate(10);
                    return true;
                }
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    SuperpoweredExample_TestPlay(pos);
                    v.vibrate(10);
                    return true;
                }
                return false;
            }
        });




    }

    public void SuperpoweredExample_Play() {  // Play/pause.
        //playing = !playing;
        onSuperPlay();
        Button b = (Button) findViewById(R.id.playPause);
       // if (b != null) b.setText(playing ? "Pause" : "Play");

    }
    public void SuperpoweredExample_Record(boolean record) {  // Play/pause.

        String fileTarget = Environment.getExternalStorageDirectory().toString() + "/" + "recording";
        String fileTargetWav = Environment.getExternalStorageDirectory().toString() + "/" + "recording.wav";
        String tempTarget = Environment.getExternalStorageDirectory().toString() + "/" + "recordingtemp.tmp";
        System.out.println("filetarget: "+fileTarget);
        onSuperRecord(record, fileTarget,fileTargetWav, tempTarget);
    }
    public void SuperpoweredExample_TestPlay(int value) {  // Play/pause.
        onTestPlay(value);
    }

    public void SuperpoweredExample_Pause() { //pause.
        onSuperPause();

    }
        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private native void SuperpoweredExample(int samplerate, int buffersize, String apkPath, int fileAoffset, int fileAlength, int fileBoffset, int fileBlength, String[] mPaths);
    private native void onSuperPlay();
    private native void onSuperRecord(boolean record, String fileTarget, String fileTargetWav, String tempTarget);
    private native void onTestPlay(int value);
    private native void onSuperPause();
    private native void onCrossfader(int value);
    private native void onFxSelect(int value);
    private native void onFxOff();
    private native void onFxValue(int value);


    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {

            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
                Log.v("1","Permission is granted");
            } else {
                Log.v("1","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_GRANTED) {
                Log.v("1","Permission is granted");
            } else {
                Log.v("1","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, 1);
            }
            // just returning true regardless, change function to use boolean if you want
            return true;

        }

        else { //permission is automatically granted on sdk<23 upon installation
            Log.v("1","Permission is granted");
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v("1","Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    public void downloadSamples(){
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                for(int i = 0; i<9; i++) {
                    String extension = "";
                    ObjectMetadata objectData = s3.getObjectMetadata("thegridsamples","file"+i);
                    if(objectData != null) {
                        Map<String, String> mapData = objectData.getUserMetadata();
                        extension = mapData.get("extension");
                    }
                    String fileTarget = Environment.getExternalStorageDirectory().toString() + "/" + "gridfile"+i+"."+extension;

                    final File file = new File(fileTarget);
                    TransferObserver observer = transferUtility.download(
                            "thegridsamples",     /* The bucket to upload to */
                            "file" + i,    /* The key for the uploaded object */
                            file        /* The file where the data to upload exists */
                    );
                    final int finalI1 = i;

                    observer.setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            System.out.println("download state changed");
                            if(state.equals(TransferState.COMPLETED)) {
                                complete++;
                                myLoaders[finalI1].setVisibility(View.GONE);
                                mFiles[finalI1] = file;
                                //SuperpoweredSetup(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString), file.getPath(), finalI1);
                                System.out.println("download state completed "+finalI1+":" + state);
                                if(complete == 9) {
                                 finishSetup(mFiles);
                                }
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            if(bytesTotal!=0) {
                                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                                System.out.println("download progress:" + percentage);
                            }
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            System.out.println("download error:" + ex.getMessage());
                        }

                    });
                }


                return null;
            }

            @Override
            protected void onPostExecute(Void mnada) {
                super.onPostExecute(mnada);
            }
        }.execute();
    }

    private void finishSetup(File[] files){
        // Get the device's sample rate and buffer size to enable low-latency Android audio output, if available.
        String samplerateString = null, buffersizeString = null;
        if (Build.VERSION.SDK_INT >= 17) {
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";

        //String testPath = Environment.getExternalStorageDirectory().toString() + "/" + "gridfile1";
        for(int i=0;i<9;i++) {
            mPaths[i] = mFiles[i].getPath();
            System.out.println("testpaths: "+mPaths[i]);
        }

        // Arguments: path to the APK file, offset and length of the two resource files, sample rate, audio buffer size.
        SuperpoweredExample(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString), getPackageResourcePath(), 0, 0, 0, 0, mPaths);
    }

    static {
        System.loadLibrary("SuperpoweredExample");
    }


    //LOOPER METHODS

    private void do_loopback(final boolean flag)
    {
        m_thread = new Thread(new Runnable() {
            public void run() {
                run_loop(flag);
            }
        });
        m_thread.start();
    }

    public AudioTrack findAudioTrack (AudioTrack track)
    {
        Log.d(TAG, "===== Initializing AudioTrack API ====");
        int m_bufferSize = AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (m_bufferSize != AudioTrack.ERROR_BAD_VALUE)
        {
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, m_bufferSize,
                    AudioTrack.MODE_STREAM);

            if (track.getState() == AudioTrack.STATE_UNINITIALIZED) {
                Log.e(TAG, "===== AudioTrack Uninitialized =====");
                return null;
            }
        }
        return track;
    }

    //from example https://stackoverflow.com/questions/25780130/audio-recording-and-streaming-in-android
/*    public AudioRecord findAudioRecord (AudioRecord recorder)
    {
        Log.d(TAG, "===== Initializing AudioRecord API =====");
        int m_bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (m_bufferSize != AudioRecord.ERROR_BAD_VALUE)
        {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, m_bufferSize);

            if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.e(TAG, "====== AudioRecord UnInitilaised ====== ");
                return null;
            }
        }
        return recorder;
    }*/

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        System.out.println( "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e("tag", rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

    public void run_loop (boolean isRunning)
    {

        /** == If Stop Button is pressed == **/
        if (isRunning == false) {
            Log.d(TAG, "=====  Stop Button is pressed ===== ");

            if (AudioRecord.STATE_INITIALIZED == recorder.getState()){
                recorder.stop();
                recorder.release();
            }
            if (AudioTrack.STATE_INITIALIZED == track.getState()){
                track.stop();
                track.release();
            }
            return;
        }


        /** ======= Initialize AudioRecord and AudioTrack ======== **/
        //recorder = findAudioRecord(recorder);
        recorder = findAudioRecord();
        boolean test = (recorder.getState() == AudioRecord.STATE_INITIALIZED);
        System.out.println("recorder state: " + test);

        if (recorder == null) {
            Log.e(TAG, "======== findAudioRecord : Returned Error! =========== ");
            return;
        }

        track = findAudioTrack(track);
        if (track == null) {
            Log.e(TAG, "======== findAudioTrack : Returned Error! ========== ");
            return;
        }

        if ((AudioRecord.STATE_INITIALIZED == recorder.getState()) &&
                (AudioTrack.STATE_INITIALIZED == track.getState()))
        {
            recorder.startRecording();
            Log.d(TAG, "========= Recorder Started... =========");
            track.play();
            Log.d(TAG, "========= Track Started... =========");
        }
        else
        {
            Log.d(TAG, "==== Initilazation failed for AudioRecord or AudioTrack =====");
            return;
        }

        /** ------------------------------------------------------ **/

    /* Recording and Playing in chunks of 320 bytes */
        bufferSize = 320;

        while (isRunning == true)
        {
        /* Read & Write to the Device */
            recorder.read(buffer, 0, bufferSize);
            track.write(buffer, 0, bufferSize);

        }
        Log.i(TAG, "Loopback exit");
        return;
    }

}
