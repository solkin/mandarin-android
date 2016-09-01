package com.tomclaw.mandarin.im;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * Created by ivsolkin on 31.08.16.
 */
public class UrlEncodedBody extends RequestBody {

    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final String body;

    public UrlEncodedBody(String body) {
        this.body = body;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(FORM_CONTENT_TYPE);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        sink.writeUtf8(body);
    }
}
