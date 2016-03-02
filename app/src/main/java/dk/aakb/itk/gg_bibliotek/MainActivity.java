package dk.aakb.itk.gg_bibliotek;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.view.WindowUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import dk.aakb.itk.brilleappen.BrilleappenClient;
import dk.aakb.itk.brilleappen.BrilleappenClientListener;

public class MainActivity extends BaseActivity implements BrilleappenClientListener,  GestureDetector.BaseListener, NetworkConnectionListener  {
    public static final String FILE_DIRECTORY = "Bibliotek";

    private static final String TAG = "bibliotek MainActivity";
    private static final int TAKE_PICTURE_REQUEST = 101;
    private static final int RECORD_VIDEO_CAPTURE_REQUEST = 102;
    private static final int SCAN_EVENT_REQUEST = 103;

    private static final String STATE_UNDELIVERED_FILES = "undelivered_files";
    private static final String STATE_CONTACTS = "contacts";
    private static final String STATE_EVENT = "url";
    private static final String STATE_EVENT_NAME = "event_name";
    private static final String STATE_EVENT_TWITTER_CAPTION = "event_twitter_caption";
    private static final String STATE_EVENT_INSTAGRAM_CAPTION = "event_instagram_caption";

    private static final int MENU_MAIN = 1;
    private static final int MENU_START = 0;

    private ArrayList<UndeliveredFile> undeliveredFiles;
    private ArrayList<Contact> contacts = new ArrayList<>();
    private String uploadFileUrl = null;
    private String username;
    private String password;
    private String eventName;
    private String captionTwitter;
    private String captionInstagram;
    private String eventUrl;
    private boolean isOffline;
    private int numberOfImages = 0;
    private int numberOfVideos = 0;

    int selectedMenu = 0;

    private GestureDetector gestureDetector;
    private Menu panelMenu;
    BrilleappenClient client;

    /**
     * On create.
     *
     * @param savedInstanceState the bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        (new NetworkConnection(this, this.getBaseContext())).execute();

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

        restoreState();

        if (uploadFileUrl != null) {
            selectedMenu = MENU_MAIN;

            // Set the main activity view.
            setContentView(R.layout.activity_layout);

            updateUI();
        }
        else {
            selectedMenu = MENU_START;

            // Set the main activity view.
            setContentView(R.layout.activity_layout_init);
        }

        listFiles();

        gestureDetector = new GestureDetector(this).setBaseListener(this);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return gestureDetector.onMotionEvent(event);
    }

    @Override
    public boolean onGesture(Gesture gesture) {
        if (Gesture.TAP.equals(gesture)) {
            openOptionsMenu();

            return true;
        }
        return false;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        panelMenu = menu;

        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            getMenuInflater().inflate(R.menu.main, menu);
        }

        if (updateMenu(menu, featureId)) {
            return true;
        }

        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        updateMenu(menu, featureId);

        return super.onPreparePanel(featureId, view, menu);
    }

    private boolean updateMenu(Menu menu, int featureId) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS ||
                featureId == Window.FEATURE_OPTIONS_PANEL) {

            // Add contacts menu, if not already added.
            if (menu.findItem(R.id.make_call_menu_item).getSubMenu().size() <= 0) {
                for (int i = 0; i < contacts.size(); i++) {
                    menu.findItem(R.id.make_call_menu_item).getSubMenu().add(R.id.main_menu_group_main, R.id.contacts_menu_item, i, contacts.get(i).getName());
                }
            }

            // Update which group is visible.
            setMenuGroupVisibilty(menu);

            // Hide the finish_menu when using voice commands.
            if (featureId == Window.FEATURE_OPTIONS_PANEL && selectedMenu == MENU_MAIN) {
                menu.findItem(R.id.finish_menu_item).setVisible(true);
            }
            else {
                menu.findItem(R.id.finish_menu_item).setVisible(false);
            }

            return true;
        }
        return false;
    }

    /**
     * Update what menu is displayed.
     */
    public void setMenuGroupVisibilty(Menu menu) {
        menu.setGroupVisible(R.id.main_menu_group_main, selectedMenu == MENU_MAIN);
        menu.setGroupVisible(R.id.main_menu_group_start, selectedMenu == MENU_START);
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
                case R.id.take_picture_menu_item:
                    Log.i(TAG, "menu: take before picture");

                    takePicture();

                    break;
                case R.id.record_video_menu_item:
                    Log.i(TAG, "menu: record video");

                    recordVideo();

                    break;
                case R.id.contacts_menu_item:
                    Log.i(TAG, "menu: make call");

                    Contact contact = contacts.get(item.getOrder());

                    Log.i(TAG, "Calling: (" + item.getOrder() + ") " + contact.getName() + " " + contact.getPhoneNumber());

                    proposeAToast(R.string.calling_name_phone, contact.getName(), contact.getPhoneNumber());

                    makeCall(contact.getPhoneNumber());

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
     * Launch the picture capture intent.
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
     * @param phoneNumber The phone number to call.
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
        Gson gson = new Gson();

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(STATE_UNDELIVERED_FILES, gson.toJson(undeliveredFiles));
        editor.putString(STATE_CONTACTS, gson.toJson(contacts));
        editor.putString(STATE_EVENT, uploadFileUrl);
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
        uploadFileUrl = sharedPref.getString(STATE_EVENT, null);
        eventName = sharedPref.getString(STATE_EVENT_NAME, null);
        captionInstagram = sharedPref.getString(STATE_EVENT_INSTAGRAM_CAPTION, null);
        captionTwitter = sharedPref.getString(STATE_EVENT_TWITTER_CAPTION, null);
        String serializedUndeliveredFiles = sharedPref.getString(STATE_UNDELIVERED_FILES, "[]");
        String serializedContacts = sharedPref.getString(STATE_CONTACTS, "[]");

        Gson gson = new Gson();
        undeliveredFiles = gson.fromJson(serializedUndeliveredFiles, new TypeToken<ArrayList<UndeliveredFile>>() {}.getType());
        contacts = gson.fromJson(serializedContacts, new TypeToken<ArrayList<Contact>>() {}.getType());

        Log.i(TAG, "Restored uploadFileUrl: " + uploadFileUrl);
        Log.i(TAG, "Restored name: " + eventName);
        Log.i(TAG, "Restored undelivered files: " + undeliveredFiles);
        Log.i(TAG, "Restored contacts: " + contacts);
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

            saveState();
            updateUI();

            sendFile(data.getStringExtra("path"), instaShare);
        } else if (requestCode == RECORD_VIDEO_CAPTURE_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received video: " + data.getStringExtra("path"));

            boolean instaShare = data.getBooleanExtra("instaShare", false);

            saveState();
            updateUI();

            sendFile(data.getStringExtra("path"), instaShare);
        } else if (requestCode == SCAN_EVENT_REQUEST && resultCode == RESULT_OK) {
            Log.i(TAG, "Received url QR: " + data.getStringExtra("result"));

            String result = data.getStringExtra("result");

            try {
                JSONObject jResult = new JSONObject(result);
                eventUrl = jResult.getString("url");

                selectedMenu = MENU_MAIN;

                setMenuGroupVisibilty(panelMenu);

                if (isOffline) {
                    setOfflineEvent();
                }
                else {
                    client = new BrilleappenClient(this, eventUrl, username, password);
                    client.getEvent();
                }
            }
            catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
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
        updateTextField(R.id.imageNumber, String.valueOf(numberOfImages), numberOfImages > 0 ? Color.WHITE : null);
        updateTextField(R.id.imageLabel, null, numberOfImages > 0 ? Color.WHITE : null);

        updateTextField(R.id.videoNumber, String.valueOf(numberOfVideos), numberOfVideos > 0 ? Color.WHITE : null);
        updateTextField(R.id.videoLabel, null, numberOfVideos > 0 ? Color.WHITE : null);

        updateTextField(R.id.eventIdentifier, eventName, eventName != null ? Color.WHITE : null);
        updateTextField(R.id.instagramTextView, captionInstagram, captionInstagram != null ? Color.WHITE : null);
        updateTextField(R.id.twitterTextView, captionTwitter, captionTwitter != null ? Color.WHITE : null);
    }

    public void setOfflineEvent() {
        captionInstagram = null;
        captionTwitter = null;
        eventName = "offline";
        contacts = new ArrayList<>();
        uploadFileUrl = null;
    }

    @Override
    public void getEventDone(BrilleappenClient client, JSONObject result) {
        try {
            Log.i(TAG, result.toString());

            if (result.getJSONArray("field_gg_instagram_caption").length() > 0) {
                captionInstagram = result.getJSONArray("field_gg_instagram_caption").getJSONObject(0).getString("value");
            }

            if (result.getJSONArray("field_gg_twitter_caption").length() > 0) {
                captionTwitter = result.getJSONArray("field_gg_twitter_caption").getJSONObject(0).getString("value");
            }

            if (result.getJSONArray("title").length() > 0) {
                eventName = result.getJSONArray("title").getJSONObject(0).getString("value");
            }

            contacts = new ArrayList<>();
            if (result.getJSONArray("field_gg_contact_people").length() > 0) {
                JSONArray jsonArray = result.getJSONArray("field_gg_contact_people");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject o = (JSONObject) jsonArray.get(i);
                    contacts.add(new Contact(o.getString("name"), o.getString("telephone")));
                }

                Log.i(TAG, contacts.toString());
            }

            uploadFileUrl = result.getString("add_file_url");

            saveState();

            // Update the UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (uploadFileUrl != null) {
                        // Set the main activity view.
                        setContentView(R.layout.activity_layout);
                    }

                    updateUI();
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void sendFile(String path, boolean share) {
        if (isOffline) {
            proposeAToast(R.string.is_offline);

            // Save for later delivery

        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    proposeAToast(R.string.uploading_file);
                }
            });
            client = new BrilleappenClient(this, uploadFileUrl, username, password);
            client.sendFile(new File(path), share);
        }
    }

    @Override
    public void sendFileDone(BrilleappenClient client, JSONObject result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
                proposeAToast(R.string.file_uploaded);
            }
        });
    }

    @Override
    public void sendFileProgress(BrilleappenClient client, final int progress, final int max) {
        Log.i(TAG, String.format("sendFileProgress: %d/%d", progress, max));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                progressBar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                progressBar.setMax(max);
                progressBar.setProgress(progress);
            }
        });
    }

    @Override
    public void notifyFileDone(BrilleappenClient client, JSONObject result) {
        // Not implemented
    }

    @Override
    public void createEventDone(BrilleappenClient client, JSONObject result) {
        // Not implemented
    }

    @Override
    public void networkConnectionDone(boolean result) {
        isOffline = !result;

        Log.i(TAG, "isOffline: " + isOffline);
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

    private void listFiles() {
        Log.i(TAG, "------------");

        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), MainActivity.FILE_DIRECTORY);
        Log.i(TAG, "Listing files in: " + f.getAbsolutePath());

        getDirectoryListing(f);

        Log.i(TAG, "------------");
    }
}
