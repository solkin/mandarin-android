package com.tomclaw.mandarin.main.icq;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
        Spinner genderSelector = findViewById(R.id.gender_selector);
        genderSelector.setAdapter(genderAdapter);
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
                int keyInt = Integer.parseInt(key);
                String[] extra = intent.getStringArrayExtra(key);
                // Strange, but... Let's check extra is not empty.
                if (extra != null && extra.length >= 1) {
                    String title = getString(Integer.parseInt(extra[0]));
                    String value = extra[1];
                    if (keyInt == R.id.friendly_name || keyInt == R.id.first_name || keyInt == R.id.last_name ||
                            keyInt == R.id.website || keyInt == R.id.about_me || keyInt == R.id.city) {
                        EditText editText = findViewById(keyInt);
                        editText.setText(value);
                    } else if (keyInt == R.id.gender) {
                        Spinner spinner = findViewById(R.id.gender_selector);
                        int spinnerIndex = 0;
                        if (value.equals(getString(R.string.male))) {
                            spinnerIndex = 1;
                        } else if (value.equals(getString(R.string.female))) {
                            spinnerIndex = 2;
                        }
                        spinner.setSelection(spinnerIndex);
                    } else if (keyInt == R.id.birth_date) {
                        DatePickerView birthDateView = findViewById(R.id.birth_date);
                        long birthDate = Long.parseLong(value);
                        birthDateView.setDate(birthDate);
                    }
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
        String friendlyName = getUserNick();
        String firstName = getFirstName();
        String lastName = getLastName();
        int gender = getSpinnerValue(R.id.gender_selector);
        long birthDate = getDateValue(R.id.birth_date);
        String city = getTextValue(R.id.city);
        String webSite = getTextValue(R.id.website);
        String aboutMe = getTextValue(R.id.about_me);

        RequestHelper.updateUserInfo(getContentResolver(), getAccountDbId(), friendlyName, firstName,
                lastName, gender, birthDate, city, webSite, aboutMe);
    }

    @Override
    protected String getUserNick() {
        return getTextValue(R.id.friendly_name);
    }

    @Override
    protected String getFirstName() {
        return getTextValue(R.id.first_name);
    }

    @Override
    protected String getLastName() {
        return getTextValue(R.id.last_name);
    }

    private String getTextValue(int viewId) {
        TextView textView = findViewById(viewId);
        return textView.getText().toString();
    }

    private int getSpinnerValue(int viewId) {
        Spinner spinner = findViewById(viewId);
        return spinner.getSelectedItemPosition();
    }

    private long getDateValue(int viewId) {
        DatePickerView datePickerView = findViewById(viewId);
        return datePickerView.getDate();
    }
}
