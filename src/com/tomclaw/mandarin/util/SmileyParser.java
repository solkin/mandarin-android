package com.tomclaw.mandarin.util;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import com.tomclaw.mandarin.R;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 02.10.13
 * Time: 23:02
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public class SmileyParser {
    // Singleton stuff
    private static SmileyParser sInstance = null;
    public static SmileyParser getInstance() { return sInstance; }
    public static void init(Context context) {
        //GH - added a null check so instances will get reused
        if (sInstance == null)
            sInstance = new SmileyParser(context);
    }
    public static void destroyInstance() {
        if (sInstance != null) {
            sInstance = null;
        }
    }

    private final Context mContext;
    private final String[] mSmileyTexts;
    private final TypedArray mSmileyDrawables;
    private final Pattern mPattern;
    private final HashMap<String, Integer> mSmileyToRes;

    private SmileyParser(Context context) {
        mContext = context;
        mSmileyTexts = mContext.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
        mSmileyDrawables = mContext.getResources().obtainTypedArray(DEFAULT_SMILEY_IMAGES);
        mSmileyToRes = buildSmileyToRes();
        mPattern = buildPattern();
    }

    public static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;
    public static final int DEFAULT_SMILEY_IMAGES = R.array.default_smileys_images;

    /**
     * Builds the hashtable we use for mapping the string version
     * of a smiley (e.g. ":-)") to a resource ID for the icon version.
     */
    private HashMap<String, Integer> buildSmileyToRes() {
        if (mSmileyDrawables.length() != mSmileyTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        HashMap<String, Integer> smileyToRes = new HashMap<String, Integer>(mSmileyTexts.length);
        for (int i = 0; i < mSmileyTexts.length; i++) {
            smileyToRes.put(mSmileyTexts[i], mSmileyDrawables.getResourceId(i, 0));
        }

        return smileyToRes;
    }

    /**
     * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder(mSmileyTexts.length * 3);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smileys
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : mSmileyTexts) {
            patternString.append(Pattern.quote(s));
            patternString.append('|');
        }
        // Replace the extra '|' with a ')'
        patternString.replace(patternString.length() - 1, patternString.length(), ")");

        return Pattern.compile(patternString.toString());
    }


    /**
     * Adds ImageSpans to a CharSequence that replace textual emoticons such
     * as :-) with a graphical version.
     *
     * @param text A CharSequence possibly containing emoticons
     * @return A CharSequence annotated with ImageSpans covering any
     *         recognized emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        Matcher matcher = mPattern.matcher(text);
        while (matcher.find()) {
            int resId = mSmileyToRes.get(matcher.group());
            builder.setSpan(new ImageSpan(mContext, resId),
                    matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }
}