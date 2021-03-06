package com.tomclaw.mandarin.main;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.VideoView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tomclaw.helpers.AppsMenuHelper;
import com.tomclaw.helpers.Files;
import com.tomclaw.mandarin.R;
import com.tomclaw.preferences.PreferenceHelper;

public class VideoViewerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String EXTRA_VIDEO_NAME = "video_name";

    private VideoView videoView;
    private MediaController mediaController;

    private Uri uri;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int themeRes = PreferenceHelper.getThemeRes(this);
        setTheme(themeRes);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.video_viewer_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Extract picture path we must show.
        Bundle extras = getIntent().getExtras();
        String uriString = extras.getString(EXTRA_VIDEO_URI);
        name = extras.getString(EXTRA_VIDEO_NAME);
        if (TextUtils.isEmpty(uriString) || TextUtils.isEmpty(name)) {
            finish();
        } else {
            uri = Uri.parse(uriString);
        }

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle(name);
        }

        videoView = findViewById(R.id.video_view);
    }

    @Override
    protected void onStart() {
        super.onStart();

        playVideo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.photo_viewer_activity_menu, menu);

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(Files.getExtFileUri(this, uri), Files.getMimeType(name));

        AppsMenuHelper.fillMenuItemSubmenu(this, menu, R.id.view_in_external_app_menu, intent);
        return true;
    }

    @Override
    protected void onPause() {
        if (videoView != null && mediaController != null) {
            mediaController.hide();
            videoView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaController != null && mediaController.isShowing()) {
            mediaController.show(0);
        }
    }

    @Override
    protected void onDestroy() {
        if (videoView != null && mediaController != null) {
            mediaController.hide();
            videoView.stopPlayback();
        }
        super.onDestroy();
    }

    private void playVideo() {
        mediaController = new CustomMediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaController.show();
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaController.show(0);
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                onPlaybackError();
                return true;
            }
        });
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(uri);
        videoView.requestFocus();
        videoView.start();
    }

    private void onPlaybackError() {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(Files.getExtFileUri(this, uri), Files.getMimeType(name));
        startActivity(intent);
        finish();
    }

    private void showActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.show();
        }
    }

    private void hideActionBar() {
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
    }

    public class CustomMediaController extends MediaController {

        public CustomMediaController(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public CustomMediaController(Context context, boolean useFastForward) {
            super(context, useFastForward);
        }

        public CustomMediaController(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keyCode = event.getKeyCode();
            final boolean uniqueDown = event.getRepeatCount() == 0
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (uniqueDown) {
                    onBackPressed();
                }
                return true;
            } else {
                return super.dispatchKeyEvent(event);
            }
        }

        @Override
        public void show(int timeout) {
            super.show(timeout);
            showActionBar();
        }

        @Override
        public void show() {
            super.show();
            showActionBar();
        }

        @Override
        public void hide() {
            super.hide();
            hideActionBar();
        }

    }

}
