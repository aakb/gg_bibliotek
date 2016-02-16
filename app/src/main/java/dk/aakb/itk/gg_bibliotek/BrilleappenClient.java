package dk.aakb.itk.gg_bibliotek;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;

public class BrilleappenClient extends AsyncTask<Object, Void, Boolean> {
    private static final String TAG = "bibliotek Brilleappen";

    private String url;
    private String username;
    private String password;
    private BrilleappenClientListener listener;

    public BrilleappenClient(BrilleappenClientListener listener, String url, String username, String password) {
        this.url = url.replaceFirst("/+$", "");
        this.username = username;
        this.password = password;
        this.listener = listener;
    }

    protected Boolean doInBackground(Object... args) {
        String action = (String)args[0];

        switch (action) {
            case "getEvent":
                getEvent();
                break;
            case "sendFile":
                File file = (File)args[1];
                boolean share = (boolean)args[2];

                sendFile(file, share);
                break;
            default:
                break;
        }
        return true;
    }

    public void getEvent() {
        try {
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            String authString = username + ":" + password;
            String authStringEnc = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String response = getResponse(connection);

            JSONObject result = null;
            try {
                result = new JSONObject(response);
            }
            catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }

            listener.getEventDone(this, result);
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void sendFile(File file, boolean share) {
        try {
            String mimeType = URLConnection.guessContentTypeFromName(file.getName());

            URL url = new URL(this.url + "?type=" + mimeType + "&share=" + (share ?  "yes" : "no"));

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            String authString = username + ":" + password;
            String authStringEnc = Base64.encodeToString(authString.getBytes(), Base64.DEFAULT);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            writeFile(dos, file);
            dos.flush();
            dos.close();

            // Response from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String response = getResponse(connection);

            JSONObject mainObject = new JSONObject(response);

            listener.sendFileDone(this, mainObject);

            Log.i(TAG, serverResponseCode + ": " + response);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage());
        }
    }

    private void writeFile(DataOutputStream dos, File file) throws Throwable {
        int maxBufferSize = 1024 * 1024;

        FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());

        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];

        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }

        fileInputStream.close();
    }

    private String getResponse(HttpURLConnection connection) {
        try {
            InputStream responseStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(responseStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            return sb.toString();
        } catch (IOException ex) {
            // @TODO: handle this!
            Log.e(TAG, ex.getMessage());
        }

        return null;
    }
}
