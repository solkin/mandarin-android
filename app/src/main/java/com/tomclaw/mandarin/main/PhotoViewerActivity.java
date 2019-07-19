package com.tomclaw.mandarin.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.tomclaw.helpers.AppsMenuHelper;
import com.tomclaw.helpers.Files;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlideApp;
import com.tomclaw.mandarin.core.TaskExecutor;
import com.tomclaw.mandarin.core.WeakObjectTask;
import com.tomclaw.design.TouchImageView;
import com.tomclaw.mandarin.util.BitmapHelper;
import com.tomclaw.mandarin.util.GifDrawable;
import com.tomclaw.mandarin.util.GifFileDecoder;
import com.tomclaw.preferences.PreferenceHelper;

import static com.bumptech.glide.request.RequestOptions.centerInsideTransform;

/**
 * Created by Solkin on 05.12.2014.
 */
public class PhotoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PICTURE_URI = "picture_uri";
    public static final String EXTRA_PICTURE_NAME = "picture_name";
    public static final String EXTRA_SELECTED_COUNT = "sending_count";
    public static final String EXTRA_PHOTO_ENTRY = "photo_entry";

    public static final String SELECTED_PHOTO_ENTRY = "selected_image_id";

    public static final int ANIMATION_DURATION = 250;

    public static final RequestOptions PREVIEW_OPTIONS = centerInsideTransform()
            .placeholder(R.drawable.ic_gallery)
            .error(R.drawable.ic_gallery)
            .format(DecodeFormat.PREFER_RGB_565)
            .priority(Priority.IMMEDIATE)
            .override(256)
            .encodeQuality(40)
            .dontAnimate()
            .downsample(DownsampleStrategy.CENTER_INSIDE);

    private View progressView;

    private TouchImageView imageView;

    private View pickerButtons;

    private View photoViewFailedView;
    private View doneButton;
    private TextView doneButtonTextView;
    private TextView doneButtonBadgeTextView;

    private Uri uri;
    private String name;
    private int selectedCount;
    private PhotoEntry photoEntry;
    private boolean hasPreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int themeRes = PreferenceHelper.getThemeRes(this);
        setTheme(themeRes);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.photo_viewer_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Extract picture path we must show.
        Bundle extras = getIntent().getExtras();
        String uriString = extras.getString(EXTRA_PICTURE_URI);
        name = extras.getString(EXTRA_PICTURE_NAME);
        selectedCount = extras.getInt(EXTRA_SELECTED_COUNT, -1);
        photoEntry = (PhotoEntry) extras.getSerializable(EXTRA_PHOTO_ENTRY);
        // Check the parameters are correct.
        if (TextUtils.isEmpty(uriString) || TextUtils.isEmpty(name)) {
            finish();
        } else {
            uri = Uri.parse(uriString);
        }

        // Preparing for action bar.
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(name);
        }

        photoViewFailedView = findViewById(R.id.photo_view_failed);

        pickerButtons = findViewById(R.id.picker_buttons);

        Button cancelButton = findViewById(R.id.cancel_button);
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
        doneButtonTextView = doneButton.findViewById(R.id.done_button_text);
        doneButtonTextView.setText(getString(R.string.send).toUpperCase());
        doneButtonBadgeTextView = doneButton.findViewById(R.id.done_button_badge);

        // Check for no selection here and...
        if (selectedCount == -1) {
            // ... hide picker buttons.
            pickerButtons.setVisibility(View.GONE);
        } else {
            // ... update picker buttons.
            updateSelectedCount();
        }

        progressView = findViewById(R.id.progress_view);

        imageView = findViewById(R.id.touch_image_view);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionBar bar = getSupportActionBar();
                if (bar != null) {
                    if (bar.isShowing()) {
                        bar.hide();
                        hidePickerButtons();
                    } else {
                        bar.show();
                        showPickerButtons();
                    }
                }
            }
        });

        GlideApp.with(this)
                .asBitmap()
                .load(photoEntry.path)
                .apply(PREVIEW_OPTIONS)
                .into(imageView);

        samplePicture();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.photo_viewer_activity_menu, menu);

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, Files.getMimeType(name));

        AppsMenuHelper.fillMenuItemSubmenu(this, menu, R.id.view_in_external_app_menu, intent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return false;
    }

    private void hidePickerButtons() {
        animatePickerButtons(new TranslateAnimation(0, 0, 0, pickerButtons.getHeight()), new AlphaAnimation(1, 0.0f),
                new AccelerateInterpolator());
    }

    private void showPickerButtons() {
        animatePickerButtons(new TranslateAnimation(0, 0, pickerButtons.getHeight(), 0), new AlphaAnimation(0.0f, 1),
                new DecelerateInterpolator());
    }

    private void animatePickerButtons(TranslateAnimation translateAnimation, AlphaAnimation alphaAnimation,
                                      Interpolator interpolator) {
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(translateAnimation);
        animationSet.addAnimation(alphaAnimation);
        animationSet.setFillAfter(true);
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setInterpolator(interpolator);

        pickerButtons.startAnimation(animationSet);
    }

    private void updateSelectedCount() {
        if (selectedCount <= 1) {
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.selectphoto_small_active, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.GONE);
        } else {
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.VISIBLE);
            doneButtonBadgeTextView.setText("" + selectedCount);
        }
    }

    private void sendSelectedPhotos() {
        Intent intent = new Intent();
        intent.putExtra(SELECTED_PHOTO_ENTRY, photoEntry);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void samplePicture() {
        TaskExecutor.getInstance().execute(new PhotoSamplingTask(this, hasPreview));
    }

    protected void setBitmap(Bitmap bitmap) {
        setDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    protected void setDrawable(Drawable drawable) {
        if (drawable != null) {
            hasPreview = true;
        }
        imageView.setImageDrawable(drawable);
        startAnimation();
    }

    private GifDrawable optAnimationDrawable() {
        Drawable drawable = imageView.getDrawable();
        // Check for this is animated drawable.
        if (drawable != null && drawable instanceof GifDrawable) {
            return ((GifDrawable) drawable);
        }
        return null;
    }

    private void startAnimation() {
        GifDrawable animationDrawable = optAnimationDrawable();
        if (animationDrawable != null && !animationDrawable.isRunning()) {
            animationDrawable.start();
        }
    }

    private void stopAnimation() {
        GifDrawable animationDrawable = optAnimationDrawable();
        if (animationDrawable != null) {
            if (animationDrawable.isRunning()) {
                animationDrawable.stop();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAnimation();
    }

    public class PhotoSamplingTask extends WeakObjectTask<PhotoViewerActivity> {

        private boolean isGif;
        private Drawable drawable;
        private boolean hasPreview;

        public PhotoSamplingTask(PhotoViewerActivity object, boolean hasPreview) {
            super(object);
            this.hasPreview = hasPreview;
            this.isGif = TextUtils.equals(Files.getFileExtensionFromPath(name).toLowerCase(), "gif");
        }

        @Override
        public void executeBackground() throws Throwable {
            PhotoViewerActivity activity = getWeakObject();
            if (activity != null) {
                boolean decoded = false;
                if (isGif) {
                    GifFileDecoder decoder = new GifFileDecoder(uri, activity.getContentResolver());
                    decoder.start();
                    drawable = new GifDrawable(decoder);
                    decoded = true;
                }
                if (!decoded) {
                    Bitmap bitmap = BitmapHelper.decodeSampledBitmapFromUri(activity, uri, 1280, 1280);
                    if (bitmap != null) {
                        drawable = new BitmapDrawable(activity.getResources(), bitmap);
                    }
                }
                if (drawable == null && !hasPreview) {
                    throw new NullPointerException();
                }
            }
        }

        @Override
        public boolean isPreExecuteRequired() {
            return true;
        }

        @Override
        public void onPreExecuteMain() {
            if (isGif) {
                progressView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onSuccessMain() {
            progressView.setVisibility(View.GONE);
            PhotoViewerActivity activity = getWeakObject();
            if (activity != null && drawable != null) {
                activity.setDrawable(drawable);
            }
        }

        @Override
        public void onFailMain() {
            progressView.setVisibility(View.GONE);
            photoViewFailedView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
        }
    }
}
