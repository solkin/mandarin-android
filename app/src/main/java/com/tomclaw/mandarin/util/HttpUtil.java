package com.tomclaw.mandarin.util;

import static com.tomclaw.mandarin.im.icq.WimConstants.WEB_API_BASE;

import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;

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
        return WEB_API_BASE + "expressions/get?f=native&type=floorLargeBuddyIcon&t=" + buddyId;
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
        return executePost(urlString, data, Collections.<String, String>emptyMap());
    }

    public static String executePost(String urlString, byte[] data, Map<String, String> props) throws IOException {
        InputStream responseStream = null;
        HttpURLConnection connection = null;
        try {
            // Create and config connection.
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_CONNECTION);
            connection.setReadTimeout(TIMEOUT_SOCKET);

            if (props.size() > 0) {
                for (String key : props.keySet()) {
                    connection.setRequestProperty(key, props.get(key));
                }
            }

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
        // Checking for this is error stream.
        Logger.log(connection.getURL() + " -> " + responseCode);
        if (responseCode >= HttpStatus.SC_BAD_REQUEST) {
            return connection.getErrorStream();
        } else {
            return connection.getInputStream();
        }
    }

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

    public static byte[] stringToArray(String string) throws IOException {
        return string.getBytes(HttpUtil.UTF8_ENCODING);
    }

    public static void fixCertificateCheckingOnPreLollipop() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            try {
                disableSSLCertificateChecking();
            } catch (NoSuchAlgorithmException ignored) {
            } catch (KeyManagementException ignored) {
            }
        }
    }

    /**
     * Disables the SSL certificate checking for new instances of {@link HttpsURLConnection}
     */
    public static void disableSSLCertificateChecking() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManager[] trustAllCerts = getTrustManagers();
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        TLSSocketFactory tlsSocketFactory = new TLSSocketFactory(sc);
        HttpsURLConnection.setDefaultSSLSocketFactory(tlsSocketFactory);
    }

    public static OkHttpClient getOkHttpClient() {
        OkHttpClient.Builder builder;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            builder = getUnsafeOkHttpClient();
        } else {
            builder = new OkHttpClient.Builder();
        }
        List<Protocol> protocols = new ArrayList<>();
        protocols.add(Protocol.HTTP_1_1);
        return builder.protocols(protocols).build();
    }

    private static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            TrustManager[] trustAllCerts = getTrustManagers();
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            TLSSocketFactory tlsSocketFactory = new TLSSocketFactory(sc);

            return new OkHttpClient.Builder()
                    .sslSocketFactory(tlsSocketFactory)
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

    private static TrustManager[] getTrustManagers() {
        return new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
            }
        }};
    }

}
