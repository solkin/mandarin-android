package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.core.exceptions.MessageNotFoundException;
import com.tomclaw.mandarin.main.fragments.ChatDialogsFragment;
import com.tomclaw.mandarin.main.fragments.ChatFragment;

/**
 * Created with IntelliJ IDEA.
 * User: solkin
 * Date: 5/5/13
 * Time: 11:49 PM
 */
public class ChatActivity extends ChiefActivity {

    private CharSequence title;

    private ChatDialogsFragment chatDialogsFragment;
    private ChatFragment chatFragment;

    private SlidingPaneLayout slidingPane;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent mainActivityIntent = new Intent(this, MainActivity.class);
                startActivity(mainActivityIntent);
                return true;
            }
            case R.id.close_chat_menu: {
                try {
                    QueryHelper.modifyDialog(getContentResolver(), chatFragment.getBuddyDbId(), false);
                } catch (Exception ignored) {
                    // Nothing to do in this case.
                }
                return true;
            }
            case R.id.clear_history_menu: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.clear_history_title);
                builder.setMessage(R.string.clear_history_text);
                builder.setPositiveButton(R.string.yes_clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            QueryHelper.clearHistory(getContentResolver(), chatFragment.getBuddyDbId());
                        } catch (Exception ignored) {
                            // Nothing to do in this case.
                        }
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(Settings.LOG_TAG, "onNewIntent");

        int buddyDbId = getIntentBuddyDbId(intent);

        setTitleByBuddyDbId(buddyDbId);

        chatFragment.selectItem(buddyDbId);
    }

    @Override
    public void onCoreServiceReady() {
        Log.d(Settings.LOG_TAG, "onCoreServiceReady");

        setContentView(R.layout.sliding_pane);

        slidingPane = (SlidingPaneLayout) findViewById(R.id.sliding_pane);

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        DataSetObserver dataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                // Checking for selection is invalid.
                if(chatDialogsFragment.getBuddyPosition(chatFragment.getBuddyDbId()) == -1) {
                    Log.d(Settings.LOG_TAG, "No selected item anymore.");
                    // Checking for another opened chat.
                    if(chatDialogsFragment.getCount() > 0) {
                        int moreActiveBuddyPosition;
                        try {
                            // Trying to obtain more active dialog position.
                            moreActiveBuddyPosition = chatDialogsFragment.getBuddyPosition(
                                    QueryHelper.getMoreActiveDialog(getContentResolver()));
                        } catch (BuddyNotFoundException ignored) {
                            // Something really strange. No opened dialogs. So switch to first position.
                            moreActiveBuddyPosition = 0;
                        } catch (MessageNotFoundException ignored) {
                            // No messages in all opened dialogs. So switch to first position.
                            moreActiveBuddyPosition = 0;
                        }
                        selectItem(moreActiveBuddyPosition);
                    } else {
                        // No more content on this activity.
                        finish();
                    }
                }
            }
        };

        AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
                slidingPane.closePane();
            }
        };

        int buddyDbId = getIntentBuddyDbId(getIntent());

        setTitleByBuddyDbId(buddyDbId);

        chatFragment = new ChatFragment(this, buddyDbId);
        chatDialogsFragment = new ChatDialogsFragment(onItemClickListener, dataSetObserver);

        getFragmentManager().beginTransaction().replace(R.id.left_pane, chatDialogsFragment).commit();
        getFragmentManager().beginTransaction().replace(R.id.right_pane, chatFragment).commit();
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
            setTitle(QueryHelper.getBuddyNick(getContentResolver(), buddyDbId));
        } catch (BuddyNotFoundException ignored) {
            Log.d(Settings.LOG_TAG, "No buddy fount by specified buddyDbId");
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title;
        getActionBar().setTitle(title);
    }

    public void selectItem(int position) {
        // Changing chat history adapter loader.
        int buddyDbId = chatDialogsFragment.getBuddyDbId(position);
        String nick = chatDialogsFragment.getBuddyNick(position);
        setTitle(nick);
        chatFragment.selectItem(buddyDbId);
    }
}
