package com.tomclaw.mandarin.core;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import com.tomclaw.mandarin.util.Logger;

import java.util.List;

/**
 * Created by ivsolkin on 22.11.16.
 * Task to execute methods from global provider
 */
public abstract class DatabaseTask extends WeakObjectTask<Context> {

    private final SQLiteDatabase sqLiteDatabase;
    private final Bundle bundle;

    public DatabaseTask(Context object, SQLiteDatabase sqLiteDatabase, Bundle bundle) {
        super(object);
        this.sqLiteDatabase = sqLiteDatabase;
        this.bundle = bundle;
    }

    public SQLiteDatabase getDatabase() {
        return sqLiteDatabase;
    }

    @Override
    public final void executeBackground() throws Throwable {
        Context context = getWeakObject();
        if (context != null) {
            long startTime = SystemClock.elapsedRealtime();
            DatabaseLayer databaseLayer = SQLiteDatabaseLayer.getInstance(sqLiteDatabase);
            try {
                sqLiteDatabase.beginTransaction();
                runInTransaction(context, databaseLayer, bundle);
                sqLiteDatabase.setTransactionSuccessful();
                notifyModifiedUris(context.getContentResolver());
            } finally {
                sqLiteDatabase.endTransaction();
                String description = getOperationDescription();
                long operationTime = SystemClock.elapsedRealtime() - startTime;
                Logger.log(description + " task took " + operationTime + " ms.");
            }
        }
    }

    protected abstract void runInTransaction(
            Context context,
            DatabaseLayer databaseLayer,
            Bundle bundle
    ) throws Throwable;

    protected abstract List<Uri> getModifiedUris();

    protected abstract String getOperationDescription();

    private void notifyModifiedUris(ContentResolver contentResolver) {
        List<Uri> uris = getModifiedUris();
        for (Uri uri : uris) {
            contentResolver.notifyChange(uri, null);
        }
    }
}
