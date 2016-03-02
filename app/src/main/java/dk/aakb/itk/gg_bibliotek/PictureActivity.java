package dk.aakb.itk.gg_bibliotek;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class PictureActivity extends CameraActivity {
    private static final int STATE_PREVIEW = 1;
    private static final int STATE_ACTION = 2;

    private byte[] pictureData;

    /**
     * On create.
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        TAG = "PictureActivity";
        contentView = R.layout.activity_camera_picture;

        super.onCreate(savedInstanceState);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textField.setText(R.string.tap_to_take_picture);
            }
        });

        state = STATE_PREVIEW;
    }

    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            if (state == STATE_PREVIEW) {
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
        File pictureFile = getOutputFile("image", "jpg");
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        } finally {
            finish();
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

            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  textField.setBackgroundColor(Color.argb(125, 0, 0, 0));
                                  textField.setTextColor(Color.WHITE);
                                  textField.setText(R.string.swipe_actions);
                              }
                          }
            );

            state = STATE_ACTION;
        }
    };
}
