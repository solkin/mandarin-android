package com.tomclaw.mandarin.util;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import com.tomclaw.mandarin.R;

/**
 * Created by Solkin on 18.10.2014.
 */
public class FileHelper {

    public static int getMimeTypeResPicture(String mimeType) {
        if (mimeType.startsWith("image")) {
            return R.drawable.files_img;
        } else if (mimeType.contains("compressed") ||
                mimeType.contains("zip") ||
                mimeType.contains("7z") ||
                mimeType.contains("rar")) {
            return R.drawable.files_zip;
        } else if (mimeType.contains("android") && mimeType.contains("package")) {
            return R.drawable.files_apk;
        } else if (mimeType.contains("text") || mimeType.contains("document") ||
                mimeType.contains("pdf") || mimeType.contains("html") || mimeType.contains("latex")) {
            return R.drawable.files_text;
        } else if (mimeType.contains("audio")) {
            return R.drawable.files_music;
        } else if (mimeType.contains("video") || mimeType.contains("flash")) {
            return R.drawable.files_video;
        } else {
            return R.drawable.files_unknown;
        }
    }

    public static String getMimeType(String path) {
        String type = null;
        String extension = getFileExtensionFromPath(path);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension.toLowerCase());
        }
        if (TextUtils.isEmpty(type)) {
            type = "application/octet-stream";
        }
        return type;
    }

    public static String getFileBaseFromName(String name) {
        String base = name;
        if (!TextUtils.isEmpty(name)) {
            int index = name.lastIndexOf(".");
            if (index != -1) {
                base = name.substring(0, index);
            }
        }
        return base;
    }

    public static String getFileExtensionFromPath(String path) {
        String suffix = "";
        if (!TextUtils.isEmpty(path)) {
            int index = path.lastIndexOf(".");
            if (index != -1) {
                suffix = path.substring(index + 1);
            }
        }
        return suffix;
    }
}
