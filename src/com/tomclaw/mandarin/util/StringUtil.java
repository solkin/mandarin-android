package com.tomclaw.mandarin.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 13.10.13
 * Time: 20:09
 */
public class StringUtil {

    public static final int DEFAULT_ALPHABET_INDEX = '?';

    public static final String UTF8_ENCODING = "UTF-8";

    private static final String MAPPING_ORIGIN = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
    private static final String MAPPING_CP1250 = "ŔÁÂĂÄĹ¨ĆÇČÉĘËĚÍÎĎĐŃŇÓÔŐÖ×ŘŮÚŰÜÝŢßŕáâăäĺ¸ćçčéęëěíîďđńňóôőö÷řůúűüýţ˙";
    private static final String MAPPING_CP1252 = "ÀÁÂÃÄÅ¨ÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäå¸æçèéêëìíîïðñòóôõö÷øùúûüýþÿ";

    public static int getAlphabetIndex(String nickName) {
        for (int c = 0; c < nickName.length(); c++) {
            char character = nickName.charAt(c);
            if (Character.isLetter(character)) {
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


    public static void copyStringToClipboard(Context context, String string, int toastText) {
        ClipboardManager clipboardManager = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", string));
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
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
        if (!StringUtil.isEmptyOrWhitespace(what)) {
            if (!StringUtil.isEmptyOrWhitespace(where)) {
                where += divider;
            }
            where += what;
        }
        return where;
    }

    public static boolean isEmptyOrWhitespace(String string) {
        return TextUtils.isEmpty(string) || TextUtils.isEmpty(string.trim());
    }
}
