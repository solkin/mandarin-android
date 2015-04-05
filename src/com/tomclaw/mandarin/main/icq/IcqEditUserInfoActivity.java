package com.tomclaw.mandarin.main.icq;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.main.EditUserInfoActivity;
import com.tomclaw.mandarin.main.views.DatePickerView;
import com.tomclaw.mandarin.util.StringUtil;

/**
 * Icq-specific user info edit activity
 * Created by Solkin on 24.03.2015.
 */
public class IcqEditUserInfoActivity extends EditUserInfoActivity {

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
    protected void onUserInfoReceived(Intent intent) {
        Bundle bundle = intent.getExtras();
        // Iterate by info keys.
        for (String key : bundle.keySet()) {
            // Check for this is field key.
            if (StringUtil.isNumeric(key)) {
                int keyInt = Integer.valueOf(key);
                if (keyInt == R.id.friendly_name || keyInt == R.id.first_name || keyInt == R.id.last_name ||
                        keyInt == R.id.website || keyInt == R.id.about_me || keyInt == R.id.city) {
                    EditText editText = (EditText) findViewById(keyInt);
                    editText.setText(bundle.getString(key));
                } else if (keyInt == R.id.gender) {
                    Spinner spinner = (Spinner) findViewById(R.id.gender_selector);
                    spinner.setSelection(bundle.getInt(key));
                } else if (keyInt == R.id.children) {
                    Spinner spinner = (Spinner) findViewById(R.id.children_selector);
                    int value = bundle.getInt(key);
                    if (value > spinner.getCount() - 1) {
                        value = spinner.getCount() - 1;
                    }
                    spinner.setSelection(value);
                } else if (keyInt == R.id.smoking) {
                    Spinner spinner = (Spinner) findViewById(R.id.smoking_selector);
                    int value = bundle.getBoolean(key) ? 1 : 0;
                    spinner.setSelection(value);
                } else if (keyInt == R.id.birth_date) {
                    DatePickerView birthDateView = (DatePickerView) findViewById(R.id.birth_date);
                    long birthDate = bundle.getLong(key);
                    birthDateView.setDate(birthDate);
                }
            }
        }
    }

    @Override
    protected void sendManualAvatarRequest(String hash) {
        RequestHelper.requestUploadAvatar(getContentResolver(), getAccountDbId(), hash);
    }

    @Override
    protected void sendEditUserInfoRequest() {
        String friendlyName = getTextValue(R.id.friendly_name);
        String firstName = getTextValue(R.id.first_name);
        String lastName = getTextValue(R.id.last_name);
        int gender = getSpinnerValue(R.id.gender_selector);
        long birthDate = getDateValue(R.id.birth_date);
        int childrenCount = getSpinnerValue(R.id.children_selector);
        boolean smoking = getSpinnerValue(R.id.smoking_selector) == 1;
        String city = getTextValue(R.id.city);
        String webSite = getTextValue(R.id.website);
        String aboutMe = getTextValue(R.id.about_me);

        RequestHelper.updateUserInfo(getContentResolver(), getAccountDbId(), friendlyName, firstName,
                lastName, gender, birthDate, childrenCount, smoking, city, webSite, aboutMe);
    }

    private String getTextValue(int viewId) {
        TextView textView = (TextView) findViewById(viewId);
        return textView.getText().toString();
    }

    private int getSpinnerValue(int viewId) {
        Spinner spinner = (Spinner) findViewById(viewId);
        return spinner.getSelectedItemPosition();
    }

    private long getDateValue(int viewId) {
        DatePickerView datePickerView = (DatePickerView) findViewById(viewId);
        return datePickerView.getDate();
    }
}
