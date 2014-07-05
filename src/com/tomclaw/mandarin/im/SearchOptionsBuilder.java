package com.tomclaw.mandarin.im;

/**
 * Created by Igor on 26.06.2014.
 */
public interface SearchOptionsBuilder {

    public enum Gender {
        Female,
        Male,
        Any
    }

    public void keyword(String option);
    public void firstName(String option);
    public void lastName(String option);
    public void online(boolean option);
    public void age(int from, int to);
    public void gender(Gender gender);
}
