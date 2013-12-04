package com.tomclaw.mandarin.im.icq;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.tomclaw.mandarin.core.AvatarCache;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 12/4/13
 * Time: 8:37 PM
 */
public class AvatarRequest extends Request<IcqAccountRoot> {

    // One http client for all avatar requests, cause they invokes coherently.
    private static final transient HttpClient httpClient;

    private static final String HASH_ALGORITHM = "MD5";
    private static final int RADIX = 10 + 26; // 10 digits + 26 letters

    private String buddyId;
    private String url;

    private transient String avatarHash;

    static {
        httpClient = new DefaultHttpClient();
    }

    public AvatarRequest() {
    }

    public AvatarRequest(String buddyId, String url) {
        this.buddyId = buddyId;
        this.url = url;
    }

    @Override
    public int buildRequest() {
        try {
            Log.d(Settings.LOG_TAG, "try to request avatar from ".concat(url));

            avatarHash = getAvatarHash(url);

            HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);
            if(parseResponse(response.getEntity().getContent())) {
                try {
                    Log.d(Settings.LOG_TAG, "Update destination buddy " + buddyId + " avatar hash to " + avatarHash);
                    QueryHelper.modifyAvatar(getAccountRoot().getContentResolver(),
                            getAccountRoot().getAccountDbId(), buddyId, avatarHash);
                    Log.d(Settings.LOG_TAG, "Avatar complex operations succeeded!");
                } catch (BuddyNotFoundException ignored) {
                    Log.d(Settings.LOG_TAG, "Hm... Buddy became not found while avatar being downloaded...");
                }
            }
            // Remove request in any case, except Network errors.
            // In McDonald's case or when avatar if bad, we'll
            // drop current task and try to download second time
            // presence or roster is coming.
            return REQUEST_DELETE;
        } catch (Throwable e) {
            e.printStackTrace();
            Log.d(Settings.LOG_TAG, "Oh, avatar download failed. We'll try again later.");
        }
        return REQUEST_PENDING;
    }

    private boolean parseResponse(InputStream inputStream) {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        // Checking for bitmap can be decoded. McDonald's may be.
        if(bitmap != null) {
            // Yeah, avatar is good.
            Log.d(Settings.LOG_TAG, "Ready to save bitmap for URL: " + url);
            return saveBitmap(bitmap);
        } else {
            Log.d(Settings.LOG_TAG, "Invalid bitmap for URL: " + url);
        }
        return false;
    }

    private boolean saveBitmap(Bitmap bitmap) {
        return AvatarCache.getInstance().saveBitmapSync(avatarHash, bitmap);
    }

    public static String getAvatarHash(String url) {
        byte[] md5 = getMD5(url.getBytes());
        BigInteger bi = new BigInteger(md5).abs();
        return bi.toString(RADIX);
    }

    private static byte[] getMD5(byte[] data) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(data);
            hash = digest.digest();
        } catch (NoSuchAlgorithmException ignored) {
        }
        return hash;
    }
}
