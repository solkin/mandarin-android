package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.icq.IcqSearchOptionsBuilder;
import com.tomclaw.design.AgePickerView;

/**
 * Created by Igor on 26.06.2014.
 */
public class SearchActivity extends ChiefActivity {

    private int accountDbId;

    private TextView keywordEdit;
    private TextView cityEdit;
    private AgePickerView agePickerView;
    private Spinner genderSpinner;
    private CheckBox onlineBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountDbId = getIntentAccountDbId(getIntent());
        if (accountDbId == -1) {
            finish();
            return;
        }

        setContentView(R.layout.search_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.search_gender_spinner_item,
                getResources().getStringArray(R.array.gender_search_spinner_strings));
        adapter.setDropDownViewResource(R.layout.search_gender_spinner_dropdown_item);
        genderSpinner = findViewById(R.id.gender_selector);
        genderSpinner.setAdapter(adapter);

        keywordEdit = findViewById(R.id.keyword_edit);
        cityEdit = findViewById(R.id.city_edit);
        agePickerView = findViewById(R.id.age_range);
        onlineBox = findViewById(R.id.online_check);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_menu, menu);
        final MenuItem item = menu.findItem(R.id.search_action_menu);
        TextView actionView = ((TextView) item.getActionView());
        actionView.setText(actionView.getText().toString().toUpperCase());
        actionView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                menu.performIdentifierAction(item.getItemId(), 0);
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.search_action_menu: {
                doSearch();
                break;
            }
        }
        return true;
    }

    private int getIntentAccountDbId(Intent intent) {
        Bundle bundle = intent.getExtras();
        int accountDbId = -1;
        // Checking for bundle condition.
        if (bundle != null) {
            accountDbId = bundle.getInt(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        }
        return accountDbId;
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }

    private void doSearch() {
        IcqSearchOptionsBuilder builder = new IcqSearchOptionsBuilder(System.currentTimeMillis());
        String keyword = keywordEdit.getText().toString();
        String city = cityEdit.getText().toString();
        // Obtain search builder instance from account.
        builder.keyword(keyword);
        if (!TextUtils.isEmpty(city)) {
            builder.city(city);
        }
        builder.online(onlineBox.isChecked());
        if (!agePickerView.isAnyAge()) {
            builder.age(agePickerView.getValueMin(), agePickerView.getValueMax());
        }
        String selectedGender = (String) genderSpinner.getSelectedItem();
        if (TextUtils.equals(selectedGender, getString(R.string.gender_female))) {
            builder.gender(Gender.Female);
        } else if (TextUtils.equals(selectedGender, getString(R.string.gender_male))) {
            builder.gender(Gender.Male);
        }

        Intent intent = new Intent(SearchActivity.this, SearchResultActivity.class);
        intent.putExtra(SearchResultActivity.SEARCH_OPTIONS, builder);
        intent.putExtra(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
        startActivity(intent);
    }
}
