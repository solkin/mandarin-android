package com.tomclaw.mandarin.util;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class MergeCursorLoader extends AsyncTaskLoader<MergeCursor> {
    final ForceLoadContentObserver mObserver;

    private QueryParametersContainer[] queryParameters;

    MergeCursor mMergeCursor;
    CancellationSignal mCancellationSignal;

    /* Runs on a worker thread */
    @Override
    public MergeCursor loadInBackground() {
        synchronized (this) {
            if (isLoadInBackgroundCanceled()) {
                throw new OperationCanceledException();
            }
            mCancellationSignal = new CancellationSignal();
        }
        try {
            ArrayList<Cursor> mergedCursors = new ArrayList<Cursor>();
            for (QueryParametersContainer container : queryParameters){
                Cursor cursor = getContext().getContentResolver().query(container.uri, container.projection,
                        container.selection, container.selectionArgs, container.sortOrder);
                if (cursor != null) {
                    try {
                        // Ensure the cursor window is filled.
                        cursor.getCount();
                        cursor.registerContentObserver(mObserver);
                    } catch (RuntimeException ex) {
                        cursor.close();
                        throw ex;
                    }
                }
                mergedCursors.add(cursor);
            }
            Cursor[] cursors = new Cursor[mergedCursors.size()];
            MergeCursor cursor = new MergeCursor(mergedCursors.toArray(cursors));
            return cursor;
        } finally {
            synchronized (this) {
                mCancellationSignal = null;
            }
        }
    }

    @Override
    public void cancelLoadInBackground() {
        super.cancelLoadInBackground();

        synchronized (this) {
            if (mCancellationSignal != null) {
                mCancellationSignal.cancel();
            }
        }
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(MergeCursor cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        MergeCursor oldCursor = mMergeCursor;
        mMergeCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    public MergeCursorLoader(Context context) {
        super(context);
        mObserver = new ForceLoadContentObserver();
    }

    public MergeCursorLoader(Context context, QueryParametersContainer[] containers) {
        super(context);
        mObserver = new ForceLoadContentObserver();
        queryParameters = containers;
    }

    public void setQueryParameters(QueryParametersContainer[] containers){
        queryParameters = containers;
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mMergeCursor != null) {
            deliverResult(mMergeCursor);
        }
        if (takeContentChanged() || mMergeCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(MergeCursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mMergeCursor != null && !mMergeCursor.isClosed()) {
            mMergeCursor.close();
        }
        mMergeCursor = null;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        for ( QueryParametersContainer container : queryParameters){
            dumpQueryParameterConteiner(prefix, fd, writer, args, container);
        }
    }

    private void dumpQueryParameterConteiner(String prefix, FileDescriptor fd, PrintWriter writer, String[] args, QueryParametersContainer container){
        writer.print(prefix); writer.print("mUri="); writer.println(container.uri);
        writer.print(prefix); writer.print("mProjection=");
        writer.println(Arrays.toString(container.projection));
        writer.print(prefix); writer.print("mSelection="); writer.println(container.selection);
        writer.print(prefix); writer.print("mSelectionArgs=");
        writer.println(Arrays.toString(container.selectionArgs));
        writer.print(prefix); writer.print("mSortOrder="); writer.println(container.sortOrder);
        writer.print(prefix); writer.print("mMergeCursor="); writer.println(mMergeCursor);
    }
}

