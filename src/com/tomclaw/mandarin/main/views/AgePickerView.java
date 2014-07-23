package com.tomclaw.mandarin.main.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.RangePickerDialog;

/**
 * Created by Solkin on 11.07.2014.
 */
public class AgePickerView extends TextView {

    private int ageMin = 14;
    private int ageMax = 99;
    private int valueMin = 19;
    private int valueMax = 26;

    public AgePickerView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AgePickerView,
                0, 0);

        try {
            ageMin = a.getInteger(R.styleable.AgePickerView_ageMin, ageMin);
            ageMax = a.getInteger(R.styleable.AgePickerView_ageMax, ageMax);
            valueMin = a.getInteger(R.styleable.AgePickerView_valueMin, valueMin);
            valueMax = a.getInteger(R.styleable.AgePickerView_valueMax, valueMax);
        } finally {
            a.recycle();
        }

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RangePickerDialog dialog = new RangePickerDialog(context, R.string.select_age,
                        ageMin, ageMax, valueMin, valueMax, new RangePickerDialog.RangePickerListener() {
                    @Override
                    public void onRangePicked(int min, int max) {
                        updateText(min, max);
                    }
                });
                dialog.show();
            }
        });

        updateText(valueMin, valueMax);
    }

    private void updateText(int min, int max) {
        valueMin = min;
        valueMax = max;
        String ageToString = getContext().getResources().getQuantityString(R.plurals.buddy_years, max, max);
        setText(getResources().getString(R.string.age_format, min, ageToString));
    }

    public boolean isAnyAge() {
        return (ageMin == valueMin) && (ageMax == valueMax);
    }

    public int getValueMin() {
        return valueMin;
    }

    public int getValueMax() {
        return valueMax;
    }
}
