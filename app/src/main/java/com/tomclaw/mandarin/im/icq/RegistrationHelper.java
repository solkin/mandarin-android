package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.core.Task;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.tomclaw.mandarin.im.icq.WimConstants.CLIENT;
import static com.tomclaw.mandarin.im.icq.WimConstants.CREATE_ACCOUNT;
import static com.tomclaw.mandarin.im.icq.WimConstants.DATA_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.DEV_ID_K;
import static com.tomclaw.mandarin.im.icq.WimConstants.EXPIRES_IN;
import static com.tomclaw.mandarin.im.icq.WimConstants.HOST_TIME;
import static com.tomclaw.mandarin.im.icq.WimConstants.ICQ;
import static com.tomclaw.mandarin.im.icq.WimConstants.LOGIN_ID;
import static com.tomclaw.mandarin.im.icq.WimConstants.LOGIN_PHONE_URL;
import static com.tomclaw.mandarin.im.icq.WimConstants.MSISDN;
import static com.tomclaw.mandarin.im.icq.WimConstants.NORMALIZE_PHONE_URL;
import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.R_PARAM;
import static com.tomclaw.mandarin.im.icq.WimConstants.SESSION_KEY;
import static com.tomclaw.mandarin.im.icq.WimConstants.SMS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;
import static com.tomclaw.mandarin.im.icq.WimConstants.TOKEN_A;
import static com.tomclaw.mandarin.im.icq.WimConstants.TOKEN_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.TRANS_ID;
import static com.tomclaw.mandarin.util.HttpUtil.executePost;

/**
 * Created by Solkin on 28.09.2014.
 */
public class RegistrationHelper {

    public static final int RESPONSE_OK = 200;

    private static final String REQUEST_NUMBER = "1";

    private static final Random random = new Random(System.currentTimeMillis());

    public static void normalizePhone(String phoneNumber, RegistrationCallback callback) {
        TaskExecutor.getInstance().execute(new NormalizePhoneTask(phoneNumber, callback));
    }

    public static void loginPhone(String phoneNumber, String sessionId, String smsCode, RegistrationCallback callback) {
        TaskExecutor.getInstance().execute(new LoginPhoneTask(phoneNumber, sessionId, smsCode, callback));
    }

    private static class NormalizePhoneTask extends Task {

        private final String phoneNumber;
        private final RegistrationCallback callback;

        public NormalizePhoneTask(String phoneNumber, RegistrationCallback callback) {
            this.phoneNumber = phoneNumber;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
            JSONObject request = new JSONObject();
            String reqId = random.nextInt(99999) + "-" + random.nextInt(99999) + random.nextInt(99999);
            request.put("reqId", reqId);
            JSONObject params = new JSONObject();
            params.put("phone", phoneNumber);
            params.put("language", "en-US");
            params.put("route", "sms");
            params.put("devId", IcqSession.DEV_ID_VALUE);
            params.put("application", "icq");
            request.put("params", params);
            String requestData = request.toString();
            // Execute normalize request.
            Map<String, String> props = new HashMap<>();
            props.put("Content-Type", "application/json");
            String responseData = executePost(NORMALIZE_PHONE_URL, requestData.getBytes(), props);
            JSONObject response = new JSONObject(responseData);
            JSONObject statusObject = response.optJSONObject("status");
            if (statusObject != null && statusObject.optInt("code") / 100 == 200) {
                JSONObject resultsObject = response.optJSONObject("results");
                if (resultsObject != null) {
                    int codeLength = resultsObject.optInt("codeLength");
                    String sessionId = resultsObject.optString("sessionId");
                    callback.onPhoneValidated(phoneNumber, codeLength, sessionId);
                    return;
                }
            }
            callback.onProtocolError();
        }

        @Override
        public void onFailBackground() {
            callback.onNetworkError();
        }
    }

    private static class LoginPhoneTask extends Task {

        private final String phoneNumber;
        private final String sessionId;
        private final String smsCode;
        private final RegistrationCallback callback;

        public LoginPhoneTask(String phoneNumber, String sessionId, String smsCode, RegistrationCallback callback) {
            this.phoneNumber = phoneNumber;
            this.sessionId = sessionId;
            this.smsCode = smsCode;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
            // Specifying normalize data.
            HttpParamsBuilder params = new HttpParamsBuilder()
                    .appendParam(DEV_ID_K, IcqSession.DEV_ID_VALUE)
                    .appendParam(MSISDN, phoneNumber)
                    .appendParam(TRANS_ID, sessionId)
                    .appendParam(R_PARAM, REQUEST_NUMBER)
                    .appendParam(SMS_CODE, smsCode)
                    .appendParam(CREATE_ACCOUNT, "1")
                    .appendParam(CLIENT, ICQ);
            // Execute normalize request.
            String loginPhoneResult = executePost(LOGIN_PHONE_URL, params);
            JSONObject jsonObject = new JSONObject(loginPhoneResult);
            JSONObject responseObject = jsonObject.getJSONObject(RESPONSE_OBJECT);
            int statusCode = responseObject.getInt(STATUS_CODE);
            if (statusCode == RESPONSE_OK) {
                JSONObject dataObject = responseObject.getJSONObject(DATA_OBJECT);
                JSONObject tokenObject = dataObject.getJSONObject(TOKEN_OBJECT);
                long expiresIn = tokenObject.getLong(EXPIRES_IN);
                String tokenA = tokenObject.getString(TOKEN_A);
                String sessionKey = dataObject.getString(SESSION_KEY);
                long hostTime = dataObject.getLong(HOST_TIME);
                String loginId = dataObject.getString(LOGIN_ID);
                callback.onPhoneLoginSuccess(loginId, tokenA, sessionKey, expiresIn, hostTime);
            } else {
                callback.onProtocolError();
            }
        }

        @Override
        public void onFailBackground() {
            callback.onNetworkError();
        }
    }

    public interface RegistrationCallback {

        void onPhoneValidated(String phoneNumber, int codeLength, String sessionId);

        void onPhoneLoginSuccess(String login, String tokenA, String sessionKey, long expiresIn, long hostTime);

        void onProtocolError();

        void onNetworkError();

    }

}
