package com.tomclaw.mandarin.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;

import com.tomclaw.mandarin.R;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/15/13
 * Time: 1:56 PM
 */
public class PreferenceHelper {

    public static boolean isShowKeyboard(Context context) {
        return getBooleanPreference(context, R.string.pref_chat_show_keyboard, R.bool.pref_chat_show_keyboard_default);
    }

    public static boolean isShowTemp(Context context) {
        return getBooleanPreference(context, R.string.pref_show_temp, R.bool.pref_show_temp_default);
    }

    public static boolean isSound(Context context) {
        return getBooleanPreference(context, R.string.pref_sound, R.bool.pref_sound_default);
    }

    public static boolean isVibrate(Context context) {
        return getBooleanPreference(context, R.string.pref_vibrate, R.bool.pref_vibrate_default);
    }

    public static boolean isLights(Context context) {
        return getBooleanPreference(context, R.string.pref_lights, R.bool.pref_lights_default);
    }

    public static boolean isQuiteChat(Context context) {
        return getBooleanPreference(context, R.string.pref_quite_chat, R.bool.pref_quite_chat_default);
    }

    public static boolean isPrivateNotifications(Context context) {
        return getBooleanPreference(context, R.string.pref_private_notifications, R.bool.pref_private_notifications_default);
    }

    public static boolean isAutorun(Context context) {
        return getBooleanPreference(context, R.string.pref_autorun, R.bool.pref_autorun_default);
    }

    public static boolean isDarkTheme(Context context) {
        return getBooleanPreference(context, R.string.pref_dark_theme, R.bool.pref_dark_theme_default);
    }

    public static boolean isIgnoreUnknown(Context context) {
        return getBooleanPreference(context, R.string.pref_ignore_unknown, R.bool.pref_ignore_unknown_default);
    }

    public static String getRosterMode(Context context) {
        return getStringPreference(context, R.string.pref_roster_mode, R.string.pref_roster_mode_default);
    }

    public static String getFilesAutoReceive(Context context) {
        return getStringPreference(context, R.string.pref_files_auto_receive, R.string.pref_files_auto_receive_default);
    }

    public static String getImageCompression(Context context) {
        return getStringPreference(context, R.string.pref_image_compression, R.string.pref_image_compression_default);
    }

    public static Uri getNotificationUri(Context context) {
        String uriValue = getStringPreference(context, R.string.pref_notification_sound,
                R.string.pref_notification_sound_default);
        // Checking for default value found.
        if (TextUtils.equals(uriValue, context.getString(R.string.pref_notification_sound_default))) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return Uri.parse(uriValue);
    }

    public static int getChatBackground(Context context) {
        boolean isChatBackground = getBooleanPreference(context, R.string.pref_chat_background, R.bool.pref_chat_background_default);
        if (isChatBackground) {
            return getThemeDrawable(context, R.attr.chat_background_doodle,
                    R.drawable.chat_background_doodle_light);
        } else {
            return getThemeDrawable(context, R.attr.chat_background_gradient,
                    R.drawable.chat_background_gradient_light);
        }
    }

    private static int getThemeDrawable(Context context, int attr, int defValue) {
        int resId = defValue;
        Resources.Theme theme = context.getTheme();
        if (theme != null) {
            int[] resources = new int[]{attr};
            TypedArray array = null;
            try {
                array = theme.obtainStyledAttributes(resources);
                if (array != null) {
                    resId = array.getResourceId(0, defValue);
                }
            } finally {
                if (array != null) {
                    array.recycle();
                }
            }
        }
        return resId;
    }

    public static boolean isShowStartHelper(Context context) {
        return getBooleanPreference(context, R.string.pref_show_start_helper, R.bool.pref_show_start_helper_default);
    }

    public static void setShowStartHelper(Context context, boolean value) {
        setBooleanPreference(context, R.string.pref_show_start_helper, value);
    }

    public static boolean isSendByEnter(Context context) {
        return getBooleanPreference(context, R.string.pref_send_by_enter, R.bool.pref_send_by_enter_default);
    }

    public static boolean isMusicAutoStatus(Context context) {
        return getBooleanPreference(context, R.string.pref_music_auto_status, R.bool.pref_music_auto_status_default);
    }

    private static boolean getBooleanPreference(Context context, int preferenceKey, int defaultValueKey) {
        return getSharedPreferences(context).getBoolean(context.getResources().getString(preferenceKey),
                context.getResources().getBoolean(defaultValueKey));
    }

    private static void setBooleanPreference(Context context, int preferenceKey, boolean value) {
        getSharedPreferences(context).edit().putBoolean(context.getResources().getString(preferenceKey),
                value).commit();
    }

    private static String getStringPreference(Context context, int preferenceKey, int defaultValueKey) {
        return getSharedPreferences(context).getString(context.getResources().getString(preferenceKey),
                context.getResources().getString(defaultValueKey));
    }

    private static void setStringPreference(Context context, int preferenceKey, String value) {
        getSharedPreferences(context).edit().putString(context.getResources().getString(preferenceKey),
                value).commit();
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
                getSharedPreferencesMode());
    }

    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    private static int getSharedPreferencesMode() {
        return Context.MODE_MULTI_PROCESS;
    }
}
