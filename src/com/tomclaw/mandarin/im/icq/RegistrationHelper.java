package com.tomclaw.mandarin.im.icq;

import android.util.Log;
import android.util.Pair;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

/**
 * Created by Solkin on 28.09.2014.
 */
public class RegistrationHelper {

    public static final int RESPONSE_OK = 200;

    private static final String REQUEST_NUMBER = "1";

    public static void normalizePhone(String countryCode, String phoneNumber, NormalizePhoneCallback callback) {
        try {
            // Specifying normalize data.
            HttpParamsBuilder params = new HttpParamsBuilder()
                    .appendParam(DEV_ID_K, IcqSession.DEV_ID_VALUE)
                    .appendParam(COUNTRY_CODE, countryCode)
                    .appendParam(PHONE_NUMBER, phoneNumber)
                    .appendParam(R_PARAM, REQUEST_NUMBER);
            // Execute normalize request.
            JSONObject jsonObject = new JSONObject(HttpUtil.executePost(NORMALIZE_PHONE_URL, params));
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            switch (statusCode) {
                case RESPONSE_OK: {
                    JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                    String msisdn = dataObject.getString(MSISDN);
                    callback.onPhoneNormalized(msisdn);
                    return;
                }
                default: {
                    callback.onProtocolError();
                    return;
                }
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "normalize: " + e.getMessage());
        }
        callback.onNetworkError();
    }

    public static void validatePhone(String msisdn, ValidatePhoneCallback callback) {
        try {
            // Specifying normalize data.
            HttpParamsBuilder params = new HttpParamsBuilder()
                    .appendParam(DEV_ID_K, IcqSession.DEV_ID_VALUE)
                    .appendParam(LOCALE, Locale.getDefault().toString())
                    .appendParam(MSISDN, msisdn)
                    .appendParam(R_PARAM, REQUEST_NUMBER)
                    .appendParam(SMS_FORMAT_TYPE, HUMAN)
                    .appendParam(CLIENT, ICQ);
            // Execute normalize request.
            JSONObject jsonObject = new JSONObject(HttpUtil.executePost(VALIDATE_PHONE_URL, params));
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            switch (statusCode) {
                case RESPONSE_OK: {
                    JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                    String transId = dataObject.getString(TRANS_ID);
                    callback.onPhoneValidated(transId);
                    return;
                }
                default: {
                    callback.onProtocolError();
                    return;
                }
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "validate: " + e.getMessage());
        }
        callback.onNetworkError();
    }

    public static void loginPhone(String msisdn, String transId, String smsCode, LoginPhoneCallback callback) {
        try {
            // Specifying normalize data.
            HttpParamsBuilder params = new HttpParamsBuilder()
                    .appendParam(DEV_ID_K, IcqSession.DEV_ID_VALUE)
                    .appendParam(MSISDN, msisdn)
                    .appendParam(TRANS_ID, transId)
                    .appendParam(R_PARAM, REQUEST_NUMBER)
                    .appendParam(SMS_CODE, smsCode)
                    .appendParam(CREATE_ACCOUNT, "1")
                    .appendParam(CLIENT, ICQ);
            // Execute normalize request.
            JSONObject jsonObject = new JSONObject(HttpUtil.executePost(LOGIN_PHONE_URL, params));
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            switch (statusCode) {
                case RESPONSE_OK: {
                    JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                    JSONObject tokenObject = dataObject.getJSONObject(TOKEN_OBJECT);
                    long expiresIn = tokenObject.getLong(EXPIRES_IN);
                    String tokenA = tokenObject.getString(TOKEN_A);
                    String sessionKey = dataObject.getString(SESSION_KEY);
                    long hostTime = dataObject.getLong(HOST_TIME);
                    String loginId = dataObject.getString(LOGIN_ID);
                    callback.onPhoneLoginSuccess(loginId, tokenA, sessionKey, expiresIn, hostTime);
                    return;
                }
                default: {
                    callback.onProtocolError();
                    return;
                }
            }
        } catch (Throwable e) {
            Log.d(Settings.LOG_TAG, "login phone: " + e.getMessage());
        }
        callback.onNetworkError();
    }

    public interface NormalizePhoneCallback {
        public void onPhoneNormalized(String msisdn);
        public void onProtocolError();
        public void onNetworkError();
    }

    public interface ValidatePhoneCallback {
        public void onPhoneValidated(String transId);
        public void onProtocolError();
        public void onNetworkError();
    }

    public interface LoginPhoneCallback {
        public void onPhoneLoginSuccess(String login, String tokenA, String sessionKey,
                                        long expiresIn, long hostTime);
        public void onProtocolError();
        public void onNetworkError();
    }
}
