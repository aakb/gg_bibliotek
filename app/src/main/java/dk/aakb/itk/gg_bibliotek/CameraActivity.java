package dk.aakb.itk.gg_bibliotek;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends Activity implements GestureDetector.BaseListener {
    private static final String TAG = "CameraActivity";
    private static final int STATE_PREVIEW = 1;
    private static final int STATE_ACTION = 2;

    private Camera camera;
    private CameraPreview cameraPreview;
    private TextView textField;
    private String filePrefix;

    private AudioManager audioManager;
    private GestureDetector gestureDetector;

    private int state;

    private byte[] pictureData;

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

        setContentView(R.layout.activity_camera);

        textField = (TextView) findViewById(R.id.text_camera_helptext);

        textField.setText("Tap to take picture");

        if (!checkCameraHardware(this)) {
            Log.i(TAG, "no camera");
            finish();
            return;
        }

        // Create an instance of Camera
        camera = getCameraInstance();

        if (camera == null) {
            finish();
            return;
        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        gestureDetector = new GestureDetector(this).setBaseListener(this);

        state = STATE_PREVIEW;

        Log.i(TAG, "Set up cameraPreview");

        // Create our Preview view and set it as the content of our activity.
        cameraPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(cameraPreview);

        Log.i(TAG, "Preview set up.");
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            if (state == STATE_PREVIEW) {
                handleSingleTap();
                return true;
            }
        }
        else if (Gesture.SWIPE_RIGHT.equals(gesture)) {
            if (state == STATE_ACTION) {
                handleForwardSwipe();
                return true;
            }
        }
        else if (Gesture.SWIPE_LEFT.equals(gesture)) {
            if (state == STATE_ACTION) {
                handleBackwardSwipe();
                return true;
            }
        }

        return false;
    }

    private void handleSingleTap() {
        Log.i(TAG, "Single tap.");

        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);

        camera.takePicture(null, null, mPicture);
    }

    private void handleForwardSwipe() {
        Log.i(TAG, "InstaShare!!!");
        returnPicture(true);
    }

    private void handleBackwardSwipe() {
        Log.i(TAG, "Archive.");
        returnPicture(false);
    }

    private void returnPicture(boolean instaShare) {
        File pictureFile = getOutputImageFile();
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(pictureData);
            fos.close();

            // Add path to file as result
            Intent returnIntent = new Intent();
            returnIntent.putExtra("path", pictureFile.getAbsolutePath());
            returnIntent.putExtra("instaShare", instaShare);
            setResult(RESULT_OK, returnIntent);

            // Finish activity
            finish();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Log.i(TAG, "getting camera instance...");
        try {
            return Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "could not getCameraInstance");
            throw e;
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
     * Picture callback.
     */
    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            releaseCamera();

            pictureData = data;

            textField.setBackgroundColor(Color.argb(125, 0, 0, 0));
            textField.setTextColor(Color.WHITE);
            textField.setText("\nSwipe FORWARD to INSTASHARE.\nSwipe BACK to ARCHIVE.\nSwipe DOWN to CANCEL.\n");

            state = STATE_ACTION;
        }
    };

    /**
     * On pause.
     */
    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    /**
     * On destroy.
     */
    @Override
    protected void onDestroy() {
        releaseCamera();
        super.onDestroy();
    }

    /**
     * Release the camera resources.
     */
    private void releaseCamera() {
        Log.i(TAG, "releasing camera");

        if (camera != null) {
            camera.stopPreview();
            cameraPreview.release();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }

    /**
     * Create a File for saving an image
     */
    private File getOutputImageFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);

        Log.i(TAG, mediaStorageDir.getAbsolutePath());

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
                filePrefix + "_image_" + timeStamp + ".jpg");
        return mediaFile;
    }
}