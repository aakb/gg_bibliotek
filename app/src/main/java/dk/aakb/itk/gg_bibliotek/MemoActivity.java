package dk.aakb.itk.gg_bibliotek;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import dk.aakb.itk.gg_bibliotek.R;

public class MemoActivity extends Activity implements GestureDetector.BaseListener {
    private static final String TAG = "MemoActivity";

    private MediaRecorder mRecorder;
    private TextView durationText;

    private Timer timer;
    private int timerExecutions = 0;
    private boolean recording = false;

    private String outputPath;
    private String filePrefix;

    private AudioManager audioManager;
    private GestureDetector gestureDetector;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching memo activity");

        // Get file prefix
        Intent intent = getIntent();
        filePrefix = intent.getStringExtra("FILE_PREFIX");

        setContentView(R.layout.activity_record_memo);

        durationText = (TextView) findViewById(R.id.text_memo_duration);
        outputPath = getOutputVideoFile().toString();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gestureDetector = new GestureDetector(this).setBaseListener(this);

        startRecording();
    }

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        super.onPause();

        timer.cancel();
        stopRecording();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        timer.cancel();
        stopRecording();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);

            if (recording) {
                Log.i(TAG, "Stop recording!");

                timer.cancel();

                try {
                    stopRecording();

                    // Add path to file as result
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("path", outputPath);
                    setResult(RESULT_OK, returnIntent);

                    recording = false;

                    // Finish activity
                    finish();
                } catch (Exception e) {
                    Log.d(TAG, "Exception stopping recording: " + e.getMessage());
                    stopRecording();
                    finish();
                }
            }

            return true;
        }
        return false;
    }

    private void startRecording() {
        durationText.setText("0 sec");

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(outputPath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        (new Timer()).schedule(new TimerTask() {
            @Override
            public void run() {

                mRecorder.start();

                recording = true;

                Log.i(TAG, "is recording");

                // Count down from videoLength seconds, then take picture.
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        timerExecutions++;

                        Log.i(TAG, "" + timerExecutions);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                durationText.setText(timerExecutions + " sec");
                            }
                        });
                    }
                }, 1000, 1000);
            }
        }, 1000);
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    /**
     * Create a File for saving a video
     */
    private File getOutputVideoFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                filePrefix + "_audio_" + timeStamp + ".mp3");
        return mediaFile;
    }
}