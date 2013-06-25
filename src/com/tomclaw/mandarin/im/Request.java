package com.tomclaw.mandarin.im;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:25 PM
 */
public abstract class Request<A extends AccountRoot> {

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
        return buildRequest();
    }

    public abstract int buildRequest();

    public abstract void onResponse();
}
