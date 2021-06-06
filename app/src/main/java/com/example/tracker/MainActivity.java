package com.example.tracker;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.FloatFFT_1D;

import java.lang.Math;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainActivity extends AppCompatActivity {
    final int SAMPLE_RATE = 44100;
    final int BUFFER_SIZE = 1764;
    final double SWEEP_TIME_SECS = 0.04;
    final double MIN_FREQ_1 = 16000; //17000;
    final double MIN_FREQ_2 = 19000; //22500;
    final double BANDWIDTH = 2500;

    RecorderStereo recorder = null;
    GraphView fftMagGraph = null;
    GraphView maxDistanceGraph = null;
    RadioGroup modeSelector = null;

    TextView calibrationResult = null;
    LineGraphSeries fftMagSeries = null;
    LineGraphSeries maxDistanceSeries = null;
    DoubleFFT_1D doubleFFT_1D = null;
    long startTime = System.currentTimeMillis();
    double timeSecs = 0;
    double timeOffset1 = 0;
    double timeOffset2 = 0;
    double maxDistance;
    double interSpeakerDistance = 0.33; //(distance between macbook speakers in meters)

    private final Handler mHandler = new Handler();
    private Runnable updatePlot;
    DataPoint[] fftMagDps = new DataPoint[]{};

    // Synchronization
    ReentrantLock lock = new ReentrantLock();

    RecorderStereo.Callback trackerCallback = new RecorderStereo.Callback() {
        @Override
        public void call(short[] data) {
            // Perform FFT
            double[] double_signal_1 = new double[BUFFER_SIZE];
            double[] double_signal_2 = new double[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                double_signal_1[i] = data[i] * hanningWindow(i) * signal(timeSecs - timeOffset1, MIN_FREQ_1);
                double_signal_2[i] = data[i] * hanningWindow(i) * signal(timeSecs - timeOffset1, MIN_FREQ_2);
                timeSecs += 1. / SAMPLE_RATE;
            }
            doubleFFT_1D.realForward(double_signal_1);
            doubleFFT_1D.realForward(double_signal_2);
            // Update Plot
            lock.lock();
            fftMagDps = new DataPoint[BUFFER_SIZE / 2];
            double[] distances = new double[BUFFER_SIZE / 2];
            double[] magnitudes = new double[BUFFER_SIZE / 2];
            for (int i = 0; i < BUFFER_SIZE / 2; i++) {
                double re = double_signal_1[i * 2];
                double im = double_signal_1[i * 2 + 1];
                double magnitude = Math.sqrt(re * re + im * im) / 32768;
                double freq = (double) i * SAMPLE_RATE / BUFFER_SIZE;
                double distance = freq * 340 * SWEEP_TIME_SECS / BANDWIDTH;
                DataPoint magDp = new DataPoint((float) distance, magnitude);
                fftMagDps[i] = magDp;
                distances[i] = distance;
                magnitudes[i] = magnitude;
            }

            maxDistance = 0;
            int stride = 10;
            double max_val = 0;
            for (int i = 0; i < BUFFER_SIZE / 2; i++) {
                Log.d("Peak Detection", "idx: " + i);
                if ((magnitudes[i] > max_val) && (i >= stride) && (i < BUFFER_SIZE / 2 - stride)) {
                    max_val = magnitudes[i];
                    Log.d("Peak Detection", "Freq: " + distances[i] + " Value: " + magnitudes[i - stride] + ", " + magnitudes[i] + ", " + magnitudes[i + stride]);
                }
                if (i < stride) {
                    if (magnitudes[i] > magnitudes[i + stride] * 2) {
                        maxDistance = distances[i];
                        break;
                    }
                } else if ((i > BUFFER_SIZE / 2 - stride)) {
                    if (magnitudes[i] > magnitudes[i - stride] * 2) {
                        maxDistance = distances[i];
                        break;
                    }
                } else {
                    if ((magnitudes[i] > magnitudes[i - stride] * 2) && (magnitudes[i] > magnitudes[i + stride] * 2)) {
                        maxDistance = distances[i];
                        break;
                    }
                }
            }
            lock.unlock();
        }
    };

    RecorderStereo.Callback calibrationCallback = new RecorderStereo.Callback() {
        @Override
        public void call(short[] data) {
            // Perform FFT
            double[] recv_signal = new double[BUFFER_SIZE];
            double[] transmit_signal = new double[BUFFER_SIZE];
            double time_secs = 0;
            for (int i = 0; i < BUFFER_SIZE; i++) {
                recv_signal[i] = data[i] * 1. / 10000;
                transmit_signal[i] = signal(time_secs, MIN_FREQ_1);
//                transmit_signal[i] = signal(time_secs, MIN_FREQ_2);
                time_secs += 1. / SAMPLE_RATE;

            }
            doubleFFT_1D.realForward(recv_signal);
            doubleFFT_1D.realForward(transmit_signal);
            double[] cross_signal = new double[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                cross_signal[i] = recv_signal[i] * transmit_signal[i];
            }
            doubleFFT_1D.realInverse(cross_signal, true);

            // Update Plot
            lock.lock();
            maxDistance = 0;
            double max_correlation = 0;
            for (int i = 0; i < BUFFER_SIZE / 2; i++) {
                if (cross_signal[i] > max_correlation) {
                    max_correlation = cross_signal[i];
                    timeOffset1 = i * 1. / SAMPLE_RATE;
                }
            }
            Log.d("Calibration", "Offset: " + timeOffset1);
            lock.unlock();
        }
    };

    private double signal(double time_secs, double MIN_FREQ) {
        double time_in_cycle = time_secs % SWEEP_TIME_SECS;
        return Math.sin(2 * Math.PI * MIN_FREQ * time_in_cycle +
                Math.PI * BANDWIDTH * time_in_cycle * time_in_cycle / SWEEP_TIME_SECS);
    }

    private double hanningWindow(int idx) {
        return 0.5 * (1 - Math.cos(2 * Math.PI * idx) / (BUFFER_SIZE - 1));
    }

//    public void smoothAmplitudes(){
//        for (int i = 0; i < fft.specSize(); ++i) {
//            float smoothed_amp = SMOOTHING_ALPHA * fft.getBand(i) + (1 - SMOOTHING_ALPHA) * prev_freqs[i];
//            fft.setBand(i, smoothed_amp);
//        }
//    }
    private void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Requesting Permission.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    123);
        } else {
            Log.d("MainActivity", "Recording Permission granted.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkRecordPermission();

        calibrationResult = (TextView) findViewById(R.id.calibrateResult);
        fftMagGraph = (GraphView) findViewById(R.id.fftMagGraph);
        maxDistanceGraph = (GraphView) findViewById(R.id.maxFreqGraph);
        initPlot();
        doubleFFT_1D = new DoubleFFT_1D(BUFFER_SIZE);


        // Set up buttons.
        final Button btnRecord = (Button) findViewById(R.id.btnRecord);
        btnRecord.setText("Record");
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnRecord.getText() == "Stop") {
                    // Speaker currently on, stop speaker.
                    try {
                        recorder.stopRecord();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    btnRecord.setText("Record");
                } else {
                    // Speaker currently off, start speaker.
                    initRecorder();
                    recorder.start();
                    btnRecord.setText("Stop");
                }
            }
        });

        modeSelector = (RadioGroup) findViewById(R.id.modeSelector);
        modeSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.modeCalibrate) {
                    recorder.sink = calibrationCallback;
                } else {
                    recorder.sink = trackerCallback;
                }
            }
        });

        updatePlot = new Runnable() {
            @Override
            public void run() {
                lock.lock();
                fftMagSeries.resetData(fftMagDps);
                long time = System.currentTimeMillis() - startTime;
                maxDistanceSeries.appendData(new DataPoint(time, maxDistance), true, 400);
                mHandler.postDelayed(this, 40);
                calibrationResult.setText("Calibrated Offset: " + timeOffset1);
                lock.unlock();
            }
        };
        mHandler.postDelayed(updatePlot, 40);

    }


    void initRecorder() {
        if (modeSelector.getCheckedRadioButtonId() == R.id.modeCalibrate) {
            recorder = new RecorderStereo(SAMPLE_RATE, BUFFER_SIZE, calibrationCallback);
        } else {
            recorder = new RecorderStereo(SAMPLE_RATE, BUFFER_SIZE, trackerCallback);
        }
    }


    void initPlot() {
        DataPoint[] dps = new DataPoint[BUFFER_SIZE / 2];
        for (int i = 0; i < BUFFER_SIZE / 2; i++) {
            DataPoint v = new DataPoint((float) i * SAMPLE_RATE / BUFFER_SIZE, 1);
            dps[i] = v;
        }
        fftMagSeries = new LineGraphSeries<DataPoint>(dps);
        fftMagGraph.addSeries(fftMagSeries);
//        fftMagGraph.getViewport().setYAxisBoundsManual(true);
//        fftMagGraph.getViewport().setMinY(0);
//        fftMagGraph.getViewport().setMaxY(50);
        fftMagGraph.getViewport().setXAxisBoundsManual(true);
        fftMagGraph.getViewport().setMinX(0);
        fftMagGraph.getViewport().setMaxX(13.6);

        maxDistanceSeries = new LineGraphSeries<DataPoint>();
        maxDistanceGraph.addSeries(maxDistanceSeries);
        maxDistanceGraph.getViewport().setYAxisBoundsManual(true);
        maxDistanceGraph.getViewport().setMinY(0);
        maxDistanceGraph.getViewport().setMaxY(2);
        maxDistanceGraph.getViewport().setXAxisBoundsManual(true);
        maxDistanceGraph.getViewport().setMinX(0);
        maxDistanceGraph.getViewport().setMaxX(10000);
        maxDistanceGraph.getViewport().setScrollable(true);
    }
}