package com.tomclaw.mandarin.util;

import android.content.Context;

public class MetricsHelper {

    public static int dp(float v, Context context) {
        return (int) (v * context.getResources().getDisplayMetrics().density + 0.5);
    }

}
