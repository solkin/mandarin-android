package com.tomclaw.mandarin.im.icq;

import com.tomclaw.mandarin.im.UrlEncodedBody;
import com.tomclaw.mandarin.util.HttpParamsBuilder;
import com.tomclaw.mandarin.util.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ivsolkin on 01.09.16.
 */
public class CabbageSession {

    private IcqAccountRoot accountRoot;

    public CabbageSession(IcqAccountRoot accountRoot) {
        this.accountRoot = accountRoot;
    }

    public void obtainToken() {
        String url = "https://rapi.icq.net/genToken";
        String token = accountRoot.getTokenA();
        long time = System.currentTimeMillis() / 1000;
        HttpParamsBuilder paramsBuilder = new HttpParamsBuilder();
        try {
            String signed = accountRoot.getSession().signRequest("POST", url, false, paramsBuilder);
            Logger.log("signed: " + signed);
            final String query = new URL(signed).getQuery();
            OkHttpClient client = new OkHttpClient.Builder().build();
            Logger.log("body: " + query);
            RequestBody body = new UrlEncodedBody(query);
            Request request = new Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .build();
            Response response = client.newCall(request).execute();
            Logger.log("response: " + response.body().string());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshToken() {
    }

    public void obtainClient() {
    }

    public void refreshClient() {
    }
}
