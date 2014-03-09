package com.tomclaw.mandarin.im.icq;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.Log;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Status;
import com.tomclaw.mandarin.im.StatusCatalogue;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 11/21/13
 * Time: 12:30 AM
 */
public class IcqStatusCatalogue extends StatusCatalogue {

    public IcqStatusCatalogue(Context context) {
        super();

        String[] statusTitles = context.getResources().getStringArray(IcqAccountRoot.getStatusNamesResource());
        String[] statusValues = context.getResources().getStringArray(IcqAccountRoot.getStatusValuesResource());
        TypedArray statusDrawables = context.getResources().obtainTypedArray(IcqAccountRoot.getStatusDrawablesResource());
        int[] statusConnect = context.getResources().getIntArray(IcqAccountRoot.getStatusConnectResource());
        int[] statusSetup = context.getResources().getIntArray(IcqAccountRoot.getStatusSetupResource());
        musicStatus = context.getResources().getInteger(IcqAccountRoot.getStatusMusicResource());

        for (int index = 0; index < statusValues.length; index++) {
            Status status = new Status(statusDrawables.getResourceId(index, R.drawable.status_icq_offline),
                    statusTitles[index], statusValues[index]);
            Log.d(Settings.LOG_TAG, "status value: " + status.getValue());
            indexMap.put(statusValues[index], index);
            statusList.add(index, status);
            if (index < statusConnect.length) {
                connectStatuses.add(statusConnect[index]);
            }
            if (index < statusSetup.length) {
                setupStatuses.add(statusSetup[index]);
            }
        }
    }
}
