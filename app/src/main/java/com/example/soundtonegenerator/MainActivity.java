package com.example.soundtonegenerator;

import android.app.Activity;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity implements View.OnClickListener{

    private final int duration = 3; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private final double freqOfTone = 1000; // hz
    private final byte generatedSnd[] = new byte[2 * numSamples];
    private AudioManager aMgr;
    Handler handler = new Handler();
    Button btngentone;

    private static final String TAG = "AudioStream";

    private Set<Integer> deviceTypes =
            new HashSet<>(Arrays.asList(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_LINE_ANALOG,
                    AudioDeviceInfo.TYPE_LINE_DIGITAL,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_HDMI,
                    AudioDeviceInfo.TYPE_HDMI_ARC,
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_ACCESSORY,
                    AudioDeviceInfo.TYPE_DOCK,
                    AudioDeviceInfo.TYPE_FM,
                    AudioDeviceInfo.TYPE_BUILTIN_MIC,
                    AudioDeviceInfo.TYPE_TELEPHONY,
                    AudioDeviceInfo.TYPE_AUX_LINE,
                    AudioDeviceInfo.TYPE_IP,
                    AudioDeviceInfo.TYPE_BUS,
                    AudioDeviceInfo.TYPE_USB_HEADSET,
                    AudioDeviceInfo.TYPE_HEARING_AID));

    @RequiresApi(api = Build.VERSION_CODES.M)
    public int findDeviceType() {
        final AudioDeviceInfo[] devices = aMgr.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for (AudioDeviceInfo device : devices) {
            final int type = device.getType();
            if (deviceTypes.contains(type)) {
                Log.i(TAG,Integer.toString(type));
                return type;
            }
        }
        return -1;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public float audioStreamVol(Context context){
        aMgr = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int curType = (int) aMgr.STREAM_MUSIC;
        int vol = aMgr.getStreamVolume(curType);
        int deviceType = findDeviceType();
        float volumedB = aMgr.getStreamVolumeDb(curType, vol, deviceType);
        return volumedB;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btngentone = (Button) findViewById(R.id.btngentone);
        btngentone.setOnClickListener(this);
        float f = audioStreamVol(MainActivity.this);
        Log.i(TAG,Float.toString(f));
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.btngentone:
                //Do something
                // Use a new tread as this can take a while
                final Thread thread = new Thread(new Runnable() {
                    public void run() {
                        genTone();
                        handler.post(new Runnable() {
                            public void run() {
                                playSound();
                            }
                        });
                    }
                });
                thread.start();
        }
    }

    void genTone(){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }
}