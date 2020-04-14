package com.tomclaw.mandarin.im.icq;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;

import com.tomclaw.mandarin.R;
import com.tomclaw.helpers.BitmapHelper;

import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_CONTENT_TYPE_FILE;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_CONTENT_TYPE_PICTURE;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_CONTENT_TYPE_PTT;
import static com.tomclaw.mandarin.core.GlobalProvider.HISTORY_CONTENT_TYPE_VIDEO;

public class IcqFileSharingHelper {

    public static void parseIcqFileSharingUrl(Resources resources, String linkCode) {
        int indexOfSlash = linkCode.indexOf('/');
        if (indexOfSlash >= 0)
            linkCode = linkCode.substring(0, indexOfSlash);
        if (linkCode.length() < 32) {
            // TODO: Text message
            return;
        }

        if (canDecodeMetadata(linkCode)) {
            int PREVIEW_MAX_WIDTH = dimen(resources, R.dimen.preview_max_width);
            int PREVIEW_MAX_HEIGHT = dimen(resources, R.dimen.preview_max_height);
            int PREVIEW_MIN_WIDTH = dimen(resources, R.dimen.preview_min_width);
            int PREVIEW_MIN_HEIGHT = dimen(resources, R.dimen.preview_min_height);

            int contentType = decodeContentType(linkCode);
            if (isPreviewable(contentType)) {
                Point aspectRatio = decodeAspectRatio(linkCode);
                Rect rect = BitmapHelper.calcPreviewScale(
                        PREVIEW_MAX_WIDTH,
                        PREVIEW_MAX_HEIGHT,
                        PREVIEW_MIN_WIDTH,
                        PREVIEW_MIN_HEIGHT,
                        aspectRatio.x, aspectRatio.y, 0
                );
                // TODO: setThumbSize(rect.width(), rect.height());
                if (isImage(contentType)) {
                    // TODO: setMimeType("image/*");
                } else {
                    // TODO: setMimeType("video/*");
                }
            } else {
                if (contentType == HISTORY_CONTENT_TYPE_PTT) {
                    int duration = parseDuration(linkCode);
                }
            }
        }

    }

    private static boolean canDecodeMetadata(String linkCode) {
        if (linkCode == null)
            return false;
        int length = linkCode.length();
        if (length < 33) {
            return false;
        }
        char lastChar = linkCode.charAt(length - 1);
        return ('A' <= lastChar && lastChar <= 'Z') || ('a' <= lastChar && lastChar <= 'z');
    }

    private static int decodeContentType(String linkCode) {
        char firstChar = linkCode.charAt(0);
        if (firstChar >= '0') {
            if (firstChar <= '7') {
                return HISTORY_CONTENT_TYPE_PICTURE;
            }
            if (firstChar <= 'F') {
                return HISTORY_CONTENT_TYPE_VIDEO;
            }
            if (firstChar == 'I' || firstChar == 'J') {
                return HISTORY_CONTENT_TYPE_PTT;
            }
        }
        return HISTORY_CONTENT_TYPE_FILE;
    }

    private static Point decodeAspectRatio(String linkCode) {
        int w1 = charToInt62(linkCode.charAt(1));
        int w2 = charToInt62(linkCode.charAt(2));
        int h1 = charToInt62(linkCode.charAt(3));
        int h2 = charToInt62(linkCode.charAt(4));

        return new Point(w1 * 62 + w2, h1 * 62 + h2);
    }

    private static int parseDuration(String linkCode) {
        try {
            return charToInt62(linkCode.charAt(3)) * 62 + charToInt62(linkCode.charAt(4));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    private static boolean isImage(int contentType) {
        return contentType == HISTORY_CONTENT_TYPE_PICTURE;
    }

    private static boolean isPreviewable(int contentType) {
        return contentType == HISTORY_CONTENT_TYPE_PICTURE || contentType == HISTORY_CONTENT_TYPE_VIDEO;
    }

    private static int charToInt62(char c) {
        int i = c - 'A';
        if (i >= 0 && i < 26) {
            return i + 26 + 10;
        }
        i = c - 'a';
        if (i >= 0 && i < 26) {
            return i + 10;
        }
        i = c - '0';
        if (i >= 0 && i < 10) {
            return i;
        }
        throw new IllegalArgumentException(String.format("'%s' is not a 62-based digit", c));
    }

    private static int dimen(Resources resources, int id) {
        return resources.getDimensionPixelSize(id);
    }

    public static String getTinyThumbnailUrl(String linkCode) {
        return String.format("https://files.icq.com/preview/max/iphone/%s?dlink=1", linkCode);
    }

    public static String getNormalThumbnailUrl(Resources resources, String linkCode) {
        return String.format("http://files.icq.com/preview/max/%s/%s/", getDensityName(resources), linkCode);
    }

    private static PreviewVariants getDensityName(Resources resources) {
        float density = resources.getDisplayMetrics().density;
        if (density >= 3.0) {
            return PreviewVariants.xxhdpi;
        }
        if (density >= 2.0) {
            return PreviewVariants.xhdpi;
        }
        if (density >= 1.5) {
            return PreviewVariants.hdpi;
        }
        return PreviewVariants.mdpi;
    }

    private enum PreviewVariants {
        xhdpi, hdpi, mdpi, static800, static600, static194, xxhdpi
    }

}
