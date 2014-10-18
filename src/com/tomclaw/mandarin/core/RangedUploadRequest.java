package com.tomclaw.mandarin.core;

import android.util.Log;
import com.tomclaw.mandarin.core.exceptions.ServerInternalException;
import com.tomclaw.mandarin.core.exceptions.UnauthorizedException;
import com.tomclaw.mandarin.core.exceptions.UnknownResponseException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.HttpUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;

/**
 * Created by Solkin on 14.10.2014.
 */
public abstract class RangedUploadRequest<A extends AccountRoot> extends Request<A> {

    private final transient HttpClient httpClient = new DefaultHttpClient();

    private String path;

    public RangedUploadRequest() {
    }

    public RangedUploadRequest(String path) {
        this.path = path;
    }

    @Override
    public int executeRequest() {
        try {
            onStarted();
            File file = new File(path);
            if(!file.exists()) {
                throw new FileNotFoundException();
            }
            long size = file.length();
            long sent = 0;
            int cache;
            byte[] buffer = new byte[getBufferSize()];
            String contentType = HttpUtil.getMimeType(path);
            boolean completed = false;
            // Obtain uploading Url.
            String url = getUrl(file.getName(), file.length());
            // Starting upload.
            do {
                InputStream input = null;
                try {
                    input = new FileInputStream(file);
                    sent = input.skip(sent);

                    HttpPost post = new HttpPost(url);
                    post.setHeader("Connection", "Keep-Alive");
                    post.setHeader("Content-Type", contentType);
                    post.setHeader("Accept-Ranges", "bytes");

                    while ((cache = input.read(buffer)) != -1 || sent < size) {
                        // Checking for continous stream.
                        if (sent + cache > size) {
                            // ... and stop at the specified size.
                            cache = (int) (size - sent);
                        }
                        byte[] entityData = new byte[cache];
                        System.arraycopy(buffer, 0, entityData, 0, cache);
                        ByteArrayEntity arrayEntity = new ByteArrayEntity(entityData);

                        // Setup content range.
                        String range = "bytes " + sent + "-" + (sent + cache - 1) + "/" + size;
                        // Update headers.
                        post.setHeader("Content-Range", range);
                        post.setEntity(arrayEntity);

                        HttpResponse httpResponse = httpClient.execute(post);
                        HttpEntity entity = httpResponse.getEntity();
                        if (entity == null) {
                            throw new IOException();
                        }
                        try {
                            int responseCode = httpResponse.getStatusLine().getStatusCode();
                            String response = EntityUtils.toString(entity);
                            if(responseCode == 200) {
                                // Uploading completed successfully.
                                onSuccess(response);
                                completed = true;
                            } else if(responseCode == 206) {
                                // Server is still hungry. Next chunk, please...
                            } else {
                                // Seems to be error code. Sadly.
                                identifyErrorResponse(responseCode);
                            }
                        } finally {
                            if(entity != null) {
                                entity.consumeContent();
                            }
                        }
                        sent += cache;
                        onBufferReleased(sent, size);
                    }
                } catch (IOException ex) {
                    // Pretty network exception.
                    Log.d(Settings.LOG_TAG, "Io exception while uploading", ex);
                    Thread.sleep(3000);
                } finally {
                    if(input != null) {
                        input.close();
                    }
                }
            } while(!completed);
            return REQUEST_DELETE;
        } catch (UnauthorizedException ex) {
            Log.d(Settings.LOG_TAG, "Unauthorized exception while uploading", ex);
            onFail();
            return REQUEST_DELETE;
        } catch (ServerInternalException ex) {
            Log.d(Settings.LOG_TAG, "Server internal exception while uploading", ex);
            onFail();
            return REQUEST_DELETE;
        } catch (UnknownResponseException ex) {
            Log.d(Settings.LOG_TAG, "Unknown response exception while uploading", ex);
            onFail();
            return REQUEST_DELETE;
        } catch (FileNotFoundException ex) {
            Log.d(Settings.LOG_TAG, "File is missing while uploading", ex);
            onFileNotFound();
            return REQUEST_DELETE;
        } catch (Throwable ex) {
            Log.d(Settings.LOG_TAG, "Unable to execute upload due to exception", ex);
            return REQUEST_PENDING;
        }
    }

    protected void identifyErrorResponse(int responseCode) throws Throwable {
        switch(responseCode) {
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

    public abstract void onFileNotFound();

    private InputStream getInputStream(int responseCode, HttpURLConnection connection)
            throws IOException {
        if (responseCode >= HttpStatus.SC_BAD_REQUEST) {
            return connection.getErrorStream();
        } else {
            return connection.getInputStream();
        }
    }

    protected abstract void onBufferReleased(long sent, long size);

    /**
     * Returns uploading range block size.
     * @return int - buffer size.
     */
    private int getBufferSize() {
        return 51200;
    }

    /**
     * Returns uploading absolute file path.
     * @return uploading file path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns request-specific upload Url.
     * @param name - uploading file name
     * @param size - uploading file size
     * @return Request-specific Url
     * @throws Throwable
     */
    protected abstract String getUrl(String name, long size) throws Throwable;
}
