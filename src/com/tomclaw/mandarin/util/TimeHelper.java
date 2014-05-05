package com.tomclaw.mandarin.util;

import android.content.Context;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;

/**
 * Created by solkin on 05/05/14.
 */
public class TimeHelper {

    private Context context;

    /**
     * Date and time format helpers
     */
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");
    private static final SimpleDateFormat simpleTimeFormat12 = new SimpleDateFormat("h:mm a");
    private static final SimpleDateFormat simpleTimeFormat24 = new SimpleDateFormat("HH:mm");

    public TimeHelper(Context context) {
        this.context = context;
    }

    private SimpleDateFormat getTimeFormat() {
        return DateFormat.is24HourFormat(context) ? simpleTimeFormat24 : simpleTimeFormat12;
    }

    public String getFormattedDate(long timestamp) {
        return simpleDateFormat.format(timestamp);
    }

    public String getFormattedTime(long timestamp) {
        return getTimeFormat().format(timestamp);
    }
}
