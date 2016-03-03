package dk.aakb.itk.brilleappen;

import org.json.JSONObject;

public interface BrilleappenClientListener {
    void createEventDone(BrilleappenClient client, JSONObject result);

    void getEventDone(BrilleappenClient client, JSONObject result);

    void sendFileDone(BrilleappenClient client, JSONObject result);

    void sendFileProgress(BrilleappenClient client, int progress, int max);

    void notifyFileDone(BrilleappenClient client, JSONObject result);
}