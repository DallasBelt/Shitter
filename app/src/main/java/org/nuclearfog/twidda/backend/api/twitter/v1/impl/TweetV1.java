package org.nuclearfog.twidda.backend.api.twitter.v1.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.nuclearfog.twidda.backend.utils.StringUtils;
import org.nuclearfog.twidda.model.Card;
import org.nuclearfog.twidda.model.Emoji;
import org.nuclearfog.twidda.model.Location;
import org.nuclearfog.twidda.model.Media;
import org.nuclearfog.twidda.model.Metrics;
import org.nuclearfog.twidda.model.Poll;
import org.nuclearfog.twidda.model.Status;
import org.nuclearfog.twidda.model.User;

/**
 * API v 1.1 implementation of a tweet
 *
 * @author nuclearfog
 */
public class TweetV1 implements Status {

	private static final long serialVersionUID = 70666106496232760L;

	/**
	 * query parameter to enable extended mode to show tweets with more than 140 characters
	 */
	public static final String PARAM_EXT_MODE = "tweet_mode=extended";

	/**
	 * query parameter to include ID of the retweet if available
	 */
	public static final String PARAM_INCL_RETWEET = "include_my_retweet=true";

	/**
	 * query parameter to include entities like urls, media or user mentions
	 */
	public static final String PARAM_ENTITIES = "include_entities=true";

	private long id;
	private long timestamp;
	private User author;
	private Status embeddedTweet;
	private Location location;
	private Media[] medias = {};

	private long retweetId;
	private int retweetCount;
	private int favoriteCount;
	private boolean isSensitive;
	private boolean isRetweeted;
	private boolean isFavorited;
	private String userMentions;
	private String text;
	private String source;
	private String host;

	private long replyUserId;
	private long replyTweetId;
	private String replyName = "";

	/**
	 * @param json      JSON object of a single tweet
	 * @param twitterId ID of the current user
	 * @throws JSONException if values are missing
	 */
	public TweetV1(JSONObject json, String host, long twitterId) throws JSONException {
		JSONObject locationJson = json.optJSONObject("place");
		JSONObject currentUserJson = json.optJSONObject("current_user_retweet");
		JSONObject embeddedTweetJson = json.optJSONObject("retweeted_status");
		JSONObject extEntities = json.optJSONObject("extended_entities");
		String tweetIdStr = json.optString("id_str", "");
		String replyNameStr = json.optString("in_reply_to_screen_name", "");
		String replyTweetIdStr = json.optString("in_reply_to_status_id_str", "0");
		String replyUsrIdStr = json.optString("in_reply_to_user_id_str", "0");
		String sourceStr = json.optString("source", "");
		String textStr = createText(json);

		author = new UserV1(json.getJSONObject("user"), twitterId);
		retweetCount = json.optInt("retweet_count");
		favoriteCount = json.optInt("favorite_count");
		isFavorited = json.optBoolean("favorited");
		isRetweeted = json.optBoolean("retweeted");
		isSensitive = json.optBoolean("possibly_sensitive");
		timestamp = StringUtils.getTime(json.optString("created_at", ""), StringUtils.TIME_TWITTER_V1);
		userMentions = StringUtils.getUserMentions(textStr, author.getScreenname());
		source = Jsoup.parse(sourceStr).text();
		this.host = host;
		// add reply name
		if (!replyNameStr.isEmpty() && !replyNameStr.equals("null")) {
			replyName = '@' + replyNameStr;
		}
		// add embedded tweet
		if (embeddedTweetJson != null) {
			embeddedTweet = new TweetV1(embeddedTweetJson, host, twitterId);
		}
		// add location
		if (locationJson != null) {
			location = new LocationV1(locationJson);
		}
		// remove short media link
		int linkPos = textStr.lastIndexOf("https://t.co/");
		if (linkPos >= 0) {
			text = textStr.substring(0, linkPos);
		} else {
			text = textStr;
		}
		// get retweet ID
		String retweetIdStr = "-1";
		if (currentUserJson != null) {
			retweetIdStr = currentUserJson.optString("id_str", "0");
		}
		// add media
		if (extEntities != null) {
			JSONArray mediaArray = extEntities.optJSONArray("media");
			if (mediaArray != null && mediaArray.length() > 0) {
				medias = new Media[mediaArray.length()];
				for (int i = 0; i < mediaArray.length(); i++) {
					JSONObject mediaItem = mediaArray.getJSONObject(i);
					medias[i] = new MediaV1(mediaItem);
				}
			}
		}
		// parse string IDs
		try {
			id = Long.parseLong(tweetIdStr);
			if (!retweetIdStr.equals("null"))
				retweetId = Long.parseLong(retweetIdStr);
			if (!replyTweetIdStr.equals("null"))
				replyTweetId = Long.parseLong(replyTweetIdStr);
			if (!replyUsrIdStr.equals("null"))
				replyUserId = Long.parseLong(replyUsrIdStr);
		} catch (NumberFormatException e) {
			throw new JSONException("bad ID:" + tweetIdStr + "," + replyUsrIdStr + "," + retweetIdStr);
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
		return timestamp;
	}


	@Override
	public String getSource() {
		return source;
	}


	@Nullable
	@Override
	public Status getEmbeddedStatus() {
		return embeddedTweet;
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
		return replyTweetId;
	}


	@Override
	public long getRepostId() {
		return retweetId;
	}


	@Override
	public int getRepostCount() {
		return retweetCount;
	}


	@Override
	public int getFavoriteCount() {
		return favoriteCount;
	}


	@Override
	public int getReplyCount() {
		// not implemented in API V1.1
		return 0;
	}


	@Override
	public int getVisibility() {
		return author.isProtected() ? VISIBLE_PRIVATE : VISIBLE_PUBLIC;
	}


	@NonNull
	@Override
	public Media[] getMedia() {
		return medias;
	}


	@NonNull
	@Override
	public Emoji[] getEmojis() {
		return new Emoji[0];
	}


	@Override
	public String getUserMentions() {
		return userMentions;
	}


	@Override
	public String getLanguage() {
		return ""; // todo implement this
	}


	@Override
	public boolean isSensitive() {
		return isSensitive;
	}


	@Override
	public boolean isSpoiler() {
		return false;
	}


	@Override
	public boolean isReposted() {
		return isRetweeted;
	}


	@Override
	public boolean isFavorited() {
		return isFavorited;
	}


	@Override
	public boolean isBookmarked() {
		return false;
	}


	@Override
	@Nullable
	public Location getLocation() {
		return location;
	}


	@Override
	public boolean isHidden() {
		return false;
	}


	@Override
	public String getUrl() {
		if (author.getScreenname().length() > 1) {
			return host + '/' + author.getScreenname().substring(1) + "/status/" + id;
		}
		return "";
	}


	@NonNull
	@Override
	public Card[] getCards() {
		return new Card[0];
	}


	@Nullable
	@Override
	public Poll getPoll() {
		return null;
	}


	@Nullable
	@Override
	public Metrics getMetrics() {
		// Twitter API 1.1 doesn't support metrics
		return null;
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof Status))
			return false;
		Status status = ((Status) obj);
		return status.getId() == id && status.getTimestamp() == getTimestamp() && status.getAuthor().equals(getAuthor());
	}


	@NonNull
	@Override
	public String toString() {
		return "from=\"" + getAuthor().getScreenname() + "\" text=\"" + getText() + "\"";
	}

	/**
	 * enable/disable retweet status and count
	 *
	 * @param isRetweeted true if this tweet should be retweeted
	 */
	public void setRetweet(boolean isRetweeted) {
		this.isRetweeted = isRetweeted;
		// fix: Twitter API v1.1 doesn't increment/decrement retweet count right
		if (!isRetweeted && retweetCount > 0) {
			retweetCount--;
		}
		if (embeddedTweet instanceof TweetV1) {
			((TweetV1) embeddedTweet).setRetweet(isRetweeted);
		}
	}

	/**
	 * enable/disable favorite status and count
	 *
	 * @param isFavorited true if this tweet should be favorited
	 */
	public void setFavorite(boolean isFavorited) {
		this.isFavorited = isFavorited;
		// fix: Twitter API v1.1 doesn't increment/decrement favorite count right
		if (!isFavorited && favoriteCount > 0) {
			favoriteCount--;
		}
		if (embeddedTweet instanceof TweetV1) {
			((TweetV1) embeddedTweet).setFavorite(isFavorited);
		}
	}

	/**
	 * overwrite embedded tweet information
	 *
	 * @param tweet new embedded tweet
	 */
	public void setEmbeddedTweet(@Nullable Status tweet) {
		this.embeddedTweet = tweet;
	}

	/**
	 * read tweet and expand urls
	 */
	@NonNull
	private String createText(JSONObject json) {
		String text = json.optString("full_text", "");
		JSONObject entities = json.optJSONObject("entities");
		if (entities != null) {
			JSONArray urls = entities.optJSONArray("urls");
			if (urls != null) {
				try {
					// check for shortened urls and replace them with full urls
					StringBuilder builder = new StringBuilder(text);
					for (int i = urls.length() - 1; i >= 0; i--) {
						JSONObject entry = urls.getJSONObject(i);
						String link = entry.getString("expanded_url");
						JSONArray indices = entry.getJSONArray("indices");
						int start = indices.getInt(0);
						int end = indices.getInt(1);
						int offset = StringUtils.calculateIndexOffset(text, start);
						builder.replace(start + offset, end + offset, link);
					}
					return StringUtils.unescapeString(builder.toString());
				} catch (JSONException e) {
					// use default tweet text
				}
			}
		}
		return StringUtils.unescapeString(text);
	}
}