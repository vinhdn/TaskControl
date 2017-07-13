package com.vinhdn.taskcontrol.log;

import android.util.Log;

/**
 * Created by vinh on 7/13/17.
 */

public class LogUtil {

    private static final boolean IS_LOG = true;

    public static void d(String tag, String log) {
        if (IS_LOG)
            Log.d(tag, log);
    }

    public static void e(String tag, String log) {
        if (IS_LOG)
            Log.e(tag, log);
    }

    public static void i(String tag, String log) {
        if (IS_LOG)
            Log.i(tag, log);
    }

    public static void w(String tag, String log) {
        if (IS_LOG)
            Log.w(tag, log);
    }
}
