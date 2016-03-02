package dk.aakb.itk.gg_bibliotek;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;

public class NetworkConnection extends AsyncTask<Void, Void, Boolean> {
    private Context context;
    private NetworkConnectionListener networkConnectionListener;

    public boolean isNetworkAvailable() {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null;
    }

    public NetworkConnection(NetworkConnectionListener networkConnectionListener, Context context) {
        this.context = context;
        this.networkConnectionListener = networkConnectionListener;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        networkConnectionListener.networkConnectionDone(isNetworkAvailable());
        return true;
    }
}
