package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityHelper;
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
public class ChatActivity extends ChiefActivity implements SlidingActivityBase {

    private CharSequence title;

    private ChatDialogsFragment chatDialogsFragment;
    private ChatFragment chatFragment;

    private SlidingActivityHelper slidingActivityHelper;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Логика SlideActivityBase завязана на slidingActivityHelper. Его нужно создать самым первым, так как он вызывается во всех переопределенных методах.
        slidingActivityHelper = new SlidingActivityHelper(this);

        super.onCreate(savedInstanceState);
        slidingActivityHelper.onCreate(savedInstanceState);

        setContentView(R.layout.content_frame);
        setBehindContentView(R.layout.menu_frame);

        getSlidingMenu().setSlidingEnabled(true);
        getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);

        handler = new Handler();
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
                //drawerToggle.onOptionsItemSelected(item);
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

        // Initialize action bar.
        ActionBar bar = getActionBar();
        bar.setTitle(R.string.dialogs);
        bar.setDisplayShowTitleEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        int buddyDbId = getIntentBuddyDbId(getIntent());

        setTitleByBuddyDbId(buddyDbId);

        chatFragment = new ChatFragment(this, buddyDbId);
        chatDialogsFragment = new ChatDialogsFragment(this);

        chatDialogsFragment.registerDataSetObserver(new DataSetObserver() {
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
        });

        chatDialogsFragment.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
                // Задержка для того, чтобы дать возможность перезагрузить контент в чате, иначе переключение будет с рывком
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getSlidingMenu().showContent();
                    }
                }, 50);

            }
        });

        getFragmentManager().beginTransaction().replace(R.id.menu_frame, chatDialogsFragment).commit();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, chatFragment).commit();

        // customize the SlidingMenu
        SlidingMenu sm = getSlidingMenu();
        sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        sm.setShadowWidthRes(R.dimen.shadow_width);
        sm.setShadowDrawable(R.drawable.shadow);
        sm.setBehindScrollScale(0.25f);
        sm.setFadeDegree(0.25f);

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
        chatFragment.selectItem(buddyDbId);
        String nick = chatDialogsFragment.getBuddyNick(position);
        setTitle(nick);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        slidingActivityHelper.onPostCreate(savedInstanceState);
    }

    @Override
    public View findViewById(int id) {
        View v = super.findViewById(id);
        if (v != null)
            return v;
        return slidingActivityHelper.findViewById(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        slidingActivityHelper.onSaveInstanceState(outState);
    }

    @Override
    public void setContentView(int id) {
        setContentView(getLayoutInflater().inflate(id, null));
    }

    /* (non-Javadoc)
     * @see android.app.Activity#setContentView(android.view.View)
     */
    @Override
    public void setContentView(View v) {
        setContentView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /* (non-Javadoc)
     * @see android.app.Activity#setContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    @Override
    public void setContentView(View v, ViewGroup.LayoutParams params) {
        super.setContentView(v, params);
        slidingActivityHelper.registerAboveContentView(v, params);
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#setBehindContentView(int)
     */
    public void setBehindContentView(int id) {
        setBehindContentView(getLayoutInflater().inflate(id, null));
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#setBehindContentView(android.view.View)
     */
    public void setBehindContentView(View v) {
        setBehindContentView(v, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#setBehindContentView(android.view.View, android.view.ViewGroup.LayoutParams)
     */
    public void setBehindContentView(View v, ViewGroup.LayoutParams params) {
        slidingActivityHelper.setBehindContentView(v, params);
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#getSlidingMenu()
     */
    public SlidingMenu getSlidingMenu() {
        return slidingActivityHelper.getSlidingMenu();
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#toggle()
     */
    public void toggle() {
        slidingActivityHelper.toggle();
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#showAbove()
     */
    public void showContent() {
        slidingActivityHelper.showContent();
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#showBehind()
     */
    public void showMenu() {
        slidingActivityHelper.showMenu();
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#showSecondaryMenu()
     */
    public void showSecondaryMenu() {
        slidingActivityHelper.showSecondaryMenu();
    }

    /* (non-Javadoc)
     * @see com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase#setSlidingActionBarEnabled(boolean)
     */
    public void setSlidingActionBarEnabled(boolean b) {
        slidingActivityHelper.setSlidingActionBarEnabled(b);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onKeyUp(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean b = slidingActivityHelper.onKeyUp(keyCode, event);
        if (b) return b;
        return super.onKeyUp(keyCode, event);
    }
}
