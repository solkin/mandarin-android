package com.tomclaw.mandarin.im;

import com.tomclaw.mandarin.util.Unobfuscatable;

import java.io.Serializable;

/**
 * Created by Solkin on 06.07.2014.
 */
public class ShortBuddyInfo implements Serializable, Unobfuscatable {

    private String buddyId;
    private String buddyNick;
    private String firstName;
    private String lastName;
    private Gender gender = Gender.Any;
    private String homeAddress;
    private long birthDate;
    private boolean isOnline = false;
    private String avatarHash;

    public ShortBuddyInfo() {
    }

    public String getBuddyId() {
        return buddyId;
    }

    public void setBuddyId(String buddyId) {
        this.buddyId = buddyId;
    }

    public String getBuddyNick() {
        return buddyNick;
    }

    public void setBuddyNick(String buddyNick) {
        this.buddyNick = buddyNick;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public long getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(long birthDate) {
        this.birthDate = birthDate;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public String getAvatarHash() {
        return avatarHash;
    }

    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }
}
