package com.tomclaw.mandarin;

import com.tomclaw.mandarin.core.Request;
import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created by solkin on 30.10.14.
 */
public abstract class RangedDownloadRequest<A extends AccountRoot> extends Request<A> {

    @Override
    public int executeRequest() {
        return REQUEST_DELETE;
    }
}
