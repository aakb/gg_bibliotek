package dk.aakb.itk.gg_bibliotek;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.glass.view.WindowUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class MainActivity extends Activity implements BrilleappenClientListener {
    public static final String FILE_DIRECTORY = "Bibliotek";

    private static final String TAG = "bibliotek MainActivity";
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_EVENT_REQUEST = 103;
    private static final String STATE_VIDEOS = "videos";
    private static final String STATE_PICTURES = "pictures";

    private static final String STATE_EVENT = "url";
    private static final String STATE_EVENT_NAME = "event_name";
    private static final String STATE_EVENT_TWITTER_CAPTION = "event_twitter_caption";
    private static final String STATE_EVENT_INSTAGRAM_CAPTION = "event_instagram_caption";

    private ArrayList<String> imagePaths = new ArrayList<>();
    private ArrayList<String> videoPaths = new ArrayList<>();

    private String url = null;
    BrilleappenClient client;
    private String username;
    private String password;

    String eventName;
    String eventUrl;
    String captionTwitter;
    String captionInstagram;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onSaveInstanceState");

        // Save the user's current game state
        savedInstanceState.putStringArrayList(STATE_VIDEOS, videoPaths);
        savedInstanceState.putStringArrayList(STATE_PICTURES, imagePaths);
        savedInstanceState.putString(STATE_EVENT, url);
        savedInstanceState.putString(STATE_EVENT_NAME, eventName);
        savedInstanceState.putString(STATE_EVENT_TWITTER_CAPTION, captionTwitter);
        savedInstanceState.putString(STATE_EVENT_INSTAGRAM_CAPTION, captionInstagram);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * On create.
     *
     * @param savedInstanceState the bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Requests a voice menu on this activity. As for any other
        // window feature, be sure to request this before
        // setContentView() is called
        getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
        getWindow().requestFeature(Window.FEATURE_OPTIONS_PANEL);

        // Get properties
        Properties properties = new Properties();
        try {
            AssetManager assetManager = getApplicationContext().getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            finish();
        }

        this.username = properties.getProperty("Username");
        this.password = properties.getProperty("Password");

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            Log.i(TAG, "Restoring savedInstance");

            // Restore state members from saved instance
            imagePaths = savedInstanceState.getStringArrayList(STATE_PICTURES);
            videoPaths = savedInstanceState.getStringArrayList(STATE_VIDEOS);
            url = savedInstanceState.getString(STATE_EVENT);
            eventName = savedInstanceState.getString(STATE_EVENT_NAME);
            captionInstagram = savedInstanceState.getString(STATE_EVENT_INSTAGRAM_CAPTION);
            captionTwitter = savedInstanceState.getString(STATE_EVENT_TWITTER_CAPTION);
        } else {
            Log.i(TAG, "Restoring state");

            // Probably initialize members with default values for a new instance
            restoreState();
        }

        if (url != null) {
            // Set the main activity view.
            setContentView(R.layout.activity_layout);

            updateUI();
        }
        else {
            // Set the main activity view.
            setContentView(R.layout.activity_layout_init);
        }

        Log.i(TAG, "------------");

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);
        Log.i(TAG, "Listing files in: " + f.getAbsolutePath());

        getDirectoryListing(f);

        Log.i(TAG, "------------");
    }

    /**
     * On create panel menu.
     *
     * @param featureId the feature id
     * @param menu      the menu to create
     * @return boolean
     */
    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            if (url != null) {
                getMenuInflater().inflate(R.menu.main, menu);
            } else {
                getMenuInflater().inflate(R.menu.start, menu);
            }

            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    /**
     * On create options menu.
     *
     * @param menu The menu to create
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (url != null) {
            getMenuInflater().inflate(R.menu.main, menu);
        } else {
            getMenuInflater().inflate(R.menu.start, menu);
        }

        return true;
    }

    /**
     * On menu item selected.
     * <p>
     * Processes the voice commands from the main menu.
     *
     * @param featureId the feature id
     * @param item      the selected menu item
     * @return boolean
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {
            switch (item.getItemId()) {
                case R.id.take_image_menu_item:
                    Log.i(TAG, "menu: take before image");

                    takePicture();

                    break;
                case R.id.record_video_menu_item:
                    Log.i(TAG, "menu: record video");

                    recordVideo();

                    break;
                case R.id.confirm_cancel:
                    Log.i(TAG, "menu: Confirm: cancel and exit");

                    cleanDirectory();
                    deleteState();

                    finish();

                    break;
                case R.id.make_call_menu_item:
                    Log.i(TAG, "menu: make call");

                    break;
                case R.id.scan_event_menu_item:
                    Intent scanEventIntent = new Intent(this, QRActivity.class);
                    startActivityForResult(scanEventIntent, SCAN_EVENT_REQUEST);

                    break;
                case R.id.finish_menu_item:
                    deleteState();
                    finish();

                    break;
                default:
                    return true;
            }
            return true;
        }

        // Pass through to super if not handled
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Launch the image capture intent.
     */
    private void takePicture() {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra("FILE_PREFIX", "");
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    /**
     * Launch the record video intent.
     */
    private void recordVideo() {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("FILE_PREFIX", "");
        startActivityForResult(intent, RECORD_VIDEO_CAPTURE_REQUEST);
    }

    /**
     * Call a phone number with an intent
     *
     * @param phoneNumber
     */
    private void makeCall(String phoneNumber) {
        Intent localIntent = new Intent();
        localIntent.putExtra("com.google.glass.extra.PHONE_NUMBER", phoneNumber);
        localIntent.setAction("com.google.glass.action.CALL_DIAL");
        sendBroadcast(localIntent);
    }

    /*
     * Save state.
     */
    private void saveState() {
        String serializedVideoPaths = (new JSONArray(videoPaths)).toString();
        String serializedImagePaths = (new JSONArray(imagePaths)).toString();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(STATE_VIDEOS, serializedVideoPaths);
        editor.putString(STATE_PICTURES, serializedImagePaths);
        editor.putString(STATE_EVENT, url);
        editor.putString(STATE_EVENT_NAME, eventName);
        editor.putString(STATE_EVENT_INSTAGRAM_CAPTION, captionInstagram);
        editor.putString(STATE_EVENT_TWITTER_CAPTION, captionTwitter);
        editor.apply();
    }

    /**
     * Remove state.
     */
    private void deleteState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Restore state.
     */
    private void restoreState() {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        url = sharedPref.getString(STATE_EVENT, null);
        eventName = sharedPref.getString(STATE_EVENT_NAME, null);
        captionInstagram = sharedPref.getString(STATE_EVENT_INSTAGRAM_CAPTION, null);
        captionTwitter = sharedPref.getString(STATE_EVENT_TWITTER_CAPTION, null);
        String serializedVideoPaths = sharedPref.getString(STATE_VIDEOS, "[]");
        String serializedImagePaths = sharedPref.getString(STATE_PICTURES, "[]");

        imagePaths = new ArrayList<>();
        videoPaths = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(serializedVideoPaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                videoPaths.add(jsonArray.getString(i));
            }

            jsonArray = new JSONArray(serializedImagePaths);
            for (int i = 0; i < jsonArray.length(); i++) {
                imagePaths.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            // ignore
        }

        Log.i(TAG, "Restored url: " + url);
        Log.i(TAG, "Restored name: " + eventName);
        Log.i(TAG, "Restored imagePaths: " + imagePaths);
        Log.i(TAG, "Restored videoPaths: " + videoPaths);
    }

    /**
     * Empty the directory.
     */
    private void cleanDirectory() {
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), FILE_DIRECTORY);
        Log.i(TAG, "Cleaning directory: " + f.getAbsolutePath());

        File[] files = f.listFiles();
        if (files != null && files.length > 0) {
            for (File inFile : files) {
                boolean success = inFile.delete();
                if (!success) {
                    Log.e(TAG, "file: " + inFile + " was not deleted (continuing).");
                }
            }
        } else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    /**
     * List all files in f.
     *
     * @param f file to list
     */
    private void getDirectoryListing(File f) {
        File[] files = f.listFiles();
        if (files != null && files.length > 0) {
            for (File inFile : files) {
                if (inFile.isDirectory()) {
                    Log.i(TAG, "(dir) " + inFile);
                    getDirectoryListing(inFile);
                } else {
                    Log.i(TAG, "" + inFile);
                }
            }
        } else {
            Log.i(TAG, "directory empty or does not exist.");
        }
    }

    /**
     * On activity result.
     * <p>
     * When an intent returns, it is intercepted in this method.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received image: " + data.getStringExtra("path"));

            boolean instaShare = data.getBooleanExtra("instaShare", false);
            imagePaths.add(data.getStringExtra("path"));

            saveState();
            updateUI();

            // Send the file
            client = new BrilleappenClient(this, url, username, password);
            client.execute("sendFile", new File(data.getStringExtra("path")), instaShare);
        } else if (requestCode == RECORD_VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received video: " + data.getStringExtra("path"));

            boolean instaShare = data.getBooleanExtra("instaShare", false);
            videoPaths.add(data.getStringExtra("path"));

            saveState();
            updateUI();

            // Send the file
            client = new BrilleappenClient(this, url, username, password);
            client.execute("sendFile", new File(data.getStringExtra("path")), instaShare);
        } else if (requestCode == SCAN_EVENT_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received url QR: " + data.getStringExtra("result"));

            String result = data.getStringExtra("result");

            try {
                JSONObject jResult = new JSONObject(result);
                eventUrl = jResult.getString("url");
                client = new BrilleappenClient(this, eventUrl, username, password);
                client.execute("getEvent");
            }
            catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            // Set the main activity view.
            setContentView(R.layout.activity_layout);

            saveState();
            updateUI();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Update a ui text view.
     *
     * @param id    id of the text view
     * @param value value to assign
     * @param color the color to set for the text field
     */
    private void updateTextField(int id, String value, Integer color) {
        TextView v = (TextView) findViewById(id);
        if (value != null) {
            v.setText(value);
        }
        if (color != null) {
            v.setTextColor(color);
        }
        v.invalidate();
    }

    /**
     * Update the UI.
     */
    private void updateUI() {
        updateTextField(R.id.imageNumber, String.valueOf(imagePaths.size()), imagePaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.imageLabel, null, imagePaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.videoNumber, String.valueOf(videoPaths.size()), videoPaths.size() > 0 ? Color.WHITE : null);
        updateTextField(R.id.videoLabel, null, videoPaths.size() > 0 ? Color.WHITE : null);

        updateTextField(R.id.eventIdentifier, eventName, eventName != null ? Color.WHITE : null);
        updateTextField(R.id.instagramTextView, captionInstagram, captionInstagram != null ? Color.WHITE : null);
        updateTextField(R.id.twitterTextView, captionTwitter, captionTwitter != null ? Color.WHITE : null);
    }

    /**
     * Send a toast
     *
     * @param message Message to display
     */
    public void proposeAToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void getEventDone(BrilleappenClient client, JSONObject result) {
        try {
            if (result.has("caption")) {
                JSONObject caption = result.getJSONObject("caption");
                captionTwitter = caption.getString("twitter");
                captionInstagram = caption.getString("instagram");
            }
            url = result.getString("url");
            eventName = result.getString("title");

            // Update the UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
            saveState();
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void sendFileDone(BrilleappenClient client, JSONObject result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                proposeAToast("File delivered!");
            }
        });
    }

    @Override
    public void notifyFileDone(BrilleappenClient client, JSONObject result) {
        // Not implemented
    }
}
