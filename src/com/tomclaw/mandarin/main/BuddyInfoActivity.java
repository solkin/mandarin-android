package com.tomclaw.mandarin.main;

import android.support.v7.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.*;
import com.tomclaw.mandarin.core.exceptions.BuddyNotFoundException;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.util.Logger;
import com.tomclaw.mandarin.util.StringUtil;

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.buddy_info_share:
                startActivity(createShareIntent());
                return true;
            case R.id.buddy_info_copy:
                StringUtil.copyStringToClipboard(this, getShareString(), R.string.buddy_info_copied);
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
        buttonSwitcher = (ViewSwitcher) findViewById(R.id.button_switcher);
        View addBuddyButton = findViewById(R.id.add_buddy_button);
        View openDialogButton = findViewById(R.id.open_dialog_button);

        addBuddyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBuddy();
            }
        });
        openDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });

        BuddyCursor buddyCursor = null;
        try {
            buddyCursor = QueryHelper.getRosterBuddyCursor(getContentResolver(), getAccountDbId(), getBuddyId());
            buttonSwitcher.setAnimateFirstView(false);
            buttonSwitcher.showNext();
        } catch (BuddyNotFoundException ignored) {
            // No buddy? Button will be ready to add buddy.
        } finally {
            if (buddyCursor != null) {
                buddyCursor.close();
            }
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.buddy_info_activity;
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
        TaskExecutor.getInstance().execute(new OpenDialogTask(this));
    }

    private String getShareString() {
        String shareString = "";
        // Checking and attaching first and last name.
        shareString = StringUtil.appendIfNotEmpty(shareString, getFirstName(), "");
        shareString = StringUtil.appendIfNotEmpty(shareString, getLastName(), " ");
        // Strong checking for buddy nick.
        if (!(TextUtils.isEmpty(getBuddyNick()) || TextUtils.equals(getBuddyNick(), getBuddyId())) &&
                !TextUtils.equals(shareString, getBuddyNick())) {
            String buddyNick = getBuddyNick();
            if (!TextUtils.isEmpty(shareString)) {
                buddyNick = "(" + buddyNick + ")";
            }
            shareString = StringUtil.appendIfNotEmpty(shareString, buddyNick, " ");
        }
        // Appending user id.
        shareString = StringUtil.appendIfNotEmpty(shareString, getBuddyId(), " - ");
        return shareString;
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareString());
        return Intent.createChooser(shareIntent, getString(R.string.share_buddy_info_via));
    }

    private class AddBuddyTask extends WeakObjectTask<Context> {

        private int accountDbId;
        private String buddyId;
        private String buddyNick;
        private String avatarHash;

        private AddBuddyTask(Context context, int accountDbId, String buddyId,
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
                // Default adding attributes. May be corrected.
                String accountType = QueryHelper.getAccountType(contentResolver, accountDbId);
                long updateTime = System.currentTimeMillis();
                String groupName = context.getString(R.string.defaultGroupName);
                int groupId = 0;
                String authorizationMsg = context.getString(R.string.authorizationMsg);
                // Buddy adding procedure.
                QueryHelper.updateOrCreateGroup(contentResolver, accountDbId, updateTime, groupName, groupId);
                QueryHelper.replaceOrCreateBuddy(contentResolver, accountDbId, accountType, updateTime,
                        groupId, groupName, buddyId, buddyNick, avatarHash);
                RequestHelper.requestAdd(contentResolver, accountDbId, buddyId, groupName, authorizationMsg);
            }
        }

        @Override
        public void onSuccessMain() {
            buttonSwitcher.showNext();
        }
    }

    private class OpenDialogTask extends WeakObjectTask<Context> {

        private int buddyDbId;

        public OpenDialogTask(Context object) {
            super(object);
        }

        @Override
        public void executeBackground() throws Throwable {
            buddyDbId = QueryHelper.getBuddyDbId(getContentResolver(), getAccountDbId(), getBuddyId());
            // Trying to open dialog with this buddy.
            QueryHelper.modifyDialog(getContentResolver(), buddyDbId, true);
        }

        @Override
        public void onSuccessMain() {
            Context context = getWeakObject();
            if (context != null) {
                Intent intent = new Intent(context, ChatActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
                startActivity(intent);
            }
        }

        @Override
        public void onFailMain() {
            Context context = getWeakObject();
            if (context != null) {
                Toast.makeText(context, R.string.no_buddy_in_roster, Toast.LENGTH_SHORT).show();
                buttonSwitcher.showPrevious();
            }
        }
    }
}
