package com.tomclaw.mandarin.main;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.PleaseWaitTask;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.ServiceInteraction;
import com.tomclaw.mandarin.core.ServiceTask;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.core.UriFile;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.BuddyObserver;
import com.tomclaw.mandarin.im.MessageData;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.im.tasks.UpdateLastReadTask;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.main.adapters.SmileysPagerAdapter;
import com.tomclaw.mandarin.main.tasks.BuddyInfoTask;
import com.tomclaw.mandarin.main.views.CirclePageIndicator;
import com.tomclaw.mandarin.main.views.ScrollingTextView;
import com.tomclaw.mandarin.util.FileHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.SmileyParser;
import com.tomclaw.mandarin.util.StringUtil;
import com.tomclaw.mandarin.util.TimeHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
    private RecyclerView chatList;
    private ChatLayoutManager chatLayoutManager;
    private ChatHistoryAdapter chatHistoryAdapter;
    private EditText messageText;
    private TextView buddyNick;
    private ScrollingTextView buddyStatusMessage;
    private ImageView buddyStatusIcon;
    private ActionMode actionMode;
    private MultiChoiceActionCallback actionCallback;
    private ContentClickListener contentClickListener;
    private ChatHistoryAdapter.SelectionModeListener selectionModeListener;
    private ChatHistoryAdapter.HistoryIntegrityListener historyIntegrityListener;

    private View popupView;
    private LinearLayout smileysFooter;
    private PopupWindow popupWindow;
    private int initKeyboardHeight;
    private int minKeyboardHeight;
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
    private DatabaseLayer databaseLayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        databaseLayer = ContentResolverLayer.from(getContentResolver());
        long time = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        // Open chat as faster as we can - without animation.
        getWindow().setWindowAnimations(0);

        setContentView(R.layout.chat_activity);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buddyNick = (TextView) toolbar.findViewById(R.id.buddy_nick);
        buddyStatusMessage = (ScrollingTextView) toolbar.findViewById(R.id.buddy_status_message);
        buddyStatusIcon = (ImageView) toolbar.findViewById(R.id.buddy_status_icon);

        // Initialize action bar.
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setDisplayShowTitleEnabled(false);

        timeHelper = new TimeHelper(this);

        Intent intent = getIntent();
        final Buddy buddy = getIntentBuddy(intent);
        SharingData sharingData = getIntentSharingData(intent);

        startTitleObservation(buddy);
        buddyObserver.touch();

        contentClickListener = new ContentClickListener();
        selectionModeListener = new ChatHistoryAdapter.SelectionModeListener() {
            @Override
            public void onItemStateChanged(ChatHistoryItem historyItem) {
                // Strange case, but let's check it to be sure.
                if (actionCallback != null && actionMode != null) {
                    actionCallback.onItemCheckedStateChanged(actionMode, historyItem.getMessageDbId());
                    chatHistoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected() {
                // Strange case, but let's check it to be sure.
                if (actionMode != null) {
                    actionMode.finish();
                }
            }

            @Override
            public void onLongClicked(ChatHistoryItem historyItem, final SelectionHelper<Long> selectionHelper) {
                if (selectionHelper.setSelectionMode(true)) {
                    actionCallback = new MultiChoiceActionCallback(selectionHelper);
                    actionMode = toolbar.startActionMode(actionCallback);
                    selectionHelper.setChecked(historyItem.getMessageDbId());
                    onItemStateChanged(historyItem);
                }
            }
        };
        historyIntegrityListener = new ChatHistoryIntegrityListener();

        chatHistoryAdapter = new ChatHistoryAdapter(this, getLoaderManager(), buddy, timeHelper);
        chatHistoryAdapter.setContentMessageClickListener(contentClickListener);
        chatHistoryAdapter.setSelectionModeListener(selectionModeListener);
        chatHistoryAdapter.setHistoryIntegrityListener(historyIntegrityListener);

        chatList = (RecyclerView) findViewById(R.id.chat_list);
        chatLayoutManager = new ChatLayoutManager(this);
        chatList.setLayoutManager(chatLayoutManager);
        chatList.setHasFixedSize(true);
        chatList.setItemAnimator(null);
        chatList.setAdapter(chatHistoryAdapter);

        int chatBackground = PreferenceHelper.getChatBackground(this);
        chatList.setBackgroundResource(chatBackground);

        // Send button and message field initialization.
        final ImageButton sendButton = (ImageButton) findViewById(R.id.send_button);
        messageText = (EditText) findViewById(R.id.message_text);
        setMessageTextFromDraft(buddy);
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

                    popupWindow.showAtLocation(chatRoot, Gravity.BOTTOM, 0, getSoftButtonsBarHeight());
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
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE |
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            messageText.requestFocus();
        }
        isSendByEnter = PreferenceHelper.isSendByEnter(this);
        Logger.log("chat activity start time: " + (System.currentTimeMillis() - time));
    }

    @SuppressLint("NewApi")
    private int getSoftButtonsBarHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int usableHeight = metrics.heightPixels;
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            int realHeight = metrics.heightPixels;
            if (realHeight > usableHeight) {
                return realHeight - usableHeight;
            } else {
                return 0;
            }
        }
        return 0;
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
        // If cursor is inside text and char right of it not whitespace or this is end of the text,
        // then add whitespace.
        if ((selectionEnd < messageText.length() - 1 && message.charAt(selectionEnd) != ' ') ||
                selectionEnd == messageText.length()) {
            smileyText += " ";
        }
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
            Buddy buddy = chatHistoryAdapter.getBuddy();
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
                    new SendMessageTask(this, buddy, message, callback));
        }
    }

    private void setTyping(boolean isTyping) {
        Buddy buddy = chatHistoryAdapter.getBuddy();
        TaskExecutor.getInstance().execute(
                new SendTypingTask(this, buddy, isTyping));
    }

    private void setTypingSync(boolean isTyping) {
        Buddy buddy = chatHistoryAdapter.getBuddy();
        int accountDbId = buddy.getAccountDbId();
        String buddyId = buddy.getBuddyId();
        RequestHelper.requestTyping(getContentResolver(), accountDbId, buddyId, isTyping);
    }

    private void onGlobalLayoutUpdated() {
        // This must be refactored.
        Rect rect = new Rect();
        chatRoot.getWindowVisibleDisplayFrame(rect);
        if (chatRoot.getRootView() != null) {
            keyboardWidth = chatRoot.getRootView().getWidth();
            int screenHeight = chatRoot.getRootView().getHeight();
            int heightDifference = screenHeight - rect.bottom - getSoftButtonsBarHeight();
            int supposedKeyboardHeight = previousHeightDifference - heightDifference;
            if (Math.abs(supposedKeyboardHeight) > minKeyboardHeight) {
                popupWindow.dismiss();
                previousHeightDifference = heightDifference;
            }
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
        searchView.setQueryHint(menu.findItem(R.id.menu_search).getTitle());
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
                QueryHelper.modifyDialog(databaseLayer, chatHistoryAdapter.getBuddy(), false);
                onBackPressed();
                return true;
            }
            case R.id.buddy_info_menu: {
                BuddyInfoTask buddyInfoTask = new BuddyInfoTask(this, chatHistoryAdapter.getBuddy());
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
                                chatHistoryAdapter.getBuddy());
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

        Buddy buddy = getIntentBuddy(intent);
        SharingData sharingData = getIntentSharingData(intent);

        // Checking for buddy is really correct.
        if (buddy == null) {
            return;
        }

        startTitleObservation(buddy);

        if (chatHistoryAdapter != null) {
            chatHistoryAdapter.close();
        }
        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this, getLoaderManager(), buddy, timeHelper);
        chatHistoryAdapter.setContentMessageClickListener(contentClickListener);
        chatHistoryAdapter.setSelectionModeListener(selectionModeListener);
        chatHistoryAdapter.setHistoryIntegrityListener(historyIntegrityListener);
        chatList.setAdapter(chatHistoryAdapter);

        setMessageTextFromDraft(buddy);
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
                    Buddy buddy = chatHistoryAdapter.getBuddy();
                    if (data.getExtras() != null && data.hasExtra(PhotoPickerActivity.SELECTED_ENTRIES)) {
                        Bundle bundle = data.getExtras().getBundle(PhotoPickerActivity.SELECTED_ENTRIES);
                        if (bundle != null) {
                            List<PhotoEntry> photoEntries = new ArrayList<>();
                            for (String key : bundle.keySet()) {
                                photoEntries.add((PhotoEntry) bundle.getSerializable(key));
                            }
                            scrollBottom();
                            TaskExecutor.getInstance().execute(
                                    new SendPhotosTask(this, buddy, photoEntries));
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
            Buddy buddy = chatHistoryAdapter.getBuddy();
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
            TaskExecutor.getInstance().execute(new SendFileTask(this, buddy, uriFile, callback));
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
    }

    @Override
    public void onCoreServiceIntent(Intent intent) {
        // TODO: must be implemented.
    }

    private Buddy getIntentBuddy(Intent intent) {
        Bundle bundle = intent.getExtras();
        Buddy buddy = null;
        // Checking for bundle condition.
        if (bundle != null) {
            buddy = bundle.getParcelable(Buddy.KEY_STRUCT);
        }
        return buddy;
    }

    private SharingData getIntentSharingData(Intent intent) {
        Bundle bundle = intent.getExtras();
        SharingData sharingData = null;
        int flags = intent.getFlags();
        // Checking for this recent apps double intent.
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            // Checking for bundle condition.
            if (bundle != null) {
                // Setup sharing data object.
                sharingData = (SharingData) bundle.getSerializable(SharingActivity.EXTRA_SHARING_DATA);
            }
        }
        return sharingData;
    }

    private void startTitleObservation(final Buddy buddy) {
        if (buddyObserver != null) {
            buddyObserver.stop();
        }
        buddyObserver = new ChatBuddyObserver(getContentResolver(), buddy);
    }

    private void setMessageTextFromDraft(Buddy buddy) {
        String enteredText;
        try {
            enteredText = QueryHelper.getBuddyDraft(
                    databaseLayer, buddy.getAccountDbId(), buddy.getBuddyId());
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
        QueryHelper.modifyBuddyDraft(databaseLayer, chatHistoryAdapter.getBuddy(), getMessageText());
    }

    public void scrollBottom() {
        chatList.scrollToPosition(0);
        chatList.requestLayout();
    }

    private void applySharingData(SharingData sharingData) {
        if (sharingData != null && sharingData.isValid()) {
            if (sharingData.getUri() != null) {
                scrollBottom();
                Buddy buddy = chatHistoryAdapter.getBuddy();
                MessageCallback callback = new MessageCallback() {

                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailed() {
                        Logger.log("sending file failed");
                    }
                };
                // Process obtained Uris.
                List<UriFile> uriFiles = new ArrayList<>();
                for (Uri uri : sharingData.getUri()) {
                    try {
                        uriFiles.add(UriFile.create(this, uri));
                    } catch (Throwable ignored) {
                    }
                }
                TaskExecutor.getInstance().execute(
                        new SendFileTask(this, buddy, uriFiles, callback));
            } else {
                String share;
                if (TextUtils.isEmpty(sharingData.getSubject())) {
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
        } catch (Throwable ignored) {
            // This is totally unknown error and we only can show the corresponding toast.
            Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
        }
    }

    private class MultiChoiceActionCallback implements ActionMode.Callback {

        private SelectionHelper<Long> selectionHelper;

        public MultiChoiceActionCallback(SelectionHelper<Long> selectionHelper) {
            this.selectionHelper = selectionHelper;
        }

        public void onItemCheckedStateChanged(ActionMode mode, long id) {
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.chat_history_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.message_copy:
                    StringUtil.copyStringToClipboard(ChatActivity.this, getSelectedMessages());
                    break;
                case R.id.message_share:
                    startActivity(createShareIntent());
                    break;
                case R.id.message_remove:
                    removeSelectedMessages(mode);
                    return true;
                default:
                    return false;
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.setSelectionMode(false);
            chatHistoryAdapter.notifyDataSetChanged();
        }

        private String getSelectedMessages() {
            // Obtain selected positions.
            Collection<Long> selectedIds = selectionHelper.getSelected();
//            return QueryHelper.getMessagesTexts(getContentResolver(),
//                    chatHistoryAdapter.getTimeHelper(), selectedIds).trim();
            return null;
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
                            QueryHelper.removeMessages(databaseLayer, selectionHelper.getSelected());
                            mode.finish();
                        }
                    })
                    .setNeutralButton(R.string.do_not_remove, null).show();
        }
    }

    private static class ClearHistoryTask extends PleaseWaitTask {

        private final Buddy buddy;

        public ClearHistoryTask(Context context, Buddy buddy) {
            super(context);
            // We need to clear history even if this buddy present in any groups.
            this.buddy = buddy;
        }

        @Override
        public void executeBackground() {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                if (contentResolver != null) {
                    QueryHelper.clearHistory(databaseLayer, buddy);
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

    public static class SendMessageTask extends WeakObjectTask<Context> {

        private final Buddy buddy;
        private String text;
        private final MessageCallback callback;

        public SendMessageTask(Context context, Buddy buddy, String text, MessageCallback callback) {
            super(context);
            this.buddy = buddy;
            this.text = text;
            this.callback = callback;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                String cookie = StringUtil.generateRandomString(32);
                int messageType = GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING;
                long messageTime = System.currentTimeMillis();
                int accountDbId = buddy.getAccountDbId();
                String buddyId = buddy.getBuddyId();
                long prevMsgId = GlobalProvider.HISTORY_MESSAGE_ID_REQUESTED;
                long msgId = Long.MAX_VALUE;

                MessageData messageData = new MessageData(accountDbId, buddyId, prevMsgId,
                        msgId, cookie, messageType, messageTime, text);
                QueryHelper.insertMessage(databaseLayer, messageData);
                RequestHelper.requestMessage(contentResolver, accountDbId, buddyId, cookie, text);
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
                Toast.makeText(context, R.string.error_sending_message, Toast.LENGTH_LONG).show();
            }
            callback.onFailed();
        }
    }

    private static class SendPhotosTask extends PleaseWaitTask {

        private final Buddy buddy;
        private final List<PhotoEntry> selectedPhotos;

        public SendPhotosTask(Context context, Buddy buddy, List<PhotoEntry> photoEntries) {
            super(context);
            this.buddy = buddy;
            this.selectedPhotos = photoEntries;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                int counter = 0;
                for (PhotoEntry photoEntry : selectedPhotos) {
                    String cookie = System.currentTimeMillis() + "/" + counter + ":" + ""/*photoEntry.hash*/;
                    File file = new File(photoEntry.path);
                    if (file.exists() && file.length() > 0) {
                        Uri uri = Uri.fromFile(file);
                        UriFile uriFile = UriFile.create(context, uri);
                        // Checking file type, size and other required information.
                        long size = uriFile.getSize();
                        int contentType = uriFile.getContentType();
                        String hash = "";//photoEntry.hash;
                        String tag = cookie + ":" + uriFile.getPath();
                        // Create outgoing file messages.
//                        QueryHelper.insertOutgoingFileMessage(contentResolver, buddyDbId, cookie, uriFile.getUri(),
//                                uriFile.getName(), contentType, size, hash, tag);
                        // Sending protocol message request.
//                        RequestHelper.requestFileSend(contentResolver, buddy, cookie, tag, uriFile);
                        counter++;
                    }
                }
            }
        }
    }

    private static class SendFileTask extends PleaseWaitTask {

        private final Buddy buddy;
        private List<UriFile> uriFiles;
        private final MessageCallback callback;
        private Random random;

        public SendFileTask(ChiefActivity activity, Buddy buddy, UriFile uriFile, MessageCallback callback) {
            this(activity, buddy, Collections.singletonList(uriFile), callback);
        }

        public SendFileTask(ChiefActivity activity, Buddy buddy, List<UriFile> uriFiles, MessageCallback callback) {
            super(activity);
            this.buddy = buddy;
            this.uriFiles = uriFiles;
            this.callback = callback;
            this.random = new Random();
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                for (UriFile uriFile : uriFiles) {
                    String cookie = String.valueOf(System.currentTimeMillis() + uriFile.hashCode() + random.nextLong());
                    // Checking file type, size and other required information.
                    long size = uriFile.getSize();
                    int contentType = uriFile.getContentType();
                    String hash = HttpUtil.getUrlHash(uriFile.toString());
                    String tag = cookie + ":" + uriFile.getPath();
                    // Check for image in bitmap cache first.
                    Bitmap bitmap = BitmapCache.getInstance().getBitmapSync(hash,
                            BitmapCache.BITMAP_SIZE_ORIGINAL, BitmapCache.BITMAP_SIZE_ORIGINAL, true, false);
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
//                    QueryHelper.insertOutgoingFileMessage(contentResolver, buddyDbId, cookie, uriFile.getUri(),
//                            uriFile.getName(), contentType, size, hash, tag);
                    // Sending protocol message request.
//                    RequestHelper.requestFileSend(contentResolver, buddy, cookie, tag, uriFile);
                }
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

    public static class SendTypingTask extends WeakObjectTask<ChiefActivity> {

        private final Buddy buddy;
        private boolean isTyping;

        public SendTypingTask(ChiefActivity activity, Buddy buddy, boolean isTyping) {
            super(activity);
            this.buddy = buddy;
            this.isTyping = isTyping;
        }

        @Override
        public void executeBackground() throws Throwable {
            ChiefActivity activity = getWeakObject();
            if (activity != null) {
                ContentResolver contentResolver = activity.getContentResolver();
                int accountDbId = buddy.getAccountDbId();
                String buddyId = buddy.getBuddyId();
                // Sending protocol typing request.
                RequestHelper.requestTyping(contentResolver, accountDbId, buddyId, isTyping);
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
            SmileyParser.getInstance().addSmileySpans(s);
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

    public abstract static class MessageCallback {

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
                    QueryHelper.updateFileState(databaseLayer,
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
                    QueryHelper.updateFileState(databaseLayer,
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
            if (FileHelper.getMimeType(contentName).startsWith("image")) {
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

    public class ChatHistoryIntegrityListener implements ChatHistoryAdapter.HistoryIntegrityListener {

        @Override
        public void onHole(Buddy buddy, long fromMessageId, long tillMessageId) {
            Logger.log("chat history hole detected");
            ContentResolver contentResolver = getContentResolver();
            DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
            int count = -Settings.HISTORY_BLOCK_SIZE;
            int accountDbId = buddy.getAccountDbId();
            String buddyId = buddy.getBuddyId();
            String patchVersion = QueryHelper.getBuddyPatchVersion(databaseLayer, buddy);
            RequestHelper.requestHistoryBlock(contentResolver,
                    accountDbId, buddyId, fromMessageId, tillMessageId, patchVersion, count);
            QueryHelper.markMessageRequested(databaseLayer, buddy, tillMessageId);
        }

        @Override
        public void onHistoryUpdated(Buddy buddy) {
            Logger.log("chat local history updated");
            ContentResolver contentResolver = getContentResolver();
            Bundle bundle = new Bundle();
            bundle.putParcelable(UpdateLastReadTask.KEY_BUDDY, buddy);
            contentResolver.call(Settings.BUDDY_RESOLVER_URI,
                    UpdateLastReadTask.class.getName(), null, bundle);
        }
    }

    public static class StopDownloadingTask extends ServiceTask<ChiefActivity> {

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
                ContentResolver contentResolver = activity.getContentResolver();
                DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                QueryHelper.updateFileState(databaseLayer, desiredState,
                        GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING, messageCookie);
            }
        }
    }

    public static class StopUploadingTask extends ServiceTask<ChiefActivity> {

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
                ContentResolver contentResolver = activity.getContentResolver();
                DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                QueryHelper.updateFileState(databaseLayer, desiredState,
                        GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING, messageCookie);
            }
        }
    }

    public class ChatBuddyObserver extends BuddyObserver {

        public ChatBuddyObserver(ContentResolver contentResolver, Buddy buddy) {
            super(contentResolver, buddy);
        }

        @Override
        public void onBuddyInfoChanged(final BuddyCursor buddyCursor) {
            final int icon = StatusUtil.getStatusDrawable(buddyCursor.getAccountType(),
                    buddyCursor.getStatus());
            final String title = buddyCursor.getBuddyNick();
            final String subtitle;

            long lastTyping = buddyCursor.getLastTyping();
            // Checking for typing no more than 5 minutes.
            if (lastTyping > 0 && System.currentTimeMillis() - lastTyping < Settings.TYPING_DELAY) {
                subtitle = getString(R.string.typing);
            } else {
                long lastSeen = buddyCursor.getLastSeen();
                if (lastSeen > 0) {
                    String lastSeenText;
                    String lastSeenDate = timeHelper.getShortFormattedDate(lastSeen * 1000);
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
                    subtitle = buddyCursor.getStatusTitle();
                }
            }

            MainExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    buddyNick.setText(title);
                    buddyStatusMessage.setText(subtitle);
                    buddyStatusIcon.setImageResource(icon);
                }
            });
        }
    }
}
