package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.database.ContentObserver;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by solkin on 05/05/14.
 */
public abstract class BuddyObserver extends ContentObserver {

    private ContentResolver contentResolver;
    private int buddyDbId;

    public BuddyObserver(ContentResolver contentResolver, int buddyDbId) {
        super(null);

        this.contentResolver = contentResolver;
        this.buddyDbId = buddyDbId;

        observe();
    }

    public void touch() {
        onChange(true);
    }

    private void observe() {
        contentResolver.registerContentObserver(Settings.BUDDY_RESOLVER_URI, true, this);
    }

    @Override
    public void onChange(boolean selfChange) {
        MainExecutor.execute(new Runnable() {
            @Override
            public void run() {
                BuddyCursor buddyCursor = null;
                try {
                    buddyCursor = QueryHelper.getBuddyCursor(contentResolver, buddyDbId);
                    onBuddyInfoChanged(buddyCursor);
                } catch (Throwable ignored) {
                    // Sadly.
                    Logger.log("Unable to get buddy cursor in buddy observer", ignored);
                } finally {
                    if (buddyCursor != null) {
                        buddyCursor.close();
                    }
                }
            }
        });

    }

    public abstract void onBuddyInfoChanged(BuddyCursor buddyCursor);

    public void stop() {
        contentResolver.unregisterContentObserver(this);
    }
}
