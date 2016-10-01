package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.BuildConfig;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.util.HttpUtil;

import java.util.concurrent.TimeUnit;

/**
 * Created by ivsolkin on 01.09.16.
 */
public class CabbageSession {

    private static final long CABBAGE_TIMEOUT = TimeUnit.SECONDS.toMillis(3);
    private IcqAccountRoot accountRoot;

    public CabbageSession(IcqAccountRoot accountRoot) {
        this.accountRoot = accountRoot;
    }

    public void obtainToken() {
        CabbageTokenRequest request = new CabbageTokenRequest();
        invokeCabbageRequest(request);
    }

    public void obtainClient() {
        String authToken = accountRoot.getAuthToken();
        CabbageAddClientRequest request = new CabbageAddClientRequest(createRequestId(), authToken,
                HttpUtil.getUserAgent(), BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME);
        invokeCabbageRequest(request);
    }

    public void refreshClient() {
        String authToken = accountRoot.getAuthToken();
        long clientId = accountRoot.getClientId();
        CabbageModClientRequest request = new CabbageModClientRequest(createRequestId(), authToken,
                clientId, HttpUtil.getUserAgent(), BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME);
        invokeCabbageRequest(request);
    }

    private void invokeCabbageRequest(Request<IcqAccountRoot> request) {
        int result = Request.REQUEST_IDLE;
        do {
            if (result != Request.REQUEST_IDLE) {
                try {
                    Thread.sleep(CABBAGE_TIMEOUT);
                } catch (InterruptedException ignored) {
                    Thread.interrupted();
                }
            }
            result = request.onRequest(accountRoot, null);
        } while (result != Request.REQUEST_DELETE && result != Request.REQUEST_SKIP);
    }

    private String createRequestId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
