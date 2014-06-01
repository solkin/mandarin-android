package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import com.tomclaw.mandarin.im.BuddyCursor;
import com.tomclaw.mandarin.util.QueryBuilder;

/**
 * Created by solkin on 05/05/14.
 */
public abstract class BuddyObserver extends ContentObserver {

    private ContentResolver contentResolver;

    private QueryBuilder queryBuilder;

    public BuddyObserver(ContentResolver contentResolver, int buddyDbId) {
        super(null);

        this.contentResolver = contentResolver;

        queryBuilder = new QueryBuilder().columnEquals(GlobalProvider.ROW_AUTO_ID, buddyDbId);
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
        Cursor buddyCursor = queryBuilder.query(contentResolver, Settings.BUDDY_RESOLVER_URI);
        if (buddyCursor.moveToFirst()) {
            onBuddyInfoChanged(new BuddyCursor(buddyCursor));
            buddyCursor.close();
        }
    }

    public abstract void onBuddyInfoChanged(BuddyCursor buddyCursor);

    public void stop() {
        contentResolver.unregisterContentObserver(this);
    }
}
