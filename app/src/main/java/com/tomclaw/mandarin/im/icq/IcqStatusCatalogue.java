package com.tomclaw.mandarin.im.icq;

import android.content.Context;
import android.content.res.TypedArray;

import com.tomclaw.mandarin.R;
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
        String[] statusValuesEn = context.getResources().getStringArray(IcqAccountRoot.getStatusValuesResourceEn());
        String[] statusValuesRu = context.getResources().getStringArray(IcqAccountRoot.getStatusValuesResourceRu());
        TypedArray statusDrawables = context.getResources().obtainTypedArray(IcqAccountRoot.getStatusDrawablesResource());
        int[] statusConnect = context.getResources().getIntArray(IcqAccountRoot.getStatusConnectResource());
        int[] statusSetup = context.getResources().getIntArray(IcqAccountRoot.getStatusSetupResource());

        initStatuses(statusValuesEn, statusConnect, statusSetup, statusTitles, statusDrawables, false);
        initStatuses(statusValuesRu, statusConnect, statusSetup, statusTitles, statusDrawables, true);
    }

    private void initStatuses(String[] statusValues, int[] statusConnect, int[] statusSetup, String[] statusTitles, TypedArray statusDrawables, boolean isIndexOnly) {
        for (int index = 0; index < statusValues.length; index++) {
            Status status = new Status(statusDrawables.getResourceId(index, R.drawable.status_icq_offline),
                    statusTitles[index], statusValues[index]);
            indexMap.put(statusValues[index].toLowerCase(), index);
            if (isIndexOnly) {
                continue;
            }
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
