package com.tomclaw.mandarin.util;

import android.os.Build;

import com.tomclaw.mandarin.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 04.11.13
 * Time: 14:40
 */
public class HttpUtil {

    public static final String GET = "GET";
    public static final String POST = "POST";

    private static final int TIMEOUT_SOCKET = 70 * 1000;
    private static final int TIMEOUT_CONNECTION = 60 * 1000;

    public static final String UTF8_ENCODING = "UTF-8";

    private static final String HASH_ALGORITHM = "MD5";
    private static final int RADIX = 10 + 26; // 10 digits + 26 letters

    public static String getUrlHash(String url) {
        byte[] md5 = getMD5(url.getBytes());
        BigInteger bi = new BigInteger(md5).abs();
        return bi.toString(RADIX);
    }

    public static String getAvatarUrl(String buddyId) {
        return "https://api.icq.net/expressions/get?f=native&type=floorLargeBuddyIcon&t=" + buddyId;
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

    public static String executePost(String urlString, HttpParamsBuilder params) throws IOException {
        return executePost(urlString, stringToArray(params.build()));
    }

    public static String executePost(String urlString, byte[] data) throws IOException {
        InputStream responseStream = null;
        HttpURLConnection connection = null;
        try {
            // Create and config connection.
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_CONNECTION);
            connection.setReadTimeout(TIMEOUT_SOCKET);

            // Execute request.
            responseStream = HttpUtil.executePost(connection, data);
            return HttpUtil.streamToString(responseStream);
        } catch (IOException ex) {
            throw new IOException(ex);
        } finally {
            try {
                if (responseStream != null) {
                    responseStream.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (IOException ignored) {
            }
        }
    }

    public static InputStream executePost(HttpURLConnection connection, String data) throws IOException {
        return executePost(connection, stringToArray(data));
    }

    public static InputStream executePost(HttpURLConnection connection, byte[] data) throws IOException {
        connection.setRequestMethod(POST);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // Write data into output stream.
        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(data);
        outputStream.flush();
        // Open connection to response.
        connection.connect();

        return getResponse(connection);
    }

    public static InputStream executeGet(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod(GET);
        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setConnectTimeout(TIMEOUT_CONNECTION);
        connection.setReadTimeout(TIMEOUT_SOCKET);

        return getResponse(connection);
    }

    private static InputStream getResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        InputStream in;
        // Checking for this is error stream.
        if (responseCode >= HttpStatus.SC_BAD_REQUEST) {
            return connection.getErrorStream();
        } else {
            return connection.getInputStream();
        }
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public static String streamToString(InputStream inputStream) throws IOException {
        return new String(streamToArray(inputStream), HttpUtil.UTF8_ENCODING);
    }

    public static byte[] streamToArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    public static byte[] stringToArray(String string) throws IOException {
        return string.getBytes(HttpUtil.UTF8_ENCODING);
    }

    public static String getUserAgent() {
        return "Mandarin/" + BuildConfig.VERSION_NAME + " (Android " + Build.VERSION.RELEASE + ")";
    }

    public static String httpToHttps(String url) {
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }
}
