package com.tomclaw.helpers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.database.DatabaseUtils;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 13.10.13
 * Time: 20:09
 */
public class Strings {

    public static final int DEFAULT_ALPHABET_INDEX = '?';

    public static final String UTF8_ENCODING = "UTF-8";

    private static final String MAPPING_ORIGIN = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String MAPPING_CP1250 = "ŔÁÂĂÄĹ¨ĆÇČÉĘËĚÍÎĎĐŃŇÓÔŐÖ×ŘŮÚŰÜÝŢßŕáâăäĺ¸ćçčéęëěíîďđńňóôőö÷řůúűüýţ˙";
    private static final String MAPPING_CP1252 = "ÀÁÂÃÄÅ¨ÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäå¸æçèéêëìíîïðñòóôõö÷øùúûüýþÿ";

    private static Random random;

    static {
        random = new Random(System.currentTimeMillis());
    }

    public static int getAlphabetIndex(String name) {
        for (int c = 0; c < name.length(); c++) {
            char character = name.charAt(c);
            if (Character.isLetterOrDigit(character)) {
                return Character.toUpperCase(character);
            }
        }
        return DEFAULT_ALPHABET_INDEX;
    }

    public static boolean isNumeric(String value) {
        return !TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value);
    }

    public static String getHmacSha256Base64(String key, String data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        final String encryptionAlgorithm = "HmacSHA256";
        SecretKey secretKey = new SecretKeySpec(data.getBytes(), encryptionAlgorithm);
        Mac messageAuthenticationCode = Mac.getInstance(encryptionAlgorithm);
        messageAuthenticationCode.init(secretKey);
        messageAuthenticationCode.update(key.getBytes());
        byte[] digest = messageAuthenticationCode.doFinal();
        return Base64.encodeToString(digest, Base64.NO_WRAP);
    }

    public static String unescapeXml(String string) {
        return Entities.XML.unescape(string);
    }

    public static void copyStringToClipboard(Context context, String string) {
        copyStringToClipboard(context, string, 0);
    }

    public static void copyStringToClipboard(Context context, String string, int toastText) {
        ClipboardManager clipboardManager = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", string));
        if (toastText > 0) {
            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
        }
    }

    public static String fixCyrillicSymbols(String string) {
        String fixed = string;
        if (!TextUtils.isEmpty(string)) {
            fixed = replaceMappedSymbols(fixed, MAPPING_CP1250, MAPPING_ORIGIN);
            fixed = replaceMappedSymbols(fixed, MAPPING_CP1252, MAPPING_ORIGIN);
        }
        return fixed;
    }

    private static String replaceMappedSymbols(String string, String mapping, String origin) {
        StringBuilder builder = new StringBuilder();
        for (int c = 0; c < string.length(); c++) {
            char stringChar = string.charAt(c);
            for (int i = 0; i < mapping.length(); i++) {
                char mappingChar = mapping.charAt(i);
                if (stringChar == mappingChar) {
                    stringChar = origin.charAt(i);
                }
            }
            builder.append(stringChar);
        }
        return builder.toString();
    }

    public static String urlEncode(String string) throws UnsupportedEncodingException {
        return URLEncoder.encode(string, UTF8_ENCODING).replace("+", "%20");
    }

    public static String appendIfNotEmpty(String where, String what, String divider) {
        if (!Strings.isEmptyOrWhitespace(what)) {
            if (!Strings.isEmptyOrWhitespace(where)) {
                where += divider;
            }
            where += what;
        }
        return where;
    }

    public static boolean isEmptyOrWhitespace(String string) {
        return TextUtils.isEmpty(string) || TextUtils.isEmpty(string.trim());
    }

    public static String formatBytes(Resources resources, long bytes) {
        if (bytes < 1024) {
            return resources.getString(R.string.bytes, bytes);
        } else if (bytes < 1024 * 1024) {
            return resources.getString(R.string.kibibytes, bytes / 1024.0f);
        } else if (bytes < 1024 * 1024 * 1024) {
            return resources.getString(R.string.mibibytes, bytes / 1024.0f / 1024.0f);
        } else {
            return resources.getString(R.string.gigibytes, bytes / 1024.0f / 1024.0f / 1024.0f);
        }
    }

    public static String formatSpeed(float bytesPerSecond) {
        float bitsPerSecond = bytesPerSecond * 8;
        int unit = 1000;
        if (bitsPerSecond < unit) return bitsPerSecond + " bits/sec";
        int exp = (int) (Math.log(bitsPerSecond) / Math.log(unit));
        String pre = String.valueOf("kmgtpe".charAt(exp - 1));
        return String.format("%.1f %sB/sec", bitsPerSecond / Math.pow(unit, exp), pre);
    }

    public static String escapeSqlWithQuotes(String value) {
        return DatabaseUtils.sqlEscapeString(value);
    }

    public static String escapeSql(String value) {
        String escaped = escapeSqlWithQuotes(value);
        escaped = escaped.substring(1, escaped.length() - 1);
        return escaped;
    }

    public static String generateRandomText() {
        int wordCount = 10 + random.nextInt(13);
        return generateRandomText(wordCount);
    }

    public static String generateRandomText(int wordCount) {
        StringBuilder sb = new StringBuilder(wordCount);
        for (int i = 0; i < wordCount; i++) { // For each letter in the word
            sb.append(generateRandomWord(i == 0)).append((i < (wordCount - 1)) ? " " : "."); // Add it to the String
        }
        return sb.toString();
    }

    public static String generateRandomWord() {
        return generateRandomWord(true);
    }

    public static String generateRandomWord(boolean capitalize) {
        int wordLength = 4 + random.nextInt(6);
        // Initialize a Random Number Generator with SysTime as the seed
        StringBuilder sb = new StringBuilder(wordLength);
        for (int i = 0; i < wordLength; i++) { // For each letter in the word
            char tmp = (char) ('a' + random.nextInt('z' - 'a')); // Generate a letter between a and z
            sb.append(tmp); // Add it to the String
        }
        String word = sb.toString();
        if (capitalize) {
            return String.valueOf(word.charAt(0)).toUpperCase() + word.substring(1);
        } else {
            return word;
        }
    }

    public static String generateRandomString() {
        return generateRandomString(16);
    }

    public static String generateRandomString(int length) {
        return generateRandomString(random, length, length);
    }

    public static String generateRandomString(Random r, int minChars, int maxChars) {
        int wordLength = minChars;
        int delta = maxChars - minChars;
        if (delta > 0) {
            wordLength += r.nextInt(delta);
        }
        StringBuilder sb = new StringBuilder(wordLength);
        for (int i = 0; i < wordLength; i++) { // For each letter in the word
            char tmp = (char) ('a' + r.nextInt('z' - 'a')); // Generate a letter between a and z
            sb.append(tmp); // Add it to the String
        }
        return sb.toString();
    }

    public static String format(String source, Map<String, String> patterns) {
        String result = source;
        for (String key : patterns.keySet()) {
            String value = patterns.get(key);
            result = result.replace('{' + key + '}', value);
        }
        return result;
    }
}
