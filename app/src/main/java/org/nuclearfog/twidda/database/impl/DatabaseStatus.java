package org.nuclearfog.twidda.database.impl;

import static org.nuclearfog.twidda.database.AppDatabase.MASK_STATUS_BOOKMARKED;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_STATUS_FAVORITED;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_STATUS_HIDDEN;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_STATUS_SENSITIVE;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_STATUS_REPOSTED;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_STATUS_SPOILER;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.database.DatabaseAdapter.StatusRegisterTable;
import org.nuclearfog.twidda.database.DatabaseAdapter.StatusTable;
import org.nuclearfog.twidda.model.Account;
import org.nuclearfog.twidda.model.Card;
import org.nuclearfog.twidda.model.Emoji;
import org.nuclearfog.twidda.model.Location;
import org.nuclearfog.twidda.model.Media;
import org.nuclearfog.twidda.model.Metrics;
import org.nuclearfog.twidda.model.Poll;
import org.nuclearfog.twidda.model.Status;
import org.nuclearfog.twidda.model.User;

import java.util.regex.Pattern;

/**
 * Implementation of a database STATUS
 *
 * @author nuclearfog
 */
public class DatabaseStatus implements Status {

	private static final long serialVersionUID = -5957556706939766801L;

	private static final Pattern MEDIA_SEPARATOR = Pattern.compile(";");

	private long id;
	private long time;
	private long embeddedId;
	private long replyID;
	private long replyUserId;
	private long myRepostId;
	private long conversationId;
	private long locationId;
	private long pollId;
	private Status embedded;
	private String[] mediaKeys = {};
	private String[] emojiKeys = {};
	private Media[] medias = {};
	private Emoji[] emojis = {};
	private Card[] cards = {};
	private Poll poll;
	private User author;
	private Location location;
	private int repostCount;
	private int favoriteCount;
	private int replyCount;
	private String replyName = "";
	private String text = "";
	private String source = "";
	private String userMentions = "";
	private String statusUrl = "";
	private String language = "";
	private boolean reposted;
	private boolean favorited;
	private boolean bookmarked;
	private boolean sensitive;
	private boolean spoiler;
	private boolean isHidden;

	/**
	 * @param cursor  database cursor
	 * @param account current user login information
	 */
	public DatabaseStatus(Cursor cursor, Account account) {
		author = new DatabaseUser(cursor, account);
		time = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.TIME));
		repostCount = cursor.getInt(cursor.getColumnIndexOrThrow(StatusTable.REPOST));
		favoriteCount = cursor.getInt(cursor.getColumnIndexOrThrow(StatusTable.FAVORITE));
		replyCount = cursor.getInt(cursor.getColumnIndexOrThrow(StatusTable.REPLY));
		id = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.ID));
		replyID = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.REPLYSTATUS));
		locationId = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.LOCATION));
		pollId = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.POLL));
		replyUserId = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.REPLYUSER));
		embeddedId = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.EMBEDDED));
		myRepostId = cursor.getLong(cursor.getColumnIndexOrThrow(StatusRegisterTable.REPOST_ID));
		conversationId = cursor.getLong(cursor.getColumnIndexOrThrow(StatusTable.CONVERSATION));
		String statusUrl = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.URL));
		String language = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.LANGUAGE));
		String mediaKeys = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.MEDIA));
		String emojiKeys = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.EMOJI));
		String source = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.SOURCE));
		String text = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.TEXT));
		String replyName = cursor.getString(cursor.getColumnIndexOrThrow(StatusTable.REPLYNAME));
		int register = cursor.getInt(cursor.getColumnIndexOrThrow(StatusRegisterTable.REGISTER));

		favorited = (register & MASK_STATUS_FAVORITED) != 0;
		reposted = (register & MASK_STATUS_REPOSTED) != 0;
		sensitive = (register & MASK_STATUS_SENSITIVE) != 0;
		isHidden = (register & MASK_STATUS_HIDDEN) != 0;
		bookmarked = (register & MASK_STATUS_BOOKMARKED) != 0;
		spoiler = (register & MASK_STATUS_SPOILER) != 0;

		if (mediaKeys != null && !mediaKeys.isEmpty())
			this.mediaKeys = MEDIA_SEPARATOR.split(mediaKeys);
		if (emojiKeys != null && !emojiKeys.isEmpty())
			this.emojiKeys = MEDIA_SEPARATOR.split(emojiKeys);
		if (statusUrl != null)
			this.statusUrl = statusUrl;
		if (language != null)
			this.language = language;
		if (replyName != null)
			this.replyName = replyName;
		if (source != null)
			this.source = source;
		if (text != null) {
			this.text = text;
			userMentions = StringTools.getUserMentions(text, author.getScreenname());
		}
	}


	@Override
	public long getId() {
		return id;
	}


	@Override
	public String getText() {
		return text;
	}


	@Override
	public User getAuthor() {
		return author;
	}


	@Override
	public long getTimestamp() {
		return time;
	}


	@Override
	public String getSource() {
		return source;
	}


	@Nullable
	@Override
	public Status getEmbeddedStatus() {
		return embedded;
	}


	@Override
	public String getReplyName() {
		return replyName;
	}


	@Override
	public long getRepliedUserId() {
		return replyUserId;
	}


	@Override
	public long getRepliedStatusId() {
		return replyID;
	}


	@Override
	public long getRepostId() {
		return myRepostId;
	}


	@Override
	public long getConversationId() {
		return conversationId;
	}


	@Override
	public int getRepostCount() {
		return repostCount;
	}


	@Override
	public int getFavoriteCount() {
		return favoriteCount;
	}


	@Override
	public int getReplyCount() {
		return replyCount;
	}


	@NonNull
	@Override
	public Media[] getMedia() {
		return medias;
	}


	@NonNull
	@Override
	public Emoji[] getEmojis() {
		return emojis;
	}


	@Override
	public String getUserMentions() {
		return userMentions;
	}


	@Override
	public String getLanguage() {
		return language;
	}


	@Override
	public boolean isSensitive() {
		return sensitive;
	}


	@Override
	public boolean isSpoiler() {
		return spoiler;
	}


	@Override
	public boolean isReposted() {
		return reposted;
	}


	@Override
	public boolean isFavorited() {
		return favorited;
	}


	@Override
	public boolean isBookmarked() {
		return bookmarked;
	}


	@Override
	@Nullable
	public Location getLocation() {
		return location;
	}


	@Override
	public boolean isHidden() {
		return isHidden;
	}


	@Override
	public String getUrl() {
		return statusUrl;
	}


	@NonNull
	@Override
	public Card[] getCards() {
		return cards;
	}


	@Nullable
	@Override
	public Poll getPoll() {
		return poll;
	}


	@Nullable
	@Override
	public Metrics getMetrics() {
		return null;
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof Status))
			return false;
		Status status = ((Status) obj);
		return status.getId() == id && status.getTimestamp() == time && status.getAuthor().equals(author);
	}


	@Override
	public int compareTo(Status status) {
		return Long.compare(status.getTimestamp(), time);
	}


	@NonNull
	@Override
	public String toString() {
		return "from=\"" + author.getScreenname() + "\" text=\"" + text + "\"";
	}

	/**
	 * get ID of the embedded status
	 *
	 * @return ID of the
	 */
	public long getEmbeddedStatusId() {
		return embeddedId;
	}

	/**
	 * @return media keys
	 */
	public String[] getMediaKeys() {
		return mediaKeys;
	}

	/**
	 * @return emoji keys
	 */
	public String[] getEmojiKeys() {
		return emojiKeys;
	}

	/**
	 * @return location ID
	 */
	public long getLocationId() {
		return locationId;
	}

	/**
	 * @return ID of an attached poll or '0'
	 */
	public long getPollId() {
		return pollId;
	}

	/**
	 * attach status referenced by {@link #embeddedId}
	 *
	 * @param embedded embedded status
	 */
	public void setEmbeddedStatus(Status embedded) {
		this.embedded = embedded;
	}

	/**
	 * add status media
	 *
	 * @param medias media array
	 */
	public void addMedia(@NonNull Media[] medias) {
		this.medias = medias;
	}

	/**
	 * add status media
	 *
	 * @param emojis media array
	 */
	public void addEmojis(@NonNull Emoji[] emojis) {
		this.emojis = emojis;
	}

	/**
	 * add location information
	 *
	 * @param location location item
	 */
	public void addLocation(Location location) {
		this.location = location;
	}

	/**
	 * add poll
	 *
	 * @param poll poll item
	 */
	public void addPoll(@Nullable Poll poll) {
		this.poll = poll;
	}
}