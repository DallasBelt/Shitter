package org.nuclearfog.twidda.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.ImageAdapter;
import org.nuclearfog.twidda.adapter.ImageAdapter.OnImageClickListener;
import org.nuclearfog.twidda.backend.ImageLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.holder.ImageHolder;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.zoomview.ZoomView;

import java.text.NumberFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.graphics.PorterDuff.Mode.SRC_IN;
import static android.media.MediaPlayer.MEDIA_ERROR_UNKNOWN;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END;
import static android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START;
import static android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START;
import static android.os.AsyncTask.Status.RUNNING;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;

/**
 * Media viewer activity for images and videos
 *
 * @author nuclearfog
 */
public class MediaViewer extends MediaActivity implements OnImageClickListener, OnSeekBarChangeListener, OnCompletionListener,
        OnPreparedListener, OnInfoListener, OnErrorListener, OnClickListener, OnTouchListener, OnSeekCompleteListener {

    /**
     * Key for the media URL, local or online, required
     */
    public static final String KEY_MEDIA_LINK = "media_link";

    /**
     * Key for the media type, required
     * {@link #MEDIAVIEWER_IMG_S}, {@link #MEDIAVIEWER_IMAGE}, {@link #MEDIAVIEWER_VIDEO} or {@link #MEDIAVIEWER_ANGIF}
     */
    public static final String KEY_MEDIA_TYPE = "media_type";

    /**
     * setup media viewer for images from storage
     */
    public static final int MEDIAVIEWER_IMG_S = 1;

    /**
     * setup media viewer for images from twitter
     */
    public static final int MEDIAVIEWER_IMAGE = 2;

    /**
     * setup media viewer for videos
     */
    public static final int MEDIAVIEWER_VIDEO = 3;

    /**
     * setup media viewer for GIF animation
     */
    public static final int MEDIAVIEWER_ANGIF = 4;

    private static final int PROGRESS_DELAY = 500;
    private static final int SPEED_FACTOR = 6;

    private static final NumberFormat formatter = NumberFormat.getIntegerInstance();

    private enum PlayStat {
        PLAY,
        PAUSE,
        FORWARD,
        BACKWARD
    }

    private ScheduledExecutorService progressUpdate;
    private ImageLoader imageAsync;

    private TextView duration, position;
    private ProgressBar loadingCircle;
    private SeekBar video_progress;
    private ImageButton playPause;
    private ImageAdapter adapter;
    private VideoView videoView;
    private ZoomView zoomImage;
    private View controlPanel;

    private String[] mediaLinks;
    private int type;

    private PlayStat playStat = PlayStat.PAUSE;
    private int videoPos = 0;


    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_media);
        RecyclerView imageList = findViewById(R.id.image_list);
        controlPanel = findViewById(R.id.media_controlpanel);
        loadingCircle = findViewById(R.id.media_progress);
        zoomImage = findViewById(R.id.image_full);
        videoView = findViewById(R.id.video_view);
        video_progress = controlPanel.findViewById(R.id.controller_progress);
        playPause = controlPanel.findViewById(R.id.controller_playpause);
        duration = controlPanel.findViewById(R.id.controller_duration);
        position = controlPanel.findViewById(R.id.controller_position);
        ImageButton forward = controlPanel.findViewById(R.id.controller_forward);
        ImageButton backward = controlPanel.findViewById(R.id.controller_backward);
        ImageButton share = controlPanel.findViewById(R.id.controller_share);

        videoView.setZOrderOnTop(true);
        GlobalSettings settings = GlobalSettings.getInstance(this);
        adapter = new ImageAdapter(settings, this);
        share.setImageResource(R.drawable.share);
        forward.setImageResource(R.drawable.forward);
        backward.setImageResource(R.drawable.backward);
        playPause.setImageResource(R.drawable.pause);
        AppStyles.setProgressColor(loadingCircle, settings.getHighlightColor());
        controlPanel.setBackgroundColor(settings.getCardColor());
        AppStyles.setSeekBarColor(settings, video_progress);
        share.setColorFilter(settings.getIconColor(), SRC_IN);
        forward.setColorFilter(settings.getIconColor(), SRC_IN);
        backward.setColorFilter(settings.getIconColor(), SRC_IN);
        playPause.setColorFilter(settings.getIconColor(), SRC_IN);
        duration.setTextColor(settings.getFontColor());
        position.setTextColor(settings.getFontColor());

        // get intent data and type
        mediaLinks = getIntent().getStringArrayExtra(KEY_MEDIA_LINK);
        type = getIntent().getIntExtra(KEY_MEDIA_TYPE, 0);

        if (mediaLinks != null && mediaLinks.length > 0) {
            switch (type) {
                case MEDIAVIEWER_IMG_S:
                    adapter.disableSaveButton();
                case MEDIAVIEWER_IMAGE:
                    zoomImage.setVisibility(VISIBLE);
                    imageList.setVisibility(VISIBLE);
                    imageList.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
                    imageList.setAdapter(adapter);
                    imageAsync = new ImageLoader(this);
                    imageAsync.execute(mediaLinks);
                    break;

                case MEDIAVIEWER_VIDEO:
                    if (!mediaLinks[0].startsWith("http"))
                        share.setVisibility(GONE); // local image
                    videoView.setVisibility(VISIBLE);
                    controlPanel.setVisibility(VISIBLE);
                    progressUpdate = Executors.newScheduledThreadPool(1);
                    progressUpdate.scheduleWithFixedDelay(new Runnable() {
                        public void run() {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    updateSeekBar();
                                }
                            });
                        }
                    }, PROGRESS_DELAY, PROGRESS_DELAY, TimeUnit.MILLISECONDS);
                case MEDIAVIEWER_ANGIF:
                    zoomImage.setVisibility(GONE);
                    Uri video = Uri.parse(mediaLinks[0]);
                    videoView.setVideoURI(video);
                    break;
            }
        }
        share.setOnClickListener(this);
        playPause.setOnClickListener(this);
        videoView.setOnTouchListener(this);
        backward.setOnTouchListener(this);
        forward.setOnTouchListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setOnErrorListener(this);
        video_progress.setOnSeekBarChangeListener(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (type == MEDIAVIEWER_VIDEO) {
            videoPos = videoView.getCurrentPosition();
            videoView.pause();
        }
    }


    @Override
    protected void onDestroy() {
        if (imageAsync != null && imageAsync.getStatus() == RUNNING)
            imageAsync.cancel(true);
        progressUpdate.shutdown();
        super.onDestroy();
    }


    @Override
    public void onClick(View v) {
        // play/pause video
        if (v.getId() == R.id.controller_playpause) {
            if (videoView.isPlaying()) {
                videoView.pause();
                playPause.setImageResource(R.drawable.play);
                playStat = PlayStat.PAUSE;
            } else {
                videoView.resume();
                playPause.setImageResource(R.drawable.pause);
                playStat = PlayStat.PLAY;
            }
        }
        // open link with another app
        else if (v.getId() == R.id.controller_share) {
            if (mediaLinks != null && mediaLinks.length > 0) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mediaLinks[0]));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException err) {
                    Toast.makeText(this, R.string.error_connection_failed, LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.controller_backward) {
            if (event.getAction() == ACTION_DOWN) {
                playStat = PlayStat.BACKWARD;
                videoView.pause();
            } else if (event.getAction() == ACTION_UP) {
                playStat = PlayStat.PLAY;
                videoView.resume();
            }
        } else if (v.getId() == R.id.controller_forward) {
            if (event.getAction() == ACTION_DOWN) {
                playStat = PlayStat.FORWARD;
                videoView.pause();
            } else if (event.getAction() == ACTION_UP) {
                playStat = PlayStat.PLAY;
                videoView.resume();
            }
        } else if (v.getId() == R.id.video_view) {
            if (event.getAction() == ACTION_DOWN) {
                if (controlPanel.getVisibility() == VISIBLE) {
                    controlPanel.setVisibility(INVISIBLE);
                } else {
                    controlPanel.setVisibility(VISIBLE);
                }
            }
        }
        return false;
    }


    @Override
    protected void onAttachLocation(@Nullable Location location) {
    }


    @Override
    protected void onMediaFetched(int resultType, String path) {
    }


    @Override
    public void onImageClick(Bitmap image) {
        zoomImage.reset();
        zoomImage.setImageBitmap(image);
    }


    @Override
    public void onImageSave(Bitmap image, int pos) {
        String link = mediaLinks[pos];
        String name = "shitter_" + link.substring(link.lastIndexOf('/') + 1);
        storeImage(image, name);
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        if (type == MEDIAVIEWER_ANGIF) {
            mp.setLooping(true);
        } else {
            playStat = PlayStat.PLAY;
            video_progress.setMax(mp.getDuration());
            duration.setText(formatter.format(mp.getDuration()));
            if (videoPos > 0) {
                mp.seekTo(videoPos);
            }
            position.setText(formatter.format(mp.getCurrentPosition()));
            mp.setOnSeekCompleteListener(this);
        }
        mp.setOnInfoListener(this);
        if (!mp.isPlaying()) {
            mp.start();
        }
    }


    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MEDIA_INFO_BUFFERING_END:
            case MEDIA_INFO_VIDEO_RENDERING_START:
                loadingCircle.setVisibility(INVISIBLE);
                return true;

            case MEDIA_INFO_BUFFERING_START:
                loadingCircle.setVisibility(VISIBLE);
                return true;
        }
        return false;
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what == MEDIA_ERROR_UNKNOWN) {
            Toast.makeText(this, R.string.error_cant_load_video, Toast.LENGTH_SHORT).show();
            finish();
            return true;
        }
        return false;
    }


    @Override
    public void onSeekComplete(MediaPlayer mp) {
        position.setText(formatter.format(mp.getCurrentPosition()));
        if (playStat == PlayStat.PLAY) {
            playPause.setImageResource(R.drawable.pause);
            mp.start();
        }
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        playPause.setImageResource(R.drawable.play);
        playStat = PlayStat.PAUSE;
        videoPos = 0;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        videoView.pause();
    }


    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        videoPos = seekBar.getProgress();
        videoView.seekTo(videoPos);
    }

    /**
     * Called from {@link ImageLoader} when all images are downloaded successfully
     */
    public void onSuccess() {
        adapter.disableLoading();
    }

    /**
     * Called from {@link ImageLoader} when an error occurs
     *
     * @param err Exception caught by {@link ImageLoader}
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(getApplicationContext(), err);
        finish();
    }

    /**
     * set downloaded image into preview list
     *
     * @param image Image container
     */
    public void setImage(ImageHolder image) {
        if (adapter.isEmpty()) {
            zoomImage.reset();
            zoomImage.setImageBitmap(image.reducedImage);
            loadingCircle.setVisibility(INVISIBLE);
        }
        adapter.addLast(image);
    }

    /**
     * updates controller panel seekbar
     */
    private void updateSeekBar() {
        switch (playStat) {
            case PLAY:
                video_progress.setProgress(videoPos);
                videoPos = videoView.getCurrentPosition();
                position.setText(formatter.format(videoPos));
                break;

            case FORWARD:
                videoPos += 2 * PROGRESS_DELAY * SPEED_FACTOR;
                if (videoPos > videoView.getDuration())
                    videoPos = videoView.getDuration();
                videoView.pause();
                videoView.seekTo(videoPos);
                break;

            case BACKWARD:
                videoPos -= 2 * PROGRESS_DELAY * SPEED_FACTOR;
                if (videoPos < 0)
                    videoPos = 0;
                videoView.pause();
                videoView.seekTo(videoPos);
                break;
        }
        video_progress.setProgress(videoPos);
    }
}