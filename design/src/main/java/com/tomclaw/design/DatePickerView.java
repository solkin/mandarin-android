package com.tomclaw.design;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Solkin on 04.04.2015.
 */
@SuppressWarnings("unused")
public class DatePickerView extends PseudoSpinnerView {

    /**
     * Date format helper
     */
    private static final transient DateFormat DATE_FORMAT = SimpleDateFormat.getDateInstance();

    private int year, month, day;

    public DatePickerView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        setOnClickListener(v -> {
            int initYear, initMonth, initDay;
            // Check if date set or not. If not set - start choosing from current date.
            if (isDateSet()) {
                initYear = year;
                initMonth = month;
                initDay = day;
            } else {
                Calendar calendar = Calendar.getInstance();
                initYear = calendar.get(Calendar.YEAR);
                initMonth = calendar.get(Calendar.MONTH);
                initDay = calendar.get(Calendar.DAY_OF_MONTH);
            }
            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, year, monthOfYear, dayOfMonth) ->
                            updateText(year, monthOfYear, dayOfMonth), initYear, initMonth, initDay
            );
            dialog.setButton(
                    DialogInterface.BUTTON_NEGATIVE,
                    getResources().getString(R.string.date_picker_cancel),
                    (dialog1, which) -> {
                    }
            );
            dialog.show();
        });

        updateText(0, 0, 0);
    }

    private void updateText(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        if (isDateSet()) {
            setText(DATE_FORMAT.format(new GregorianCalendar(year, month, day).getTimeInMillis()));
        } else {
            setText(R.string.date_not_set);
        }
    }

    public void setDate(long milliseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliseconds);
        updateText(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    public boolean isDateSet() {
        return day != 0 || month != 0 || year != 0;
    }

    public long getDate() {
        return new GregorianCalendar(year, month, day).getTimeInMillis();
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
