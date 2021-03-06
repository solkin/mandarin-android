package com.tomclaw.mandarin.main;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.tomclaw.helpers.Strings;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.RequestHelper;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.Buddy;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.util.Logger;

import com.tomclaw.mandarin.util.MetricsManager;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 4/5/13
 * Time: 7:57 PM
 */
public class BuddyInfoActivity extends AbstractInfoActivity {

    private ViewSwitcher buttonSwitcher;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.buddy_info_menu, menu);
        menu.findItem(R.id.buddy_ignore).setTitle(isIgnored() ?
                R.string.buddy_unignore : R.string.buddy_ignore);
        prepareShareMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.buddy_info_share:
                return true;
            case R.id.buddy_info_copy:
                Strings.copyStringToClipboard(this, getShareString(), R.string.buddy_info_copied);
                return true;
            case R.id.buddy_ignore:
                boolean updatedIgnored = !isIgnored();
                RequestHelper.updateIgnoreBuddyState(getContentResolver(),
                        getAccountDbId(), getBuddyId(), updatedIgnored);
                setIgnored(updatedIgnored);
                invalidateOptionsMenu();
                refreshBuddyInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.log("BuddyInfoActivity onCreate");

        // Preparing for action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(R.string.buddy_info);
        }
        buttonSwitcher = findViewById(R.id.button_switcher);
        View addBuddyButton = findViewById(R.id.add_buddy_button);
        View openDialogButton = findViewById(R.id.open_dialog_button);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            FrameLayout.LayoutParams p;
            p = (FrameLayout.LayoutParams) addBuddyButton.getLayoutParams();
            p.setMargins(0, 0, 0, 0);
            addBuddyButton.setLayoutParams(p);
            p = (FrameLayout.LayoutParams) openDialogButton.getLayoutParams();
            p.setMargins(0, 0, 0, 0);
            openDialogButton.setLayoutParams(p);
        }

        addBuddyButton.setOnClickListener(v -> addBuddy());
        openDialogButton.setOnClickListener(v -> openDialog());

        BuddyCursor buddyCursor = null;
        try {
            DatabaseLayer databaseLayer = ContentResolverLayer.from(getContentResolver());
            buddyCursor = QueryHelper.getRosterBuddyCursor(databaseLayer, getAccountDbId(), getBuddyId());
            buttonSwitcher.setAnimateFirstView(false);
            buttonSwitcher.showNext();
        } catch (BuddyNotFoundException ignored) {
            // No buddy? Button will be ready to add buddy.
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
        MetricsManager.trackEvent("Open buddy info");
    }

    @Override
    protected int getLayout() {
        return R.layout.buddy_info_activity;
    }

    @Override
    protected int getDefaultAvatar() {
        return R.drawable.def_avatar;
    }

    @Override
    public void setIgnored(boolean ignored) {
        super.setIgnored(ignored);
        invalidateOptionsMenu();
    }

    public void onBuddyInfoRequestError() {
        Toast.makeText(this, R.string.error_show_buddy_info, Toast.LENGTH_SHORT).show();
    }

    private void addBuddy() {
        AddBuddyTask addBuddyTask = new AddBuddyTask(this, getAccountDbId(),
                getBuddyId(), getBuddyName(), getAvatarHash());
        TaskExecutor.getInstance().execute(addBuddyTask);
    }

    private void openDialog() {
        Buddy buddy = new Buddy(getAccountDbId(), getBuddyId());
        TaskExecutor.getInstance().execute(new OpenDialogTask(this, buddy));
    }

    private static class AddBuddyTask extends WeakObjectTask<BuddyInfoActivity> {

        private int accountDbId;
        private String buddyId;
        private String buddyNick;
        private String avatarHash;

        private AddBuddyTask(BuddyInfoActivity context, int accountDbId, String buddyId,
                             String buddyNick, String avatarHash) {
            super(context);
            this.accountDbId = accountDbId;
            this.buddyId = buddyId;
            this.buddyNick = buddyNick;
            this.avatarHash = avatarHash;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                // Default adding attributes. May be corrected.
                String accountType = QueryHelper.getAccountType(databaseLayer, accountDbId);
                long updateTime = System.currentTimeMillis();
                String groupName = context.getString(R.string.defaultGroupName);
                int groupId = 0;
                String authorizationMsg = context.getString(R.string.authorizationMsg);
                // Buddy adding procedure.
                QueryHelper.updateOrCreateGroup(databaseLayer, accountDbId, updateTime, groupName, groupId);
                QueryHelper.replaceOrCreateBuddy(databaseLayer, accountDbId, accountType, updateTime,
                        groupId, groupName, buddyId, buddyNick, avatarHash);
                RequestHelper.requestAdd(contentResolver, accountDbId, buddyId, groupName, authorizationMsg);
            }
        }

        @Override
        public void onSuccessMain() {
            BuddyInfoActivity activity = getWeakObject();
            if (activity != null) {
                activity.buttonSwitcher.showNext();
            }
        }
    }

    private static class OpenDialogTask extends WeakObjectTask<BuddyInfoActivity> {

        private Buddy buddy;

        OpenDialogTask(BuddyInfoActivity object, Buddy buddy) {
            super(object);
            this.buddy = buddy;
        }

        @Override
        public void executeBackground() throws Throwable {
            Context context = getWeakObject();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                DatabaseLayer databaseLayer = ContentResolverLayer.from(contentResolver);
                // Trying to open dialog with this buddy.
                QueryHelper.modifyDialog(databaseLayer, buddy, true);
            }
        }

        @Override
        public void onSuccessMain() {
            Context context = getWeakObject();
            if (context != null) {
                Intent intent = new Intent(context, ChatActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Buddy.KEY_STRUCT, buddy);
                context.startActivity(intent);
            }
        }

        @Override
        public void onFailMain() {
            BuddyInfoActivity activity = getWeakObject();
            if (activity != null) {
                Toast.makeText(activity, R.string.no_buddy_in_roster, Toast.LENGTH_SHORT).show();
                activity.buttonSwitcher.showPrevious();
            }
        }
    }
}
