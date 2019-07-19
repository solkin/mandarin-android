package com.tomclaw.helpers;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Igor on 13.05.2015.
 */
public class AppsMenuHelper {

    public static void fillMenuItemSubmenu(AppCompatActivity activity, Menu menu, int menuId, Intent intent) {
        fillMenuItemSubmenu(activity, menu, menuId, intent, 0);
    }

    public static void fillMenuItemSubmenu(AppCompatActivity activity, Menu menu, int menuId, Intent intent, int requestCode) {
        // Find specified menu id in menu and clear its submenu.
        SubMenu subMenu = menu.findItem(menuId).getSubMenu();
        subMenu.clear();
        // Obtain list of packages for specified intent.
        PackageManager packageManager = activity.getPackageManager();
        List<ResolveInfo> resolveInfoList = packageManager
                .queryIntentActivities(intent, 0);
        // Prepare click listener.
        MenuItem.OnMenuItemClickListener onMenuItemClickListener =
                new AppsMenuHelper.ShareMenuItemClickListener(activity, resolveInfoList, intent, requestCode);
        // Fill menu item with submenu elements with app name and icon.
        int i = 0;
        for (ResolveInfo resolveInfo : resolveInfoList) {
            try {
                subMenu.add(0, i, i, resolveInfo.loadLabel(packageManager))
                        .setIcon(resolveInfo.loadIcon(packageManager))
                        .setOnMenuItemClickListener(onMenuItemClickListener);
                i++;
            } catch (Throwable ignored) {
                // Bad package.
            }
        }
    }

    /**
     * Reusable listener for handling share item clicks.
     */
    public static class ShareMenuItemClickListener implements MenuItem.OnMenuItemClickListener {

        private final int requestCode;
        private WeakReference<AppCompatActivity> weakActivity;
        private final List<ResolveInfo> resolveInfoList;
        private final Intent intent;

        public ShareMenuItemClickListener(AppCompatActivity activity, List<ResolveInfo> resolveInfoList, Intent intent, int requestCode) {
            this.weakActivity = new WeakReference<>(activity);
            this.resolveInfoList = resolveInfoList;
            this.intent = intent;
            this.requestCode = requestCode;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AppCompatActivity activity = weakActivity.get();
            if (activity != null && intent != null) {
                ResolveInfo resolveInfo = resolveInfoList.get(item.getItemId());
                final ActivityInfo activityInfo = resolveInfo.activityInfo;
                final ComponentName name = new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
                intent.setComponent(name);
                final String action = intent.getAction();
                if (Intent.ACTION_SEND.equals(action) ||
                        Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                }
                if (Intent.ACTION_PICK.equals(action) ||
                        Intent.ACTION_GET_CONTENT.equals(action)) {
                    activity.startActivityForResult(intent, requestCode);
                } else {
                    activity.startActivity(intent);
                }
            }
            return true;
        }
    }
}
