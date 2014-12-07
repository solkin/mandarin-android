package com.tomclaw.mandarin.main;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.*;
import android.widget.*;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.BitmapCache;
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.views.LazyImageView;
import com.tomclaw.mandarin.main.views.ThumbnailView;
import com.tomclaw.mandarin.util.FileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Solkin on 04.11.2014.
 */
public class PhotoPickerActivity extends Activity {

    private static final int PICK_IMAGE_RESULT_CODE = 4;
    public static final String SELECTED_ENTRIES = "selected_entries";

    private static final int PHOTO_VIEW_RESULT_CODE = 5;

    private static final String[] projectionPhotos = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION
    };

    private ArrayList<AlbumEntry> albums;
    private View doneButton;
    private TextView doneButtonTextView;
    private TextView doneButtonBadgeTextView;
    private GridView mediaGrid;
    private AlbumEntry selectedAlbum = null;
    private int itemWidth, itemHeight;
    private HashMap<Integer, PhotoEntry> selectedPhotos = new HashMap<Integer, PhotoEntry>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(PreferenceHelper.isDarkTheme(this) ?
                R.style.Theme_Mandarin_Dark : R.style.Theme_Mandarin_Light);

        setContentView(R.layout.photo_picker_activity);

        // Preparing for action bar.
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            bar.setTitle(R.string.gallery);
            bar.setIcon(R.drawable.ic_ab_logo);
        }

        albums = loadGalleryPhotosAlbums();

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendSelectedPhotos();
            }
        });

        cancelButton.setText(getString(R.string.cancel).toUpperCase());
        doneButtonTextView = (TextView) doneButton.findViewById(R.id.done_button_text);
        doneButtonTextView.setText(getString(R.string.send).toUpperCase());
        doneButtonBadgeTextView = (TextView) doneButton.findViewById(R.id.done_button_badge);

        mediaGrid = (GridView) findViewById(R.id.media_grid);
        mediaGrid.setAdapter(new AlbumsAdapter(this, albums));
        mediaGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectedAlbum == null) {
                    if (i < 0 || i >= albums.size()) {
                        return;
                    }
                    selectedAlbum = albums.get(i);
                    getActionBar().setTitle(selectedAlbum.bucketName);
                    mediaGrid.setAdapter(new PhotosAdapter(getBaseContext(), selectedAlbum));
                    fixLayoutInternal();
                } else {
                    if (i < 0 || i >= selectedAlbum.photos.size()) {
                        return;
                    }
                    PhotoEntry photoEntity = selectedAlbum.photos.get(i);
                    File photoFile = new File(photoEntity.path);
                    Uri uri = Uri.fromFile(photoFile);
                    if(TextUtils.equals(FileHelper.getFileExtensionFromPath(photoEntity.path), "gif")) {
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, FileHelper.getMimeType(photoEntity.path));
                        startActivity(intent);
                    } else {
                        int selectedCount = selectedPhotos.size();
                        if(!selectedPhotos.containsKey(photoEntity.imageId)) {
                            selectedCount ++;
                        }
                        Intent intent = new Intent(PhotoPickerActivity.this, PhotoViewerActivity.class);
                        intent.putExtra(PhotoViewerActivity.EXTRA_PICTURE_NAME, photoFile.getName());
                        intent.putExtra(PhotoViewerActivity.EXTRA_PICTURE_URI, uri.toString());
                        intent.putExtra(PhotoViewerActivity.EXTRA_PREVIEW_HASH, photoEntity.hash);
                        intent.putExtra(PhotoViewerActivity.EXTRA_SELECTED_COUNT, selectedCount);
                        intent.putExtra(PhotoViewerActivity.EXTRA_PHOTO_ENTRY, photoEntity);
                        startActivityForResult(intent, PHOTO_VIEW_RESULT_CODE);
                    }
                }
            }
        });

        fixLayoutInternal();
        updateSelectedCount();

        Log.d(Settings.LOG_TAG, "albums: " + albums.size());
    }

    private void sendSelectedPhotos() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        for (PhotoEntry photoEntry : selectedPhotos.values()) {
            bundle.putSerializable(photoEntry.path, photoEntry);
        }
        intent.putExtra(SELECTED_ENTRIES, bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.photo_picker_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
            case R.id.system_picker_menu: {
                try {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, PICK_IMAGE_RESULT_CODE);
                } catch (Throwable ignored) {
                    // No such application?!
                }
                break;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (selectedAlbum != null) {
            selectedAlbum = null;
            mediaGrid.setAdapter(new AlbumsAdapter(this, albums));
            getActionBar().setTitle(getString(R.string.gallery));
            fixLayoutInternal();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_IMAGE_RESULT_CODE: {
                if (resultCode == RESULT_OK) {
                    setResult(resultCode, data);
                    finish();
                }
                break;
            }
            case PHOTO_VIEW_RESULT_CODE: {
                if (resultCode == RESULT_OK) {
                    PhotoEntry photoEntry = (PhotoEntry) data.getSerializableExtra(
                            PhotoViewerActivity.SELECTED_PHOTO_ENTRY);
                    if(photoEntry != null) {
                        selectedPhotos.put(photoEntry.imageId, photoEntry);
                        sendSelectedPhotos();
                    }
                }
                break;
            }
        }
    }

    private void fixLayoutInternal() {
        int position = mediaGrid.getFirstVisiblePosition();
        WindowManager manager = (WindowManager) getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);

        int columnsCount = 2;
        if (selectedAlbum != null) {
            if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                columnsCount = 5;
            } else {
                columnsCount = 3;
            }
        } else {
            if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                columnsCount = 4;
            }
        }
        mediaGrid.setNumColumns(columnsCount);
        itemWidth = (displaySize.x - ((columnsCount + 1) * BitmapCache.convertDpToPixel(4, this))) / columnsCount;
        itemHeight = itemWidth;
        mediaGrid.setColumnWidth(itemWidth);

        ((BaseAdapter) mediaGrid.getAdapter()).notifyDataSetChanged();
        mediaGrid.setSelection(position);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixLayoutInternal();
    }

    public ArrayList<AlbumEntry> loadGalleryPhotosAlbums() {
        final ArrayList<AlbumEntry> albumsSorted = new ArrayList<AlbumEntry>();
        SparseArray<AlbumEntry> albums = new SparseArray<AlbumEntry>();
        AlbumEntry allPhotosAlbum = null;
        String cameraFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/" + "Camera/";
        Integer cameraAlbumId = null;

        Cursor cursor = null;
        try {
            cursor = MediaStore.Images.Media.query(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionPhotos, "", null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
            if (cursor != null) {
                int imageIdColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                int bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
                int bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                int orientationColumn = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);

                while (cursor.moveToNext()) {
                    int imageId = cursor.getInt(imageIdColumn);
                    int bucketId = cursor.getInt(bucketIdColumn);
                    String bucketName = cursor.getString(bucketNameColumn);
                    String path = cursor.getString(dataColumn);
                    long dateTaken = cursor.getLong(dateColumn);
                    int orientation = cursor.getInt(orientationColumn);

                    if (path == null || path.length() == 0) {
                        continue;
                    }

                    PhotoEntry photoEntry = new PhotoEntry(bucketId, imageId, dateTaken, path, orientation);

                    if (allPhotosAlbum == null) {
                        allPhotosAlbum = new AlbumEntry(0, getString(R.string.all_pictures), photoEntry);
                        albumsSorted.add(0, allPhotosAlbum);
                    }
                    allPhotosAlbum.addPhoto(photoEntry);

                    AlbumEntry albumEntry = albums.get(bucketId);
                    if (albumEntry == null) {
                        albumEntry = new AlbumEntry(bucketId, bucketName, photoEntry);
                        albums.put(bucketId, albumEntry);
                        if (cameraAlbumId == null && path.startsWith(cameraFolder)) {
                            albumsSorted.add(0, albumEntry);
                            cameraAlbumId = bucketId;
                        } else {
                            albumsSorted.add(albumEntry);
                        }
                    }

                    albumEntry.addPhoto(photoEntry);
                }
            }
        } catch (Exception e) {
            Log.d("tmessages", "", e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.d("tmessages", "", e);
                }
            }
        }
        return albumsSorted;
    }

    private class AlbumsAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<AlbumEntry> albums;

        private LayoutInflater inflater;

        public AlbumsAdapter(Context context, ArrayList<AlbumEntry> albums) {
            this.context = context;
            this.albums = albums;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return albums.size();
        }

        @Override
        public AlbumEntry getItem(int position) {
            return albums.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = inflater.inflate(R.layout.photo_picker_album_layout, viewGroup, false);
            }
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = itemWidth;
            params.height = itemHeight;
            view.setLayoutParams(params);

            AlbumEntry albumEntry = getItem(position);
            ThumbnailView imageView = (ThumbnailView) view.findViewById(R.id.media_photo_image);
            showThumbnail(imageView, albumEntry.coverPhoto);
            TextView textView = (TextView) view.findViewById(R.id.album_name);
            textView.setText(albumEntry.bucketName);
            textView = (TextView) view.findViewById(R.id.album_count);
            textView.setText("" + albumEntry.photos.size());
            return view;
        }
    }

    private class PhotosAdapter extends BaseAdapter {

        private Context context;
        private AlbumEntry albumEntry;

        private LayoutInflater inflater;

        private PhotosAdapter(Context context, AlbumEntry albumEntry) {
            this.context = context;
            this.albumEntry = albumEntry;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return albumEntry.photos.size();
        }

        @Override
        public PhotoEntry getItem(int position) {
            return albumEntry.photos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = inflater.inflate(R.layout.photo_picker_photo_layout, viewGroup, false);
                View checkImageView = view.findViewById(R.id.photo_check_frame);
                checkImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PhotoEntry photoEntry = getItem((Integer) ((View) v.getParent()).getTag());
                        if (selectedPhotos.containsKey(photoEntry.imageId)) {
                            selectedPhotos.remove(photoEntry.imageId);
                        } else {
                            selectedPhotos.put(photoEntry.imageId, photoEntry);
                        }
                        updateSelectedPhoto((View) v.getParent(), photoEntry);
                        updateSelectedCount();
                    }
                });
            }
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = itemWidth;
            params.height = itemHeight;
            view.setLayoutParams(params);
            PhotoEntry photoEntry = selectedAlbum.photos.get(position);
            ThumbnailView imageView = (ThumbnailView) view.findViewById(R.id.media_photo_image);
            // imageView.setTag(position);
            view.setTag(position);
            showThumbnail(imageView, photoEntry);
            updateSelectedPhoto(view, photoEntry);
            boolean showing = false;//PhotoViewer.getInstance().isShowingImage(photoEntry.path);
            //imageView.imageReceiver.setVisible(!showing, false);
            View frameView = view.findViewById(R.id.photo_frame);
            frameView.setVisibility(showing ? View.GONE : View.VISIBLE);
            ImageView checkImageView = (ImageView) view.findViewById(R.id.photo_check);
            checkImageView.setVisibility(showing ? View.GONE : View.VISIBLE);
            return view;
        }
    }

    private void showThumbnail(LazyImageView imageView, PhotoEntry photoEntry) {
        if (photoEntry != null && photoEntry.path != null && photoEntry.imageId != 0) {
            BitmapCache.getInstance().getThumbnailAsync(imageView, photoEntry.hash, photoEntry.imageId, R.drawable.ic_gallery);
        } else {
            imageView.setPlaceholder(R.drawable.ic_gallery);
        }
    }

    private void updateSelectedPhoto(View view, PhotoEntry photoEntry) {
        View frameView = view.findViewById(R.id.photo_frame);
        ImageView checkImageView = (ImageView) view.findViewById(R.id.photo_check);
        if (selectedPhotos.containsKey(photoEntry.imageId)) {
            frameView.setBackgroundResource(R.drawable.photo_border);
            checkImageView.setImageResource(R.drawable.selectphoto_small_active);
            checkImageView.setBackgroundColor(getResources().getColor(R.color.check_selected_color));
        } else {
            frameView.setBackgroundResource(0);
            checkImageView.setImageResource(R.drawable.selectphoto_small);
            checkImageView.setBackgroundColor(getResources().getColor(R.color.check_unselected_color));
        }
    }

    private void updateSelectedCount() {
        if (selectedPhotos.isEmpty()) {
            doneButtonTextView.setTextColor(0xff999999);
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.selectphoto_small_grey, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.GONE);
            doneButton.setEnabled(false);
        } else {
            doneButtonTextView.setTextColor(0xffffffff);
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.VISIBLE);
            doneButtonBadgeTextView.setText("" + selectedPhotos.size());
            doneButton.setEnabled(true);
        }
    }
}
