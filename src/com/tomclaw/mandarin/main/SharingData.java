package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Igor on 23.04.2015.
 */
public class SharingData implements Serializable {

    public static final transient String EXTRA_SUBJECT = "android.intent.extra.SUBJECT";
    public static final transient String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final transient String EXTRA_STREAM = "android.intent.extra.STREAM";

    private String subject, text;
    private List<String> uris;

    public SharingData() {
    }

    public SharingData(String subject, String text, List<Uri> uris) {
        this.subject = subject;
        this.text = text;
        if (uris != null) {
            this.uris = new ArrayList<>();
            for (Uri uri : uris) {
                this.uris.add(uri.toString());
            }
        }
    }

    public SharingData(Intent intent) {
        boolean actionSend = Intent.ACTION_SEND.equals(intent.getAction());
        boolean actionSendMultiple = Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction());
        if (actionSend | actionSendMultiple) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                if (extras.containsKey(EXTRA_SUBJECT)) {
                    subject = extras.getString(EXTRA_SUBJECT);
                }
                if (extras.containsKey(EXTRA_TEXT)) {
                    text = extras.getString(EXTRA_TEXT);
                }
                if (extras.containsKey(EXTRA_STREAM)) {
                    if (actionSend) {
                        Uri uri = extras.getParcelable(EXTRA_STREAM);
                        this.uris = Collections.singletonList(uri.toString());
                    } else {
                        List<Uri> uris = extras.getParcelableArrayList(EXTRA_STREAM);
                        this.uris = new ArrayList<>();
                        for (Uri uri : uris) {
                            this.uris.add(uri.toString());
                        }
                    }
                }
            }
        }
    }

    public String getSubject() {
        return subject;
    }

    public String getText() {
        return text;
    }

    public List<Uri> getUri() {
        if (uris != null) {
            List<Uri> uris = new ArrayList<>();
            for (String uri : this.uris) {
                uris.add(Uri.parse(uri));
            }
            return uris;
        }
        return null;
    }

    public boolean isValid() {
        return !TextUtils.isEmpty(text) || uris != null;
    }
}
