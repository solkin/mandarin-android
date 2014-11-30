package com.tomclaw.mandarin.core;

import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.core.exceptions.ServerInternalException;
import com.tomclaw.mandarin.core.exceptions.UnauthorizedException;
import com.tomclaw.mandarin.core.exceptions.UnknownResponseException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.VariableBuffer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;

/**
 * Created by Solkin on 14.10.2014.
 */
public abstract class RangedUploadRequest<A extends AccountRoot> extends Request<A> {

    private final transient HttpClient httpClient = new DefaultHttpClient();

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
            HttpPost post = new HttpPost(url);
            post.setHeader("Connection", "Keep-Alive");
            post.setHeader("Content-Type", contentType);
            post.setHeader("Accept-Ranges", "bytes");
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
                        byte[] entityData = new byte[cache];
                        System.arraycopy(buffer.getBuffer(), 0, entityData, 0, cache);
                        ByteArrayEntity arrayEntity = new ByteArrayEntity(entityData);

                        // Setup content range.
                        String range = "bytes " + sent + "-" + (sent + cache - 1) + "/" + size;
                        // Update headers.
                        post.setHeader("Content-Range", range);
                        post.setEntity(arrayEntity);

                        buffer.onExecuteStart();
                        HttpResponse httpResponse = httpClient.execute(post);
                        HttpEntity entity = httpResponse.getEntity();
                        buffer.onExecuteCompleted(cache);
                        if (entity == null) {
                            throw new IOException();
                        }
                        try {
                            int responseCode = httpResponse.getStatusLine().getStatusCode();
                            if (responseCode == 200) {
                                // Uploading completed successfully.
                                successReply = EntityUtils.toString(entity);
                                completed = true;
                            } else if (responseCode == 206) {
                                // Server is still hungry. Next chunk, please...
                            } else {
                                // Seems to be error code. Sadly.
                                identifyErrorResponse(responseCode);
                            }
                        } finally {
                            if (entity != null) {
                                entity.consumeContent();
                            }
                        }
                        sent += cache;
                        onBufferReleased(sent, size);
                        checkInterrupted();
                    }
                } catch (FileNotFoundException ex) {
                    // Where is my file?! :'(
                    throw ex;
                } catch (InterruptedIOException ex) {
                    // Thread interrupted exception.
                    Log.d(Settings.LOG_TAG, "Interruption while uploading", ex);
                    throw ex;
                } catch (InterruptedException ex) {
                    // Thread Io interrupted exception.
                    Log.d(Settings.LOG_TAG, "Interruption while uploading", ex);
                    throw ex;
                } catch (IOException ex) {
                    // Pretty network exception.
                    Log.d(Settings.LOG_TAG, "Io exception while uploading", ex);
                    Thread.sleep(3000);
                } finally {
                    if (input != null) {
                        input.close();
                    }
                }
            } while (!completed);
            if(!TextUtils.isEmpty(successReply)) {
                onSuccess(successReply);
            }
            return REQUEST_DELETE;
        } catch (UnauthorizedException ex) {
            Log.d(Settings.LOG_TAG, "Unauthorized exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (ServerInternalException ex) {
            Log.d(Settings.LOG_TAG, "Server internal exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (UnknownResponseException ex) {
            Log.d(Settings.LOG_TAG, "Unknown response exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (SecurityException ex) {
            Log.d(Settings.LOG_TAG, "Security exception while uploading", ex);
            onFail();
            return REQUEST_LATER;
        } catch (FileNotFoundException ex) {
            Log.d(Settings.LOG_TAG, "File is missing while uploading", ex);
            onFileNotFound();
            return REQUEST_LATER;
        } catch (InterruptedIOException ex) {
            Log.d(Settings.LOG_TAG, "Upload interrupted", ex);
            onCancel();
            return REQUEST_LATER;
        } catch (InterruptedException ex) {
            Log.d(Settings.LOG_TAG, "Upload interrupted while read", ex);
            onCancel();
            return REQUEST_LATER;
        } catch (Throwable ex) {
            Log.d(Settings.LOG_TAG, "Unable to execute upload due to exception", ex);
            onPending();
            return REQUEST_PENDING;
        }
    }

    private void checkInterrupted() throws InterruptedException {
        if(Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    protected void identifyErrorResponse(int responseCode) throws Throwable {
        Log.d(Settings.LOG_TAG, "uploading error: " + responseCode);
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
