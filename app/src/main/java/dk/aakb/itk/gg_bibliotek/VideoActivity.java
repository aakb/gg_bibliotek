package dk.aakb.itk.gg_bibliotek;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class VideoActivity extends Activity implements GestureDetector.BaseListener {
    private static final String TAG = "VideoActivity";

    private int state;
    private static final int STATE_RECORDING = 1;
    private static final int STATE_ACTION = 2;

    private Camera camera;
    private CameraPreview cameraPreview;
    private TextView textField;
    private String filePrefix;

    private AudioManager audioManager;
    private GestureDetector gestureDetector;

    private TextView durationText;
    private MediaRecorder mediaRecorder;
    private Timer timer;
    private int timerExecutions = 0;
    private boolean recording = false;
    private File outputFile;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "Launching activity");

        // Get file prefix
        Intent intent = getIntent();
        filePrefix = intent.getStringExtra("FILE_PREFIX");

        setContentView(R.layout.activity_camera_video);

        durationText = (TextView) findViewById(R.id.text_camera_duration);

        TextView textField = (TextView) findViewById(R.id.text_camera_helptext);

        textField.setText("Tap to stop recording");

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
            return;
        }

        camera = getCameraInstance();
        if (camera == null) {
            finish();
            return;
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gestureDetector = new GestureDetector(this).setBaseListener(this);

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        state = STATE_RECORDING;

        // Reset timer executions.
        timerExecutions = 0;
        durationText = (TextView) findViewById(R.id.text_camera_duration);
        launchUnlimitedVideo();
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            if (state == STATE_RECORDING) {
                handleSingleTap();
                return true;
            }
        } else if (Gesture.SWIPE_RIGHT.equals(gesture)) {
            if (state == STATE_ACTION) {
                handleForwardSwipe();
                return true;
            }
        } else if (Gesture.SWIPE_LEFT.equals(gesture)) {
            if (state == STATE_ACTION) {
                handleBackwardSwipe();
                return true;
            }
        } else if (Gesture.SWIPE_DOWN.equals(gesture)) {
            if (state == STATE_ACTION) {
                handleDownSwipe();
                return true;
            }
        }

        return false;
    }

    private void handleSingleTap() {
        Log.i(TAG, "Single tap.");

        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);

        if (recording) {
            Log.i(TAG, "Stop recording!");

            try {
                mediaRecorder.stop();  // stop the recording
                releaseTimer();
                releaseMediaRecorder(); // release the MediaRecorder object
                releaseCamera();
                recording = false;

                textField.setBackgroundColor(Color.argb(125, 0, 0, 0));
                textField.setTextColor(Color.WHITE);
                textField.setText("\nSwipe FORWARD to INSTASHARE.\nSwipe BACK to ARCHIVE.\nSwipe DOWN to CANCEL.\n");

                state = STATE_ACTION;
            } catch (Exception e) {
                Log.d(TAG, "Exception stopping recording: " + e.getMessage());
                releaseMediaRecorder();
                releaseCamera();
                finish();
            }
        }
    }

    private void handleForwardSwipe() {
        Log.i(TAG, "InstaShare!!!");
        returnVideo(true);
    }

    private void handleBackwardSwipe() {
        Log.i(TAG, "Archive.");
        returnVideo(false);
    }

    private void handleDownSwipe() {
        Log.i(TAG, "Delete video.");
        outputFile.delete();
        finish();
    }

    private void returnVideo(boolean instaShare) {
        // Add path to file as result
        Intent returnIntent = new Intent();
        returnIntent.putExtra("path", outputFile.getAbsolutePath());
        returnIntent.putExtra("instaShare", instaShare);
        setResult(RESULT_OK, returnIntent);

        // Finish activity
        finish();
    }

    private void launchUnlimitedVideo() {
        durationText.setText("0 sec");

        // Catch all errors, and release camera on error.
        try {
            Log.i(TAG, "start preparing video recording");

            Log.i(TAG, "Setting camera hint");
            Camera.Parameters params = camera.getParameters();
            params.setRecordingHint(true);
            camera.setParameters(params);

            Log.i(TAG, "new media recorder");
            mediaRecorder = new MediaRecorder();

            Log.i(TAG, "setting up error listener");
            mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                public void onError(MediaRecorder mediarecorder1, int k, int i1) {
                    Log.e(TAG, String.format("Media Recorder error: k=%d, i1=%d", k, i1));
                }
            });

            // Step 1: Unlock and set camera to MediaRecorder. Clear preview.
            Log.i(TAG, "unlock and set camera to MediaRecorder");
            camera.unlock();
            mediaRecorder.setCamera(camera);

            // Step 2: Set sources
            Log.i(TAG, "set sources");
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            Log.i(TAG, "set camcorder profile");
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

            // Step 4: Set output file
            Log.i(TAG, "set output file");
            outputFile = getOutputVideoFile();
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());

            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        // Step 5: Set the preview output
                        Log.i(TAG, "set preview");
                        mediaRecorder.setPreviewDisplay(cameraPreview.getHolder().getSurface());

                        Log.i(TAG, "finished configuration.");

                        // Step 6: Prepare configured MediaRecorder
                        mediaRecorder.prepare();
                    } catch (IOException e) {
                        Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
                        releaseMediaRecorder();
                        releaseCamera();
                        finish();
                    }

                    Log.i(TAG, "prepare successful");

                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder.start();

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
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder (" + e.getCause() + "): " + e.getMessage());
            releaseMediaRecorder();
            releaseCamera();
            finish();
        } catch (Exception e) {
            Log.d(TAG, "Exception preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            releaseCamera();
            finish();
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Log.i(TAG, "getting camera instance...");
        try {
            return Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.e(TAG, "could not getCameraInstance");
            throw e;
            // Camera is not available (in use or does not exist)
        }
    }

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        releaseTimer();
        releaseMediaRecorder();
        releaseCamera();

        super.onPause();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        releaseTimer();
        releaseMediaRecorder();
        releaseCamera();

        super.onDestroy();
    }

    /**
     * Release the media recorder.
     */
    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    /**
     * Release the camera.
     */
    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            cameraPreview.release();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    private void releaseTimer() {
        if (timer != null) {
            timer.cancel();
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
                filePrefix + "_video_" + timeStamp + ".mp4");
        return mediaFile;
    }
}
