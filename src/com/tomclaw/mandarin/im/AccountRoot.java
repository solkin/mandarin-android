package com.tomclaw.mandarin.im;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 3/28/13
 * Time: 1:54 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AccountRoot implements Parcelable {

    /**
     * Creator object that the AIDL generated service class
     * will be looking for when it's time to recreate this
     * AIDLObject on the other side.
     */
    public static final Creator<AccountRoot> CREATOR
            = new Parcelable.Creator<AccountRoot>() {

        /**
         * Instantiate the desired AIDLObject subclass by name and provide
         * it with its data bundle.
         * @param in The AIDLObject's data.
         * @return An AIDLObject, or null if error.
         */
        public AccountRoot createFromParcel(Parcel in) {
            String className = in.readString();
            Bundle instanceData = in.readBundle();

            try {
                Constructor<?> implementerConstructor = AndroidMagicConstructorMaker.make(Class.forName(className));
                implementerConstructor.setAccessible(true);
                AccountRoot implementer = (AccountRoot) implementerConstructor.newInstance();

                return implementer;

            } catch (Exception e) {
                Log.e("AIDLObject.CREATOR.createFromParcel", e.getCause().getMessage());
            }

            return null;
        }

        /**
         * Required by Parcelable
         */
        public AccountRoot[] newArray(int size) {
            return new AccountRoot[size];
        }
    };
    /**
     * User info
     */
    protected String userId;
    protected String userNick;
    protected String userPassword;
    protected int statusIndex;
    protected String statusText;
    /**
     * Service info
     */
    protected int serviceId;
    protected String serviceHost;
    protected int servicePort;
    /**
     * User data
     */
    protected List<GroupItem> groupItems = new ArrayList<GroupItem>();

    public List<GroupItem> getGroupItems() {
        return groupItems;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserNick() {
        return userNick;
    }

    public void setUserNick(String userNick) {
        this.userNick = userNick;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public abstract int getServiceIcon();

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(userNick);
        dest.writeString(userPassword);
        dest.writeInt(statusIndex);
        dest.writeString(statusText);
        dest.writeInt(serviceId);
        dest.writeString(serviceHost);
        dest.writeInt(servicePort);
        dest.writeTypedList(groupItems);
    }

    public void readFromParcel(Parcel in) {
        userId = in.readString();
        userNick = in.readString();
        userPassword = in.readString();
        statusIndex = in.readInt();
        statusText = in.readString();
        serviceId = in.readInt();
        serviceHost = in.readString();
        servicePort = in.readInt();
        groupItems = in.createTypedArrayList(GroupItem.CREATOR);
    }

    /**
     * Create a new no-args constructor for any class by its name.
     *
     * @version 1.0
     */
    private static class AndroidMagicConstructorMaker {

        @SuppressWarnings("unchecked")
        public static <T> Constructor<T> make(Class<T> clazz) throws Exception {
            Constructor<?> constr = Constructor.class.getDeclaredConstructor(
                    Class.class, // Class<T> declaringClass
                    Class[].class, // Class<?>[] parameterTypes
                    Class[].class, // Class<?>[] checkedExceptions
                    int.class); // int slot
            constr.setAccessible(true);

            return (Constructor<T>) constr.newInstance(clazz, new Class[0],
                    new Class[0], 1);
        }
    }
}
