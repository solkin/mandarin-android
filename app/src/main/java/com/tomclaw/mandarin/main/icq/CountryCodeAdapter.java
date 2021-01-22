package com.tomclaw.mandarin.main.icq;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.util.Country;

import org.zakariya.stickyheaders.SectioningAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for sectioned country code selection
 * Created by solkin on 25/03/2018.
 */
public class CountryCodeAdapter extends SectioningAdapter implements Filterable {

    private List<Country> originalCountries;
    private List<Country> countries;
    private List<Section> sections = new ArrayList<>();
    private LayoutInflater inflater;
    private OnClickListener listener;

    CountryCodeAdapter(Context context, List<Country> countries) {
        this.inflater = LayoutInflater.from(context);
        this.countries = countries;
        this.originalCountries = countries;
        recreateSections();
    }

    void setClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    private void recreateSections() {
        sections.clear();

        char alpha = 0;
        Section currentSection = null;
        for (Country country : countries) {
            char alphabetIndex = (char) country.alphabetIndex;
            if (alphabetIndex != alpha) {
                if (currentSection != null) {
                    sections.add(currentSection);
                }

                currentSection = new Section();
                alpha = alphabetIndex;
                currentSection.alphabetIndex = String.valueOf(alpha);
            }

            if (currentSection != null) {
                currentSection.countryList.add(country);
            }
        }

        if (currentSection != null) {
            sections.add(currentSection);
        }
        notifyAllSectionsDataSetChanged();
    }

    @Override
    public int getNumberOfSections() {
        return sections.size();
    }

    @Override
    public int getNumberOfItemsInSection(int sectionIndex) {
        Section section = sections.get(sectionIndex);
        return section.countryList.size();
    }

    @Override
    public boolean doesSectionHaveHeader(int sectionIndex) {
        return true;
    }

    @Override
    public boolean doesSectionHaveFooter(int sectionIndex) {
        return false;
    }

    @Override
    public GhostHeaderViewHolder onCreateGhostHeaderViewHolder(ViewGroup parent) {
        final View ghostView = new View(parent.getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        ghostView.setLayoutParams(params);

        return new GhostHeaderViewHolder(ghostView);
    }

    @Override
    public ItemViewHolder onCreateItemViewHolder(ViewGroup parent, int itemType) {
        View v = inflater.inflate(R.layout.country_item, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public HeaderViewHolder onCreateHeaderViewHolder(ViewGroup parent, int headerType) {
        View v = inflater.inflate(R.layout.roster_sticky_header, parent, false);
        return new HeaderViewHolder(v);
    }

    @Override
    public void onBindItemViewHolder(SectioningAdapter.ItemViewHolder viewHolder,
                                     int sectionIndex, int itemIndex, int itemType) {
        Section section = sections.get(sectionIndex);
        ItemViewHolder holder = (ItemViewHolder) viewHolder;
        Country country = section.countryList.get(itemIndex);
        holder.bind(country, listener);
    }

    @Override
    public void onBindHeaderViewHolder(SectioningAdapter.HeaderViewHolder viewHolder,
                                       int sectionIndex, int headerType) {
        Section section = sections.get(sectionIndex);
        HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
        holder.bind(section.alphabetIndex);
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
                recreateSections();
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


    private class Section {

        private String alphabetIndex;
        private List<Country> countryList = new ArrayList<>();

    }

    class ItemViewHolder extends SectioningAdapter.ItemViewHolder {

        private TextView textView;
        private TextView detailTextView;

        ItemViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.row_text);
            detailTextView = itemView.findViewById(R.id.row_text_detail);
        }

        @SuppressLint("SetTextI18n")
        void bind(final Country country, final OnClickListener listener) {
            textView.setText(country.name);
            detailTextView.setText("+" + country.code);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onItemClicked(country);
                    }
                }
            });
        }
    }

    class HeaderViewHolder extends SectioningAdapter.HeaderViewHolder {

        TextView titleTextView;

        HeaderViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.header_text);
        }

        void bind(String alphabetIndex) {
            titleTextView.setText(alphabetIndex);
        }
    }

    interface OnClickListener {

        void onItemClicked(Country country);

    }

}
