package com.tomclaw.mandarin.im.icq;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.core.Settings;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/4/13
 * Time: 8:37 PM
 */
public class AvatarRequest extends Request<IcqAccountRoot> {

    // One http client for all avatar requests, cause they invokes coherently.
    private static final transient HttpClient httpClient;

    private int accountDbId;
    private String buddyDb;
    private String url;

    static {
        httpClient = new DefaultHttpClient();
    }

    public AvatarRequest() {
    }

    public AvatarRequest(int accountDbId, String buddyDb, String url) {
        this.accountDbId = accountDbId;
        this.buddyDb = buddyDb;
        this.url = url;
    }

    @Override
    public int buildRequest() {
        try {
            Log.d(Settings.LOG_TAG, "try to request avatar from ".concat(url));

            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);
            parseResponse(response.getEntity().getContent());

            // Remove request in any case, except Network errors.
            // In McDonald's case or when avatar if bad, we'll
            // drop current task and try to download second time
            // presence or roster is coming.
            return REQUEST_DELETE;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return REQUEST_PENDING;
    }

    private void parseResponse(InputStream inputStream) {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        // Checking for bitmap can be decoded. McDonald's may be.
        if(bitmap != null) {
            // Yeah, avatar is good.
            Log.d(Settings.LOG_TAG, "Ready to save bitmap for URL: " + url);
            saveBitmap(bitmap);
        } else {
            Log.d(Settings.LOG_TAG, "Invalid bitmap for URL: " + url);
        }
    }

    private void saveBitmap(Bitmap bitmap) {
        String avatarHash = getAvatarHash();

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File file = new File(path, avatarHash + ".png");

        try {
            // Make sure the Pictures directory exists.
            path.mkdirs();

            OutputStream os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, os);
            os.close();

        } catch (IOException e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    private final String getAvatarHash() {
        // TODO: real hash!
        return String.valueOf(url.hashCode());
    }
}
