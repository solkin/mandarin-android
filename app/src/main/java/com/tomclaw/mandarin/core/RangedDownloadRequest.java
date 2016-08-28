package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.core.exceptions.DownloadCancelledException;
import com.tomclaw.mandarin.core.exceptions.DownloadException;
import com.tomclaw.mandarin.im.AccountRoot;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.VariableBuffer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
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
            onStarted();
            URL url = new URL(getUrl());
            long size = getSize();
            FileOutputStream outputStream = getOutputStream();
            long read = outputStream.getChannel().size();
            VariableBuffer buffer = new VariableBuffer();
            onDownload();
            do {
                outputStream.getChannel().position(read);
                String range = "bytes=" + read + "-" + (size - 1);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Range", range);
                Logger.log("Range: " + range);
                try {
                    int statusCode = connection.getResponseCode();
                    Logger.log("Server returns " + statusCode);
                    if (statusCode == 503) {
                        throw new IllegalStateException();
                    } else if (statusCode != 200 && statusCode != 206) {
                        throw new DownloadException();
                    }
                    InputStream input = connection.getInputStream();
                    int cache;
                    buffer.onExecuteStart();
                    while ((cache = input.read(buffer.calculateBuffer())) != -1) {
                        buffer.onExecuteCompleted(cache);
                        outputStream.write(buffer.getBuffer(), 0, cache);
                        outputStream.flush();
                        read += cache;
                        onBufferReleased(read, size);
                        buffer.onExecuteStart();
                        // Checking for thread is interrupted.
                        if (Thread.interrupted()) {
                            // Request is interrupted.
                            throw new DownloadCancelledException();
                        }
                    }
                } catch (InterruptedIOException ex) {
                    // Request might be interrupted.
                    throw new DownloadCancelledException();
                } catch (IOException ex) {
                    // Pretty network exception.
                    Logger.log("Io exception while downloading", ex);
                    Thread.sleep(3000);
                }
            } while (read < size);
            onSuccess();
            Logger.log("Download completed successfully.");
        } catch (DownloadException ex) {
            onFail();
            Logger.log("Server returned strange error.");
            return REQUEST_DELETE;
        } catch (InterruptedIOException ex) {
            Logger.log("Download interrupted.");
            onCancel();
            return REQUEST_LATER;
        } catch (InterruptedException ex) {
            Logger.log("Download interrupted.");
            onCancel();
            return REQUEST_LATER;
        } catch (DownloadCancelledException ex) {
            // No need to process task this time.
            onCancel();
            return REQUEST_LATER;
        } catch (FileNotFoundException ex) {
            onFileNotFound();
            return REQUEST_DELETE;
        } catch (IOException ex) {
            onPending();
            return REQUEST_PENDING;
        } catch (IllegalStateException ex) {
            Logger.log("Server is temporary unavailable.");
            onPending();
            return REQUEST_PENDING;
        } catch (Throwable ex) {
            onPending();
            return REQUEST_PENDING;
        }
        return REQUEST_DELETE;
    }

    /**
     * Returns remote content Url, needs to be downloaded.
     *
     * @return String - download Url.
     */
    public abstract String getUrl() throws Throwable;

    /**
     * Returns remote content size. It may be resolved right now.
     *
     * @return long - remote content size.
     */
    public abstract long getSize();

    /**
     * Output stream to store downloaded data.
     *
     * @return FileOutputStream to store data.
     */
    public abstract FileOutputStream getOutputStream() throws FileNotFoundException;

    protected abstract void onStarted() throws Throwable;

    protected abstract void onDownload();

    protected abstract void onBufferReleased(long sent, long size);

    protected abstract void onFileNotFound();

    protected abstract void onFail();

    protected abstract void onCancel();

    protected abstract void onSuccess();

    protected abstract void onPending();
}
