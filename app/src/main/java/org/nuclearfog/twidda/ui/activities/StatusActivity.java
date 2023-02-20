package org.nuclearfog.twidda.ui.activities;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.OnLongClickListener;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_SHORT;
import static org.nuclearfog.twidda.ui.activities.SearchActivity.KEY_SEARCH_QUERY;
import static org.nuclearfog.twidda.ui.activities.StatusEditor.KEY_STATUS_EDITOR_REPLYID;
import static org.nuclearfog.twidda.ui.activities.StatusEditor.KEY_STATUS_EDITOR_TEXT;
import static org.nuclearfog.twidda.ui.activities.UsersActivity.KEY_USERS_ID;
import static org.nuclearfog.twidda.ui.activities.UsersActivity.KEY_USERS_MODE;
import static org.nuclearfog.twidda.ui.activities.UsersActivity.USERS_FAVORIT;
import static org.nuclearfog.twidda.ui.activities.UsersActivity.USERS_REPOST;
import static org.nuclearfog.twidda.ui.fragments.StatusFragment.KEY_STATUS_FRAGMENT_ID;
import static org.nuclearfog.twidda.ui.fragments.StatusFragment.KEY_STATUS_FRAGMENT_MODE;
import static org.nuclearfog.twidda.ui.fragments.StatusFragment.KEY_STATUS_FRAGMENT_SEARCH;
import static org.nuclearfog.twidda.ui.fragments.StatusFragment.STATUS_FRAGMENT_REPLY;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.NestedScrollView.OnScrollChangeListener;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.nuclearfog.tag.Tagger;
import org.nuclearfog.tag.Tagger.OnTagClickListener;
import org.nuclearfog.textviewtool.LinkAndScrollMovement;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.api.ConnectionException;
import org.nuclearfog.twidda.backend.async.StatusAction;
import org.nuclearfog.twidda.backend.async.StatusAction.StatusParam;
import org.nuclearfog.twidda.backend.async.StatusAction.StatusResult;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.AsyncExecutor.AsyncCallback;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.backend.utils.PicassoBuilder;
import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.config.Configuration;
import org.nuclearfog.twidda.config.GlobalSettings;
import org.nuclearfog.twidda.model.Card;
import org.nuclearfog.twidda.model.Location;
import org.nuclearfog.twidda.model.Media;
import org.nuclearfog.twidda.model.Poll;
import org.nuclearfog.twidda.model.Status;
import org.nuclearfog.twidda.model.User;
import org.nuclearfog.twidda.ui.adapter.PreviewAdapter;
import org.nuclearfog.twidda.ui.adapter.PreviewAdapter.OnCardClickListener;
import org.nuclearfog.twidda.ui.dialogs.ConfirmDialog;
import org.nuclearfog.twidda.ui.dialogs.ConfirmDialog.OnConfirmListener;
import org.nuclearfog.twidda.ui.dialogs.MetricsDialog;
import org.nuclearfog.twidda.ui.fragments.StatusFragment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.regex.Pattern;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

/**
 * Status Activity to show status and user information
 *
 * @author nuclearfog
 */
public class StatusActivity extends AppCompatActivity implements OnClickListener, OnScrollChangeListener, AsyncCallback<StatusResult>,
		OnLongClickListener, OnTagClickListener, OnConfirmListener, OnCardClickListener {

	/**
	 * Activity result code to update existing status information
	 */
	public static final int RETURN_STATUS_UPDATE = 0x789CD38B;

	/**
	 * Activity result code if a status was not found or removed
	 */
	public static final int RETURN_STATUS_REMOVED = 0x8B03DB84;

	/**
	 * key used for status information
	 * value type is {@link Status}
	 * If no status object exists, {@link #KEY_STATUS_ID} and {@link #KEY_STATUS_NAME} will be used instead
	 */
	public static final String KEY_STATUS_DATA = "status_data";

	/**
	 * key for the status ID value, alternative to {@link #KEY_STATUS_DATA}
	 * value type is Long
	 */
	public static final String KEY_STATUS_ID = "status_id";

	/**
	 * key for the status author's name. alternative to {@link #KEY_STATUS_DATA}
	 * value type is String
	 */
	public static final String KEY_STATUS_NAME = "status_author";

	/**
	 * key to return updated status information
	 * value type is {@link Status}
	 */
	public static final String INTENT_STATUS_UPDATE_DATA = "status_update_data";

	/**
	 * key to return an ID if status was deleted
	 * value type is Long
	 */
	public static final String INTENT_STATUS_REMOVED_ID = "status_removed_id";

	/**
	 * regex pattern of a status URL
	 */
	public static final Pattern TWITTER_LINK_PATTERN = Pattern.compile("https://twitter.com/\\w+/status/\\d+");

	private static final int MENU_GROUP_COPY = 0x157426;

	@Nullable
	private ClipboardManager clip;
	private GlobalSettings settings;
	private StatusAction statusAsync;
	private Picasso picasso;

	private PreviewAdapter adapter;
	private ConfirmDialog confirmDialog;
	private MetricsDialog metricsDialog;

	private NestedScrollView container;
	private ViewGroup root, header, body;
	private TextView statusApi, createdAt, statusText, screenName, userName, locationName, sensitive_media;
	private Button replyButton, repostButton, likeButton, replyName, locationButton, repostNameButton, bookmarkButton;
	private ImageView profileImage;
	private RecyclerView cardList;
	private Toolbar toolbar;

	@Nullable
	private Status status;
	private long id;
	private boolean hidden;


	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(AppStyles.setFontScale(newBase));
	}


	@Override
	protected void onCreate(@Nullable Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.page_status);
		root = findViewById(R.id.page_status_root);
		header = findViewById(R.id.page_status_header);
		body = findViewById(R.id.page_status_body);
		container = findViewById(R.id.page_status_scroll);
		toolbar = findViewById(R.id.page_status_toolbar);
		replyButton = findViewById(R.id.page_status_reply);
		repostButton = findViewById(R.id.page_status_repost);
		likeButton = findViewById(R.id.page_status_favorite);
		bookmarkButton = findViewById(R.id.page_status_bookmark);
		userName = findViewById(R.id.page_status_username);
		screenName = findViewById(R.id.page_status_screenname);
		profileImage = findViewById(R.id.page_status_profile);
		replyName = findViewById(R.id.page_status_reply_reference);
		statusText = findViewById(R.id.page_status_text);
		createdAt = findViewById(R.id.page_status_date);
		statusApi = findViewById(R.id.page_status_api);
		locationName = findViewById(R.id.page_status_location_name);
		locationButton = findViewById(R.id.page_status_location_coordinates);
		sensitive_media = findViewById(R.id.page_status_sensitive);
		repostNameButton = findViewById(R.id.page_status_reposter_reference);
		cardList = findViewById(R.id.page_status_cards);

		picasso = PicassoBuilder.get(this);
		settings = GlobalSettings.getInstance(this);
		clip = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		adapter = new PreviewAdapter(settings, picasso, this);
		// get parameter
		String replyUsername = "";
		Object data = getIntent().getSerializableExtra(KEY_STATUS_DATA);
		if (data instanceof Status) {
			status = (Status) data;
			Status embedded = status.getEmbeddedStatus();
			if (embedded != null) {
				id = embedded.getId();
				replyUsername = embedded.getAuthor().getScreenname();
			} else {
				id = status.getId();
				hidden = status.isHidden();
				replyUsername = status.getAuthor().getScreenname();
			}
		} else {
			id = getIntent().getLongExtra(KEY_STATUS_ID, -1L);
		}
		// initialize status reply list
		Bundle param = new Bundle();
		param.putInt(KEY_STATUS_FRAGMENT_MODE, STATUS_FRAGMENT_REPLY);
		param.putString(KEY_STATUS_FRAGMENT_SEARCH, replyUsername);
		param.putLong(KEY_STATUS_FRAGMENT_ID, id);
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.replace(R.id.page_status_reply_fragment, StatusFragment.class, param);
		fragmentTransaction.commit();

		replyButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.answer, 0, 0, 0);
		repostButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.repost, 0, 0, 0);
		locationButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.location, 0, 0, 0);
		sensitive_media.setCompoundDrawablesWithIntrinsicBounds(R.drawable.sensitive, 0, 0, 0);
		replyName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.back, 0, 0, 0);
		repostNameButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.repost, 0, 0, 0);
		bookmarkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.bookmark, 0, 0, 0);
		statusText.setMovementMethod(LinkAndScrollMovement.getInstance());
		statusText.setLinkTextColor(settings.getHighlightColor());
		cardList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
		cardList.setAdapter(adapter);
		if (settings.likeEnabled()) {
			likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.like, 0, 0, 0);
		} else {
			likeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.favorite, 0, 0, 0);
		}
		toolbar.setTitle("");
		setSupportActionBar(toolbar);
		AppStyles.setTheme(root);

		confirmDialog = new ConfirmDialog(this);
		metricsDialog = new MetricsDialog(this);
		confirmDialog.setConfirmListener(this);
		repostNameButton.setOnClickListener(this);
		replyName.setOnClickListener(this);
		replyButton.setOnClickListener(this);
		repostButton.setOnClickListener(this);
		likeButton.setOnClickListener(this);
		profileImage.setOnClickListener(this);
		locationButton.setOnClickListener(this);
		repostButton.setOnLongClickListener(this);
		likeButton.setOnLongClickListener(this);
		repostNameButton.setOnLongClickListener(this);
		locationButton.setOnLongClickListener(this);
		bookmarkButton.setOnLongClickListener(this);
		container.setOnScrollChangeListener(this);
	}


	@Override
	protected void onStart() {
		super.onStart();
		if (statusAsync == null) {
			statusAsync = new StatusAction(this);
			// print status object and get and update it
			if (status != null) {
				setStatus(status);
				StatusParam param = new StatusParam(StatusParam.ONLINE, status.getId());
				statusAsync.execute(param, this);
			}
			// Load status from database first if no status is defined
			else {
				StatusParam param = new StatusParam(StatusParam.ONLINE, id);
				statusAsync.execute(param, this);
			}
		}
	}


	@Override
	protected void onDestroy() {
		if (statusAsync != null && !statusAsync.isIdle())
			statusAsync.cancel();
		super.onDestroy();
	}


	@Override
	public void onBackPressed() {
		Intent returnData = new Intent();
		returnData.putExtra(INTENT_STATUS_UPDATE_DATA, status);
		setResult(RETURN_STATUS_UPDATE, returnData);
		super.onBackPressed();
	}


	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		body.getLayoutParams().height = root.getMeasuredHeight() - toolbar.getMeasuredHeight();
		container.scrollTo(0, 0);
	}


	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu m) {
		getMenuInflater().inflate(R.menu.status, m);
		AppStyles.setOverflowIcon(toolbar, settings.getIconColor());
		return super.onCreateOptionsMenu(m);
	}


	@Override
	public boolean onPrepareOptionsMenu(@NonNull Menu m) {
		if (status == null)
			return super.onPrepareOptionsMenu(m);

		MenuItem optDelete = m.findItem(R.id.menu_status_delete);
		MenuItem optHide = m.findItem(R.id.menu_status_hide);
		MenuItem optCopy = m.findItem(R.id.menu_status_copy);
		MenuItem optMetrics = m.findItem(R.id.menu_status_metrics);
		SubMenu copyMenu = optCopy.getSubMenu();

		Status status = this.status;
		if (status.getEmbeddedStatus() != null) {
			status = status.getEmbeddedStatus();
		}
		if (status.getRepliedUserId() == settings.getLogin().getId() && status.getAuthor().getId() != settings.getLogin().getId()) {
			optHide.setVisible(true);
			if (hidden) {
				optHide.setTitle(R.string.menu_tweet_unhide);
			} else {
				optHide.setTitle(R.string.menu_tweet_hide);
			}
		}
		if (status.getAuthor().isCurrentUser()) {
			optDelete.setVisible(true);
		}
		if (status.getMetrics() != null) {
			optMetrics.setVisible(true);
		}
		// add media link items
		// check if menu doesn't contain media links already
		if (copyMenu.size() == 2) {
			int mediaCount = status.getMedia().length;
			for (int i = 0; i < mediaCount; i++) {
				// create sub menu entry and use array index as item ID
				String text = getString(R.string.menu_media_link) + ' ' + (i + 1);
				copyMenu.add(MENU_GROUP_COPY, i, Menu.NONE, text);
			}
		}
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (status == null)
			return super.onOptionsItemSelected(item);

		Status status = this.status;
		if (status.getEmbeddedStatus() != null)
			status = status.getEmbeddedStatus();
		// Delete status option
		if (item.getItemId() == R.id.menu_status_delete) {
			confirmDialog.show(ConfirmDialog.DELETE_STATUS);
		}
		// hide status
		else if (item.getItemId() == R.id.menu_status_hide) {
			StatusParam param;
			if (hidden) {
				param = new StatusParam(StatusParam.UNHIDE, status.getId());
			} else {
				param = new StatusParam(StatusParam.HIDE, status.getId());
			}
			statusAsync = new StatusAction(this);
			statusAsync.execute(param, this);
		}
		// get status link
		else if (item.getItemId() == R.id.menu_status_browser) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(status.getUrl()));
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException err) {
				Toast.makeText(getApplicationContext(), R.string.error_connection_failed, LENGTH_SHORT).show();
			}
		}
		// copy status link to clipboard
		else if (item.getItemId() == R.id.menu_status_copy_text) {
			if (clip != null) {
				ClipData linkClip = ClipData.newPlainText("status text", status.getText());
				clip.setPrimaryClip(linkClip);
				Toast.makeText(getApplicationContext(), R.string.info_tweet_text_copied, LENGTH_SHORT).show();
			}
		}
		// copy status link to clipboard
		else if (item.getItemId() == R.id.menu_status_copy_link) {
			if (clip != null) {
				ClipData linkClip = ClipData.newPlainText("status link", status.getUrl());
				clip.setPrimaryClip(linkClip);
				Toast.makeText(getApplicationContext(), R.string.info_tweet_link_copied, LENGTH_SHORT).show();
			}
		}
		// open status metrics page
		else if (item.getItemId() == R.id.menu_status_metrics) {
			if (status.getMetrics() != null) {
				metricsDialog.show(status.getMetrics());
			}
		}
		// copy media links
		else if (item.getGroupId() == MENU_GROUP_COPY) {
			int index = item.getItemId();
			Media[] medias = status.getMedia();
			if (index >= 0 && index < medias.length) {
				if (clip != null) {
					ClipData linkClip = ClipData.newPlainText("status media link", medias[index].getUrl());
					clip.setPrimaryClip(linkClip);
					Toast.makeText(getApplicationContext(), R.string.info_tweet_medialink_copied, LENGTH_SHORT).show();
				}
			}
		}
		return true;
	}


	@Override
	public void onClick(View v) {
		if (status != null) {
			Status status = this.status;
			if (status.getEmbeddedStatus() != null)
				status = status.getEmbeddedStatus();
			// answer to the status
			if (v.getId() == R.id.page_status_reply) {
				String prefix = status.getUserMentions();
				Intent intent = new Intent(this, StatusEditor.class);
				intent.putExtra(KEY_STATUS_EDITOR_REPLYID, status.getId());
				if (!prefix.isEmpty())
					intent.putExtra(KEY_STATUS_EDITOR_TEXT, prefix);
				startActivity(intent);
			}
			// show user reposting this status
			else if (v.getId() == R.id.page_status_repost) {
				Intent userList = new Intent(this, UsersActivity.class);
				userList.putExtra(KEY_USERS_ID, status.getId());
				userList.putExtra(KEY_USERS_MODE, USERS_REPOST);
				startActivity(userList);
			}
			// show user favoriting this status
			else if (v.getId() == R.id.page_status_favorite) {
				Intent userList = new Intent(this, UsersActivity.class);
				userList.putExtra(KEY_USERS_ID, status.getId());
				userList.putExtra(KEY_USERS_MODE, USERS_FAVORIT);
				startActivity(userList);
			}
			// open profile of the status author
			else if (v.getId() == R.id.page_status_profile) {
				Intent profile = new Intent(getApplicationContext(), ProfileActivity.class);
				profile.putExtra(ProfileActivity.KEY_PROFILE_USER, status.getAuthor());
				startActivity(profile);
			}
			// open replied status
			else if (v.getId() == R.id.page_status_reply_reference) {
				Intent answerIntent = new Intent(getApplicationContext(), StatusActivity.class);
				answerIntent.putExtra(KEY_STATUS_ID, status.getRepliedStatusId());
				answerIntent.putExtra(KEY_STATUS_NAME, status.getReplyName());
				startActivity(answerIntent);
			}
			// open status location coordinates
			else if (v.getId() == R.id.page_status_location_coordinates) {
				if (status.getLocation() != null) {
					Intent locationIntent = new Intent(Intent.ACTION_VIEW);
					locationIntent.setData(Uri.parse("geo:" + status.getLocation().getCoordinates() + "?z=14"));
					try {
						startActivity(locationIntent);
					} catch (ActivityNotFoundException err) {
						Toast.makeText(getApplicationContext(), R.string.error_no_card_app, LENGTH_SHORT).show();
					}
				}
			}
			// go to user reposting this status
			else if (v.getId() == R.id.page_status_reposter_reference) {
				Intent profile = new Intent(getApplicationContext(), ProfileActivity.class);
				profile.putExtra(ProfileActivity.KEY_PROFILE_USER, this.status.getAuthor());
				startActivity(profile);
			}
		}
	}


	@Override
	public boolean onLongClick(View v) {
		if (status != null && (statusAsync == null || statusAsync.isIdle())) {
			// repost this status
			if (v.getId() == R.id.page_status_repost) {
				StatusParam param;
				if (status.isReposted()) {
					param = new StatusParam(StatusParam.UNREPOST, status.getId());
				} else {
					param = new StatusParam(StatusParam.REPOST, status.getId());
				}
				statusAsync = new StatusAction(this);
				statusAsync.execute(param, this);
				Toast.makeText(getApplicationContext(), R.string.info_loading, LENGTH_SHORT).show();
				return true;
			}
			// favorite this status
			else if (v.getId() == R.id.page_status_favorite) {
				StatusParam param;
				if (status.isFavorited()) {
					param = new StatusParam(StatusParam.UNFAVORITE, status.getId());
				} else {
					param = new StatusParam(StatusParam.FAVORITE, status.getId());
				}
				statusAsync = new StatusAction(this);
				statusAsync.execute(param, this);
				Toast.makeText(getApplicationContext(), R.string.info_loading, LENGTH_SHORT).show();
				return true;
			}
			// go to original status
			else if (v.getId() == R.id.page_status_reposter_reference) {
				Status embeddedStatus = status.getEmbeddedStatus();
				if (embeddedStatus != null) {
					Intent intent = new Intent(this, StatusActivity.class);
					intent.putExtra(KEY_STATUS_DATA, embeddedStatus);
					startActivity(intent);
				}
				return true;
			}
			// copy status coordinates
			else if (v.getId() == R.id.page_status_location_coordinates) {
				Location location;
				if (status.getEmbeddedStatus() != null) {
					location = status.getEmbeddedStatus().getLocation();
				} else {
					location = status.getLocation();
				}
				if (clip != null && location != null) {
					ClipData linkClip = ClipData.newPlainText("Status location coordinates", location.getCoordinates());
					clip.setPrimaryClip(linkClip);
					Toast.makeText(getApplicationContext(), R.string.info_tweet_location_copied, LENGTH_SHORT).show();
				}
				return true;
			}
			// bookmark status
			else if (v.getId() == R.id.page_status_bookmark) {
				StatusParam param;
				if (status.isBookmarked()) {
					param = new StatusParam(StatusParam.UNBOOKMARK, status.getId());
				} else {
					param = new StatusParam(StatusParam.BOOKMARK, status.getId());
				}
				statusAsync = new StatusAction(this);
				statusAsync.execute(param, this);
				Toast.makeText(getApplicationContext(), R.string.info_loading, LENGTH_SHORT).show();
				return true;
			}
		}
		return false;
	}


	@Override
	public void onScrollChange(@NonNull NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
		if (scrollY == header.getMeasuredHeight()) {
			// unlock child scrolling
		} else {
			// lock child view from scrolling
		}
	}


	@Override
	public void onConfirm(int type, boolean rememberChoice) {
		if (status != null) {
			Status status = this.status;
			if (status.getEmbeddedStatus() != null) {
				status = status.getEmbeddedStatus();
			}
			// delete status
			if (type == ConfirmDialog.DELETE_STATUS) {
				StatusParam param = new StatusParam(StatusParam.DELETE, status.getId());
				statusAsync = new StatusAction(this);
				statusAsync.execute(param, this);
			}
			// confirm playing video without proxy
			else if (type == ConfirmDialog.PROXY_CONFIRM) {
				settings.setIgnoreProxyWarning(rememberChoice);
				Media[] mediaItems = status.getMedia();
				if (mediaItems.length > 0) {
					Uri uri = Uri.parse(mediaItems[0].getUrl());
					Intent mediaIntent = new Intent(this, VideoViewer.class);
					mediaIntent.putExtra(VideoViewer.VIDEO_URI, uri);
					mediaIntent.putExtra(VideoViewer.ENABLE_VIDEO_CONTROLS, true);
					startActivity(mediaIntent);
				}
			}
		}
	}


	@Override
	public void onCardClick(Card card, int type) {
		if (type == OnCardClickListener.TYPE_LINK) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(card.getUrl()));
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException err) {
				Toast.makeText(getApplicationContext(), R.string.error_connection_failed, LENGTH_SHORT).show();
			}
		} else if (type == OnCardClickListener.TYPE_IMAGE) {
			String imageUrl = card.getImageUrl();
			if (!imageUrl.isEmpty()) {
				Intent mediaIntent = new Intent(this, ImageViewer.class);
				mediaIntent.putExtra(ImageViewer.IMAGE_URI, Uri.parse(card.getImageUrl()));
				startActivity(mediaIntent);
			}
		}
	}


	@Override
	public void onMediaClick(Media media) {
		Uri uri = Uri.parse(media.getUrl());
		if (media.getMediaType() == Media.PHOTO) {
			Intent mediaIntent = new Intent(this, ImageViewer.class);
			mediaIntent.putExtra(ImageViewer.IMAGE_URI, uri);
			startActivity(mediaIntent);
		} else if (media.getMediaType() == Media.VIDEO) {
			if (!settings.isProxyEnabled() || settings.ignoreProxyWarning()) {
				Intent mediaIntent = new Intent(this, VideoViewer.class);
				mediaIntent.putExtra(VideoViewer.VIDEO_URI, uri);
				mediaIntent.putExtra(VideoViewer.ENABLE_VIDEO_CONTROLS, true);
				startActivity(mediaIntent);
			} else {
				confirmDialog.show(ConfirmDialog.PROXY_CONFIRM);
			}
		} else if (media.getMediaType() == Media.GIF) {
			if (!settings.isProxyEnabled() || settings.ignoreProxyWarning()) {
				Intent mediaIntent = new Intent(this, VideoViewer.class);
				mediaIntent.putExtra(VideoViewer.VIDEO_URI, uri);
				mediaIntent.putExtra(VideoViewer.ENABLE_VIDEO_CONTROLS, false);
				startActivity(mediaIntent);
			} else {
				confirmDialog.show(ConfirmDialog.PROXY_CONFIRM);
			}
		}
	}


	@Override
	public void onTagClick(String tag) {
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(KEY_SEARCH_QUERY, tag);
		startActivity(intent);
	}

	/**
	 * called when a link is clicked
	 *
	 * @param tag link string
	 */
	@Override
	public void onLinkClick(String tag) {
		Uri link = Uri.parse(tag);
		// check if the link points to another status
		if (TWITTER_LINK_PATTERN.matcher(link.getScheme() + "://" + link.getHost() + link.getPath()).matches()) {
			List<String> segments = link.getPathSegments();
			Intent intent = new Intent(this, StatusActivity.class);
			intent.putExtra(KEY_STATUS_ID, Long.parseLong(segments.get(2)));
			intent.putExtra(KEY_STATUS_NAME, segments.get(0));
			startActivity(intent);
		}
		// open link in a browser
		else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(link);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException err) {
				Toast.makeText(getApplicationContext(), R.string.error_connection_failed, LENGTH_SHORT).show();
			}
		}
	}


	@Override
	public void onVoteClick(Poll poll, int[] selection) {
		// todo add implementation
	}

	/**
	 * load status into UI
	 *
	 * @param status Tweet information
	 */
	public void setStatus(@NonNull Status status) {
		this.status = status;
		if (status.getEmbeddedStatus() != null) {
			repostNameButton.setVisibility(VISIBLE);
			repostNameButton.setText(status.getAuthor().getScreenname());
			status = status.getEmbeddedStatus();
		} else {
			repostNameButton.setVisibility(GONE);
		}
		User author = status.getAuthor();
		Location location = status.getLocation();
		invalidateOptionsMenu();

		if (status.isReposted()) {
			AppStyles.setDrawableColor(repostButton, settings.getRepostIconColor());
		} else {
			AppStyles.setDrawableColor(repostButton, settings.getIconColor());
		}
		if (status.isFavorited()) {
			AppStyles.setDrawableColor(likeButton, settings.getFavoriteIconColor());
		} else {
			AppStyles.setDrawableColor(likeButton, settings.getIconColor());
		}
		if (status.isBookmarked()) {
			AppStyles.setDrawableColor(bookmarkButton, settings.getBookmarkColor());
		} else {
			AppStyles.setDrawableColor(bookmarkButton, settings.getIconColor());
		}
		if (author.isVerified()) {
			userName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.verify, 0, 0, 0);
			AppStyles.setDrawableColor(userName, settings.getIconColor());
		} else {
			userName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
		if (author.isProtected()) {
			screenName.setCompoundDrawablesWithIntrinsicBounds(R.drawable.lock, 0, 0, 0);
			AppStyles.setDrawableColor(screenName, settings.getIconColor());
		} else {
			screenName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}
		userName.setText(author.getUsername());
		screenName.setText(author.getScreenname());
		createdAt.setText(SimpleDateFormat.getDateTimeInstance().format(status.getTimestamp()));
		replyButton.setText(StringTools.NUMBER_FORMAT.format(status.getReplyCount()));
		likeButton.setText(StringTools.NUMBER_FORMAT.format(status.getFavoriteCount()));
		repostButton.setText(StringTools.NUMBER_FORMAT.format(status.getRepostCount()));
		if (!status.getSource().isEmpty()) {
			statusApi.setText(R.string.tweet_sent_from);
			statusApi.append(status.getSource());
			statusApi.setVisibility(VISIBLE);
		} else {
			statusApi.setVisibility(GONE);
		}
		if (!status.getText().isEmpty()) {
			Spannable spannableText = Tagger.makeTextWithLinks(status.getText(), settings.getHighlightColor(), this);
			statusText.setVisibility(VISIBLE);
			statusText.setText(spannableText);
		} else {
			statusText.setVisibility(GONE);
		}
		if (status.getRepliedStatusId() > 0) {
			if (!status.getReplyName().isEmpty())
				replyName.setText(status.getReplyName());
			else
				replyName.setText(R.string.status_replyname_empty);
			replyName.setVisibility(VISIBLE);
		} else {
			replyName.setVisibility(GONE);
		}
		if (status.isSensitive()) {
			sensitive_media.setVisibility(VISIBLE);
		} else {
			sensitive_media.setVisibility(GONE);
		}
		String profileImageUrl = author.getProfileImageThumbnailUrl();
		if (settings.imagesEnabled() && !profileImageUrl.isEmpty()) {
			Transformation roundCorner = new RoundedCornersTransformation(4, 0);
			picasso.load(profileImageUrl).transform(roundCorner).error(R.drawable.no_image).into(profileImage);
		} else {
			profileImage.setImageResource(0);
		}
		if (location != null) {
			if (!location.getPlace().isEmpty()) {
				locationName.setVisibility(VISIBLE);
				locationName.setText(location.getFullName());
			} else {
				locationName.setVisibility(GONE);
			}
			if (!location.getCoordinates().isEmpty()) {
				locationButton.setVisibility(VISIBLE);
				locationButton.setText(location.getCoordinates());
			} else {
				locationButton.setVisibility(GONE);
			}
		} else {
			locationName.setVisibility(GONE);
			locationButton.setVisibility(GONE);
		}
		if (repostButton.getVisibility() != VISIBLE) {
			if (settings.getLogin().getConfiguration() == Configuration.MASTODON) {
				bookmarkButton.setVisibility(VISIBLE);
			} else {
				bookmarkButton.setVisibility(GONE);
			}
			repostButton.setVisibility(VISIBLE);
			likeButton.setVisibility(VISIBLE);
			replyButton.setVisibility(VISIBLE);
		}
		if ((status.getCards().length > 0 || status.getMedia().length > 0) || status.getPoll() != null) {
			cardList.setVisibility(VISIBLE);
			adapter.replaceAll(status);
			statusText.setMaxLines(5);
		} else {
			cardList.setVisibility(GONE);
			statusText.setMaxLines(10);
		}
	}


	@Override
	public void onResult(StatusResult result) {
		if (result.status != null) {
			setStatus(result.status);
		}
		switch (result.mode) {
			case StatusResult.DATABASE: // update database status
				StatusParam param = new StatusParam(StatusParam.ONLINE, id);
				statusAsync.execute(param, this);
				break;

			case StatusResult.REPOST:
				Toast.makeText(getApplicationContext(), R.string.info_tweet_retweeted, LENGTH_SHORT).show();
				break;

			case StatusResult.UNREPOST:
				Toast.makeText(getApplicationContext(), R.string.info_tweet_unretweeted, LENGTH_SHORT).show();
				// todo remove old retweet from list fragment
				break;

			case StatusResult.FAVORITE:
				if (settings.likeEnabled())
					Toast.makeText(getApplicationContext(), R.string.info_tweet_liked, LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), R.string.info_tweet_favored, LENGTH_SHORT).show();
				break;

			case StatusResult.UNFAVORITE:
				if (settings.likeEnabled())
					Toast.makeText(getApplicationContext(), R.string.info_tweet_unliked, LENGTH_SHORT).show();
				else
					Toast.makeText(getApplicationContext(), R.string.info_tweet_unfavored, LENGTH_SHORT).show();
				break;

			case StatusResult.BOOKMARK:
				Toast.makeText(getApplicationContext(), R.string.info_tweet_bookmarked, LENGTH_SHORT).show();
				break;

			case StatusResult.UNBOOKMARK:
				Toast.makeText(getApplicationContext(), R.string.info_tweet_unbookmarked, LENGTH_SHORT).show();
				break;

			case StatusResult.HIDE:
				hidden = true;
				invalidateOptionsMenu();
				Toast.makeText(getApplicationContext(), R.string.info_reply_hidden, LENGTH_SHORT).show();
				break;

			case StatusResult.UNHIDE:
				hidden = false;
				invalidateOptionsMenu();
				Toast.makeText(getApplicationContext(), R.string.info_reply_unhidden, LENGTH_SHORT).show();
				break;

			case StatusResult.DELETE:
				if (status != null) {
					Toast.makeText(getApplicationContext(), R.string.info_tweet_removed, LENGTH_SHORT).show();
					Intent returnData = new Intent();
					if (status.getEmbeddedStatus() != null)
						returnData.putExtra(INTENT_STATUS_REMOVED_ID, status.getEmbeddedStatus().getId());
					else
						returnData.putExtra(INTENT_STATUS_REMOVED_ID, status.getId());
					setResult(RETURN_STATUS_REMOVED, returnData);
					finish();
				}
				break;

			case StatusResult.ERROR:
				String message = ErrorHandler.getErrorMessage(this, result.exception);
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
				if (status == null) {
					finish();
				} else if (result.exception != null && result.exception.getErrorCode() == ConnectionException.RESOURCE_NOT_FOUND) {
					// Mark status as removed, so it can be removed from the list
					Intent returnData = new Intent();
					returnData.putExtra(INTENT_STATUS_REMOVED_ID, status.getId());
					setResult(RETURN_STATUS_REMOVED, returnData);
					finish();
				}
				break;
		}
	}
}