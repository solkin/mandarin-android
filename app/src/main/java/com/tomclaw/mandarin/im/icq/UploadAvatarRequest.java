package com.tomclaw.mandarin.im.icq;

import android.graphics.Bitmap;
import android.util.Pair;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.util.HttpUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.tomclaw.mandarin.im.icq.WimConstants.RESPONSE_OBJECT;
import static com.tomclaw.mandarin.im.icq.WimConstants.STATUS_CODE;

/**
 * Created by Igor on 04.04.2015.
 */
public class UploadAvatarRequest extends WimRequest {

    public static final transient int TYPE_NORMAL_AVATAR = 1;
    public static final transient int TYPE_BIG_AVATAR = 2;
    public static final transient int TYPE_LARGE_AVATAR = 3;

    private static final transient int SIZE_NORMAL = 64;
    private static final transient int SIZE_BIG = 128;
    private static final transient int SIZE_LARGE = 600;

    private int type;
    private String hash;

    public UploadAvatarRequest(int type, String hash) {
        this.type = type;
        this.hash = hash;
    }

    @Override
    protected String getHttpRequestType() {
        return HttpUtil.POST;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        // Parsing response.
        JSONObject responseObject = response.getJSONObject(RESPONSE_OBJECT);
        int statusCode = responseObject.getInt(STATUS_CODE);
        // Check for server reply.
        if (statusCode == WIM_OK ||
                statusCode == 460 ||
                statusCode == 462 ||
                statusCode == 600) {
            return REQUEST_DELETE;
        }
        // Maybe incorrect aim sid or other strange error we've not recognized.
        return REQUEST_SKIP;
    }

    @Override
    protected byte[] getBody() throws IOException {
        int size;
        Bitmap.CompressFormat format;
        int quality;
        switch (type) {
            case TYPE_BIG_AVATAR: {
                size = SIZE_BIG;
                format = Bitmap.CompressFormat.JPEG;
                quality = 80;
                break;
            }
            case TYPE_LARGE_AVATAR: {
                size = SIZE_LARGE;
                format = Bitmap.CompressFormat.JPEG;
                quality = 70;
                break;
            }
            default: {
                size = SIZE_NORMAL;
                format = Bitmap.CompressFormat.JPEG;
                quality = 90;
                break;
            }
        }
        Bitmap bitmap = BitmapCache.getInstance().getBitmapSync(hash, size, size, true, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(format, quality, os);
        os.flush();
        os.close();
        return os.toByteArray();
    }

    @Override
    protected String getUrl() {
        return getAccountRoot().getWellKnownUrls().getWebApiBase()
                .concat("expressions/upload");
    }

    @Override
    protected boolean isUrlWithParameters() {
        return true;
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        String typeString;
        switch (type) {
            case TYPE_BIG_AVATAR: {
                typeString = "bigBuddyIcon";
                break;
            }
            case TYPE_LARGE_AVATAR: {
                typeString = "largeBuddyIcon";
                break;
            }
            default: {
                typeString = "buddyIcon";
                break;
            }
        }
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<>(WimConstants.AIM_SID, getAccountRoot().getAimSid()));
        params.add(new Pair<>(WimConstants.FORMAT, WimConstants.FORMAT_JSON));
        params.add(new Pair<>(WimConstants.TYPE, typeString));
        return params;
    }
}
