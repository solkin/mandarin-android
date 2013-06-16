package com.tomclaw.mandarin.im;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 6/9/13
 * Time: 7:25 PM
 */
public abstract class Request {

    /**
     * Request state flags
     */
    public static final int REQUEST_PENDING = 0x00;
    public static final int REQUEST_SENT = 0x01;
    public static final int REQUEST_DELETE = 0xff;

    /**
     * Builds outgoing request and sends it over the network.
     * @return int - status we must setup to this request
     */
    public abstract int onRequest(AccountRoot accountRoot);

    public abstract void onResponse();
}
