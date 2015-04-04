package com.tomclaw.mandarin.main.views;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.RangePickerDialog;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Solkin on 04.04.2015.
 */
public class DatePickerView extends TextView {

    private int year, month, day;

    public DatePickerView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(context,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                updateText(dayOfMonth, monthOfYear + 1, year);
                            }
                        }, year, month, day);
                dialog.show();
            }
        });

        updateText(0, 0, 0);
    }

    private void updateText(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
        if (day == 0 && month == 0 && year == 0) {
            setText(R.string.date_not_set);
        } else {
            String ageToString = getContext().getResources().getString(R.string.date_format, day, month, year);
            setText(ageToString);
        }
    }

    public void setDate(long milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        updateText(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.YEAR));
    }

    public boolean isDateSet() {
        return day != 0 || month != 0 || year != 0;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }
}
