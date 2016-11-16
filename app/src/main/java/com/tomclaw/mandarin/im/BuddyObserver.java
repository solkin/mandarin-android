package com.tomclaw.mandarin.im;

import android.content.ContentResolver;
import android.database.ContentObserver;

import com.tomclaw.mandarin.core.ContentResolverLayer;
import com.tomclaw.mandarin.core.DatabaseLayer;
import com.tomclaw.mandarin.core.MainExecutor;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.util.Logger;

/**
 * Created by solkin on 05/05/14.
 */
public abstract class BuddyObserver extends ContentObserver {

    private final ContentResolver contentResolver;
    private final DatabaseLayer databaseLayer;
    private final Buddy buddy;

    public BuddyObserver(ContentResolver contentResolver, Buddy buddy) {
        super(null);

        this.contentResolver = contentResolver;
        this.databaseLayer = ContentResolverLayer.from(contentResolver);
        this.buddy = buddy;

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
                    buddyCursor = QueryHelper.getBuddyCursor(databaseLayer,
                            buddy.getAccountDbId(), buddy.getBuddyId());
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
