package com.example.tracker;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

// class to access the stereo microphones
public class RecorderStereo extends Thread {

    public static interface Callback {
        void call(short[] data);
    }

    public boolean recording;
    int samplingfrequency;
    public short[] temp;
    int count;
    AudioRecord rec;
    int minbuffersize;
    Callback sink;

    // sink: define a callback instance to process the received data
    public RecorderStereo(int samplingfreq, int bufferSize, Callback sink) {
        samplingfrequency = samplingfreq;
        minbuffersize = bufferSize; //AudioRecord.getMinBufferSize(samplingfrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.d("recorder", samplingfreq + " " + minbuffersize);
        this.sink = sink;
        count = 0;

        rec = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingfrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minbuffersize * 2);
        Log.d("recorder", "Min Buf Size: " + AudioRecord.getMinBufferSize(samplingfrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
        Log.d("recorder", "initialized? " + (rec.getState() == AudioRecord.STATE_INITIALIZED));
        Log.d("recorder", "bad value: " +  AudioRecord.ERROR_BAD_VALUE);
        temp = new short[minbuffersize];
    }

    public void stopRecord() throws InterruptedException {
        this.recording = false;
        this.join();
    }

    public void run() {
        int bytesread = 0;
        rec.startRecording();
        recording = true;
        try {
            while (recording) {
                while (bytesread < minbuffersize) {
                    bytesread += rec.read(temp, bytesread, minbuffersize - bytesread);
                }
                bytesread = 0;
                sink.call(temp);
            }
            rec.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}