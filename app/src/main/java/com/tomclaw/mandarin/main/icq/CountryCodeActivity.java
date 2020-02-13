package com.tomclaw.mandarin.main.icq;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.util.CountriesProvider;
import com.tomclaw.mandarin.util.Country;

import org.zakariya.stickyheaders.StickyHeaderLayoutManager;

import java.util.List;

/**
 * Country code selection activity
 * Created by Solkin on 02.10.2014.
 */
public class CountryCodeActivity extends AppCompatActivity {

    public static String EXTRA_COUNTRY_SHORT_NAME = "country_short_name";

    private SearchView.OnQueryTextListener onQueryTextListener;
    private CountryCodeAdapter countryCodeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int themeRes = PreferenceHelper.getThemeRes(this);
        setTheme(themeRes);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.country_code_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setHomeButtonEnabled(true);
        }

        List<Country> countries = CountriesProvider.getInstance().getCountries(this);
        countryCodeAdapter = new CountryCodeAdapter(this, countries);
        RecyclerView recyclerView = findViewById(R.id.countries_recycler);
        recyclerView.setAdapter(countryCodeAdapter);
        recyclerView.setLayoutManager(new StickyHeaderLayoutManager());
        countryCodeAdapter.setClickListener(new CountryCodeAdapter.OnClickListener() {
            @Override
            public void onItemClicked(Country country) {
                Intent intent = new Intent()
                        .putExtra(EXTRA_COUNTRY_SHORT_NAME, country.getShortName());
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        onQueryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                countryCodeAdapter.getFilter().filter(newText);
                return false;
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.country_code_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        searchView.setQueryHint(menu.findItem(R.id.menu_search).getTitle());
        searchView.setOnQueryTextListener(onQueryTextListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return false;
    }

}
