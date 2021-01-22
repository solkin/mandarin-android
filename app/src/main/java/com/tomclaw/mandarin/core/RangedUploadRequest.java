package com.tomclaw.mandarin.core;

import android.text.TextUtils;

import com.tomclaw.mandarin.core.exceptions.ServerInternalException;
import com.tomclaw.mandarin.core.exceptions.UnauthorizedException;
import com.tomclaw.mandarin.core.exceptions.UnknownResponseException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.AlterableBody;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.VariableBuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.tomclaw.mandarin.util.HttpStatus.SC_OK;
import static com.tomclaw.mandarin.util.HttpStatus.SC_PARTIAL_CONTENT;
import static com.tomclaw.mandarin.util.HttpUtil.getOkHttpClient;

/**
 * Created by Solkin on 14.10.2014.
 */
public abstract class RangedUploadRequest<A extends AccountRoot> extends Request<A> {

    private final transient OkHttpClient httpClient = getOkHttpClient();

    private static final String CONTENT_RANGE = "Content-Range";

    public RangedUploadRequest() {
    }

    @Override
    public int executeRequest() {
        try {
            onStarted();
            VirtualFile virtualFile = getVirtualFile();
            long size = virtualFile.getSize();
            long sent = 0;
            int cache;
            VariableBuffer buffer = new VariableBuffer();
            String contentType = virtualFile.getMimeType();
            boolean completed = false;
            String successReply = null;
            // Obtain uploading Url.
            String url = getUrl(virtualFile.getName(), virtualFile.getSize());
            // Starting upload.
            MediaType type = MediaType.parse("application/octet-stream");
            AlterableBody body = new AlterableBody(type);
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Connection", "Keep-Alive")
                    .addHeader("User-Agent", HttpUtil.getUserAgent())
                    .addHeader("Accept-Ranges", "bytes")
                    .post(body)
                    .build();

            do {
                InputStream input = null;
                try {
                    input = virtualFile.openInputStream(getAccountRoot().getContext());
                    sent = input.skip(sent);
                    while ((cache = input.read(buffer.calculateBuffer())) != -1 || sent < size) {
                        checkInterrupted();
                        // Checking for continuous stream.
                        if (sent + cache > size) {
                            // ... and stop at the specified size.
                            cache = (int) (size - sent);
                        }

                        // Setup content range.
                        String range = "bytes " + sent + "-" + (sent + cache - 1) + "/" + size;
                        Logger.log("upload range: " + range);

                        body.setContent(buffer.getBuffer());
                        body.setOffset(0);
                        body.setByteCount(cache);

                        request = request.newBuilder()
                                .header(CONTENT_RANGE, range)
                                .build();

                        checkInterrupted();

                        buffer.onExecuteStart();
                        Response response = httpClient.newCall(request).execute();
                        buffer.onExecuteCompleted(cache);

                        try {
                            int responseCode = response.code();
                            switch (responseCode) {
                                case SC_OK:
                                    // Uploading completed successfully.
                                    successReply = response.body().string();
                                    completed = true;
                                    break;
                                case SC_PARTIAL_CONTENT:
                                    // Server is still hungry. Next chunk, please...
                                    break;
                                default:
                                    // Seems to be error code. Sadly.
                                    identifyErrorResponse(responseCode);
                                    break;
                            }
                        } finally {
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                responseBody.close();
                            }
                        }
                        sent += cache;
                        onBufferReleased(sent, size);
                        checkInterrupted();
                    }
                } catch (FileNotFoundException ex) {
                    // Where is my file?! :'(
                    throw ex;
                } catch (SocketTimeoutException ex) {
                    // Pretty network exception.
                    Logger.log("SocketTimeoutException exception while uploading", ex);
                    Thread.sleep(3000);
                } catch (InterruptedIOException ex) {
                    // Thread interrupted exception.
                    Logger.log("Interruption while uploading", ex);
                    throw ex;
                } catch (IOException ex) {
                    // Pretty network exception.
                    Logger.log("Io exception while uploading", ex);
                    Thread.sleep(3000);
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            } while (!completed);
            if (!TextUtils.isEmpty(successReply)) {
                onSuccess(successReply);
            }
            return REQUEST_DELETE;
        } catch (UnauthorizedException ex) {
            Logger.log("Unauthorized exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (ServerInternalException ex) {
            Logger.log("Server internal exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (UnknownResponseException ex) {
            Logger.log("Unknown response exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (SecurityException ex) {
            Logger.log("Security exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (FileNotFoundException ex) {
            Logger.log("File is missing while uploading", ex);
            onFileNotFound();
            return REQUEST_LATER;
        } catch (InterruptedIOException ex) {
            Logger.log("Upload interrupted", ex);
            onCancel();
            return REQUEST_LATER;
        } catch (InterruptedException ex) {
            Logger.log("Upload interrupted while read", ex);
            onCancel();
            return REQUEST_LATER;
        } catch (Throwable ex) {
            Logger.log("Unable to execute upload due to exception", ex);
            onPending();
            return REQUEST_PENDING;
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Logger.log("Upload interrupted by thread iterruption");
            throw new InterruptedException();
        }
    }

    protected void identifyErrorResponse(int responseCode) throws Throwable {
        Logger.log("uploading error: " + responseCode);
        switch (responseCode) {
            case 401: {
                throw new UnauthorizedException();
            }
            case 500: {
                throw new ServerInternalException();
            }
            default: {
                throw new UnknownResponseException();
            }
        }
    }

    protected abstract void onStarted() throws Throwable;

    protected abstract void onSuccess(String response) throws Throwable;

    protected abstract void onFail();

    protected abstract void onCancel();

    protected abstract void onFileNotFound();

    protected abstract void onPending();

    protected abstract void onBufferReleased(long sent, long size);

    /**
     * Returns uploading virtual file.
     *
     * @return uploading virtual file
     */
    public abstract VirtualFile getVirtualFile();

    /**
     * Returns request-specific upload Url.
     *
     * @param name - uploading file name
     * @param size - uploading file size
     * @return Request-specific Url
     * @throws Throwable
     */
    protected abstract String getUrl(String name, long size) throws Throwable;
}
