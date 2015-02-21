package com.tomclaw.mandarin.core;

import android.os.Parcel;
import android.os.Parcelable;
import com.tomclaw.mandarin.util.Logger;

/**
 * Packs itself prior to its shuttling across processes,
 * and is responsible for orchestrating its own reconstruction
 * on the other side.
 */
public abstract class CoreObject implements Parcelable {

    public CoreObject() {
    }

    /**
     * Specify special flags for marshaling process
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Write this object's data and save
     * that, and its name, in the parcel
     * that will be used in reconstruction.
     *
     * @param out The parcel in which to save its data.
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(getClass().getName());
        writeInstanceData(out);
    }

    /**
     * Creator object that the AIDL generated service class
     * will be looking for when it's time to recreate this
     * CoreObject on the other side.
     */
    public static final Creator<CoreObject> CREATOR
            = new Parcelable.Creator<CoreObject>() {

        /**
         * Instantiate the desired CoreObject subclass by name and provide
         * it with its data bundle.
         * @param in The CoreObject's data.
         * @return An CoreObject, or null if error.
         */
        @Override
        public CoreObject createFromParcel(Parcel in) {
            String className = in.readString();
            try {
                CoreObject implementer = (CoreObject) Class.forName(className).newInstance();
                implementer.readInstanceData(in);
                return implementer;
            } catch (Throwable e) {
                e.printStackTrace();
                Logger.log("CoreObject Creator error: " + e.getMessage());
            }
            return null;
        }

        /**
         * Required by Parcelable
         */
        @Override
        public CoreObject[] newArray(int size) {
            return new CoreObject[size];
        }
    };

    /**
     * Set up instance using provided data.
     *
     * @param parcel Parcel created by this object before IPC.
     */
    protected abstract void readInstanceData(Parcel parcel);

    /**
     * Write instance data prior to shuttling.
     *
     * @param parcel Parcel in which to write instance data.
     */
    protected abstract void writeInstanceData(Parcel parcel);
}
