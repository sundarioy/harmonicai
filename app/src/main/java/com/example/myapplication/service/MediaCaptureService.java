package com.example.myapplication.service;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import linc.com.pcmdecoder.PCMDecoder;

import static android.media.AudioFormat.CHANNEL_IN_MONO;

public class MediaCaptureService extends Service {

    private File file;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private AudioRecord audioRecord;
    private Thread audioCaptureThread;

    private static final int NUM_SAMPLES_PER_READ = 1024;
    private static final int BYTES_PER_SAMPLE = 2; // 2 bytes since we hardcoded the PCM 16-bit format
    private static final int BUFFER_SIZE_IN_BYTES = NUM_SAMPLES_PER_READ * BYTES_PER_SAMPLE;
    private static final int SERVICE_ID = 2666;
    private static final int SAMPLE_RATE = 44100;
    private static final int DECODE_SAMPLE_RATE = 22000;
    private static final int SAMPLE_BITRATE = 128000;
    private static final int SAMPLE_CHANNEL = CHANNEL_IN_MONO;
    private static final int DECODE_CHANNEL = 2;

    private static final String NOTIFICATION_CHANNEL_ID = "AudioCapture harmonicai channel";
    public static final String ACTION_START = "AudioCaptureService:Start";
    public static final String ACTION_STOP = "AudioCaptureService:Stop";
    public static final String EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("harmonicai", "service onCreate: ");
        createNotificationChannel();
        startForeground(SERVICE_ID, new NotificationCompat.Builder(this,
                NOTIFICATION_CHANNEL_ID).build());
        mediaProjectionManager = (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            Log.e("harmonicai", "onStartCommand: "+intent.getAction());
            if (intent.getAction().equalsIgnoreCase(ACTION_START)){
                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) intent.getParcelableExtra(EXTRA_RESULT_DATA));
                startAudioCapture();
                return Service.START_STICKY;
            } else {
                stopAudioCapture();
                return Service.START_NOT_STICKY;
            }
        } else {
            return Service.START_NOT_STICKY;
        }
    }

    private void startAudioCapture(){
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(SAMPLE_CHANNEL)
                .build();

        audioRecord = new AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
                .setAudioPlaybackCaptureConfig(config)
                .build();

        audioRecord.startRecording();

        audioCaptureThread = new Thread(){
            @Override
            public void run() {
                file = createAudioFile();
                Log.e("harmonicai", "filepath: "+file.getAbsolutePath());
                writeAudioToFile(file);
            }
        };
        audioCaptureThread.start();
    }

    private void stopAudioCapture(){
        Log.e("harmonicai", "stopAudioCapture: ");
        try {
            audioCaptureThread.interrupt();
            audioCaptureThread.join();
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            mediaProjection.stop();
        } catch (Exception e){
            e.printStackTrace();
        }

        try {
            stopSelf();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private File createAudioFile() {
        File audioCapturesDirectory = new File(getExternalFilesDir(null), "/record");
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs();
        }

        String timestamp = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(new Date());
        String fileName = "harmonicai-"+timestamp+".pcm";

        Log.e("harmonicai", "createAudioFile: "+fileName);

        return new File(audioCapturesDirectory.getAbsolutePath() + "/" + fileName);
    }

    private void writeAudioToFile(final File outputFile) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            short[] capturedAudioSamples = new short[NUM_SAMPLES_PER_READ];

            while (!audioCaptureThread.isInterrupted()) {
                audioRecord.read(capturedAudioSamples, 0, NUM_SAMPLES_PER_READ);

                fileOutputStream.write(
                        getByteArray(capturedAudioSamples),
                        0,
                        BUFFER_SIZE_IN_BYTES
                );
            }

            fileOutputStream.close();
            Log.e("harmonicai", "Audio capture finished for "+outputFile.getAbsolutePath());

            convertToMp3(outputFile);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void convertToMp3(File pcmFile) {
        String timestamp = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.US).format(new Date());
        String fileName = "harmonicai-"+timestamp+".mp3";

        final File mp3File = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        PCMDecoder.encodeToMp3(
                pcmFile.getAbsolutePath(),
                DECODE_CHANNEL,
                SAMPLE_BITRATE,
                DECODE_SAMPLE_RATE,
                mp3File.getAbsolutePath()
        );

        ContextCompat.getMainExecutor(getBaseContext()).execute(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), "Output file: "+mp3File.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private byte[] getByteArray(short[] sData){
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void createNotificationChannel(){
        NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                "Audio Capture Service Channel", NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager manager = (NotificationManager) this.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }
}
