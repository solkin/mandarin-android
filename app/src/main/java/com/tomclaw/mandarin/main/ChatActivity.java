package com.tomclaw.mandarin.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.TypefaceSpan;
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

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.Snackbar;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.BuddyObserver;
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
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.im.StatusUtil;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.main.adapters.SmileysPagerAdapter;
import com.tomclaw.mandarin.main.tasks.BuddyInfoTask;
import com.tomclaw.mandarin.main.views.CirclePageIndicator;
import com.tomclaw.mandarin.main.views.ScrollingTextView;
import com.tomclaw.mandarin.util.FileHelper;
import com.tomclaw.mandarin.util.HttpUtil;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.QueryBuilder;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.SmileyParser;
import com.tomclaw.mandarin.util.StringUtil;
import com.tomclaw.mandarin.util.TimeHelper;

import net.hockeyapp.android.metrics.MetricsManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.tomclaw.mandarin.util.PermissionsHelper.hasPermissions;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 */
public class ChatActivity extends ChiefActivity {

    private static final int PICK_FILE_RESULT_CODE = 1;
    private static final int PICK_GALLERY_RESULT_CODE = 2;

    private static final int REQUEST_PICK_GALLERY = 3;
    private static final int REQUEST_PICK_VIDEO = 4;
    private static final int REQUEST_PICK_DOCUMENT = 5;
    private static final int REQUEST_CLICK_INCOMING = 6;
    private static final int REQUEST_CLICK_OUTGOING = 7;
    private static final int REQUEST_HISTORY_EXPORT = 8;

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

    private ChatHistoryItem clickedContentItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        long time = System.currentTimeMillis();
        super.onCreate(savedInstanceState);

        // Open chat as faster as we can - without animation.
        getWindow().setWindowAnimations(0);

        setContentView(R.layout.chat_activity);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buddyNick = toolbar.findViewById(R.id.buddy_nick);
        buddyStatusMessage = toolbar.findViewById(R.id.buddy_status_message);
        buddyStatusIcon = toolbar.findViewById(R.id.buddy_status_icon);

        // Initialize action bar.
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setDisplayShowTitleEnabled(false);

        timeHelper = new TimeHelper(this);

        Intent intent = getIntent();
        final int buddyDbId = getIntentBuddyDbId(intent);
        SharingData sharingData = getIntentSharingData(intent);

        startTitleObservation(buddyDbId);
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

        chatHistoryAdapter = new ChatHistoryAdapter(this, getLoaderManager(), buddyDbId, timeHelper);
        chatHistoryAdapter.setContentMessageClickListener(contentClickListener);
        chatHistoryAdapter.setSelectionModeListener(selectionModeListener);

        chatList = findViewById(R.id.chat_list);
        chatLayoutManager = new ChatLayoutManager(this);
        chatLayoutManager.setDataChangedListener(new ChatLayoutManager.DataChangedListener() {
            @Override
            public void onDataChanged() {
                readVisibleMessages();
            }
        });
        chatList.addOnScrollListener(new ChatScrollListener(chatLayoutManager));
        chatList.setLayoutManager(chatLayoutManager);
        chatList.setHasFixedSize(true);
        chatList.setAdapter(chatHistoryAdapter);
        chatList.setItemAnimator(null);

        int chatBackground = PreferenceHelper.getChatBackground(this);
        chatList.setBackgroundResource(chatBackground);

        // Send button and message field initialization.
        final ImageButton sendButton = findViewById(R.id.send_button);
        messageText = findViewById(R.id.message_text);
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

        chatRoot = findViewById(R.id.chat_root);
        popupView = getLayoutInflater().inflate(R.layout.smileys_popup, chatRoot, false);
        smileysFooter = findViewById(R.id.smileys_footer);

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
        ImageView smileysButton = findViewById(R.id.smileys_button);
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

        MetricsManager.trackEvent("Open chat");
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
            MetricsManager.trackEvent("Send message");
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
        smileysPager = popupView.findViewById(R.id.smileys_pager);
        smileysPager.setOffscreenPageLimit(3);

        smileysAdapter = new SmileysPagerAdapter(ChatActivity.this,
                keyboardWidth, keyboardHeight, callback);
        smileysPager.setAdapter(smileysAdapter);

        CirclePageIndicator pageIndicator = popupView.findViewById(R.id.circle_pager);
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
            case R.id.export_history_menu: {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.export_history)
                        .setMessage(R.string.export_history_text)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkStoragePermissions(REQUEST_HISTORY_EXPORT);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
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
                        MetricsManager.trackEvent("Clear history");
                    }
                });
                builder.setNegativeButton(R.string.do_not_clear, null);
                builder.show();
                return true;
            }
            case R.id.send_picture_menu: {
                checkStoragePermissions(REQUEST_PICK_GALLERY);
                return true;
            }
            case R.id.send_video_menu: {
                checkStoragePermissions(REQUEST_PICK_VIDEO);
                return true;
            }
            case R.id.send_document_menu: {
                checkStoragePermissions(REQUEST_PICK_DOCUMENT);
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
        }
        chatHistoryAdapter = new ChatHistoryAdapter(ChatActivity.this, getLoaderManager(), buddyDbId, timeHelper);
        chatHistoryAdapter.setContentMessageClickListener(contentClickListener);
        chatHistoryAdapter.setSelectionModeListener(selectionModeListener);
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
        super.onActivityResult(requestCode, resultCode, data);
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
                            List<PhotoEntry> photoEntries = new ArrayList<>();
                            for (String key : bundle.keySet()) {
                                photoEntries.add((PhotoEntry) bundle.getSerializable(key));
                            }
                            scrollBottom();
                            TaskExecutor.getInstance().execute(new SendPhotosTask(this, buddyDbId, photoEntries));
                            MetricsManager.trackEvent("Send photos");
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
            MetricsManager.trackEvent("Send file");
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

    private void startTitleObservation(final int buddyDbId) {
        if (buddyObserver != null) {
            buddyObserver.stop();
        }
        buddyObserver = new ChatBuddyObserver(getContentResolver(), buddyDbId);
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
            // Ultra high-demand thread required.
            // But this thread way must be rewritten.
            new Thread(new ReadMessagesTask(this, buddyDbId,
                    firstMessageDbId, lastMessageDbId)).start();
        }
    }

    private void readVisibleMessages() {
        final int firstVisiblePosition = chatLayoutManager.findFirstVisibleItemPosition();
        final int lastVisiblePosition = chatLayoutManager.findLastVisibleItemPosition();
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
        chatList.post(new Runnable() {
            @Override
            public void run() {
                chatList.scrollToPosition(0);
                chatList.requestLayout();
            }
        });
    }

    private void applySharingData(SharingData sharingData) {
        if (sharingData != null && sharingData.isValid()) {
            if (sharingData.getUri() != null) {
                scrollBottom();
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
                // Process obtained Uris.
                List<UriFile> uriFiles = new ArrayList<>();
                for (Uri uri : sharingData.getUri()) {
                    try {
                        uriFiles.add(UriFile.create(this, uri));
                    } catch (Throwable ignored) {
                    }
                }
                TaskExecutor.getInstance().execute(
                        new SendFileTask(this, buddyDbId, uriFiles, callback));
                MetricsManager.trackEvent("Send shared file");
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

    private void checkStoragePermissions(final int request) {
        final String PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (hasPermissions(this, PERMISSION)) {
            onPermissionGranted(request);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION)) {
                // Show an explanation to the user
                new AlertDialog.Builder(ChatActivity.this)
                        .setTitle(R.string.permission_request_title)
                        .setMessage(getPermissionsRequestMessage(request))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(
                                        ChatActivity.this,
                                        new String[]{PERMISSION},
                                        request
                                );
                            }
                        })
                        .show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{PERMISSION}, request);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted(requestCode);
        } else {
            Snackbar.make(chatList, getPermissionsRequestMessage(requestCode), Snackbar.LENGTH_LONG).show();
        }
    }

    private void onPermissionGranted(int request) {
        switch (request) {
            case REQUEST_PICK_GALLERY: {
                startActivityForResult(new Intent(this, PhotoPickerActivity.class), PICK_GALLERY_RESULT_CODE);
                break;
            }
            case REQUEST_PICK_VIDEO: {
                try {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("video/*");
                    startActivityForResult(photoPickerIntent, PICK_FILE_RESULT_CODE);
                } catch (Throwable ignored) {
                    // No video picker application.
                    Toast.makeText(this, R.string.no_video_picker, Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case REQUEST_PICK_DOCUMENT: {
                startActivityForResult(new Intent(this, DocumentPickerActivity.class), PICK_FILE_RESULT_CODE);
                break;
            }
            case REQUEST_CLICK_INCOMING: {
                ChatHistoryItem historyItem = this.clickedContentItem;
                onIncomingClicked(historyItem.getContentState(), historyItem.getContentTag(),
                        historyItem.getContentUri(), historyItem.getContentName(),
                        historyItem.getPreviewHash(), historyItem.getMessageCookie());
                break;
            }
            case REQUEST_CLICK_OUTGOING: {
                ChatHistoryItem historyItem = this.clickedContentItem;
                onOutgoingClicked(historyItem.getContentState(), historyItem.getContentTag(),
                        historyItem.getContentUri(), historyItem.getContentName(),
                        historyItem.getPreviewHash(), historyItem.getMessageCookie());
                break;
            }
            case REQUEST_HISTORY_EXPORT: {
                ExportHistoryTask exportHistoryTask = new ExportHistoryTask(
                        this,
                        timeHelper,
                        chatHistoryAdapter.getBuddyDbId()
                );
                TaskExecutor.getInstance().execute(exportHistoryTask);
                MetricsManager.trackEvent("Export history");
                break;
            }
        }
    }

    @StringRes
    private static int getPermissionsRequestMessage(int request) {
        switch (request) {
            case REQUEST_PICK_GALLERY:
            case REQUEST_PICK_VIDEO:
            case REQUEST_PICK_DOCUMENT:
            case REQUEST_CLICK_INCOMING:
            case REQUEST_CLICK_OUTGOING:
                return R.string.share_files_permission_request_message;
            case REQUEST_HISTORY_EXPORT:
                return R.string.history_export_permission_request_message;
        }
        throw new IllegalArgumentException("No message for request type: " + request);
    }

    private void onIncomingClicked(int contentState, String contentTag, String contentUri,
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

    private void onOutgoingClicked(int contentState, String contentTag, String contentUri,
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
        if (FileHelper.getMimeType(contentName).startsWith("image")) {
            Intent intent = new Intent(this, PhotoViewerActivity.class);
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

        MultiChoiceActionCallback(SelectionHelper<Long> selectionHelper) {
            this.selectionHelper = selectionHelper;
        }

        void onItemCheckedStateChanged(ActionMode mode, long id) {
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
            selectionHelper.setSelectionMode(false);
            chatHistoryAdapter.notifyDataSetChanged();
        }

        private String getSelectedMessages() {
            // Obtain selected positions.
            Collection<Long> selectedIds = selectionHelper.getSelectedIds();
            return QueryHelper.getMessagesTexts(getContentResolver(),
                    chatHistoryAdapter.getTimeHelper(), selectedIds).trim();
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
            final Collection<Long> selectedIds = new ArrayList<>(selectionHelper.getSelectedIds());
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

    private class ChatScrollListener extends RecyclerView.OnScrollListener {

        private LinearLayoutManager layoutManager;
        private int startFirstVisiblePosition, startLastVisiblePosition;

        private ChatScrollListener(LinearLayoutManager layoutManager) {
            this.layoutManager = layoutManager;
            startFirstVisiblePosition = -1;
            startLastVisiblePosition = -1;
        }

        @Override
        public void onScrollStateChanged(RecyclerView view, int scrollState) {
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            switch (scrollState) {
                case RecyclerView.SCROLL_STATE_DRAGGING: {
                    // Scroll stared.
                    if (startFirstVisiblePosition == -1 && startLastVisiblePosition == -1) {
                        startFirstVisiblePosition = firstVisiblePosition;
                        startLastVisiblePosition = lastVisiblePosition;
                    }
                    break;
                }
                case RecyclerView.SCROLL_STATE_IDLE: {
                    // Scroll ended.
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
                    startFirstVisiblePosition = -1;
                    startLastVisiblePosition = -1;
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
    }

    private class ReadMessagesTask extends WeakObjectTask<Context> {

        private final int buddyDbId;
        private final long firstMessageDbId;
        private final long lastMessageDbId;

        ReadMessagesTask(Context context, int buddyDbId,
                         long firstMessageDbId, long lastMessageDbId) {
            super(context);
            this.buddyDbId = buddyDbId;
            this.firstMessageDbId = Math.min(firstMessageDbId, lastMessageDbId);
            this.lastMessageDbId = Math.max(firstMessageDbId, lastMessageDbId);
        }

        @Override
        public void executeBackground() throws MessageNotFoundException {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                if (PreferenceHelper.isQuiteChat(context)) {
                    QueryHelper.fastReadMessages(contentResolver,
                            buddyDbId, firstMessageDbId, lastMessageDbId);
                }
                QueryHelper.readMessages(contentResolver,
                        buddyDbId, firstMessageDbId, lastMessageDbId);
            }
        }
    }

    private class ClearHistoryTask extends PleaseWaitTask {

        private final int buddyDbId;

        ClearHistoryTask(Context context, int buddyDbId) {
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

    private class ExportHistoryTask extends PleaseWaitTask {

        private final TimeHelper timeHelper;
        private final int buddyDbId;
        private String infoExportPath = null;

        ExportHistoryTask(Context context, TimeHelper timeHelper, int buddyDbId) {
            super(context);
            this.timeHelper = timeHelper;
            this.buddyDbId = buddyDbId;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                if (contentResolver != null) {
                    String buddyId = QueryHelper.getBuddyId(contentResolver, buddyDbId);
                    QueryBuilder queryBuilder = new QueryBuilder()
                            .columnEquals(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId)
                            .ascending(GlobalProvider.ROW_AUTO_ID);
                    String type;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        type = Environment.DIRECTORY_DOCUMENTS;
                    } else {
                        type = Environment.DIRECTORY_DOWNLOADS;
                    }
                    File directory = Environment.getExternalStoragePublicDirectory(type);
                    String fileName = "history_" + buddyId + ".txt";
                    infoExportPath = directory.getName() + "/" + fileName;
                    File file = new File(directory, fileName);
                    file.getParentFile().mkdirs();
                    OutputStream outputStream = null;
                    try {
                        file.delete();
                        file.createNewFile();
                        outputStream = new FileOutputStream(file);
                        PrintWriter writer = new PrintWriter(outputStream);
                        QueryHelper.outputMessagesTexts(contentResolver, timeHelper, queryBuilder, writer);
                        writer.flush();
                    } finally {
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onSuccessMain() {
            Context context = getWeakObject();
            if (context != null) {
                String text = context.getString(R.string.history_exported);
                Spannable s = new SpannableString(text + infoExportPath);
                s.setSpan(new TypefaceSpan("monospace"), text.length(), s.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(s);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        }

        @Override
        public void onFailMain() {
            Context context = getWeakObject();
            if (context != null) {
                Toast.makeText(context, R.string.export_history_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class SendMessageTask extends WeakObjectTask<ChiefActivity> {

        private final int buddyDbId;
        private String message;
        private final MessageCallback callback;

        SendMessageTask(ChiefActivity activity, int buddyDbId, String message, MessageCallback callback) {
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

        SendPhotosTask(Context context, int buddyDbId, List<PhotoEntry> photoEntries) {
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
        private List<UriFile> uriFiles;
        private final MessageCallback callback;
        private Random random;

        SendFileTask(ChiefActivity activity, int buddyDbId, UriFile uriFile, MessageCallback callback) {
            this(activity, buddyDbId, Collections.singletonList(uriFile), callback);
        }

        SendFileTask(ChiefActivity activity, int buddyDbId, List<UriFile> uriFiles, MessageCallback callback) {
            super(activity);
            this.buddyDbId = buddyDbId;
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
                    QueryHelper.insertOutgoingFileMessage(contentResolver, buddyDbId, cookie, uriFile.getUri(),
                            uriFile.getName(), contentType, size, hash, tag);
                    // Sending protocol message request.
                    RequestHelper.requestFileSend(contentResolver, buddyDbId, cookie, tag, uriFile);
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

    private class SendTypingTask extends WeakObjectTask<ChiefActivity> {

        private final int buddyDbId;
        private boolean isTyping;

        SendTypingTask(ChiefActivity activity, int buddyDbId, boolean isTyping) {
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

    abstract class MessageCallback {

        public abstract void onSuccess();

        public abstract void onFailed();
    }

    private class ContentClickListener implements ChatHistoryAdapter.ContentMessageClickListener {

        @Override
        public void onClicked(ChatHistoryItem historyItem) {
            switch (historyItem.getMessageType()) {
                case GlobalProvider.HISTORY_MESSAGE_TYPE_INCOMING: {
                    ChatActivity.this.clickedContentItem = historyItem;
                    checkStoragePermissions(REQUEST_CLICK_INCOMING);
                    break;
                }
                case GlobalProvider.HISTORY_MESSAGE_TYPE_OUTGOING: {
                    ChatActivity.this.clickedContentItem = historyItem;
                    checkStoragePermissions(REQUEST_CLICK_OUTGOING);
                    break;
                }
            }
        }
    }

    private static class StopDownloadingTask extends ServiceTask<ChiefActivity> {

        private String contentTag;
        private String messageCookie;

        StopDownloadingTask(ChiefActivity object, String contentTag, String messageCookie) {
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

    private static class StopUploadingTask extends ServiceTask<ChiefActivity> {

        private String contentTag;
        private String messageCookie;

        StopUploadingTask(ChiefActivity object, String contentTag, String messageCookie) {
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

    private class ChatBuddyObserver extends BuddyObserver {

        ChatBuddyObserver(ContentResolver contentResolver, int buddyDbId) {
            super(contentResolver, buddyDbId);
        }

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
                    subtitle = buddyCursor.getBuddyStatusTitle();
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
