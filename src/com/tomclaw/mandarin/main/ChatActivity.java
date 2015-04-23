package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.main.adapters.SmileysPagerAdapter;
import com.tomclaw.mandarin.main.tasks.BuddyInfoTask;
import com.tomclaw.mandarin.main.views.CirclePageIndicator;
import com.tomclaw.mandarin.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 */
public class ChatActivity extends ChiefActivity {

    private static final int PICK_FILE_RESULT_CODE = 1;
    private static final int PICK_GALLERY_RESULT_CODE = 2;

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
    private boolean isGoToDestroy;
    private BuddyObserver buddyObserver;
    private TimeHelper timeHelper;
    private MessageWatcher messageWatcher;
    private boolean isSendByEnter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        long time = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.chat_activity);

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);

        timeHelper = new TimeHelper(this);

        Intent intent = getIntent();
        final int buddyDbId = getIntentBuddyDbId(intent);
        SharingData sharingData = getIntentSharingData(intent);

        startTitleObservation(buddyDbId);
        buddyObserver.touch();

        chatHistoryAdapter = new ChatHistoryAdapter(this, getLoaderManager(), buddyDbId, timeHelper);
        chatHistoryAdapter.setContentMessageClickListener(new ContentClickListener());

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
        final ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        messageText = (EditText) findViewById(R.id.message_text);
        setMessageTextFromDraft(buddyDbId);
        applySharingData(sharingData);
        messageText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                hidePopup();
            }
        });
        messageWatcher = new MessageWatcher();
        messageText.addTextChangedListener(messageWatcher);
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

        // Checking for we must show keyboard automatically.
        if (PreferenceHelper.isShowKeyboard(this)) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            messageText.requestFocus();
        }
        isSendByEnter = PreferenceHelper.isSendByEnter(this);
        Logger.log("chat activity start time: " + (System.currentTimeMillis() - time));
    }

    private String getMessageText() {
        Editable editable = messageText.getText();
        if (editable != null) {
            return editable.toString();
        }
        return "";
    }

    private void insertSmileyText(String smileyText) {
        String message = getMessageText();
        int selectionStart = messageText.getSelectionStart();
        int selectionEnd = messageText.getSelectionEnd();
        // Checking for spaces needed on the left or right side of this smile.
        if (selectionStart > 0 && message.charAt(selectionStart - 1) != ' ') {
            smileyText = " " + smileyText;
        }
        smileyText += " ";
        // Inserting smile into current message.
        message = message.substring(0, selectionStart) + smileyText + message.substring(selectionEnd);
        messageText.setText(message);
        int selection = selectionStart + smileyText.length();
        // Check for selection range is correct.
        if (selection >= 0 && selection <= messageText.length()) {
            messageText.setSelection(selection);
        }
    }

    private void sendMessage() {
        final String message = getMessageText().trim();
        Logger.log("message = " + message);
        if (!TextUtils.isEmpty(message)) {
            int buddyDbId = chatHistoryAdapter.getBuddyDbId();
            messageText.setText("");
            scrollBottom();
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
                    new SendMessageTask(this, buddyDbId, message, callback));
        }
    }

    private void setTyping(boolean isTyping) {
        int buddyDbId = chatHistoryAdapter.getBuddyDbId();
        TaskExecutor.getInstance().execute(
                new SendTypingTask(this, buddyDbId, isTyping));
    }

    private void setTypingSync(boolean isTyping) {
        RequestHelper.requestTyping(getContentResolver(),
                chatHistoryAdapter.getBuddyDbId(), isTyping);
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
            // Stop typing status.
            if (!TextUtils.isEmpty(getMessageText())) {
                messageWatcher.stop();
                setTypingSync(false);
            }
            saveMessageTextAsDraft();
        }
        buddyObserver.stop();
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
            case R.id.send_picture_menu: {
                startActivityForResult(new Intent(this, PhotoPickerActivity.class), PICK_GALLERY_RESULT_CODE);
                return true;
            }
            case R.id.send_video_menu: {
                try {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("video/*");
                    startActivityForResult(photoPickerIntent, PICK_FILE_RESULT_CODE);
                    return true;
                } catch (Throwable ignored) {
                    // No video picker application.
                    Toast.makeText(this, R.string.no_video_picker, Toast.LENGTH_SHORT).show();
                }
            }
            case R.id.send_document_menu: {
                startActivityForResult(new Intent(this, DocumentPickerActivity.class), PICK_FILE_RESULT_CODE);
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
        Logger.log("onNewIntent");

        // Save currently entered text as draft before switching.
        saveMessageTextAsDraft();

        int buddyDbId = getIntentBuddyDbId(intent);
        SharingData sharingData = getIntentSharingData(intent);

        // Checking for buddy db id is really correct.
        if (buddyDbId == -1) {
            return;
        }

        startTitleObservation(buddyDbId);

        if (chatHistoryAdapter != null) {
            chatHistoryAdapter.close();
            chatHistoryAdapter.notifyDataSetInvalidated();
        }
        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this, getLoaderManager(), buddyDbId, timeHelper);
        chatHistoryAdapter.setContentMessageClickListener(new ContentClickListener());
        chatList.setAdapter(chatHistoryAdapter);

        setMessageTextFromDraft(buddyDbId);
        applySharingData(sharingData);
    }

    @Override
    public void onBackPressed() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        } else {
            openMainActivity();
        }
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_FILE_RESULT_CODE: {
                if (resultCode == RESULT_OK) {
                    onFilePicked(data.getData());
                }
                break;
            }
            case PICK_GALLERY_RESULT_CODE: {
                if (resultCode == RESULT_OK) {
                    int buddyDbId = chatHistoryAdapter.getBuddyDbId();
                    if (data.getExtras() != null && data.hasExtra(PhotoPickerActivity.SELECTED_ENTRIES)) {
                        Bundle bundle = data.getExtras().getBundle(PhotoPickerActivity.SELECTED_ENTRIES);
                        if (bundle != null) {
                            List<PhotoEntry> photoEntries = new ArrayList<PhotoEntry>();
                            for (String key : bundle.keySet()) {
                                photoEntries.add((PhotoEntry) bundle.getSerializable(key));
                            }
                            scrollBottom();
                            TaskExecutor.getInstance().execute(new SendPhotosTask(this, buddyDbId, photoEntries));
                        }
                    } else if (data.getData() != null) {
                        onFilePicked(data.getData());
                    }
                }
            }
        }
    }

    private void onFilePicked(Uri uri) {
        try {
            int buddyDbId = chatHistoryAdapter.getBuddyDbId();
            MessageCallback callback = new MessageCallback() {

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailed() {
                    Logger.log("sending file failed");
                }
            };
            scrollBottom();
            UriFile uriFile = UriFile.create(this, uri);
            TaskExecutor.getInstance().execute(
                    new SendFileTask(this, buddyDbId, uriFile, callback));
        } catch (Throwable ignored) {
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
    public void onCoreServiceIntent(Intent intent) {
        // TODO: must be implemented.
    }

    private int getIntentBuddyDbId(Intent intent) {
        Bundle bundle = intent.getExtras();
        int buddyDbId = -1;
        // Checking for bundle condition.
        if (bundle != null) {
            // Setup active page.
            buddyDbId = bundle.getInt(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
        }
        return buddyDbId;
    }

    private SharingData getIntentSharingData(Intent intent) {
        Bundle bundle = intent.getExtras();
        SharingData sharingData = null;
        // Checking for bundle condition.
        if (bundle != null) {
            // Setup sharing data object.
            sharingData = (SharingData) bundle.getSerializable(SharingActivity.EXTRA_SHARING_DATA);
        }
        return sharingData;
    }

    private void startTitleObservation(final int buddyDbId) {
        if (buddyObserver != null) {
            buddyObserver.stop();
        }
        buddyObserver = new BuddyObserver(getContentResolver(), buddyDbId) {
            @Override
            public void onBuddyInfoChanged(final BuddyCursor buddyCursor) {
                final int icon = StatusUtil.getStatusDrawable(buddyCursor.getBuddyAccountType(),
                        buddyCursor.getBuddyStatus());
                final String title = buddyCursor.getBuddyNick();
                final String subtitle;

                long lastTyping = buddyCursor.getBuddyLastTyping();
                // Checking for typing no more than 5 minutes.
                if (lastTyping > 0 && System.currentTimeMillis() - lastTyping < Settings.TYPING_DELAY) {
                    subtitle = getString(R.string.typing);
                } else {
                    long lastSeen = buddyCursor.getBuddyLastSeen();
                    if (lastSeen > 0) {
                        String lastSeenText;
                        String lastSeenDate = timeHelper.getFormattedDate(lastSeen * 1000);
                        String lastSeenTime = timeHelper.getFormattedTime(lastSeen * 1000);

                        Calendar today = Calendar.getInstance();
                        today = TimeHelper.clearTimes(today);

                        Calendar yesterday = Calendar.getInstance();
                        yesterday.add(Calendar.DAY_OF_YEAR, -1);
                        yesterday = TimeHelper.clearTimes(yesterday);

                        if (lastSeen * 1000 > today.getTimeInMillis()) {
                            lastSeenText = getString(R.string.last_seen_time, lastSeenTime);
                        } else if (lastSeen * 1000 > yesterday.getTimeInMillis()) {
                            lastSeenText = getString(R.string.last_seen_date, getString(R.string.yesterday), lastSeenTime);
                        } else {
                            lastSeenText = getString(R.string.last_seen_date, lastSeenDate, lastSeenTime);
                        }

                        subtitle = lastSeenText;
                    } else {
                        subtitle = buddyCursor.getBuddyStatusTitle();
                    }
                }

                MainExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        ActionBar actionBar = getActionBar();
                        actionBar.setTitle(title);
                        actionBar.setSubtitle(subtitle);
                        actionBar.setIcon(icon);
                    }
                });
            }
        };
    }

    private void setMessageTextFromDraft(int buddyDbId) {
        String enteredText;
        try {
            enteredText = QueryHelper.getBuddyDraft(getContentResolver(), buddyDbId);
        } catch (BuddyNotFoundException ignored) {
            enteredText = null;
        }
        if (!TextUtils.isEmpty(enteredText)) {
            messageText.setText(enteredText);
            messageText.setSelection(enteredText.length());
        } else {
            messageText.setText("");
        }
    }

    private void saveMessageTextAsDraft() {
        QueryHelper.modifyBuddyDraft(getContentResolver(), chatHistoryAdapter.getBuddyDbId(), getMessageText());
    }

    private void readMessagesAsync(int buddyDbId, long firstMessageDbId, long lastMessageDbId) {
        // This can be executed while activity became invisible to user,
        // so we must check it here. After activity restored, messages will be read automatically.
        // Also, activity might be gone to destroy in a moments.
        if (!isPaused && !isGoToDestroy) {
            TaskExecutor.getInstance().execute(new ReadMessagesTask(getContentResolver(), buddyDbId,
                    firstMessageDbId, lastMessageDbId));
        }
    }

    private void readVisibleMessages() {
        final int firstVisiblePosition = chatList.getFirstVisiblePosition();
        final int lastVisiblePosition = chatList.getLastVisiblePosition();
        Logger.log("Reading visible messages ["
                + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
        // Checking for the list view is ready.
        if (lastVisiblePosition >= firstVisiblePosition) {
            final int buddyDbId = chatHistoryAdapter.getBuddyDbId();
            try {
                final long firstMessageDbId = chatHistoryAdapter.getMessageDbId(firstVisiblePosition);
                final long lastMessageDbId = chatHistoryAdapter.getMessageDbId(lastVisiblePosition);
                readMessagesAsync(buddyDbId, firstMessageDbId, lastMessageDbId);
            } catch (MessageNotFoundException ignored) {
                Logger.log("Error while marking messages as read positions ["
                        + firstVisiblePosition + "] -> [" + lastVisiblePosition + "]");
            }
        }
    }

    public void scrollBottom() {
        chatList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        chatList.requestLayout();
        chatList.postDelayed(new Runnable() {
            @Override
            public void run() {
                chatList.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
            }
        }, 300);
    }

    private void applySharingData(SharingData sharingData) {
        if(sharingData != null && sharingData.isValid()) {
            if(sharingData.getUri() != null) {
                // Process obtained Uris.
                for(Uri uri : sharingData.getUri()) {
                    onFilePicked(uri);
                }
            } else {
                String share;
                if(TextUtils.isEmpty(sharingData.getSubject())) {
                    share = sharingData.getText();
                } else {
                    share = sharingData.getSubject().concat("\n").concat(sharingData.getText());
                }
                // Set text to field and move cursor to the end.
                messageText.setText(share);
                messageText.setSelection(share.length());
            }
        }
    }

    @Override
    public void startActivity(Intent intent) {
        try {
            // First attempt at fixing an HTC broken by evil Apple patents.
            if (intent.getComponent() != null
                    && ".HtcLinkifyDispatcherActivity".equals(intent.getComponent().getShortClassName())) {
                intent.setComponent(null);
            }
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            /*
             * Probably an HTC broken by evil Apple patents. This is not perfect,
             * but better than crashing the whole application.
             */
            super.startActivity(Intent.createChooser(intent, null));
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
                    removeSelectedMessages(mode);
                    return true;
                case R.id.message_unread:
                    unreadSelectedMessages(mode);
                    return true;
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
                    Logger.log("Error while copying message on position " + position);
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

        private void removeSelectedMessages(final ActionMode mode) {
            new AlertDialog.Builder(ChatActivity.this)
                    .setTitle(R.string.remove_messages)
                    .setMessage(R.string.remove_selected_messages)
                    .setPositiveButton(R.string.yes_remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectionHelper.getSelectedIds();
                            QueryHelper.removeMessages(getContentResolver(), selectionHelper.getSelectedIds());
                            mode.finish();
                        }
                    })
                    .setNeutralButton(R.string.do_not_remove, null).show();
        }

        private void unreadSelectedMessages(final ActionMode mode) {
            final Collection<Long> selectedIds = new ArrayList<Long>(selectionHelper.getSelectedIds());
            if (!selectedIds.isEmpty() && QueryHelper.isIncomingMessagesPresent(getContentResolver(), selectedIds)) {
                new AlertDialog.Builder(ChatActivity.this)
                        .setTitle(R.string.unread_messages)
                        .setMessage(R.string.mark_messages_unread)
                        .setPositiveButton(R.string.yes_mark, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mode.finish();
                                isGoToDestroy = true;
                                QueryHelper.unreadMessages(getContentResolver(), selectedIds);
                                openMainActivity();
                            }
                        })
                        .setNeutralButton(R.string.no_need, null).show();
            } else {
                Toast.makeText(ChatActivity.this, R.string.no_incoming_selected, Toast.LENGTH_SHORT).show();
            }
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
                    Logger.log("Scroll: " + firstPosition + " -> " + lastPosition);
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

    private class ReadMessagesTask extends WeakObjectTask<ContentResolver> {

        private final int buddyDbId;
        private final long firstMessageDbId;
        private final long lastMessageDbId;

        public ReadMessagesTask(ContentResolver contentResolver, int buddyDbId,
                                long firstMessageDbId, long lastMessageDbId) {
            super(contentResolver);
            this.buddyDbId = buddyDbId;
            this.firstMessageDbId = firstMessageDbId;
            this.lastMessageDbId = lastMessageDbId;
        }

        @Override
        public void executeBackground() throws MessageNotFoundException {
            ContentResolver contentResolver = getWeakObject();
            if (contentResolver != null) {
                QueryHelper.readMessages(contentResolver,
                        buddyDbId, firstMessageDbId, lastMessageDbId);
            }
        }
    }

    private class ClearHistoryTask extends PleaseWaitTask {

        private final int buddyDbId;

        public ClearHistoryTask(Context context, int buddyDbId) {
            super(context);
            this.buddyDbId = buddyDbId;
        }

        @Override
        public void executeBackground() {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                if (contentResolver != null) {
                    QueryHelper.clearHistory(contentResolver, buddyDbId);
                }
            }
        }

        @Override
        public void onFailMain() {
            Context context = getWeakObject();
            if (context != null) {
                // Show error.
                Toast.makeText(context, R.string.error_clearing_history, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SendMessageTask extends WeakObjectTask<ChiefActivity> {

        private final int buddyDbId;
        private String message;
        private final MessageCallback callback;

        public SendMessageTask(ChiefActivity activity, int buddyDbId, String message, MessageCallback callback) {
            super(activity);
            this.buddyDbId = buddyDbId;
            this.message = message;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
            ChiefActivity activity = getWeakObject();
            if (activity != null) {
                ContentResolver contentResolver = activity.getContentResolver();
                String cookie = String.valueOf(System.currentTimeMillis());
                boolean isCollapseMessages = PreferenceHelper.isCollapseMessages(activity);
                QueryHelper.insertMessage(contentResolver, isCollapseMessages, buddyDbId,
                        GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, cookie, message);
                // Sending protocol message request.
                RequestHelper.requestMessage(contentResolver, buddyDbId, cookie, message);
            }
        }

        @Override
        public void onSuccessMain() {
            callback.onSuccess();
        }

        @Override
        public void onFailMain() {
            ChiefActivity activity = getWeakObject();
            if (activity != null) {
                // Show error.
                Toast.makeText(activity, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
            callback.onFailed();
        }
    }

    private class SendPhotosTask extends PleaseWaitTask {

        private final int buddyDbId;
        private final List<PhotoEntry> selectedPhotos;

        public SendPhotosTask(Context context, int buddyDbId, List<PhotoEntry> photoEntries) {
            super(context);
            this.buddyDbId = buddyDbId;
            this.selectedPhotos = photoEntries;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                int counter = 0;
                for (PhotoEntry photoEntry : selectedPhotos) {
                    String cookie = System.currentTimeMillis() + "/" + counter + ":" + photoEntry.hash;
                    File file = new File(photoEntry.path);
                    if (file.exists() && file.length() > 0) {
                        Uri uri = Uri.fromFile(file);
                        UriFile uriFile = UriFile.create(context, uri);
                        // Checking file type, size and other required information.
                        long size = uriFile.getSize();
                        int contentType = uriFile.getContentType();
                        String hash = photoEntry.hash;
                        String tag = cookie + ":" + uriFile.getPath();
                        // Create outgoing file messages.
                        QueryHelper.insertOutgoingFileMessage(contentResolver, buddyDbId, cookie, uriFile.getUri(),
                                uriFile.getName(), contentType, size, hash, tag);
                        // Sending protocol message request.
                        RequestHelper.requestFileSend(contentResolver, buddyDbId, cookie, tag, uriFile);
                        counter++;
                    }
                }
            }
        }
    }

    private class SendFileTask extends PleaseWaitTask {

        private final int buddyDbId;
        private UriFile uriFile;
        private final MessageCallback callback;

        public SendFileTask(ChiefActivity activity, int buddyDbId, UriFile uriFile, MessageCallback callback) {
            super(activity);
            this.buddyDbId = buddyDbId;
            this.uriFile = uriFile;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                String cookie = String.valueOf(System.currentTimeMillis());
                // Checking file type, size and other required information.
                long size = uriFile.getSize();
                int contentType = uriFile.getContentType();
                String hash = HttpUtil.getUrlHash(uriFile.toString());
                String tag = cookie + ":" + uriFile.getPath();
                // Check for image in bitmap cache first.
                Bitmap bitmap = BitmapCache.getInstance().getBitmapSync(hash, BitmapCache.BITMAP_SIZE_ORIGINAL, BitmapCache.BITMAP_SIZE_ORIGINAL, true, false);
                if (bitmap == null) {
                    // Try to create thumbnail from selected Uri.
                    bitmap = uriFile.getThumbnail(context);
                }
                // Check and store bitmap in bitmap cache.
                if (bitmap == null) {
                    // No bitmap - no hash.
                    hash = "";
                } else {
                    // Cache bitmap in Ram immediately
                    BitmapCache.getInstance().cacheBitmapOriginal(hash, bitmap);
                    // ... and async saving in storage.
                    BitmapCache.getInstance().saveBitmapAsync(hash, bitmap, Bitmap.CompressFormat.JPEG);
                }
                QueryHelper.insertOutgoingFileMessage(contentResolver, buddyDbId, cookie, uriFile.getUri(),
                        uriFile.getName(), contentType, size, hash, tag);
                // Sending protocol message request.
                RequestHelper.requestFileSend(contentResolver, buddyDbId, cookie, tag, uriFile);
            }
        }

        @Override
        public void onSuccessMain() {
            callback.onSuccess();
        }

        @Override
        public void onFailMain() {
            Context context = getWeakObject();
            if (context != null) {
                // Show error.
                Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
            callback.onFailed();
        }
    }

    public class SendTypingTask extends WeakObjectTask<ChiefActivity> {

        private final int buddyDbId;
        private boolean isTyping;

        public SendTypingTask(ChiefActivity activity, int buddyDbId, boolean isTyping) {
            super(activity);
            this.buddyDbId = buddyDbId;
            this.isTyping = isTyping;
        }

        @Override
        public void executeBackground() throws Throwable {
            ChiefActivity activity = getWeakObject();
            if (activity != null) {
                ContentResolver contentResolver = activity.getContentResolver();
                // Sending protocol typing request.
                RequestHelper.requestTyping(contentResolver, buddyDbId, isTyping);
            }
        }
    }

    private class MessageWatcher implements TextWatcher {

        private boolean isTimerDown = true;
        private CountDownTimer typingTimer = new CountDownTimer(Settings.TYPING_DELAY, Settings.TYPING_DELAY) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                stop();
                setTyping(false);
            }
        };

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isSendByEnter && s.length() > 0 && s.charAt(s.length() - 1) == '\n') {
                sendMessage();
            }
            if (isTimerDown) {
                if (TextUtils.isEmpty(s)) {
                    // There was a text typed, timer is gone and
                    // now text was cleared. Nothing to be done.
                    return;
                }
                // No timer yet or timer is finished. Let's start typing.
                setTyping(true);
            } else {
                stop();
                // Checking for empty text view, so, we must stop typing.
                if (TextUtils.isEmpty(s)) {
                    typingTimer.onFinish();
                    return;
                }
            }
            start();
        }

        private void start() {
            typingTimer.start();
            isTimerDown = false;
        }

        private void stop() {
            typingTimer.cancel();
            isTimerDown = true;
        }
    }

    public abstract class MessageCallback {

        public abstract void onSuccess();

        public abstract void onFailed();
    }

    public class ContentClickListener implements ChatHistoryAdapter.ContentMessageClickListener {

        @Override
        public void onClicked(ChatHistoryItem historyItem) {
            switch (historyItem.getMessageType()) {
                case GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING: {
                    onIncomingClicked(historyItem.getContentState(), historyItem.getContentTag(),
                            historyItem.getContentUri(), historyItem.getContentName(),
                            historyItem.getPreviewHash(), historyItem.getMessageCookie());
                    break;
                }
                case GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING: {
                    onOutgoingClicked(historyItem.getContentState(), historyItem.getContentTag(),
                            historyItem.getContentUri(), historyItem.getContentName(),
                            historyItem.getPreviewHash(), historyItem.getMessageCookie());
                    break;
                }
            }
        }

        public void onIncomingClicked(int contentState, String contentTag, String contentUri,
                                      String contentName, String previewHash, String messageCookie) {
            switch (contentState) {
                case GlobalProvider.HISTORY_CONTENT_STATE_STOPPED: {
                    RequestHelper.startDelayedRequest(getContentResolver(), contentTag);
                    QueryHelper.updateFileState(getContentResolver(),
                            GlobalProvider.HISTORY_CONTENT_STATE_WAITING,
                            GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING,
                            messageCookie);
                    break;
                }
                case GlobalProvider.HISTORY_CONTENT_STATE_WAITING:
                case GlobalProvider.HISTORY_CONTENT_STATE_RUNNING: {
                    TaskExecutor.getInstance().execute(
                            new StopDownloadingTask(ChatActivity.this, contentTag, messageCookie));
                    break;
                }
                case GlobalProvider.HISTORY_CONTENT_STATE_STABLE: {
                    viewContent(contentName, contentUri, previewHash);
                    break;
                }
            }
        }

        public void onOutgoingClicked(int contentState, String contentTag, String contentUri,
                                      String contentName, String previewHash, String messageCookie) {
            switch (contentState) {
                case GlobalProvider.HISTORY_CONTENT_STATE_FAILED:
                case GlobalProvider.HISTORY_CONTENT_STATE_STOPPED: {
                    RequestHelper.startDelayedRequest(getContentResolver(), contentTag);
                    QueryHelper.updateFileState(getContentResolver(),
                            GlobalProvider.HISTORY_CONTENT_STATE_WAITING,
                            GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING,
                            messageCookie);
                    break;
                }
                case GlobalProvider.HISTORY_CONTENT_STATE_WAITING:
                case GlobalProvider.HISTORY_CONTENT_STATE_RUNNING: {
                    TaskExecutor.getInstance().execute(
                            new StopUploadingTask(ChatActivity.this, contentTag, messageCookie));
                    break;
                }
                case GlobalProvider.HISTORY_CONTENT_STATE_STABLE: {
                    viewContent(contentName, contentUri, previewHash);
                    break;
                }
            }
        }

        private void viewContent(String contentName, String contentUri, String previewHash) {
            if (FileHelper.getMimeType(contentName).startsWith("image") &&
                    !TextUtils.equals(FileHelper.getFileExtensionFromPath(contentName), "gif")) {
                Intent intent = new Intent(ChatActivity.this, PhotoViewerActivity.class);
                intent.putExtra(PhotoViewerActivity.EXTRA_PICTURE_NAME, contentName);
                intent.putExtra(PhotoViewerActivity.EXTRA_PICTURE_URI, contentUri);
                intent.putExtra(PhotoViewerActivity.EXTRA_PREVIEW_HASH, previewHash);
                startActivity(intent);
            } else {
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(contentUri), FileHelper.getMimeType(contentName));
                startActivity(intent);
            }
        }
    }

    public class StopDownloadingTask extends ServiceTask {

        private String contentTag;
        private String messageCookie;

        public StopDownloadingTask(ChiefActivity object, String contentTag, String messageCookie) {
            super(object);
            this.contentTag = contentTag;
            this.messageCookie = messageCookie;
        }

        @Override
        public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
            ChiefActivity activity = getWeakObject();
            if (activity != null) {
                boolean wasActive = interaction.stopDownloadRequest(contentTag);
                int desiredState;
                // Checking for the task was active and will be stopped by itself,
                // or it was in queue and it needs to be switched to waiting state manually.
                if (wasActive) {
                    desiredState = GlobalProvider.HISTORY_CONTENT_STATE_INTERRUPT;
                } else {
                    desiredState = GlobalProvider.HISTORY_CONTENT_STATE_STOPPED;
                }
                QueryHelper.updateFileState(activity.getContentResolver(), desiredState,
                        GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, messageCookie);
            }
        }
    }

    public class StopUploadingTask extends ServiceTask {

        private String contentTag;
        private String messageCookie;

        public StopUploadingTask(ChiefActivity object, String contentTag, String messageCookie) {
            super(object);
            this.contentTag = contentTag;
            this.messageCookie = messageCookie;
        }

        @Override
        public void executeServiceTask(ServiceInteraction interaction) throws Throwable {
            ChiefActivity activity = getWeakObject();
            if (activity != null) {
                boolean wasActive = interaction.stopUploadingRequest(contentTag);
                int desiredState;
                // Checking for the task was active and will be stopped by itself,
                // or it was in queue and it needs to be switched to waiting state manually.
                if (wasActive) {
                    desiredState = GlobalProvider.HISTORY_CONTENT_STATE_INTERRUPT;
                } else {
                    desiredState = GlobalProvider.HISTORY_CONTENT_STATE_STOPPED;
                }
                QueryHelper.updateFileState(activity.getContentResolver(), desiredState,
                        GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, messageCookie);
            }
        }
    }
}
