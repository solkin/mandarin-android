package com.tomclaw.mandarin.im;

import android.os.Parcel;
import com.tomclaw.mandarin.core.CoreObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 4/21/13
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class Roster extends CoreObject {

    private List<GroupItem> groupItems = new ArrayList<GroupItem>();

    public List<GroupItem> getGroupItems() {
        return groupItems;
    }

    @Override
    protected void readInstanceData(Parcel parcel) {
        groupItems = parcel.createTypedArrayList(GroupItem.CREATOR);
    }

    @Override
    protected void writeInstanceData(Parcel parcel) {
        parcel.writeTypedList(groupItems);
    }
}
