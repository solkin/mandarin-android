package com.tomclaw.mandarin.main.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.PleaseWaitTask;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.Task;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.main.BuddyInfoTask;
import com.tomclaw.mandarin.main.ChatListView;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.lang.ref.WeakReference;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 10/25/13
 * Time: 7:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatFragment extends Fragment {

    private ChatHistoryAdapter chatHistoryAdapter;
    private ChatListView chatList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return getActivity().getLayoutInflater().inflate(R.layout.chat_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int buddyDbId = getInitBuddyDbId();
        Log.d(Settings.LOG_TAG, "Chat buddyDbId = " + buddyDbId);
        if (buddyDbId == -1)
            return;

        chatHistoryAdapter = new ChatHistoryAdapter(getActivity(), getLoaderManager(), buddyDbId);

        chatList = (ChatListView) getActivity().findViewById(R.id.chat_list);
        chatList.setAdapter(chatHistoryAdapter);
        chatList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        chatList.setOnScrollListener(new ChatScrollListener());
        chatList.setOnDataChangedListener(new ChatListView.DataChangedListener() {
            @Override
            public void onDataChanged() {
                readVisibleMessages();
            }
        });

;        // Send button and message field initialization.
        final ImageButton sendButton = (ImageButton) getActivity().findViewById(R.id.send_button);
        final TextView messageText = (TextView) getActivity().findViewById(R.id.message_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = messageText.getText().toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    int buddyDbId = chatHistoryAdapter.getBuddyDbId();
                    MessageCallback callback = new MessageCallback() {

                        @Override
                        public void onSuccess() {
                            messageText.setText("");
                        }

                        @Override
                        public void onFailed() {
                        }
                    };
                    TaskExecutor.getInstance().execute(
                            new SendMessageTask((ChiefActivity) getActivity(), buddyDbId, message, callback));
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.chat_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close_chat_menu: {
                try {
                    QueryHelper.modifyDialog(getActivity().getContentResolver(), chatHistoryAdapter.getBuddyDbId(), false);
                    getActivity().onBackPressed();
                } catch (Exception ignored) {
                    // Nothing to do in this case.
                }
                return true;
            }
            case R.id.buddy_info_menu: {
                BuddyInfoTask buddyInfoTask = new BuddyInfoTask(getActivity(), chatHistoryAdapter.getBuddyDbId());
                TaskExecutor.getInstance().execute(buddyInfoTask);
                return true;
            }
            case R.id.clear_history_menu: {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.clear_history_title);
                builder.setMessage(R.string.clear_history_text);
                builder.setPositiveButton(R.string.yes_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClearHistoryTask clearHistoryTask = new ClearHistoryTask(getActivity(),
                                chatHistoryAdapter.getBuddyDbId());
                        TaskExecutor.getInstance().execute(clearHistoryTask);
                    }
                });
                builder.setNegativeButton(R.string.do_not_clear, null);
                builder.show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static ChatFragment newInstance(int buddyDbId) {
        ChatFragment chatFragment = new ChatFragment();

        Bundle args = new Bundle();
        args.putInt(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        chatFragment.setArguments(args);

        return chatFragment;
    }

    private int getInitBuddyDbId() {
        Bundle bundle = getArguments();
        if (bundle == null)
            return -1;
        return bundle.getInt(GlobalProvider.HISTORY_BUDDY_DB_ID, -1);
    }

    private void readVisibleMessages() {
        final int firstVisiblePosition = chatList.getFirstVisiblePosition();
        final int lastVisiblePosition = chatList.getLastVisiblePosition();
        Log.d(Settings.LOG_TAG, "Reading visible messages ["
                + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
        // Checking for the list view is ready.
        if(lastVisiblePosition >= firstVisiblePosition) {
            final int buddyDbId = chatHistoryAdapter.getBuddyDbId();
            try {
                final long firstMessageDbId = chatHistoryAdapter.getMessageDbId(firstVisiblePosition);
                final long lastMessageDbId = chatHistoryAdapter.getMessageDbId(lastVisiblePosition);
                TaskExecutor.getInstance().execute(new ReadMessagesTask(getActivity().getContentResolver(), buddyDbId,
                        firstMessageDbId, lastMessageDbId));
            } catch (MessageNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Error while marking messages as read positions ["
                        + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
            }
        }
    }

    public int getBuddyDbId() {
        return chatHistoryAdapter.getBuddyDbId();
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer, Long> selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper<Integer, Long>();
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.chat_history_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;  // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.message_copy:
                    ClipboardManager clipboardManager = (ClipboardManager)
                            getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", getSelectedMessages()));
                    break;
                case R.id.message_share:
                    startActivity(createShareIntent());
                    break;
                case R.id.message_create_note:
                    break;
                default:
                    return false;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.clearSelection();
        }

        private String getSelectedMessages() {
            StringBuilder selectionBuilder = new StringBuilder();
            // Obtain selected positions.
            Collection<Integer> selectedPositions = selectionHelper.getSelectedPositions();
            // Iterating for all selected positions.
            for (int position : selectedPositions) {
                try {
                    selectionBuilder.append(chatHistoryAdapter.getMessageText(position)).append('\n').append('\n');
                } catch (MessageNotFoundException ignored) {
                    Log.d(Settings.LOG_TAG, "Error while copying message on position " + position);
                }
            }
            return selectionBuilder.toString().trim();
        }

        private Intent createShareIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getSelectedMessages());
            return Intent.createChooser(shareIntent, getString(R.string.share_messages_via));
        }
    }

    private class ChatScrollListener implements AbsListView.OnScrollListener {

        private int startFirstVisiblePosition, startLastVisiblePosition;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            int firstVisiblePosition = view.getFirstVisiblePosition();
            int lastVisiblePosition = view.getLastVisiblePosition();
            switch (scrollState) {
                case SCROLL_STATE_TOUCH_SCROLL: {
                    // Scroll stared.
                    startFirstVisiblePosition = firstVisiblePosition;
                    startLastVisiblePosition = lastVisiblePosition;
                    break;
                }
                case SCROLL_STATE_IDLE: {
                    // Scroll ended.
                    int firstPosition;
                    int lastPosition;
                    if (firstVisiblePosition > startFirstVisiblePosition) {
                        // Scroll to bottom.
                        firstPosition = startFirstVisiblePosition;
                        lastPosition = lastVisiblePosition;
                    } else {
                        // Scroll to top.
                        firstPosition = firstVisiblePosition;
                        lastPosition = startLastVisiblePosition;
                    }
                    Log.d(Settings.LOG_TAG, "Scroll: " + firstPosition + " -> " + lastPosition);
                    final int buddyDbId = chatHistoryAdapter.getBuddyDbId();
                    try {
                        final long firstMessageDbId = chatHistoryAdapter.getMessageDbId(firstPosition);
                        final long lastMessageDbId = chatHistoryAdapter.getMessageDbId(lastPosition);
                        TaskExecutor.getInstance().execute(new ReadMessagesTask(getActivity().getContentResolver(), buddyDbId,
                                firstMessageDbId, lastMessageDbId));
                    } catch (MessageNotFoundException ignored) {
                    }
                    break;
                }
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    }

    private class ReadMessagesTask extends Task {

        private final WeakReference<ContentResolver> weakContentResolver;
        private final int buddyDbId;
        private final long firstMessageDbId;
        private final long lastMessageDbId;

        public ReadMessagesTask(ContentResolver contentResolver, int buddyDbId,
                                long firstMessageDbId, long lastMessageDbId) {
            this.weakContentResolver = new WeakReference<ContentResolver>(contentResolver);
            this.buddyDbId = buddyDbId;
            this.firstMessageDbId = firstMessageDbId;
            this.lastMessageDbId = lastMessageDbId;
        }

        @Override
        public void executeBackground() throws MessageNotFoundException {
            ContentResolver contentResolver = weakContentResolver.get();
            if (contentResolver != null) {
                QueryHelper.readMessages(contentResolver,
                        buddyDbId, firstMessageDbId, lastMessageDbId);
            }
        }
    }

    private class ClearHistoryTask extends PleaseWaitTask {

        private final int buddyDbId;
        private final WeakReference<ContentResolver> weakContentResolver;

        public ClearHistoryTask(Context context, int buddyDbId) {
            super(context);
            this.weakContentResolver = new WeakReference<ContentResolver>(context.getContentResolver());
            this.buddyDbId = buddyDbId;
        }

        @Override
        public void executeBackground() {
            ContentResolver contentResolver = weakContentResolver.get();
            if (contentResolver != null) {
                QueryHelper.clearHistory(contentResolver, buddyDbId);
            }
        }

        @Override
        public void onFailMain() {
            Context context = getWeakContext().get();
            if (context != null) {
                // Show error.
                Toast.makeText(context, R.string.error_clearing_history, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SendMessageTask extends Task {

        private final int buddyDbId;
        private String message;
        private final MessageCallback callback;
        private final WeakReference<ChiefActivity> weakActivity;

        public SendMessageTask(ChiefActivity activity, int buddyDbId, String message, MessageCallback callback) {
            this.buddyDbId = buddyDbId;
            this.message = message;
            this.weakActivity = new WeakReference<ChiefActivity>(activity);
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
            ChiefActivity activity = weakActivity.get();
            if (activity != null) {
                String appSession = activity.getServiceInteraction().getAppSession();
                ContentResolver contentResolver = activity.getContentResolver();
                String cookie = String.valueOf(System.currentTimeMillis());
                boolean isCollapseMessages = PreferenceHelper.isCollapseMessages(activity);
                QueryHelper.insertMessage(contentResolver, isCollapseMessages, buddyDbId, 2, // TODO: real message type
                        cookie, message, false);
                // Sending protocol message request.
                RequestHelper.requestMessage(contentResolver, appSession,
                        buddyDbId, cookie, message);
            }
        }

        @Override
        public void onSuccessMain() {
            callback.onSuccess();
        }

        @Override
        public void onFailMain() {
            ChiefActivity activity = weakActivity.get();
            if (activity != null) {
                // Show error.
                Toast.makeText(activity, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
            callback.onFailed();
        }
    }

    public abstract class MessageCallback {

        public abstract void onSuccess();

        public abstract void onFailed();
    }
}
