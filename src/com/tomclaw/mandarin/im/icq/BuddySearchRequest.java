package com.tomclaw.mandarin.im.icq;

import android.util.Pair;
import com.tomclaw.mandarin.im.SearchOptions;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by solkin on 09/03/14.
 */
public class BuddySearchRequest extends WimRequest {

    private final SearchOptions searchOptions;
    private final int nToGet;
    private final int nToSkip;
    private String locale;

    public BuddySearchRequest(SearchOptions searchOptions, int nToGet, int nToSkip, String locale) {
        this.searchOptions = searchOptions;
        this.nToGet = nToGet;
        this.nToSkip = nToSkip;
        this.locale = locale;
    }

    @Override
    protected int parseJson(JSONObject response) throws JSONException {
        return 0;
    }

    @Override
    protected String getUrl() {
        return "memberDir/search";
    }

    @Override
    protected List<Pair<String, String>> getParams() {
        StringBuilder match = new StringBuilder();

        List<Pair<String, String>> params = new ArrayList<Pair<String, String>>();
        params.add(new Pair<String, String>("aimsid", getAccountRoot().getAimSid()));
        params.add(new Pair<String, String>("f", "json"));
        params.add(new Pair<String, String>("infoLevel", "mid"));
        params.add(new Pair<String, String>("nToSkip", String.valueOf(nToSkip)));
        params.add(new Pair<String, String>("nToGet", String.valueOf(nToGet)));
        params.add(new Pair<String, String>("locale", locale));
        params.add(new Pair<String, String>("match", match.toString()));
        return params;
    }
}
