package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.service.MediaCaptureService;

import java.io.File;
import java.io.IOException;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

public class MainActivity extends AppCompatActivity {
    private Dialog mDialog;
    private Button mBtnDialogCancel;
    private Button mBtnDialogQuit;
    private Button mBtnRecord;
    private Button mBtnC3;
    private Button mBtnD3;
    private Button mBtnE3;
    private Button mBtnF3;
    private Button mBtnG3;
    private Button mBtnA3;
    private Button mBtnB3;
    private Button mBtnC4;
    private Button mBtnD4;
    private Button mBtnE4;

    private MediaPlayer mediaPlayerC3;
    private MediaPlayer mediaPlayerD3;
    private MediaPlayer mediaPlayerE3;
    private MediaPlayer mediaPlayerF3;
    private MediaPlayer mediaPlayerG3;
    private MediaPlayer mediaPlayerA3;
    private MediaPlayer mediaPlayerB3;
    private MediaPlayer mediaPlayerC4;
    private MediaPlayer mediaPlayerD4;
    private MediaPlayer mediaPlayerE4;
    private MediaRecorder mediaSoundDetect;
    private MediaProjectionManager mediaProjectionManager;

    private static final int REQUEST_HARMONICAI = 2602;
    private static final int REQUEST_CODE_MEDIA_PROJECTION = 262;

    private int mainBackgroundColor;
    private float currVolume = 0;
    private final float maxAmplitude = 15000;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDeviceScreen();
        setContentView(R.layout.activity_main);
        initView();
        initListener();
    }

    @Override
    public void onBackPressed() {
        mDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_HARMONICAI){
            if (grantResults[0] != PERMISSION_GRANTED){
                Toast.makeText(this, getString(R.string.permissions_warning),
                        Toast.LENGTH_LONG).show();

                Intent intent = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mediaSoundDetect.stop();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                    MainActivity.this,
                    getString(R.string.t_start_recording),
                    Toast.LENGTH_SHORT
                ).show();

                Intent audioCaptureIntent = new Intent(MainActivity.this, MediaCaptureService.class);
                audioCaptureIntent.setAction(MediaCaptureService.ACTION_START);
                audioCaptureIntent.putExtra(MediaCaptureService.EXTRA_RESULT_DATA, data);
                ContextCompat.startForegroundService(MainActivity.this, audioCaptureIntent);
            } else {
                Toast.makeText(
                        MainActivity.this, "Request to get MediaProjection denied.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void initDeviceScreen() {
        // set full screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    private void initView(){
        // init permission
        if (isPermissionNotGranted()){
            requestPermission();
        }

        // init view
        mBtnRecord = findViewById(R.id.btn_record);
        mBtnC3 = findViewById(R.id.c3);
        mBtnD3 = findViewById(R.id.d3);
        mBtnE3 = findViewById(R.id.e3);
        mBtnF3 = findViewById(R.id.f3);
        mBtnG3 = findViewById(R.id.g3);
        mBtnA3 = findViewById(R.id.a3);
        mBtnB3 = findViewById(R.id.b3);
        mBtnC4 = findViewById(R.id.c4);
        mBtnD4 = findViewById(R.id.d4);
        mBtnE4 = findViewById(R.id.e4);
        mainBackgroundColor = ContextCompat.getColor(this, R.color.bg_main_color);

        // init dialog
        Drawable dialogDrawable = ContextCompat.getDrawable(this, R.drawable.background_pop_up);
        mDialog = new Dialog(this);
        mDialog.setContentView(R.layout.custom_dialog);
        mDialog.getWindow().setBackgroundDrawable(dialogDrawable);
        mDialog.getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mDialog.setCancelable(true);

        // init dialog button view
        mBtnDialogCancel = mDialog.findViewById(R.id.btn_cancel);
        mBtnDialogQuit = mDialog.findViewById(R.id.btn_quit);

        // init media player
        mediaPlayerC3 = MediaPlayer.create(this, R.raw.harmonica1);
        mediaPlayerD3 = MediaPlayer.create(this, R.raw.harmonica2);
        mediaPlayerE3 = MediaPlayer.create(this, R.raw.harmonica3);
        mediaPlayerF3 = MediaPlayer.create(this, R.raw.harmonica4);
        mediaPlayerG3 = MediaPlayer.create(this, R.raw.harmonica5);
        mediaPlayerA3 = MediaPlayer.create(this, R.raw.harmonica6);
        mediaPlayerB3 = MediaPlayer.create(this, R.raw.harmonica7);
        mediaPlayerC4 = MediaPlayer.create(this, R.raw.harmonica8);
        mediaPlayerD4 = MediaPlayer.create(this, R.raw.harmonica9);
        mediaPlayerE4 = MediaPlayer.create(this, R.raw.harmonica10);

        mediaPlayerC3.setLooping(true);
        mediaPlayerD3.setLooping(true);
        mediaPlayerE3.setLooping(true);
        mediaPlayerF3.setLooping(true);
        mediaPlayerG3.setLooping(true);
        mediaPlayerA3.setLooping(true);
        mediaPlayerB3.setLooping(true);
        mediaPlayerC4.setLooping(true);
        mediaPlayerD4.setLooping(true);
        mediaPlayerE4.setLooping(true);

        // init microphone detection
        initSoundDetect();
    }

    private void initSoundDetect(){
        mediaSoundDetect = new MediaRecorder();
        mediaSoundDetect.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaSoundDetect.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaSoundDetect.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaSoundDetect.setOutputFile("/dev/null");
        try {
            mediaSoundDetect.prepare();
            mediaSoundDetect.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener(){
        mBtnDialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // close dialog
                mDialog.dismiss();
            }
        });

        mBtnDialogQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // close apps
                mDialog.dismiss();
                finish();
            }
        });

        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording){
                    isRecording = false;
                    mBtnRecord.setText(getString(R.string.t_record));
                    mBtnRecord.setBackgroundResource(R.drawable.record_button);

                    stopAudioCapture();
                } else {
                    isRecording = true;
                    mBtnRecord.setText("");
                    mBtnRecord.setBackgroundResource(R.drawable.recording_button);

                    // media projection request
                    startMediaProjectionRequest();
                }
            }
        });

        mBtnC3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnC3.setBackgroundColor(Color.RED);
                if (!mediaPlayerC3.isPlaying()) mediaPlayerC3.start();
                return false;
            }
        });

        mBtnC3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                            || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnC3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerC3.isPlaying()) mediaPlayerC3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerC3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnD3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnD3.setBackgroundColor(Color.RED);
                if (!mediaPlayerD3.isPlaying()) mediaPlayerD3.start();
                return false;
            }
        });

        mBtnD3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnD3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerD3.isPlaying()) mediaPlayerD3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerD3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnE3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnE3.setBackgroundColor(Color.RED);
                if (!mediaPlayerE3.isPlaying()) mediaPlayerE3.start();
                return false;
            }
        });

        mBtnE3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnE3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerE3.isPlaying()) mediaPlayerE3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerE3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnF3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnF3.setBackgroundColor(Color.RED);
                if (!mediaPlayerF3.isPlaying()) mediaPlayerF3.start();
                return false;
            }
        });

        mBtnF3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnF3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerF3.isPlaying()) mediaPlayerF3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerF3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnG3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnG3.setBackgroundColor(Color.RED);
                if (!mediaPlayerG3.isPlaying()) mediaPlayerG3.start();
                return false;
            }
        });

        mBtnG3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnG3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerG3.isPlaying()) mediaPlayerG3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerG3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnA3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnA3.setBackgroundColor(Color.RED);
                if (!mediaPlayerA3.isPlaying()) mediaPlayerA3.start();
                return false;
            }
        });

        mBtnA3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnA3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerA3.isPlaying()) mediaPlayerA3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerA3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnB3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnB3.setBackgroundColor(Color.RED);
                if (!mediaPlayerB3.isPlaying()) mediaPlayerB3.start();
                return false;
            }
        });

        mBtnB3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnB3.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerB3.isPlaying()) mediaPlayerB3.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerB3.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnC4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnC4.setBackgroundColor(Color.RED);
                if (!mediaPlayerC4.isPlaying()) mediaPlayerC4.start();
                return false;
            }
        });

        mBtnC4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnC4.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerC4.isPlaying()) mediaPlayerC4.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerC4.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnD4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnD4.setBackgroundColor(Color.RED);
                if (!mediaPlayerD4.isPlaying()) mediaPlayerD4.start();
                return false;
            }
        });

        mBtnD4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnD4.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerD4.isPlaying()) mediaPlayerD4.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerD4.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });

        mBtnE4.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mBtnE4.setBackgroundColor(Color.RED);
                if (!mediaPlayerE4.isPlaying()) mediaPlayerE4.start();
                return false;
            }
        });

        mBtnE4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getAction() == MotionEvent.ACTION_UP){
                    mBtnE4.setBackgroundColor(mainBackgroundColor);
                    if(mediaPlayerE4.isPlaying()) mediaPlayerE4.pause();
                } else if (motionEvent.getAction() == MotionEvent.ACTION_DOWN
                        || motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    getCurrentVolume();
                    mediaPlayerE4.setVolume(currVolume, currVolume);
                }
                return false;
            }
        });
    }

    private double soundLevel() {
        if (isPermissionNotGranted()) {
            requestPermission();
            return 0;
        }

        return mediaSoundDetect.getMaxAmplitude();
    }

    private boolean isPermissionNotGranted(){
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED);
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.FOREGROUND_SERVICE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                },
                REQUEST_HARMONICAI);
    }

    private void getCurrentVolume(){
        double soundLevel = soundLevel();
        float percent = (float) (soundLevel / maxAmplitude);

        Log.e("harmonicai", "getCurrentVolume: "+soundLevel);

        if (soundLevel < 400) {
            currVolume = 0;
        } else {
            currVolume = percent * 1;
            if (currVolume > 1) currVolume = 1;
        }
    }

    private void startMediaProjectionRequest(){
        mediaProjectionManager = (MediaProjectionManager) this.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MEDIA_PROJECTION);
    }

    private void stopAudioCapture(){
        Intent intent = new Intent(MainActivity.this, MediaCaptureService.class);
        intent.setAction(MediaCaptureService.ACTION_STOP);
        ContextCompat.startForegroundService(MainActivity.this, intent);
    }
}
