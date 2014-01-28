package com.tomclaw.mandarin.util;

import android.util.Log;
import android.util.Pair;

import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.icq.WimConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.11.13
 * Time: 14:40
 */
public class HttpUtil {

    private static final String HASH_ALGORITHM = "MD5";
    private static final int RADIX = 10 + 26; // 10 digits + 26 letters

    /**
     * Builds Url request string from specified parameters.
     *
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

    public static String getUrlHash(String url) {
        byte[] md5 = getMD5(url.getBytes());
        BigInteger bi = new BigInteger(md5).abs();
        return bi.toString(RADIX);
    }

    private static byte[] getMD5(byte[] data) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(data);
            hash = digest.digest();
        } catch (NoSuchAlgorithmException ignored) {
        }
        return hash;
    }

    public static void writeStringToConnection(HttpURLConnection connection, String data) throws IOException {
        OutputStream outputStream = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        writer.write(data);
        writer.flush();
        writer.close();
    }

    public static String readStringFromConnection(HttpURLConnection connection) throws IOException {
        InputStream in = connection.getInputStream();
        String response = StringUtil.streamToString(in);
        in.close();
        return response;
    }

    public static String executePOST(HttpURLConnection connection, String data) throws IOException {
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        HttpUtil.writeStringToConnection(connection, data);
        // Open connection to response
        connection.connect();
        // Read response
        return HttpUtil.readStringFromConnection(connection);
    }

    public static String executeGET(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setDoOutput(false);

        return readStringFromConnection(connection);
    }
}
