package com.tomclaw.mandarin.core;

import android.content.Context;
import com.tomclaw.mandarin.R;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/15/13
 * Time: 1:56 PM
 */
public class PreferenceHelper {

    public static boolean isCollapseMessages(Context context) {
        return getBooleanPreference(context, R.string.pref_collapse_messages, R.bool.pref_collapse_messages_default);
    }

    private static boolean getBooleanPreference(Context context, int preferenceKey, int defaultValueKey) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences",
                Context.MODE_MULTI_PROCESS).getBoolean(context.getResources().getString(preferenceKey),
                context.getResources().getBoolean(defaultValueKey));
    }
}
