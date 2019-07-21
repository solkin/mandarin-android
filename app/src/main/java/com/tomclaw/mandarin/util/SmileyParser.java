package com.tomclaw.mandarin.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import com.tomclaw.mandarin.R;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 02.10.13
 * Time: 23:02
 * A class for annotating a CharSequence with spans to convert textual emoticons
 * to graphical ones.
 */
public class SmileyParser {

    @SuppressLint("StaticFieldLeak")
    private static SmileyParser instance = null;

    public static SmileyParser getInstance() {
        return instance;
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new SmileyParser(context);
        }
    }

    private static final int DEFAULT_SMILEY_TEXTS = R.array.default_smiley_texts;
    private static final int DEFAULT_SMILEY_IMAGES = R.array.default_smileys_images;

    private final Context context;
    private final String[] smileyTexts;
    private final TypedArray smileyDrawables;
    private final Pattern pattern;
    private final HashMap<String, Integer> smileyToRes;
    private final SparseArray<String> resToSmileys;

    private SmileyParser(Context context) {
        this.context = context;
        smileyTexts = this.context.getResources().getStringArray(DEFAULT_SMILEY_TEXTS);
        smileyDrawables = this.context.getResources().obtainTypedArray(DEFAULT_SMILEY_IMAGES);
        smileyToRes = new HashMap<>();
        resToSmileys = new SparseArray<>();
        buildSmileys();
        pattern = buildPattern();
    }

    /**
     * Builds the hashtable we use for mapping the string version
     * of a smiley (e.g. ":-)") to a resource ID for the icon version.
     */
    private void buildSmileys() {
        if (smileyDrawables.length() != smileyTexts.length) {
            // Throw an exception if someone updated DEFAULT_SMILEY_RES_IDS
            // and failed to update arrays.xml
            throw new IllegalStateException("Smiley resource ID/text mismatch");
        }

        for (int i = 0; i < smileyTexts.length; i++) {
            int resourceId = smileyDrawables.getResourceId(i, 0);
            smileyToRes.put(smileyTexts[i], resourceId);
            if (TextUtils.isEmpty(resToSmileys.get(resourceId))) {
                resToSmileys.put(resourceId, smileyTexts[i]);
            }
        }
    }

    /**
     * Builds the regular expression we use to find smileys in {@link #addSmileySpans}.
     */
    private Pattern buildPattern() {
        // Set the StringBuilder capacity with the assumption that the average
        // smiley is 3 characters long.
        StringBuilder patternString = new StringBuilder(smileyTexts.length * 3);

        // Build a regex that looks like (:-)|:-(|...), but escaping the smileys
        // properly so they will be interpreted literally by the regex matcher.
        patternString.append('(');
        for (String s : smileyTexts) {
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
     * recognized emoticons.
     */
    public CharSequence addSmileySpans(CharSequence text) {
        if (text == null) {
            return null;
        }
        return addSmileySpans(new SpannableStringBuilder(text));
    }

    /**
     * Adds ImageSpans to a Spannable that replace textual emoticons such
     * as :-) with a graphical version.
     *
     * @param text A Spannable possibly containing emoticons
     * @return A Spannable annotated with ImageSpans covering any
     * recognized emoticons.
     */
    public Spannable addSmileySpans(Spannable text) {
        ImageSpan[] spans = text.getSpans(0, text.length(), ImageSpan.class);
        for (ImageSpan span : spans) {
            text.removeSpan(span);
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int resId = smileyToRes.get(matcher.group());
            text.setSpan(new ImageSpan(context, resId),
                    matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return text;
    }

    public int getSmileysCount() {
        return resToSmileys.size();
    }

    public int getSmiley(int index) {
        return resToSmileys.keyAt(index);
    }

    public String getSmileyText(int index) {
        return resToSmileys.valueAt(index);
    }
}