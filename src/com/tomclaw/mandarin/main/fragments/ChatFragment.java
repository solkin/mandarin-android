package com.tomclaw.mandarin.main.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.TextView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.main.ChatListView;
import com.tomclaw.mandarin.main.ChiefActivity;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;

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

    private ChiefActivity activity;

    public ChatFragment(ChiefActivity activity, int buddyDbId){
        this.activity = activity;
        chatHistoryAdapter = new ChatHistoryAdapter(activity, activity.getLoaderManager(), buddyDbId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return activity.getLayoutInflater().inflate(R.layout.chat_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        chatList = (ChatListView) getActivity().findViewById(R.id.chat_list);
        chatList.setAdapter(chatHistoryAdapter);
        chatList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        chatList.setOnScrollListener(new ChatScrollListener());
        chatList.setOnDataChangedListener(new ChatListView.DataChangedListener() {
            @Override
            public void onDataChanged() {
                try {
                    readVisibleMessages();
                } catch (RuntimeException exception) {
                    // onDataChanged вызывается раньше onLoadFinished. Поэтому метод пытается работать с уже закрытым курсором.
                    Log.d(Settings.LOG_TAG, "Error while marking messages as read positions. " + exception.getMessage());
                }
            }
        });

        // Send button and message field initialization.
        ImageButton sendButton = (ImageButton) activity.findViewById(R.id.send_button);
        final TextView messageText = (TextView) activity.findViewById(R.id.message_text);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageText.getText().toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    try {
                        int buddyDbId = chatHistoryAdapter.getBuddyDbId();
                        String cookie = String.valueOf(System.currentTimeMillis());
                        String appSession = activity.getServiceInteraction().getAppSession();
                        QueryHelper.insertMessage(activity.getContentResolver(), buddyDbId, 2, // TODO: real message type
                                cookie, message, false);
                        // Sending protocol message request.
                        RequestHelper.requestMessage(activity.getContentResolver(), appSession,
                                buddyDbId, cookie, message);
                        // Clearing text view.
                        messageText.setText("");
                    } catch (Exception e) {
                        e.printStackTrace();
                        // TODO: Couldn't put message into database. This exception must be processed.
                    }
                }
            }
        });
    }

    private boolean readVisibleMessages() {
        int firstVisiblePosition = chatList.getFirstVisiblePosition();
        int lastVisiblePosition = chatList.getLastVisiblePosition();
        Log.d(Settings.LOG_TAG, "Reading visible messages ["
                + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
        // Checking for the list view is ready.
        if(lastVisiblePosition >= firstVisiblePosition) {
            try {
                QueryHelper.readMessages(getActivity().getContentResolver(),
                        chatHistoryAdapter.getBuddyDbId(),
                        chatHistoryAdapter.getMessageDbId(firstVisiblePosition),
                        chatHistoryAdapter.getMessageDbId(lastVisiblePosition));
                return true;
            } catch (MessageNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Error while marking messages as read positions ["
                        + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
            }
        }
        return false;
    }

    public void selectItem(int buddyDbId) {
        chatHistoryAdapter.setBuddyDbId(buddyDbId);
    }

    public int getBuddyDbId() {
        return chatHistoryAdapter.getBuddyDbId();
    }

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper();
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
                    ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
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
                    if(firstVisiblePosition > startFirstVisiblePosition) {
                        // Scroll to bottom.
                        firstPosition = startFirstVisiblePosition;
                        lastPosition = lastVisiblePosition;
                    } else {
                        // Scroll to top.
                        firstPosition = firstVisiblePosition;
                        lastPosition = startLastVisiblePosition;
                    }
                    Log.d(Settings.LOG_TAG, "Scroll: " + firstPosition + " -> " + lastPosition);
                    try {
                        QueryHelper.readMessages(getActivity().getContentResolver(),
                                chatHistoryAdapter.getBuddyDbId(),
                                chatHistoryAdapter.getMessageDbId(firstPosition),
                                chatHistoryAdapter.getMessageDbId(lastPosition));
                    } catch (MessageNotFoundException ignored) {
                        Log.d(Settings.LOG_TAG, "Error while marking messages as read");
                    }
                    break;
                }
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    }
}
