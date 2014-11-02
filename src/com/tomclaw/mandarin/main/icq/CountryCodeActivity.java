package com.tomclaw.mandarin.main.icq;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.util.CountriesProvider;
import com.tomclaw.mandarin.util.Country;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Solkin on 02.10.2014.
 */
public class CountryCodeActivity extends Activity {

    public static String EXTRA_COUNTRY_SHORT_NAME = "country_short_name";

    private SearchView.OnQueryTextListener onQueryTextListener;
    private CountryCodeAdapter countryCodeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        bar.setIcon(R.drawable.ic_ab_logo);

        setContentView(R.layout.country_code_activity);

        countryCodeAdapter = new CountryCodeAdapter(this, CountriesProvider.getInstance().getCountries(this));
        StickyListHeadersListView listView = (StickyListHeadersListView) findViewById(R.id.countries_list_view);
        listView.setAdapter(countryCodeAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Country country = countryCodeAdapter.getItem(position);
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_COUNTRY_SHORT_NAME, country.getShortName()));
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
            TextView textView = (TextView) convertView.findViewById(R.id.header_text);
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
            TextView textView = (TextView) convertView.findViewById(R.id.settings_row_text);
            TextView detailTextView = (TextView) convertView.findViewById(R.id.settings_row_text_detail);

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
            List<Country> filtered = new ArrayList<Country>();
            for (Country country : originalCountries) {
                if (country.contains(constraint)) {
                    filtered.add(country);
                }
            }
            return filtered;
        }
    }
}
