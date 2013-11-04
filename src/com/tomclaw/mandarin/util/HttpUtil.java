package com.tomclaw.mandarin.util;

import android.util.Pair;
import com.tomclaw.mandarin.im.icq.WimConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.11.13
 * Time: 14:40
 */
public class HttpUtil {

    /**
     * Builds Url request string from specified parameters.
     * @param pairs
     * @return String - Url request parameters.
     * @throws java.io.UnsupportedEncodingException
     */
    public static String prepareParameters(List<Pair<String, String>> pairs)
            throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        // Perform pair concatenation.
        for (Pair<String, String> pair : pairs) {
            if (builder.length() > 0) {
                builder.append(WimConstants.AMP);
            }
            builder.append(pair.first)
                    .append(WimConstants.EQUAL)
                    .append(URLEncoder.encode(pair.second, "UTF-8")
                            .replace("+", "%20"));
        }
        return builder.toString();
    }
}
