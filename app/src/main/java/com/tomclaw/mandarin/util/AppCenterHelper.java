package com.tomclaw.mandarin.util;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

public class AppCenterHelper {

    private static final String APP_IDENTIFIER_KEY = "com.microsoft.appcenter.appIdentifier";

    public static void checkForCrashes(Application app) {
        String appIdentifier = getAppIdentifier(app);
        AppCenter.start(app, appIdentifier, Analytics.class, Crashes.class);
    }

    /**
     * Retrieve the HockeyApp AppIdentifier from the Manifest
     *
     * @param context usually your Activity
     * @return the HockeyApp AppIdentifier
     */
    private static String getAppIdentifier(Context context) {
        String appIdentifier = getManifestString(context, APP_IDENTIFIER_KEY);
        if (TextUtils.isEmpty(appIdentifier)) {
            throw new IllegalArgumentException("HockeyApp app identifier was not configured correctly in manifest or build configuration.");
        }
        return appIdentifier;
    }

    @SuppressWarnings("SameParameterValue")
    private static String getManifestString(Context context, String key) {
        return getBundle(context).getString(key);
    }

    private static Bundle getBundle(Context context) {
        Bundle bundle;
        try {
            bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        return bundle;
    }
}
