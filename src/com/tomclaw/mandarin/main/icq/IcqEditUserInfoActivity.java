package com.tomclaw.mandarin.main.icq;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.EditUserInfoActivity;
import com.tomclaw.mandarin.main.views.DatePickerView;
import com.tomclaw.mandarin.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Icq-specific user info edit activity
 * Created by Solkin on 24.03.2015.
 */
public class IcqEditUserInfoActivity extends EditUserInfoActivity {

    /**
     * Date format helper
     */
    private static final transient SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Override
    protected void afterCreate() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.user_info_gender_spinner_item,
                getResources().getStringArray(R.array.gender_edit_spinner_strings));
        genderAdapter.setDropDownViewResource(R.layout.user_info_gender_spinner_dropdown_item);
        Spinner genderSelector = (Spinner) findViewById(R.id.gender_selector);
        genderSelector.setAdapter(genderAdapter);

        ArrayAdapter<String> childrenAdapter = new ArrayAdapter<>(this, R.layout.user_info_gender_spinner_item,
                getResources().getStringArray(R.array.children_spinner_strings));
        childrenAdapter.setDropDownViewResource(R.layout.user_info_gender_spinner_dropdown_item);
        Spinner childrenSelector = (Spinner) findViewById(R.id.children_selector);
        childrenSelector.setAdapter(childrenAdapter);

        ArrayAdapter<String> smokingAdapter = new ArrayAdapter<>(this, R.layout.user_info_gender_spinner_item,
                getResources().getStringArray(R.array.smoking_spinner_strings));
        smokingAdapter.setDropDownViewResource(R.layout.user_info_gender_spinner_dropdown_item);
        Spinner smokingSelector = (Spinner) findViewById(R.id.smoking_selector);
        smokingSelector.setAdapter(smokingAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.icq_edit_user_info_activity;
    }

    @Override
    protected void onUserInfoRequestError() {
    }

    @Override
    protected void onUserInfoReceived(Intent intent) {
        Bundle bundle = intent.getExtras();
        // Iterate by info keys.
        for (String key : bundle.keySet()) {
            // Check for this is field key.
            if (StringUtil.isNumeric(key)) {
                int keyInt = Integer.valueOf(key);
                if(keyInt == R.id.friendly_name || keyInt == R.id.first_name || keyInt == R.id.last_name ||
                        keyInt == R.id.website || keyInt == R.id.about_me || keyInt == R.id.city) {
                    EditText editText = (EditText) findViewById(keyInt);
                    editText.setText(bundle.getString(key));
                } else if(keyInt == R.id.gender) {
                    Spinner spinner = (Spinner) findViewById(R.id.gender_selector);
                    spinner.setSelection(bundle.getInt(key));
                } else if(keyInt == R.id.children) {
                    Spinner spinner = (Spinner) findViewById(R.id.children_selector);
                    int value = bundle.getInt(key);
                    if(value > spinner.getCount() - 1) {
                        value = spinner.getCount() - 1;
                    }
                    spinner.setSelection(value);
                } else if(keyInt == R.id.smoking) {
                    Spinner spinner = (Spinner) findViewById(R.id.smoking_selector);
                    int value = bundle.getBoolean(key) ? 1 : 0;
                    spinner.setSelection(value);
                } else if(keyInt == R.id.birth_date) {
                    DatePickerView birthDateView = (DatePickerView) findViewById(R.id.birth_date);
                    long birthDate = bundle.getLong(key);
                    birthDateView.setDate(birthDate);
                    // TextView birthDateView = (TextView) findViewById(R.id.birth_date);
                    // birthDateView.setText(simpleDateFormat.format(birthDate));
                }
            }
        }
    }

    @Override
    protected void sendEditUserInfoRequest() {
    }
}
