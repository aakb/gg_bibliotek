package dk.aakb.itk.gg_bibliotek;

import org.json.JSONObject;

public interface BrilleappenClientListener {
    void getEventDone(BrilleappenClient client, JSONObject result);

    void sendFileDone(BrilleappenClient client, JSONObject result);

    void notifyFileDone(BrilleappenClient client, JSONObject result);
}