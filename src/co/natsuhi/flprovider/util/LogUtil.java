
package co.natsuhi.flprovider.util;

import android.util.Log;
import co.natsuhi.flprovider.BuildConfig;

public class LogUtil {

    public static void d(String tag, String msg) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg);
        }
    }
}
