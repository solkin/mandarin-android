package com.tomclaw.mandarin.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import static android.support.v4.content.PermissionChecker.checkSelfPermission;

public class PermissionsHelper {

    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}
