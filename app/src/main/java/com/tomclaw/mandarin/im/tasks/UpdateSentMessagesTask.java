package com.tomclaw.mandarin.im.tasks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.DatabaseTask;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.SentMessageData;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.helpers.Timer;

import java.util.Collections;
import java.util.List;

import static com.tomclaw.mandarin.core.QueryHelper.updateSentMessage;

/**
 * Created by solkin on 05.07.17.
 */
public class UpdateSentMessagesTask extends DatabaseTask {

    public static String KEY_MESSAGES_DATA = "key_messages_data";

    public UpdateSentMessagesTask(Context object, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(object, sqLiteDatabase, bundle);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void runInTransaction(Context context, DatabaseLayer databaseLayer, Bundle bundle) throws Throwable {
        Timer timer = new Timer().start();
        List<SentMessageData> datas = bundle.getParcelableArrayList(KEY_MESSAGES_DATA);
        if (datas == null) {
            return;
        }
        for (SentMessageData data : datas) {
            updateSentMessage(databaseLayer, data);
        }

        Logger.log("messages processing time: " + timer.stop()
                + " (" + datas.size() + " messages)");
    }

    @Override
    protected List<Uri> getModifiedUris() {
        return Collections.singletonList(Settings.HISTORY_RESOLVER_URI);
    }

    @Override
    protected String getOperationDescription() {
        return "update sent";
    }
}
