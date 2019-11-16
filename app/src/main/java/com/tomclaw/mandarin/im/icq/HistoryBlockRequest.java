package com.tomclaw.mandarin.im.icq;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.gson.JsonObject;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.im.icq.dto.HistoryMessages;
import com.tomclaw.mandarin.im.icq.dto.Message;
import com.tomclaw.mandarin.im.tasks.HistoryMergeTask;
import com.tomclaw.mandarin.util.GsonSingleton;
import com.tomclaw.mandarin.util.Logger;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ivsolkin on 26.11.16.
 */
public class HistoryBlockRequest extends CabbageRequest {

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
    protected int parseResults(JSONObject results) {
        GsonSingleton gson = GsonSingleton.getInstance();
        HistoryMessages historyMessages = gson.fromJson(results.toString(), HistoryMessages.class);
        Logger.log("messages received: " + historyMessages.getMessages().size());
        ArrayList<MessageData> messages = new ArrayList<>();
        int accountDbId = getAccountRoot().getAccountDbId();
        long prevMsgId = GlobalProvider.HISTORY_MESSAGE_ID_INVALID;
        if (!historyMessages.getMessages().isEmpty()) {
            List<Message> sortedMessages = historyMessages.getMessages();
            Collections.sort(sortedMessages, new MessagesComparator());
            for (Message message : sortedMessages) {
                int messageType = message.isOutgoing() ?
                        GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING :
                        GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING;
                long messageTime = TimeUnit.SECONDS.toMillis(message.getTime());
                MessageData messageData = new MessageData(accountDbId, buddyId, prevMsgId,
                        message.getMsgId(), message.getReqId(), messageType, messageTime, message.getText());
                messages.add(messageData);
                prevMsgId = message.getMsgId();
            }
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(HistoryMergeTask.KEY_MESSAGES, messages);
        if (count < 0 || messages.isEmpty()) {
            // This is all messages between fromMessageId and tillMessageId, so we can mark
            // tillMessageId, that its previous message is the last message from received
            // messages or fromMessageId, if no messages received.
            Buddy buddy = new Buddy(accountDbId, buddyId);
            long lastMsgPrevId = (prevMsgId == GlobalProvider.HISTORY_MESSAGE_ID_INVALID) ?
                    fromMessageId : prevMsgId;
            bundle.putParcelable(HistoryMergeTask.KEY_LAST_MESSAGE_BUDDY, buddy);
            bundle.putLong(HistoryMergeTask.KEY_LAST_MESSAGE_PREV_ID, lastMsgPrevId);
            bundle.putLong(HistoryMergeTask.KEY_LAST_MESSAGE_ID, tillMessageId);
        }
        getAccountRoot().getContentResolver().call(Settings.HISTORY_RESOLVER_URI,
                HistoryMergeTask.class.getName(), null, bundle);
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
        // Cabbage protocol has upside-down from-till index ¯\_(ツ)_/¯
        long from = fromMessageId;
        long till = tillMessageId;
        if (from != -1 && from < till) {
            long tmp = from;
            from = till;
            till = tmp;
        }
        params.addProperty("sn", buddyId);
        params.addProperty("fromMsgId", from);
        params.addProperty("count", count);
        params.addProperty("patchVersion", patch);
        params.addProperty("aimSid", aimSid);
        params.addProperty("lang", localeId);
        if (till != 0) {
            params.addProperty("tillMsgId", till);
        }
    }
}
