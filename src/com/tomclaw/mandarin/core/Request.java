package com.tomclaw.mandarin.core;

import com.tomclaw.mandarin.im.AccountRoot;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:25 PM
 */
public abstract class Request<A extends AccountRoot> {

    /**
     * Request types
     */
    public static final int REQUEST_TYPE_SHORT = 0x00;
    public static final int REQUEST_TYPE_DOWNLOAD = 0x01;
    public static final int REQUEST_TYPE_UPLOAD = 0x02;

    /**
     * Request state flags
     */
    public static final int REQUEST_PENDING = 0x00;
    public static final int REQUEST_SENT = 0x01;
    public static final int REQUEST_DELETE = 0xff;

    private transient A accountRoot;

    public A getAccountRoot() {
        return accountRoot;
    }

    /**
     * Builds outgoing request and sends it over the network.
     *
     * @return int - status we must setup to this request
     */
    public final int onRequest(A accountRoot) {
        this.accountRoot = accountRoot;
        return executeRequest();
    }

    public abstract int executeRequest();
}
