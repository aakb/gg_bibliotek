package dk.aakb.itk.gg_bibliotek;

import android.app.Activity;
import android.widget.Toast;

/**
 * Created by rimi on 01/03/16.
 */
public abstract class BaseActivity extends Activity {

    /**
     * Send a toast
     *
     * @param message Message to display
     */
    private void proposeAToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    protected void proposeAToast(int resId) {
        proposeAToast(getString(resId));
    }

    protected void proposeAToast(int resId, Object... formatArgs) {
        proposeAToast(getString(resId, formatArgs));
    }

}
