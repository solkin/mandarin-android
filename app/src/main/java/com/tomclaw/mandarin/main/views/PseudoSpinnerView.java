package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import com.tomclaw.mandarin.R;

/**
 * Created by solkin on 29.05.15.
 */
public class PseudoSpinnerView extends TextView {

    public PseudoSpinnerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{R.attr.spinner_triangle_color});
        try {
            int accentColor = a.getColor(0, 0);
            getBackground().setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP);
        } finally {
            a.recycle();
        }
    }
}
