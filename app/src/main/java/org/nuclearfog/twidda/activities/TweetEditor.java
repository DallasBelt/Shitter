package org.nuclearfog.twidda.activities;

import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static org.nuclearfog.twidda.activities.MediaViewer.*;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.TweetUpdater;
import org.nuclearfog.twidda.backend.api.holder.TweetUpdate;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.dialog.ConfirmDialog;
import org.nuclearfog.twidda.dialog.ConfirmDialog.DialogType;
import org.nuclearfog.twidda.dialog.ConfirmDialog.OnConfirmListener;
import org.nuclearfog.twidda.dialog.ProgressDialog;
import org.nuclearfog.twidda.dialog.ProgressDialog.OnProgressStopListener;

import java.util.LinkedList;
import java.util.List;

/**
 * Tweet editor activity. Media files and location can be attached to a tweet.
 *
 * @author nuclearfog
 */
public class TweetEditor extends MediaActivity implements OnClickListener, OnProgressStopListener, OnConfirmListener {

    /**
     * type of media attached to the tweet
     */
    private enum MediaType {
        NONE,
        GIF,
        IMAGE,
        VIDEO
    }

    /**
     * key for the replied tweet if any
     */
    public static final String KEY_TWEETPOPUP_REPLYID = "tweet_replyID";

    /**
     * key for the text added to the tweet if any
     */
    public static final String KEY_TWEETPOPUP_TEXT = "tweet_text";

    private static final String MIME_GIF = "image/gif";
    private static final String MIME_IMAGE_ALL = "image/";
    private static final String MIME_VIDEO_ALL = "video/";

    /**
     * max amount of images (limited to 4 by twitter)
     */
    private static final int MAX_IMAGES = 4;

    /**
     * max amount of mentions in a tweet
     */
    private static final int MAX_MENTIONS = 10;

    private TweetUpdater uploaderAsync;
    private GlobalSettings settings;

    private ImageButton mediaBtn, previewBtn, locationBtn;
    private ConfirmDialog errorDialog, closingDialog;
    private Dialog loadingCircle;
    private EditText tweetText;
    private View locationPending;

    private Location location;
    private List<Uri> mediaPath = new LinkedList<>();
    private MediaType selectedFormat = MediaType.NONE;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppStyles.setFontScale(newBase));
    }


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.popup_tweet);
        ViewGroup root = findViewById(R.id.tweet_popup);
        ImageView background = findViewById(R.id.tweet_popup_background);
        ImageButton tweetButton = findViewById(R.id.tweet_send);
        ImageButton closeButton = findViewById(R.id.close);
        locationBtn = findViewById(R.id.tweet_add_location);
        mediaBtn = findViewById(R.id.tweet_add_media);
        previewBtn = findViewById(R.id.tweet_prev_media);
        tweetText = findViewById(R.id.tweet_input);
        locationPending = findViewById(R.id.location_progress);

        loadingCircle = new ProgressDialog(this, this);
        errorDialog = new ConfirmDialog(this, DialogType.TWEET_EDITOR_ERROR, this);
        closingDialog = new ConfirmDialog(this, DialogType.TWEET_EDITOR_LEAVE, this);

        settings = GlobalSettings.getInstance(this);

        Intent data = getIntent();
        String prefix = data.getStringExtra(KEY_TWEETPOPUP_TEXT);
        if (prefix != null) {
            tweetText.append(prefix);
        }
        previewBtn.setImageResource(R.drawable.image);
        mediaBtn.setImageResource(R.drawable.attachment);
        locationBtn.setImageResource(R.drawable.location);
        tweetButton.setImageResource(R.drawable.tweet);
        closeButton.setImageResource(R.drawable.cross);
        AppStyles.setEditorTheme(root, background);

        closeButton.setOnClickListener(this);
        tweetButton.setOnClickListener(this);
        mediaBtn.setOnClickListener(this);
        previewBtn.setOnClickListener(this);
        locationBtn.setOnClickListener(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isLocating()) {
            locationPending.setVisibility(VISIBLE);
            locationBtn.setVisibility(INVISIBLE);
        } else {
            locationPending.setVisibility(INVISIBLE);
            locationBtn.setVisibility(VISIBLE);
        }
    }


    @Override
    protected void onDestroy() {
        loadingCircle.dismiss();
        if (uploaderAsync != null && uploaderAsync.getStatus() == RUNNING)
            uploaderAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        showClosingMsg();
    }


    @Override
    public void onClick(View v) {
        // send tweet
        if (v.getId() == R.id.tweet_send) {
            String tweetStr = tweetText.getText().toString();
            // check if tweet is empty
            if (tweetStr.trim().isEmpty() && mediaPath.isEmpty()) {
                Toast.makeText(this, R.string.error_empty_tweet, LENGTH_SHORT).show();
            }
            // check if mentions exceed the limit
            else if (!settings.isCustomApiSet() && StringTools.countMentions(tweetStr) > MAX_MENTIONS) {
                Toast.makeText(this, R.string.error_mention_exceed, LENGTH_SHORT).show();
            }
            // check if GPS location is pending
            else if (isLocating()) {
                Toast.makeText(this, R.string.info_location_pending, LENGTH_SHORT).show();
            }
            // check if gps locating is not pending
            else if (uploaderAsync == null || uploaderAsync.getStatus() != RUNNING) {
                updateTweet();
            }
        }
        // close tweet editor
        else if (v.getId() == R.id.close) {
            showClosingMsg();
        }
        // Add media to the tweet
        else if (v.getId() == R.id.tweet_add_media) {
            if (selectedFormat == MediaType.IMAGE) {
                getMedia(REQUEST_IMAGE);
            } else {
                getMedia(REQUEST_IMG_VID);
            }
        }
        // open media preview
        else if (v.getId() == R.id.tweet_prev_media) {
            Intent image = new Intent(this, MediaViewer.class);
            Uri[] uris = mediaPath.toArray(new Uri[0]);
            image.putExtra(KEY_MEDIA_URI, uris);
            if (selectedFormat == MediaType.VIDEO) {
                image.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_VIDEO);
                startActivity(image);
            } else if (selectedFormat != MediaType.NONE) {
                image.putExtra(KEY_MEDIA_TYPE, MEDIAVIEWER_IMAGE);
                startActivity(image);
            }
        }
        // add location to the tweet
        else if (v.getId() == R.id.tweet_add_location) {
            locationPending.setVisibility(VISIBLE);
            locationBtn.setVisibility(INVISIBLE);
            getLocation(true);
        }
    }


    @Override
    protected void onAttachLocation(@Nullable Location location) {
        if (location != null) {
            Toast.makeText(this, R.string.info_gps_attached, LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.error_gps, LENGTH_LONG).show();
        }
        locationPending.setVisibility(INVISIBLE);
        locationBtn.setVisibility(VISIBLE);
        this.location = location;
    }


    @Override
    protected void onMediaFetched(int resultType, @NonNull Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime == null) {
            Toast.makeText(this, R.string.error_file_format, LENGTH_SHORT).show();
        }
        // check if file is a gif image
        else if (mime.equals(MIME_GIF)) {
            if (selectedFormat == MediaType.NONE) {
                selectedFormat = MediaType.GIF;
                previewBtn.setImageResource(R.drawable.gif);
                AppStyles.setDrawableColor(previewBtn, settings.getIconColor());
                previewBtn.setVisibility(VISIBLE);
                mediaBtn.setVisibility(GONE);
                mediaPath.add(uri);
            } else {
                Toast.makeText(this, R.string.info_cant_add_video, LENGTH_SHORT).show();
            }
        }
        // check if file is an image with supported extension
        else if (mime.startsWith(MIME_IMAGE_ALL)) {
            if (selectedFormat == MediaType.NONE)
                selectedFormat = MediaType.IMAGE;
            if (selectedFormat == MediaType.IMAGE) {
                // add up to 4 images
                if (mediaPath.size() < MAX_IMAGES) {
                    mediaPath.add(uri);
                    previewBtn.setVisibility(VISIBLE);
                    // if limit reached, remove mediaselect button
                    if (mediaPath.size() == MAX_IMAGES) {
                        mediaBtn.setVisibility(GONE);
                    }
                }
            } else {
                Toast.makeText(this, R.string.info_cant_add_gif, LENGTH_SHORT).show();
            }
        }
        // check if file is a video with supported extension
        else if (mime.startsWith(MIME_VIDEO_ALL)) {
            if (selectedFormat == MediaType.NONE) {
                selectedFormat = MediaType.VIDEO;
                previewBtn.setImageResource(R.drawable.video);
                AppStyles.setDrawableColor(previewBtn, settings.getIconColor());
                previewBtn.setVisibility(VISIBLE);
                mediaBtn.setVisibility(GONE);
                mediaPath.add(uri);
            }
        }
        // file type is not supported
        else  {
            Toast.makeText(this, R.string.error_file_format, LENGTH_SHORT).show();
        }
    }


    @Override
    public void stopProgress() {
        if (uploaderAsync != null && uploaderAsync.getStatus() == RUNNING) {
            uploaderAsync.cancel(true);
        }
    }


    @Override
    public void onConfirm(DialogType type) {
        if (type == DialogType.TWEET_EDITOR_ERROR) {
            updateTweet();
        } else if (type == DialogType.TWEET_EDITOR_LEAVE) {
            finish();
        }
    }

    /**
     * called after sending tweet
     */
    public void onSuccess() {
        Toast.makeText(this, R.string.info_tweet_sent, LENGTH_LONG).show();
        finish();
    }

    /**
     * Show confirmation dialog if an error occurs while sending tweet
     */
    public void onError(@Nullable ErrorHandler.TwitterError error) {
        if (!errorDialog.isShowing()) {
            String message = ErrorHandler.getErrorMessage(this, error);
            errorDialog.setMessage(message);
            errorDialog.show();
        }
        if (loadingCircle.isShowing()) {
            loadingCircle.dismiss();
        }
    }

    /**
     * show confirmation dialog when closing edited tweet
     */
    private void showClosingMsg() {
        if (tweetText.length() > 0 || !mediaPath.isEmpty()) {
            if (!closingDialog.isShowing()) {
                closingDialog.show();
            }
        } else {
            finish();
        }
    }

    /**
     * update tweet information
     */
    private void updateTweet() {
        Intent data = getIntent();
        long inReplyId = data.getLongExtra(KEY_TWEETPOPUP_REPLYID, 0);
        String tweetStr = tweetText.getText().toString();
        TweetUpdate tweet = new TweetUpdate(tweetStr, inReplyId);
        // add media
        if (selectedFormat == MediaType.IMAGE || selectedFormat == MediaType.GIF)
            tweet.addMedia(getApplicationContext(), mediaPath);
        else if (selectedFormat == MediaType.VIDEO)
            tweet.addMedia(getApplicationContext(), mediaPath);
        // add location
        if (location != null)
            tweet.addLocation(location);
        // send tweet
        uploaderAsync = new TweetUpdater(this, tweet);
        uploaderAsync.execute();
        if (!loadingCircle.isShowing()) {
            loadingCircle.show();
        }
    }
}