package com.tomclaw.mandarin.util;

import android.content.Context;
import android.os.Build;

import static androidx.core.content.PermissionChecker.checkSelfPermission;

import androidx.core.content.PermissionChecker;

public class PermissionsHelper {

    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (checkSelfPermission(context, permission) != PermissionChecker.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}
