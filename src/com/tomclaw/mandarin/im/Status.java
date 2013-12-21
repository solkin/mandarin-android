package com.tomclaw.mandarin.im;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/21/13
 * Time: 12:24 AM
 */
public class Status {

    private int drawable;
    private String title;
    private String value;

    public Status(int drawable, String title, String value) {
        this.drawable = drawable;
        this.title = title;
        this.value = value;
    }

    public int getDrawable() {
        return drawable;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }
}
