package com.tomclaw.mandarin.im.icq;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.im.icq.dto.HistoryMessages;
import com.tomclaw.mandarin.im.icq.dto.Message;
import com.tomclaw.mandarin.im.tasks.HistoryMergeTask;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by ivsolkin on 26.11.16.
 */
public class HistoryBlockRequest extends CabbageTrueRequest {

    private final String buddyId;
    private final long fromMessageId;
    private final long tillMessageId;
    private final String patchVersion;
    private final int count;

    public HistoryBlockRequest(String buddyId, long fromMessageId, long tillMessageId,
                               String patchVersion, int count) {
        super();
        this.buddyId = buddyId;
        this.fromMessageId = fromMessageId;
        this.tillMessageId = tillMessageId;
        this.patchVersion = patchVersion;
        this.count = count;
    }

    @Override
    protected int parseResults(JSONObject results) throws Throwable {
        GsonSingleton gson = GsonSingleton.getInstance();
        HistoryMessages historyMessages = gson.fromJson(results.toString(), HistoryMessages.class);
        Logger.log("messages received: " + historyMessages.getMessages().size());
        if (!historyMessages.getMessages().isEmpty()) {
            int accountDbId = getAccountRoot().getAccountDbId();
            long prevMsgId = -1;
            ArrayList<MessageData> messages = new ArrayList<>();
            for (Message message : historyMessages.getMessages()) {
                int messageType = message.isOutgoing() ?
                        GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING :
                        GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING;
                long messageTime = TimeUnit.SECONDS.toMillis(message.getTime());
                MessageData messageData = new MessageData(accountDbId, buddyId, prevMsgId,
                        message.getMsgId(), message.getReqId(), messageType, messageTime, message.getText());
                messages.add(messageData);
                prevMsgId = message.getMsgId();
            }
            Bundle bundle = new Bundle();
            bundle.putSerializable(HistoryMergeTask.KEY_MESSAGES, messages);
            getAccountRoot().getContentResolver().call(Settings.HISTORY_RESOLVER_URI,
                    HistoryMergeTask.class.getName(), null, bundle);
        }
        return REQUEST_DELETE;
    }

    @Override
    protected String getMethodName() {
        return "getHistory";
    }

    @Override
    protected void appendParams(JsonObject params) {
        String aimSid = getAccountRoot().getAimSid();
        String localeId = getAccountRoot().getLocaleId();
        String patch = TextUtils.isEmpty(patchVersion) ? "init" : patchVersion;
        params.addProperty("sn", buddyId);
        params.addProperty("fromMsgId", fromMessageId);
        params.addProperty("count", count);
        params.addProperty("patchVersion", patch);
        params.addProperty("aimSid", aimSid);
        params.addProperty("lang", localeId);
        if (tillMessageId != 0) {
            params.addProperty("tillMsgId", tillMessageId);
        }
    }
}
