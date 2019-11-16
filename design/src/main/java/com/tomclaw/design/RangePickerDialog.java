package com.tomclaw.design;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;

/**
 * Created by Solkin on 11.07.2014.
 */
@SuppressWarnings("WeakerAccess")
public class RangePickerDialog extends AlertDialog {

    private NumberPicker pickerMin;
    private NumberPicker pickerMax;

    @SuppressLint("InflateParams")
    public RangePickerDialog(Context context, int title, int min, int max,
                             int initMin, int initMax, final RangePickerListener listener) {
        super(context);

        setTitle(title);

        View view = LayoutInflater.from(context).inflate(R.layout.range_picker_dialog, null);

        pickerMin = view.findViewById(R.id.range_min);
        pickerMax = view.findViewById(R.id.range_max);

        pickerMin.setMinValue(min);
        pickerMin.setMaxValue(max);

        pickerMax.setMinValue(min);
        pickerMax.setMaxValue(max);

        pickerMin.setValue(initMin);
        pickerMax.setValue(initMax);

        pickerMin.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (getMinValue() > getMaxValue()) {
                setMaxValue(getMinValue());
            }
        });

        pickerMax.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (getMinValue() > getMaxValue()) {
                setMinValue(getMaxValue());
            }
        });

        setView(view);

        setButton(BUTTON_NEGATIVE, context.getString(R.string.not_now), (dialog, which) -> dismiss());
        setButton(BUTTON_NEUTRAL, context.getString(R.string.age_any), (dialog, which) -> listener.onRangeAny());
        setButton(BUTTON_POSITIVE, context.getString(R.string.apply), (dialog, which) -> listener.onRangePicked(getMinValue(), getMaxValue()));
    }

    private void setMinValue(int value) {
        pickerMin.setValue(value);
    }

    private void setMaxValue(int value) {
        pickerMax.setValue(value);
    }

    public int getMinValue() {
        return pickerMin.getValue();
    }

    public int getMaxValue() {
        return pickerMax.getValue();
    }

    public interface RangePickerListener {

        void onRangePicked(int min, int max);

        void onRangeAny();

    }

}
