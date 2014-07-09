package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.im.Gender;
import com.tomclaw.mandarin.im.SearchOptionsBuilder;
import com.tomclaw.mandarin.im.icq.IcqSearchOptionsBuilder;

/**
 * Created by Igor on 26.06.2014.
 */
public class SearchActivity extends ChiefActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int accountDbId = getIntentAccountDbId(getIntent());
        if(accountDbId == -1) {
            finish();
            return;
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setContentView(R.layout.search_activity);

        final TextView firstName = (TextView) findViewById(R.id.first_name_edit);
        final TextView lastName = (TextView) findViewById(R.id.last_name_edit);
        final TextView keywordName = (TextView) findViewById(R.id.keyword_edit);

        final CheckBox onlineBox = (CheckBox) findViewById(R.id.online_check);
        final RadioGroup genderGroup = (RadioGroup) findViewById(R.id.gender_group);

        final TextView ageFrom = (TextView) findViewById(R.id.age_from_edit);
        final TextView ageTo = (TextView) findViewById(R.id.age_to_edit);

        Button searchButton = (Button) findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IcqSearchOptionsBuilder builder = new IcqSearchOptionsBuilder();
                // Obtain search builder instance from account.
                builder.firstName(firstName.getText().toString());
                builder.lastName(lastName.getText().toString());
                builder.keyword(keywordName.getText().toString());
                builder.online(onlineBox.isChecked());
                builder.age(Integer.parseInt(ageFrom.getText().toString()),
                        Integer.parseInt(ageTo.getText().toString()));
                Gender gender;
                switch (genderGroup.getCheckedRadioButtonId()) {
                    case R.id.female_radio: {
                        gender = Gender.Female;
                        break;
                    }
                    case R.id.male_radio: {
                        gender = Gender.Male;
                        break;
                    }
                    default: {
                        gender = Gender.Any;
                        break;
                    }
                }
                builder.gender(gender);

                Intent intent = new Intent(SearchActivity.this, SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.SEARCH_OPTIONS, builder);
                intent.putExtra(GlobalProvider.ROSTER_BUDDY_ACCOUNT_DB_ID, accountDbId);
                startActivity(intent);
            }
        });
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
            }
        }
        return true;
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
    }
}
