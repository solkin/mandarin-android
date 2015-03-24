package com.tomclaw.mandarin.main.icq;

import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.EditUserInfoActivity;

/**
 * Created by Solkin on 24.03.2015.
 */
public class IcqEditUserInfoActivity extends EditUserInfoActivity {

    @Override
    protected void afterCreate() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<String>(this, R.layout.user_info_gender_spinner_item,
                getResources().getStringArray(R.array.gender_spinner_strings));
        genderAdapter.setDropDownViewResource(R.layout.user_info_gender_spinner_dropdown_item);
        Spinner genderSelector = (Spinner) findViewById(R.id.gender_selector);
        genderSelector.setAdapter(genderAdapter);

        ArrayAdapter<String> childrenAdapter = new ArrayAdapter<String>(this, R.layout.user_info_gender_spinner_item,
                getResources().getStringArray(R.array.children_spinner_strings));
        childrenAdapter.setDropDownViewResource(R.layout.user_info_gender_spinner_dropdown_item);
        Spinner childrenSelector = (Spinner) findViewById(R.id.children_selector);
        childrenSelector.setAdapter(childrenAdapter);

        ArrayAdapter<String> smokingAdapter = new ArrayAdapter<String>(this, R.layout.user_info_gender_spinner_item,
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

    }
}
