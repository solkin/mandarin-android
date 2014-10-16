package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.Task;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.HttpUtil;
import org.json.JSONObject;

import java.util.Locale;

import static com.tomclaw.mandarin.im.icq.WimConstants.*;

/**
 * Created by Solkin on 28.09.2014.
 */
public class RegistrationHelper {

    public static final int RESPONSE_OK = 200;

    private static final String REQUEST_NUMBER = "1";

    public static void normalizePhone(String countryCode, String phoneNumber, RegistrationCallback callback) {
        TaskExecutor.getInstance().execute(new NormalizePhoneTask(countryCode, phoneNumber, callback));
    }

    public static void validatePhone(String msisdn, RegistrationCallback callback) {
        TaskExecutor.getInstance().execute(new ValidatePhoneTask(msisdn, callback));
    }

    public static void loginPhone(String msisdn, String transId, String smsCode, RegistrationCallback callback) {
        TaskExecutor.getInstance().execute(new LoginPhoneTask(msisdn, transId, smsCode, callback));
    }

    private static class NormalizePhoneTask extends Task {

        private String countryCode;
        private String phoneNumber;
        private RegistrationCallback callback;

        public NormalizePhoneTask(String countryCode, String phoneNumber, RegistrationCallback callback) {
            this.countryCode = countryCode;
            this.phoneNumber = phoneNumber;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
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
                }
            }
        }

        @Override
        public void onFailBackground() {
            callback.onNetworkError();
        }
    }

    private static class ValidatePhoneTask extends Task {

        private final String msisdn;
        private final RegistrationCallback callback;

        private ValidatePhoneTask(String msisdn, RegistrationCallback callback) {
            this.msisdn = msisdn;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
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
                    callback.onPhoneValidated(msisdn, transId);
                    return;
                }
                default: {
                    callback.onProtocolError();
                }
            }
        }

        @Override
        public void onFailBackground() {
            callback.onNetworkError();
        }
    }

    private static class LoginPhoneTask extends Task {

        private final String msisdn;
        private final String transId;
        private final String smsCode;
        private final RegistrationCallback callback;

        public LoginPhoneTask(String msisdn, String transId, String smsCode, RegistrationCallback callback) {
            this.msisdn = msisdn;
            this.transId = transId;
            this.smsCode = smsCode;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
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
                }
            }
        }

        @Override
        public void onFailBackground() {
            callback.onNetworkError();
        }
    }

    public interface RegistrationCallback {
        public void onPhoneNormalized(String msisdn);
        public void onPhoneValidated(String msisdn, String transId);
        public void onPhoneLoginSuccess(String login, String tokenA, String sessionKey, long expiresIn, long hostTime);
        public void onProtocolError();
        public void onNetworkError();
    }
}
