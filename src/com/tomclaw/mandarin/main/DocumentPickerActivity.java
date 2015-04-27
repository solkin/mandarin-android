package com.tomclaw.mandarin.main;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.util.FileHelper;
import com.tomclaw.mandarin.util.StringUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * File picker activity
 * Created by solkin on 05.11.14.
 */
public class DocumentPickerActivity extends AppCompatActivity {

    private static final int PICK_FILE_RESULT_CODE = 4;

    private ActionBar actionBar;

    private ListView listView;
    private ListAdapter listAdapter;
    private File currentDir;
    private TextView emptyView;
    private List<ListItem> items = new ArrayList<>();
    private boolean receiverRegistered = false;
    private List<HistoryEntry> history = new ArrayList<>();
    private long sizeLimit = 1024 * 1024 * 1024;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        if (currentDir == null) {
                            listRoots();
                        } else {
                            listFiles(currentDir);
                        }
                    } catch (Exception ignored) {
                    }
                }
            };
            if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
                listView.postDelayed(runnable, 1000);
            } else {
                runnable.run();
            }
        }
    };

    @Override
    public void onDestroy() {
        try {
            if (receiverRegistered) {
                unregisterReceiver(receiver);
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstance) {
        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        super.onCreate(savedInstance);

        if (!receiverRegistered) {
            receiverRegistered = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
            filter.addAction(Intent.ACTION_MEDIA_CHECKING);
            filter.addAction(Intent.ACTION_MEDIA_EJECT);
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_NOFS);
            filter.addAction(Intent.ACTION_MEDIA_REMOVED);
            filter.addAction(Intent.ACTION_MEDIA_SHARED);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
            filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            filter.addDataScheme("file");
            registerReceiver(receiver, filter);
        }

        setContentView(R.layout.document_pick_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.select_file));
        }

        listAdapter = new ListAdapter(this);
        emptyView = (TextView) findViewById(R.id.searchEmptyView);
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                return true;
            }
        });
        listView = (ListView) findViewById(R.id.listView);
        listView.setEmptyView(emptyView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ListItem item = items.get(position);
                File file = item.file;
                if (file == null) {
                    HistoryEntry entry = history.remove(history.size() - 1);
                    setActivityTitle(entry.title);
                    if (entry.dir != null) {
                        listFiles(entry.dir);
                    } else {
                        listRoots();
                    }
                    listView.setSelectionFromTop(entry.scrollItem, entry.scrollOffset);
                } else if (file.isDirectory()) {
                    HistoryEntry entry = new HistoryEntry();
                    entry.scrollItem = listView.getFirstVisiblePosition();
                    entry.scrollOffset = listView.getChildAt(0).getTop();
                    entry.dir = currentDir;
                    entry.title = getActivityTitle();
                    if (!listFiles(file)) {
                        return;
                    }
                    history.add(entry);
                    setActivityTitle(item.title);
                    listView.setSelection(0);
                } else {
                    if (!file.canRead()) {
                        showErrorBox(getString(R.string.access_error));
                        return;
                    }
                    if (sizeLimit != 0) {
                        if (file.length() > sizeLimit) {
                            showErrorBox(getString(R.string.file_upload_limit,
                                    StringUtil.formatBytes(getResources(), sizeLimit)));
                            return;
                        }
                    }
                    if (file.length() == 0) {
                        return;
                    }
                    Intent intent = new Intent(getIntent().getAction(), Uri.fromFile(file));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
        listRoots();
    }

    public void setActivityTitle(CharSequence title) {
        actionBar.setTitle(title);
    }

    public String getActivityTitle() {
        return actionBar.getTitle().toString();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.document_picker_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.system_picker_menu: {
                try {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("*/*");
                    startActivityForResult(photoPickerIntent, PICK_FILE_RESULT_CODE);
                } catch (Throwable ignored) {
                    // No such application?!
                }
                break;
            }
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (history.size() > 0) {
            HistoryEntry entry = history.remove(history.size() - 1);
            setActivityTitle(entry.title);
            if (entry.dir != null) {
                listFiles(entry.dir);
            } else {
                listRoots();
            }
            listView.setSelectionFromTop(entry.scrollItem, entry.scrollOffset);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_FILE_RESULT_CODE: {
                if (resultCode == RESULT_OK) {
                    setResult(resultCode, data);
                    finish();
                }
            }
        }
    }

    @SuppressLint("SdCardPath")
    private boolean listFiles(File dir) {
        if (!dir.canRead()) {
            if (dir.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().toString())
                    || dir.getAbsolutePath().startsWith("/sdcard")
                    || dir.getAbsolutePath().startsWith("/mnt/sdcard")) {
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                        && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    currentDir = dir;
                    items.clear();
                    String state = Environment.getExternalStorageState();
                    if (Environment.MEDIA_SHARED.equals(state)) {
                        emptyView.setText(getString(R.string.usb_active));
                    } else {
                        emptyView.setText(getString(R.string.not_mounted));
                    }
                    listAdapter.notifyDataSetChanged();
                    return true;
                }
            }
            showErrorBox(getString(R.string.access_error));
            return false;
        }
        emptyView.setText(getString(R.string.no_files));
        File[] files;
        try {
            files = dir.listFiles();
        } catch (Exception e) {
            showErrorBox(e.getLocalizedMessage());
            return false;
        }
        if (files == null) {
            showErrorBox(getString(R.string.unknown_error));
            return false;
        }
        currentDir = dir;
        items.clear();
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() != rhs.isDirectory()) {
                    return lhs.isDirectory() ? -1 : 1;
                }
                return lhs.getName().compareToIgnoreCase(rhs.getName());
            }
        });
        for (File file : files) {
            if (file.getName().startsWith(".")) {
                continue;
            }
            ListItem item = new ListItem();
            item.title = file.getName();
            item.file = file;
            if (file.isDirectory()) {
                item.icon = R.drawable.files_folder;
            } else {
                item.subtitle = StringUtil.formatBytes(getResources(), file.length());
                String mimeType = FileHelper.getMimeType(file.getName());
                item.icon = FileHelper.getMimeTypeResPicture(mimeType);
            }
            items.add(item);
        }
        ListItem item = new ListItem();
        item.title = "..";
        item.subtitle = "";
        item.icon = R.drawable.files_folder_open;
        item.file = null;
        items.add(0, item);
        listAdapter.notifyDataSetChanged();
        return true;
    }

    private void showErrorBox(String error) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(error)
                .setPositiveButton(R.string.got_it, null)
                .show();
    }

    @SuppressLint("SdCardPath")
    private void listRoots() {
        currentDir = null;
        items.clear();
        String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        ListItem ext = new ListItem();
        if (Build.VERSION.SDK_INT < 9 || Environment.isExternalStorageRemovable()) {
            ext.title = getString(R.string.sd_card);
        } else {
            ext.title = getString(R.string.internal_storage);
        }
        ext.icon = Build.VERSION.SDK_INT < 9 || Environment.isExternalStorageRemovable() ?
                R.drawable.files_sd_storage : R.drawable.files_storage;
        ext.subtitle = getRootSubtitle(extStorage);
        ext.file = Environment.getExternalStorageDirectory();
        items.add(ext);
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            HashMap<String, ArrayList<String>> aliases = new HashMap<>();
            ArrayList<String> result = new ArrayList<>();
            String extDevice = null;
            while ((line = reader.readLine()) != null) {
                if ((!line.contains("/mnt") && !line.contains("/storage") && !line.contains("/sdcard")) || line.contains("asec") || line.contains("tmpfs") || line.contains("none")) {
                    continue;
                }
                String[] info = line.split(" ");
                if (!aliases.containsKey(info[0])) {
                    aliases.put(info[0], new ArrayList<String>());
                }
                aliases.get(info[0]).add(info[1]);
                if (info[1].equals(extStorage)) {
                    extDevice = info[0];
                }
                result.add(info[1]);
            }
            reader.close();
            if (extDevice != null) {
                result.removeAll(aliases.get(extDevice));
                for (String path : result) {
                    try {
                        ListItem item = new ListItem();
                        if (path.toLowerCase().contains("sd")) {
                            item.title = getString(R.string.sd_card);
                        } else {
                            item.title = getString(R.string.external_storage);
                        }
                        item.icon = R.drawable.files_sd_storage;
                        item.subtitle = getRootSubtitle(path);
                        item.file = new File(path);
                        items.add(item);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        ListItem fs = new ListItem();
        fs.title = "/";
        fs.subtitle = getString(R.string.system_root);
        fs.icon = R.drawable.files_folder;
        fs.file = new File("/");
        items.add(fs);

        addPublicDirectory(Environment.DIRECTORY_PICTURES);
        addPublicDirectory(Environment.DIRECTORY_MUSIC);
        addPublicDirectory(Environment.DIRECTORY_MOVIES);
        addPublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        listAdapter.notifyDataSetChanged();
    }

    private void addPublicDirectory(String directoryType) {
        try {
            File publicDirectory = Environment.getExternalStoragePublicDirectory(directoryType);
            if (publicDirectory.exists()) {
                ListItem fs = new ListItem();
                fs.title = publicDirectory.getName();
                fs.icon = R.drawable.files_folder;
                fs.file = publicDirectory;
                items.add(fs);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private String getRootSubtitle(String path) {
        StatFs stat = new StatFs(path);
        long total, free;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            total = stat.getTotalBytes();
            free = stat.getAvailableBytes();
        } else {
            total = (long) stat.getBlockCount() * (long) stat.getBlockSize();
            free = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        if (total == 0) {
            return "";
        }
        return getString(R.string.free_of_total, StringUtil.formatBytes(getResources(), free), StringUtil.formatBytes(getResources(), total));
    }

    private class ListAdapter extends BaseAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public int getViewTypeCount() {
            return 2;
        }

        public int getItemViewType(int pos) {
            return items.get(pos).subtitle.length() > 0 ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ListItem item = items.get(position);
            if (view == null) {
                view = View.inflate(mContext, R.layout.document_item, null);
                if (item.subtitle.length() == 0) {
                    view.findViewById(R.id.docs_item_info).setVisibility(View.GONE);
                }
            }
            ((TextView) view.findViewById(R.id.docs_item_title)).setText(item.title);
            ((TextView) view.findViewById(R.id.docs_item_info)).setText(item.subtitle);
            ImageView imageView = (ImageView) view.findViewById(R.id.docs_item_thumb);
            imageView.setImageResource(item.icon);
            imageView.setVisibility(View.VISIBLE);
            return view;
        }
    }

    private class ListItem {
        int icon;
        String title;
        String subtitle = "";
        File file;
    }

    private class HistoryEntry {
        int scrollItem, scrollOffset;
        File dir;
        String title;
    }
}
