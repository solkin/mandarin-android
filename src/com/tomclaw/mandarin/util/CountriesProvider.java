package com.tomclaw.mandarin.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.tomclaw.mandarin.core.Settings;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by Solkin on 22.10.2014.
 */
public class CountriesProvider {

    private static class Holder {

        static CountriesProvider instance = new CountriesProvider();
    }

    public static CountriesProvider getInstance() {
        return Holder.instance;
    }

    private List<Country> countries = new ArrayList<Country>();

    public List<Country> getCountries(Context context) {
        if (countries.isEmpty()) {
            try {
                InputStream stream = context.getApplicationContext().getResources().getAssets()
                        .open("countries.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] args = line.split(";");
                    Country c = new Country(args[2], Integer.parseInt(args[0]), args[1]);
                    countries.add(c);
                }
                reader.close();
                stream.close();
            } catch (Exception ex) {
                Log.d(Settings.LOG_TAG, ex.getMessage());
            }
            Collections.sort(countries);
        }
        return countries;
    }

    public Country getCountryByCurrentLocale(Context context, String defaultLocale) throws CountryNotFoundException {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String simCountryIso = manager.getSimCountryIso().toUpperCase();
        String networkCountryIso = manager.getNetworkCountryIso().toUpperCase();
        String localeCountryIso = Locale.getDefault().getCountry().toUpperCase();
        String countryIso;
        if (!TextUtils.isEmpty(simCountryIso)) {
            countryIso = simCountryIso;
        } else if (!TextUtils.isEmpty(simCountryIso)) {
            countryIso = networkCountryIso;
        } else {
            countryIso = localeCountryIso;
        }
        return getCountryByLocale(context, countryIso, defaultLocale);
    }

    public Country getCountryByLocale(Context context, String locale, String defaultLocale) throws CountryNotFoundException {
        List<Country> countries = getCountries(context);
        Country defaultCountry = null;
        for (Country country : countries) {
            if (TextUtils.equals(country.getShortName(), defaultLocale)) {
                defaultCountry = country;
            }
            if (TextUtils.equals(country.getShortName(), locale)) {
                return country;
            }
        }
        if (defaultCountry != null) {
            return defaultCountry;
        }
        throw new CountryNotFoundException();
    }

    public Country getCountryByCode(Context context, int code) throws CountryNotFoundException {
        List<Country> countries = getCountries(context);
        for (Country country : countries) {
            if (country.getCode() == code) {
                return country;
            }
        }
        throw new CountryNotFoundException();
    }

    public class CountryNotFoundException extends Throwable {
    }
}
