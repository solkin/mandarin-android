package com.tomclaw.mandarin.util;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class AlterableBody extends RequestBody {

    private MediaType type;
    private byte[] content;
    private int offset;
    private int byteCount;

    private static final int PRE_BUFFER = 32768;

    public AlterableBody(MediaType type) {
        this.type = type;
    }

    @Override
    public MediaType contentType() {
        return type;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        int bytesWritten = 0;
        while (bytesWritten < byteCount) {
            int left = byteCount - bytesWritten;
            if (left > PRE_BUFFER) {
                left = PRE_BUFFER;
            }
            sink.write(content, offset + bytesWritten, left);
            bytesWritten += left;
        }
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }
}
