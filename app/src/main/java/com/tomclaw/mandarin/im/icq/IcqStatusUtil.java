package com.tomclaw.mandarin.im.icq;

import android.text.TextUtils;

import com.tomclaw.mandarin.im.StatusNotFoundException;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.util.NameValuePair;
import com.tomclaw.mandarin.util.UrlParser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Created by ivsolkin on 23.11.16.
 * ICQ base and extended (mood) status parsing helper
 */
public class IcqStatusUtil {

    public static String getStatusTitle(String accountType, String moodTitle, int statusIndex) {
        // Define status title.
        String statusTitle;
        if (TextUtils.isEmpty(moodTitle)) {
            // Default title for status index.
            statusTitle = StatusUtil.getStatusTitle(accountType, statusIndex);
        } else {
            // Buddy specified title.
            statusTitle = moodTitle;
        }
        return statusTitle;
    }

    public static int getStatusIndex(String accountType, String moodIcon, String buddyStatus) {
        int statusIndex;
        // Checking for mood present.
        if (!TextUtils.isEmpty(moodIcon)) {
            try {
                return StatusUtil.getStatusIndex(accountType, parseMood(moodIcon));
            } catch (StatusNotFoundException ignored) {
            }
        }
        try {
            statusIndex = StatusUtil.getStatusIndex(accountType, buddyStatus);
        } catch (StatusNotFoundException ex) {
            statusIndex = StatusUtil.STATUS_OFFLINE;
        }
        return statusIndex;
    }

    /**
     * Returns "id" parameter value from specified URL
     */
    private static String getIdParam(String url) {
        URI uri = URI.create(url);
        for (NameValuePair param : UrlParser.parse(uri, "UTF-8")) {
            if (param.getName().equals("id")) {
                return param.getValue();
            }
        }
        return "";
    }

    /**
     * Parsing specified URL for "id" parameter, decoding it from UTF-8 byte array in HEX presentation
     */
    @SuppressWarnings("WeakerAccess")
    public static String parseMood(String moodUrl) {
        if (moodUrl != null) {
            final String id = getIdParam(moodUrl);

            InputStream is = new InputStream() {
                int pos = 0;
                int length = id.length();

                @Override
                public int read() throws IOException {
                    if (pos == length) return -1;
                    char c1 = id.charAt(pos++);
                    char c2 = id.charAt(pos++);

                    return (Character.digit(c1, 16) << 4) | Character.digit(c2, 16);
                }
            };
            DataInputStream dis = new DataInputStream(is);
            try {
                return dis.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return moodUrl;
    }
}
