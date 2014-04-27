package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.main.adapters.SmileysPagerAdapter;
import com.tomclaw.mandarin.main.views.CirclePageIndicator;
import com.tomclaw.mandarin.util.SelectionHelper;

import java.lang.ref.WeakReference;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 */
public class ChatActivity extends ChiefActivity {

    private LinearLayout chatRoot;
    private ChatListView chatList;
    private ChatHistoryAdapter chatHistoryAdapter;
    private EditText messageText;

    private View popupView;
    private LinearLayout smileysFooter;
    private PopupWindow popupWindow;
    private int initKeyboardHeight;
    private int minKeyboardHeight;
    private int diffKeyboardHeight;
    private int previousHeightDifference;
    private int keyboardWidth;
    private int keyboardHeight;
    private boolean isKeyboardVisible;
    private SmileysPagerAdapter smileysAdapter;
    private ViewPager smileysPager;
    private OnSmileyClickCallback callback;
    private boolean isConfigurationChanging;
    private boolean isPaused;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_activity);

        // Checking for we must show keyboard automatically.
        if (PreferenceHelper.isShowKeyboard(this)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        int buddyDbId = getIntentBuddyDbId(getIntent());

        setTitleByBuddyDbId(buddyDbId);

        chatHistoryAdapter = new ChatHistoryAdapter(this, getLoaderManager(), buddyDbId);

        chatList = (ChatListView) findViewById(R.id.chat_list);
        chatList.setAdapter(chatHistoryAdapter);
        chatList.setMultiChoiceModeListener(new MultiChoiceModeListener());
        chatList.setOnScrollListener(new ChatScrollListener());
        chatList.setOnDataChangedListener(new ChatListView.DataChangedListener() {
            @Override
            public void onDataChanged() {
                readVisibleMessages();
            }
        });

        int chatBackground = PreferenceHelper.getChatBackground(this);
        chatList.setBackgroundResource(chatBackground);

        // Send button and message field initialization.
        String enteredText;
        try {
            enteredText = QueryHelper.getBuddyDraft(getContentResolver(), buddyDbId);
        } catch (BuddyNotFoundException ignored) {
            enteredText = null;
        }
        final ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        messageText = (EditText) findViewById(R.id.message_text);
        if (!TextUtils.isEmpty(enteredText)) {
            messageText.setText(enteredText);
            messageText.setSelection(enteredText.length());
        }
        messageText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                hidePopup();
            }
        });
        messageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });
        messageText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_ENTER: {
                        if (PreferenceHelper.isSendByEnter(ChatActivity.this)) {
                            sendMessage();
                            return true;
                        }
                    }
                    default: {
                        return false;
                    }
                }
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        chatRoot = (LinearLayout) findViewById(R.id.chat_root);
        popupView = getLayoutInflater().inflate(R.layout.smileys_popup, chatRoot, false);
        smileysFooter = (LinearLayout) findViewById(R.id.smileys_footer);

        // Defining default height of keyboard.
        initKeyboardHeight = (int) getResources().getDimension(R.dimen.init_keyboard_height);
        minKeyboardHeight = (int) getResources().getDimension(R.dimen.min_keyboard_height);
        diffKeyboardHeight = (int) getResources().getDimension(R.dimen.diff_keyboard_height);
        updateKeyboardHeight(initKeyboardHeight);

        callback = new OnSmileyClickCallback() {

            @Override
            public void onSmileyClick(String smileyText) {
                insertSmileyText(smileyText);
            }
        };

        // Showing and Dismissing pop up on clicking smileys button.
        ImageView smileysButton = (ImageView) findViewById(R.id.smileys_button);
        smileysButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss();
                } else {
                    // This must be refactored.
                    smileysAdapter = new SmileysPagerAdapter(ChatActivity.this,
                            keyboardWidth, keyboardHeight, callback);
                    smileysPager.setAdapter(smileysAdapter);

                    popupWindow.setHeight(keyboardHeight);
                    if (isKeyboardVisible) {
                        smileysFooter.setVisibility(LinearLayout.GONE);
                    } else {
                        smileysFooter.setVisibility(LinearLayout.VISIBLE);
                    }
                    popupWindow.showAtLocation(chatRoot, Gravity.BOTTOM, 0, 0);
                }
            }
        });

        initPopupView();

        ViewTreeObserver treeObserver = chatRoot.getViewTreeObserver();
        if (treeObserver != null) {
            treeObserver.addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {

                        @Override
                        public void onGlobalLayout() {
                            // This must be refactored.
                            onGlobalLayoutUpdated();
                            if (isConfigurationChanging) {
                                isConfigurationChanging = false;
                                updateKeyboardHeight(initKeyboardHeight);
                                smileysAdapter = new SmileysPagerAdapter(ChatActivity.this,
                                        keyboardWidth, keyboardHeight, callback);
                                smileysPager.setAdapter(smileysAdapter);
                            }
                        }
                    }
            );
        }
    }

    private String getMessageText() {
        Editable editable = messageText.getText();
        if (editable != null) {
            return editable.toString();
        }
        return "";
    }

    private void insertSmileyText(String smileyText) {
        smileyText = " " + smileyText + " ";
        int selectionStart = messageText.getSelectionStart();
        int selectionEnd = messageText.getSelectionEnd();
        String message = getMessageText();
        message = message.substring(0, selectionStart) + smileyText + message.substring(selectionEnd);
        messageText.setText(message);
        messageText.setSelection(selectionStart + smileyText.length());
    }

    private void sendMessage() {
        final String message = getMessageText().trim();
        Log.d(Settings.LOG_TAG, "message = " + message);
        if (!TextUtils.isEmpty(message)) {
            int buddyDbId = chatHistoryAdapter.getBuddyDbId();
            messageText.setText("");
            MessageCallback callback = new MessageCallback() {

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailed() {
                    messageText.setText(message);
                }
            };
            TaskExecutor.getInstance().execute(
                    new SendMessageTask(ChatActivity.this, buddyDbId, message, callback));
        }
    }

    private void onGlobalLayoutUpdated() {
        // This must be refactored.
        Rect rect = new Rect();
        chatRoot.getWindowVisibleDisplayFrame(rect);
        if (chatRoot.getRootView() != null) {
            keyboardWidth = chatRoot.getRootView().getWidth();
            int screenHeight = chatRoot.getRootView().getHeight();
            int heightDifference = screenHeight - (rect.bottom);
            if (previousHeightDifference - heightDifference > diffKeyboardHeight) {
                popupWindow.dismiss();
            }
            previousHeightDifference = heightDifference;
            if (heightDifference > minKeyboardHeight) {
                isKeyboardVisible = true;
                updateKeyboardHeight(heightDifference);
            } else {
                isKeyboardVisible = false;
            }
        }
    }

    private void updateKeyboardHeight(int height) {
        if (height > minKeyboardHeight && height != keyboardHeight) {
            keyboardHeight = height;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, keyboardHeight);
            smileysFooter.setLayoutParams(params);
        }
    }

    /**
     * Defining all components of smileys keyboard
     */
    private void initPopupView() {
        smileysPager = (ViewPager) popupView.findViewById(R.id.smileys_pager);
        smileysPager.setOffscreenPageLimit(3);

        smileysAdapter = new SmileysPagerAdapter(ChatActivity.this,
                keyboardWidth, keyboardHeight, callback);
        smileysPager.setAdapter(smileysAdapter);

        CirclePageIndicator pageIndicator = (CirclePageIndicator) popupView.findViewById(R.id.circle_pager);
        pageIndicator.setViewPager(smileysPager);

        // Creating a pop window for smileys keyboard.
        popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT,
                keyboardHeight, false);

        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                smileysFooter.setVisibility(LinearLayout.GONE);
            }
        });
    }

    private void hidePopup() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(messageText.getWindowToken(), 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Think about this.
        isConfigurationChanging = true;
        hideKeyboard();
        hidePopup();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        if (messageText != null) {
            QueryHelper.modifyBuddyDraft(getContentResolver(), chatHistoryAdapter.getBuddyDbId(), getMessageText());
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menu inflating.
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Configure the search info and add any event listeners.
        SearchView.OnQueryTextListener onQueryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                chatHistoryAdapter.getFilter().filter(newText);
                return false;
            }
        };
        searchView.setOnQueryTextListener(onQueryTextListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.close_chat_menu: {
                QueryHelper.modifyDialog(getContentResolver(), chatHistoryAdapter.getBuddyDbId(), false);
                onBackPressed();
                return true;
            }
            case R.id.buddy_info_menu: {
                BuddyInfoTask buddyInfoTask = new BuddyInfoTask(this, chatHistoryAdapter.getBuddyDbId());
                TaskExecutor.getInstance().execute(buddyInfoTask);
                return true;
            }
            case R.id.clear_history_menu: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.clear_history_title);
                builder.setMessage(R.string.clear_history_text);
                builder.setPositiveButton(R.string.yes_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClearHistoryTask clearHistoryTask = new ClearHistoryTask(ChatActivity.this,
                                chatHistoryAdapter.getBuddyDbId());
                        TaskExecutor.getInstance().execute(clearHistoryTask);
                    }
                });
                builder.setNegativeButton(R.string.do_not_clear, null);
                builder.show();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(Settings.LOG_TAG, "onNewIntent");

        int buddyDbId = getIntentBuddyDbId(intent);

        setTitleByBuddyDbId(buddyDbId);

        if (chatHistoryAdapter != null) {
            chatHistoryAdapter.notifyDataSetInvalidated();
        }
        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this, getLoaderManager(), buddyDbId);
        chatList.setAdapter(chatHistoryAdapter);
    }

    @Override
    public void onBackPressed() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        } else {
            Intent intent = new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        readVisibleMessages();
    }

    @Override
    public void onCoreServiceReady() {
    }

    @Override
    public void onCoreServiceDown() {
        // TODO: must be implemented.
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // TODO: must be implemented.
    }

    private int getIntentBuddyDbId(Intent intent) {
        Bundle bundle = intent.getExtras();
        int buddyDbId = -1;
        // Checking for bundle condition.
        if (bundle != null && bundle.containsKey(GlobalProvider.HISTORY_BUDDY_DB_ID)) {
            // Setup active page.
            buddyDbId = bundle.getInt(GlobalProvider.HISTORY_BUDDY_DB_ID, 0);
        }
        return buddyDbId;
    }

    private void setTitleByBuddyDbId(int buddyDbId) {
        try {
            // This will provide buddy nick by db id.
            getActionBar().setTitle(QueryHelper.getBuddyNick(getContentResolver(), buddyDbId));
        } catch (BuddyNotFoundException ignored) {
            Log.d(Settings.LOG_TAG, "No buddy fount by specified buddyDbId");
        }
    }

    private void readMessagesAsync(int buddyDbId, long firstMessageDbId, long lastMessageDbId) {
        // This can be executed while activity became invisible to user,
        // so we must check it here. After activity restored, messages will be read automatically.
        if (!isPaused) {
            TaskExecutor.getInstance().execute(new ReadMessagesTask(getContentResolver(), buddyDbId,
                    firstMessageDbId, lastMessageDbId));
        }
    }

    private void readVisibleMessages() {
        final int firstVisiblePosition = chatList.getFirstVisiblePosition();
        final int lastVisiblePosition = chatList.getLastVisiblePosition();
        Log.d(Settings.LOG_TAG, "Reading visible messages ["
                + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
        // Checking for the list view is ready.
        if (lastVisiblePosition >= firstVisiblePosition) {
            final int buddyDbId = chatHistoryAdapter.getBuddyDbId();
            try {
                final long firstMessageDbId = chatHistoryAdapter.getMessageDbId(firstVisiblePosition);
                final long lastMessageDbId = chatHistoryAdapter.getMessageDbId(lastVisiblePosition);
                readMessagesAsync(buddyDbId, firstMessageDbId, lastMessageDbId);
            } catch (MessageNotFoundException ignored) {
                Log.d(Settings.LOG_TAG, "Error while marking messages as read positions ["
                        + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
            }
        }
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
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.chat_history_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;  // Return false because nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.message_copy:
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("", getSelectedMessages()));
                    break;
                case R.id.message_share:
                    startActivity(createShareIntent());
                    break;
                case R.id.message_remove:
                    removeSelectedMessages();
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

        private void removeSelectedMessages() {
            selectionHelper.getSelectedIds();
            QueryHelper.removeMessages(getContentResolver(), selectionHelper.getSelectedIds());
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
                        readMessagesAsync(buddyDbId, firstMessageDbId, lastMessageDbId);
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
