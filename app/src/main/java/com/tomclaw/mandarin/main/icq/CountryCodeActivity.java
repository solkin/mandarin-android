package com.tomclaw.mandarin.main.icq;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.CountriesProvider;
import com.tomclaw.mandarin.util.Country;
import com.tomclaw.preferences.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
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

        // Initialize action bar.
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);

        countryCodeAdapter = new CountryCodeAdapter(this, CountriesProvider.getInstance().getCountries(this));
        StickyListHeadersListView listView = findViewById(R.id.countries_list_view);
        listView.setAdapter(countryCodeAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Country country = countryCodeAdapter.getItem(position);
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_COUNTRY_SHORT_NAME, country.getShortName()));
            finish();
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
        // Configure the search info and add event listener.
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

    private class CountryCodeAdapter extends BaseAdapter implements StickyListHeadersAdapter, Filterable {

        private LayoutInflater inflater;
        private List<Country> originalCountries;
        private List<Country> countries;

        private CountryCodeAdapter(Context context, List<Country> countries) {
            this.inflater = LayoutInflater.from(context);
            this.originalCountries = countries;
            this.countries = countries;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.roster_sticky_header, parent, false);
            }
            TextView textView = convertView.findViewById(R.id.header_text);
            textView.setText(Character.toString((char) getItem(position).alphabetIndex));
            return convertView;
        }

        @Override
        public long getHeaderId(int position) {
            return getItem(position).alphabetIndex;
        }

        @Override
        public int getCount() {
            return countries.size();
        }

        @Override
        public Country getItem(int position) {
            return countries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.country_item, parent, false);
            }
            TextView textView = convertView.findViewById(R.id.settings_row_text);
            TextView detailTextView = convertView.findViewById(R.id.settings_row_text_detail);

            Country c = getItem(position);
            textView.setText(c.name);
            detailTextView.setText("+" + c.code);

            return convertView;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    List<Country> filteredResults = getFilteredResults(constraint);
                    FilterResults results = new FilterResults();
                    results.values = filteredResults;
                    results.count = filteredResults.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    countries = (List<Country>) results.values;
                    notifyDataSetChanged();
                }
            };
        }

        private List<Country> getFilteredResults(CharSequence constraint) {
            List<Country> filtered = new ArrayList<>();
            for (Country country : originalCountries) {
                if (country.contains(constraint)) {
                    filtered.add(country);
                }
            }
            return filtered;
        }
    }
}
