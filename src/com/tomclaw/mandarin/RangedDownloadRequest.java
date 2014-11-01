package com.tomclaw.mandarin;

import android.util.Log;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.im.AccountRoot;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by solkin on 30.10.14.
 */
public abstract class RangedDownloadRequest<A extends AccountRoot> extends Request<A> {

    public RangedDownloadRequest() {
    }

    @Override
    public int executeRequest() {
        try {
            URL url = new URL(getUrl());
            long size = getSize();
            FileOutputStream outputStream = getOutputStream();
            long read = outputStream.getChannel().size();
            byte[] buffer = new byte[getBufferSize()];
            boolean completed = false;
            do {
                outputStream.getChannel().position(read);
                String range = "bytes=" + read + "-" + (size - 1);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Range", range);
                Log.d(Settings.LOG_TAG, "Range: " + range);
                try {
                    while (read < size) {
                        int statusCode = connection.getResponseCode();
                        Log.d(Settings.LOG_TAG, "Server returns " + statusCode);
                        try {
                            if (statusCode == 200) {
                                completed = true;
                            } else if (statusCode == 503) {
                                throw new IllegalStateException();
                            } else if (statusCode != 206) {
                                throw new DownloadException();
                            }
                            InputStream input = connection.getInputStream();
                            int cache;
                            while ((cache = input.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, cache);
                                outputStream.flush();
                                read += cache;
                                onBufferReleased(read, size);
                            }
                        } finally {
                            connection.disconnect();
                        }
                    }
                } catch (IOException ex) {
                    completed = false;
                    // Pretty network exception.
                    Log.d(Settings.LOG_TAG, "Io exception while downloading", ex);
                    Thread.sleep(3000);
                }
            } while(!completed);
            onSuccess();
            Log.d(Settings.LOG_TAG, "Download completed successfully.");
        } catch (FileNotFoundException ex) {
            onFileNotFound();
            return REQUEST_DELETE;
        } catch (IOException ex) {
            return REQUEST_PENDING;
        } catch (IllegalStateException ex) {
            Log.d(Settings.LOG_TAG, "Server is temporary unavailable.");
            return REQUEST_PENDING;
        } catch (DownloadException ex) {
            onError();
            Log.d(Settings.LOG_TAG, "Server returned strange error.");
            return REQUEST_DELETE;
        } catch (Throwable ex) {
            return REQUEST_PENDING;
        }
        return REQUEST_DELETE;
    }

    /**
     * Returns remote content Url, needs to be downloaded.
     * @return String - download Url.
     */
    public abstract String getUrl() throws Throwable;

    /**
     * Returns remote content size. It may be resolved right now.
     * @return long - remote content size.
     */
    public abstract long getSize();

    /**
     * Output stream to store downloaded data.
     * @return FileOutputStream to store data.
     */
    public abstract FileOutputStream getOutputStream() throws FileNotFoundException;

    /**
     * Returns download block size.
     *
     * @return int - buffer size.
     */
    private int getBufferSize() {
        return 51200;
    }

    protected abstract void onBufferReleased(long sent, long size);

    protected abstract void onFileNotFound();

    protected abstract void onError();

    protected abstract void onSuccess();
}
