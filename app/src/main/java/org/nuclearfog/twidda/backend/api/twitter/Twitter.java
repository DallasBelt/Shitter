package org.nuclearfog.twidda.backend.api.twitter;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuclearfog.twidda.BuildConfig;
import org.nuclearfog.twidda.backend.api.Connection;
import org.nuclearfog.twidda.backend.api.ConnectionException;
import org.nuclearfog.twidda.backend.api.twitter.impl.TwitterAccount;
import org.nuclearfog.twidda.backend.api.twitter.impl.TwitterNotification;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.LocationV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.MessageV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.RelationV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.TrendV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.TweetV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.UserListV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v1.UserV1;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.LocationV2;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.MediaV2;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.PollV2;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.TweetV2;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.UserV2;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.maps.LocationV2Map;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.maps.MediaV2Map;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.maps.PollV2Map;
import org.nuclearfog.twidda.backend.api.twitter.impl.v2.maps.UserV2Map;
import org.nuclearfog.twidda.backend.lists.Messages;
import org.nuclearfog.twidda.backend.lists.UserLists;
import org.nuclearfog.twidda.backend.lists.Users;
import org.nuclearfog.twidda.backend.update.MediaStatus;
import org.nuclearfog.twidda.backend.update.ProfileUpdate;
import org.nuclearfog.twidda.backend.update.StatusUpdate;
import org.nuclearfog.twidda.backend.update.UserListUpdate;
import org.nuclearfog.twidda.backend.utils.ConnectionBuilder;
import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.database.FilterDatabase;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.model.Account;
import org.nuclearfog.twidda.model.Location;
import org.nuclearfog.twidda.model.Notification;
import org.nuclearfog.twidda.model.Relation;
import org.nuclearfog.twidda.model.Status;
import org.nuclearfog.twidda.model.Trend;
import org.nuclearfog.twidda.model.User;
import org.nuclearfog.twidda.model.UserList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * Twitter API 1.1/2.0 implementation
 *
 * @author nuclearfog
 */
public class Twitter implements Connection {

	private static final String OAUTH = "1.0";

	// API addresses
	public static final String API = "https://api.twitter.com";
	private static final String UPLOAD = "https://upload.twitter.com";
	private static final String DOWNLOAD = "https://ton.twitter.com/";

	// authentication endpoints
	public static final String AUTHENTICATE = API + "/oauth/authenticate";
	private static final String REQUEST_TOKEN = API + "/oauth/request_token";
	private static final String OAUTH_VERIFIER = API + "/oauth/access_token";
	private static final String CREDENTIALS = API + "/1.1/account/verify_credentials.json";

	// user ID endpoints
	private static final String IDS_BLOCKED_USERS = API + "/1.1/blocks/ids.json";
	private static final String IDS_MUTED_USERS = API + "/1.1/mutes/users/ids.json";

	// user endpoints
	private static final String USERS_MUTES = API + "/1.1/mutes/users/list.json";
	private static final String USER_FOLLOW = API + "/1.1/friendships/create.json";
	private static final String USER_UNFOLLOW = API + "/1.1/friendships/destroy.json";
	private static final String USER_BLOCK = API + "/1.1/blocks/create.json";
	private static final String USER_UNBLOCK = API + "/1.1/blocks/destroy.json";
	private static final String USER_MUTE = API + "/1.1/mutes/users/create.json";
	private static final String USER_UNMUTE = API + "/1.1/mutes/users/destroy.json";
	private static final String USER_LOOKUP = API + "/1.1/users/show.json";
	private static final String USERS_FOLLOWING = API + "/1.1/friends/list.json";
	private static final String USERS_FOLLOWER = API + "/1.1/followers/list.json";
	private static final String USERS_SEARCH = API + "/1.1/users/search.json";
	private static final String USERS_LIST_MEMBER = API + "/1.1/lists/members.json";
	private static final String USERS_LIST_SUBSCRIBER = API + "/1.1/lists/subscribers.json";
	private static final String USERS_LOOKUP = API + "/1.1/users/lookup.json";
	private static final String USERS_BLOCKED_LIST = API + "/1.1/blocks/list.json";
	private static final String USERS_FOLLOW_INCOMING = API + "/1.1/friendships/incoming.json";
	private static final String USERS_FOLLOW_OUTGOING = API + "/1.1/friendships/outgoing.json";

	// tweet endpoints
	private static final String TWEETS_HOME_TIMELINE = API + "/1.1/statuses/home_timeline.json";
	private static final String TWEETS_MENTIONS = API + "/1.1/statuses/mentions_timeline.json";
	private static final String TWEETS_USER = API + "/1.1/statuses/user_timeline.json";
	private static final String TWEETS_USER_FAVORITS = API + "/1.1/favorites/list.json";
	private static final String TWEETS_LIST = API + "/1.1/lists/statuses.json";
	private static final String TWEET_LOOKUP = API + "/1.1/statuses/show.json";
	private static final String TWEET2_LOOKUP = API + "/2/tweets/";
	private static final String TWEET_SEARCH = API + "/1.1/search/tweets.json";
	private static final String TWEET_FAVORITE = API + "/1.1/favorites/create.json";
	private static final String TWEET_UNFAVORITE = API + "/1.1/favorites/destroy.json";
	private static final String TWEET_RETWEET = API + "/1.1/statuses/retweet/";
	private static final String TWEET_UNRETWEET = API + "/1.1/statuses/unretweet/";
	private static final String TWEET_UPLOAD = API + "/1.1/statuses/update.json";
	private static final String TWEET_DELETE = API + "/1.1/statuses/destroy/";
	private static final String TWEET_SEARCH_2 = API + "/2/tweets/search/recent";
	private static final String TWEET_UNI = API + "/2/tweets/";

	// userlist endpoints
	private static final String USERLIST_SHOW = API + "/1.1/lists/show.json";
	private static final String USERLIST_FOLLOW = API + "/1.1/lists/subscribers/create.json";
	private static final String USERLIST_UNFOLLOW = API + "/1.1/lists/subscribers/destroy.json";
	private static final String USERLIST_CREATE = API + "/1.1/lists/create.json";
	private static final String USERLIST_UPDATE = API + "/1.1/lists/update.json";
	private static final String USERLIST_DESTROY = API + "/1.1/lists/destroy.json";
	private static final String USERLIST_OWNERSHIP = API + "/1.1/lists/list.json";
	private static final String USERLIST_MEMBERSHIP = API + "/1.1/lists/memberships.json";
	private static final String USERLIST_ADD_USER = API + "/1.1/lists/members/create.json";
	private static final String USERLIST_DEL_USER = API + "/1.1/lists/members/destroy.json";

	// directmessage endpoints
	private static final String DIRECTMESSAGE = API + "/1.1/direct_messages/events/list.json";
	private static final String DIRECTMESSAGE_CREATE = API + "/1.1/direct_messages/events/new.json";
	private static final String DIRECTMESSAGE_DELETE = API + "/1.1/direct_messages/events/destroy.json";

	// profile update endpoints
	private static final String PROFILE_UPDATE = API + "/1.1/account/update_profile.json";
	private static final String PROFILE_UPDATE_IMAGE = API + "/1.1/account/update_profile_image.json";
	private static final String PROFILE_UPDATE_BANNER = API + "/1.1/account/update_profile_banner.json";

	// other endpoints
	private static final String TRENDS = API + "/1.1/trends/place.json";
	private static final String LOCATIONS = API + "/1.1/trends/available.json";
	private static final String RELATION = API + "/1.1/friendships/show.json";
	private static final String MEDIA_UPLOAD = UPLOAD + "/1.1/media/upload.json";

	private static final MediaType TYPE_STREAM = MediaType.parse("application/octet-stream");
	private static final MediaType TYPE_JSON = MediaType.parse("application/json");
	private static final MediaType TYPE_TEXT = MediaType.parse("text/plain");

	private static final String JSON = ".json";

	/**
	 * To upload big files like videos, files must be chunked in segments.
	 * Twitter can handle up to 1000 segments with max 5MB.
	 */
	private static final int CHUNK_MAX_BYTES = 1024 * 1024;

	/**
	 * maximum polling request
	 */
	private static final int POLLING_MAX_RETRIES = 12;

	private OkHttpClient client;
	private GlobalSettings settings;
	private FilterDatabase filterDatabase;
	private Tokens tokens;

	/**
	 * initiate singleton instance
	 *
	 * @param context context used to initiate databases
	 */
	public Twitter(Context context) {
		settings = GlobalSettings.getInstance(context);
		tokens = Tokens.getInstance(context);
		filterDatabase = new FilterDatabase(context);
		client = ConnectionBuilder.create(context, 0);
	}

	/**
	 * create authorisation link
	 *
	 * @param params optional string parameters containing oauth token, token secret
	 * @return a temporary oauth token created by Twitter
	 */
	@Override
	public String getAuthorisationLink(String... params) throws TwitterException {
		try {
			Response response;
			if (params.length == 2)
				response = post(REQUEST_TOKEN, new ArrayList<>(), params[0], params[1]);
			else
				response = post(REQUEST_TOKEN, new ArrayList<>(), tokens.getConsumerKey(true), tokens.getConsumerSecret(true));
			ResponseBody body = response.body();
			if (response.code() == 200 && body != null) {
				String res = body.string();
				// extract oauth_token from url
				Uri uri = Uri.parse(AUTHENTICATE + "?" + res);
				String requestToken = uri.getQueryParameter("oauth_token");
				return Twitter.AUTHENTICATE + "?oauth_token=" + requestToken;
			}
			throw new TwitterException(response);
		} catch (IOException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * login to twitter using pin and add store access tokens
	 *
	 * @param paramsStr parameters (oauth pin & authorisation link required, oauth token, token secret optional)
	 */
	@Override
	public Account loginApp(String... paramsStr) throws TwitterException {
		try {
			List<String> params = new ArrayList<>();
			String tempOauthToken = Uri.parse(paramsStr[0]).getQueryParameter("oauth_token");
			params.add("oauth_verifier=" + paramsStr[1]);
			params.add("oauth_token=" + tempOauthToken);
			Response response;
			if (paramsStr.length == 4)
				response = post(OAUTH_VERIFIER, params, paramsStr[2], paramsStr[3]);
			else
				response = post(OAUTH_VERIFIER, params, tokens.getConsumerKey(true), tokens.getConsumerSecret(true));
			ResponseBody body = response.body();
			if (response.code() == 200 && body != null) {
				// extract tokens from link
				String res = body.string();
				Uri uri = Uri.parse(OAUTH_VERIFIER + "?" + res);
				String oauthToken = uri.getQueryParameter("oauth_token");
				String tokenSecret = uri.getQueryParameter("oauth_token_secret");
				// check if login works
				User user;
				Account account;
				if (paramsStr.length == 4) { // use custom API keys
					user = getCredentials(paramsStr[2], paramsStr[3], oauthToken, tokenSecret);
					account = new TwitterAccount(oauthToken, tokenSecret, paramsStr[2], paramsStr[3], user);
				} else { // use default API keys
					user = getCredentials(tokens.getConsumerKey(true), tokens.getConsumerSecret(true), oauthToken, tokenSecret);
					account = new TwitterAccount(oauthToken, tokenSecret, user);
				}
				// save login credentials
				settings.setLogin(account, false);
				return account;
			}
			throw new TwitterException(response);
		} catch (IOException e) {
			throw new TwitterException(e);
		}
	}


	@Override
	public User showUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		return getUser1(USER_LOOKUP, params);
	}


	@Override
	public User showUser(String name) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (name.startsWith("@"))
			name = name.substring(1);
		params.add("screen_name=" + StringTools.encode(name));
		return getUser1(USER_LOOKUP, params);
	}


	@Override
	public Users searchUsers(String search, long page) throws TwitterException {
		// search endpoint only supports pages parameter
		long currentPage = page > 0 ? page : 1;
		long nextPage = currentPage + 1;

		List<String> params = new ArrayList<>();
		params.add("q=" + StringTools.encode(search));
		params.add("page=" + currentPage);
		Users result = getUsers1(USERS_SEARCH, params);
		// notice that there are no more results
		// if result size is less than the requested size
		if (result.size() < settings.getListSize())
			nextPage = 0;
		if (settings.filterResults())
			filterUsers(result);
		result.setPrevCursor(currentPage - 1);
		result.setNextCursor(nextPage);
		return result;
	}


	@Override
	public Users getRepostingUsers(long tweetId) throws TwitterException {
		String endpoint = TWEET_UNI + tweetId + "/retweeted_by";
		return getUsers2(endpoint);
	}


	@Override
	public Users getFavoritingUsers(long tweetId) throws TwitterException {
		String endpoint = TWEET_UNI + tweetId + "/liking_users";
		return getUsers2(endpoint);
	}


	@Override
	public Users getFollowing(long id, long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		params.add("cursor=" + cursor);
		return getUsers1(USERS_FOLLOWING, params);
	}


	@Override
	public Users getFollower(long id, long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		params.add("cursor=" + cursor);
		return getUsers1(USERS_FOLLOWER, params);
	}


	@Override
	public Users getListMember(long id, long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + id);
		params.add("cursor=" + cursor);
		Users result = getUsers1(USERS_LIST_MEMBER, params);
		// fix API returns zero previous_cursor when the end of the list is reached
		// override previous cursor
		if (cursor == -1L)
			result.setPrevCursor(0);
		else
			result.setPrevCursor(cursor);
		return result;
	}


	@Override
	public Users getListSubscriber(long id, long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + id);
		params.add("cursor=" + cursor);
		Users result = getUsers1(USERS_LIST_SUBSCRIBER, params);
		// fix API returns zero previous_cursor when the end of the list is reached
		// override previous cursor
		if (cursor == -1L)
			result.setPrevCursor(0);
		else
			result.setPrevCursor(cursor);
		return result;
	}


	@Override
	public Users getBlockedUsers(long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("cursor=" + cursor);
		return getUsers1(USERS_BLOCKED_LIST, params);
	}


	@Override
	public Users getMutedUsers(long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("cursor=" + cursor);
		return getUsers1(USERS_MUTES, params);
	}


	@Override
	public Users getIncomingFollowRequests(long cursor) throws TwitterException {
		long[] ids = getUserIDs(USERS_FOLLOW_INCOMING, cursor);
		// remove last array entry (cursor) from ID list
		return getUsers1(Arrays.copyOf(ids, ids.length - 1));
	}


	@Override
	public Users getOutgoingFollowRequests(long cursor) throws TwitterException {
		long[] ids = getUserIDs(USERS_FOLLOW_OUTGOING, cursor);
		// remove last array entry (cursor) from ID list
		return getUsers1(Arrays.copyOf(ids, ids.length - 1));
	}


	@Override
	public Relation getUserRelationship(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("source_id=" + settings.getLogin().getId());
		params.add("target_id=" + id);
		try {
			Response response = get(RELATION, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				return new RelationV1(json);
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}


	@Override
	public void followUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		getUser1(USER_FOLLOW, params);
	}


	@Override
	public void unfollowUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		getUser1(USER_UNFOLLOW, params);
	}


	@Override
	public void blockUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		User user = getUser1(USER_BLOCK, params);
		filterDatabase.addUser(user.getId());
	}


	@Override
	public void blockUser(String name) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (name.startsWith("@"))
			name = name.substring(1);
		params.add("screen_name=" + StringTools.encode(name));
		User user = getUser1(USER_BLOCK, params);
		filterDatabase.addUser(user.getId());
	}


	@Override
	public void unblockUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		getUser1(USER_UNBLOCK, params);
	}


	@Override
	public void muteUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		getUser1(USER_MUTE, params);
	}


	@Override
	public void muteUser(String name) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (name.startsWith("@"))
			name = name.substring(1);
		params.add("screen_name=" + StringTools.encode(name));
		getUser1(USER_MUTE, params);
	}


	@Override
	public void unmuteUser(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("user_id=" + id);
		getUser1(USER_UNMUTE, params);
	}


	@Override
	public List<Status> searchStatuses(String search, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		params.add("q=" + StringTools.encode(search + " +exclude:retweets"));
		params.add("result_type=recent");
		List<Status> result = getTweets1(TWEET_SEARCH, params);
		if (settings.filterResults())
			filterTweets(result);
		return result;
	}


	@Override
	public List<Trend> getTrends() throws TwitterException {
		long id = settings.getTrendLocation().getId();
		List<String> params = new ArrayList<>();
		params.add("id=" + id);
		try {
			Response response = get(TRENDS, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONArray json = new JSONArray(body.string());
				JSONArray trends = json.getJSONObject(0).getJSONArray("trends");
				List<Trend> result = new ArrayList<>(trends.length() + 1);

				for (int pos = 0; pos < trends.length(); pos++) {
					JSONObject trend = trends.getJSONObject(pos);
					result.add(new TrendV1(trend, pos, id));
				}
				return result;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}


	@Override
	public List<Location> getLocations() throws TwitterException {
		try {
			Response response = get(LOCATIONS, new ArrayList<>(0));
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONArray locations = new JSONArray(body.string());
				List<Location> result = new ArrayList<>(locations.length() + 1);
				for (int pos = 0; pos < locations.length(); pos++) {
					JSONObject location = locations.getJSONObject(pos);
					result.add(new LocationV1(location));
				}
				return result;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}


	@Override
	public List<Status> getHomeTimeline(long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 0)
			params.add("max_id=" + maxId);
		return getTweets1(TWEETS_HOME_TIMELINE, params);
	}


	@Override
	public List<Status> getUserTimeline(long id, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		params.add("user_id=" + id);
		return getTweets1(TWEETS_USER, params);
	}


	@Override
	public List<Status> getUserTimeline(String name, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		params.add("screen_name=" + StringTools.encode(name));
		return getTweets1(TWEETS_USER, params);
	}


	@Override
	public List<Status> getUserFavorits(long id, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		params.add("user_id=" + id);
		return getTweets1(TWEETS_USER_FAVORITS, params);
	}


	@Override
	public List<Status> getUserFavorits(String name, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		params.add("screen_name=" + StringTools.encode(name));
		return getTweets1(TWEETS_USER_FAVORITS, params);
	}


	@Override
	public List<Status> getUserlistStatuses(long id, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		params.add("list_id=" + id);
		return getTweets1(TWEETS_LIST, params);
	}


	@Override
	public List<Status> getStatusReplies(long id, long minId, long maxId) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("until_id=" + maxId);
		params.add("query=" + StringTools.encode("conversation_id:" + id));
		List<Status> result = getTweets2(TWEET_SEARCH_2, params);
		List<Status> replies = new LinkedList<>();
		// chose only the first tweet of a conversation
		for (Status reply : result) {
			if (reply.getRepliedStatusId() == id) {
				replies.add(reply);
			}
		}
		if (settings.filterResults() && !replies.isEmpty())
			filterTweets(replies);
		return replies;
	}


	@Override
	public Status showStatus(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("id=" + id);
		Status status = getTweet1(TWEET_LOOKUP, params);
		try {
			params.clear();
			return getTweet2(TWEET2_LOOKUP + id, params, status);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return status;
	}


	@Override
	public Status favoriteStatus(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("id=" + id);
		TweetV1 result = getTweet1(TWEET_FAVORITE, params);
		result.setFavorite(true);
		return result;
	}


	@Override
	public Status unfavoriteStatus(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("id=" + id);
		TweetV1 result = getTweet1(TWEET_UNFAVORITE, params);
		result.setFavorite(false);
		return result;
	}


	@Override
	public Status repostStatus(long id) throws TwitterException {
		TweetV1 result = getTweet1(TWEET_RETWEET + id + JSON, new ArrayList<>());
		result.setRetweet(true);
		return result;
	}


	@Override
	public Status removeRepost(long id) throws TwitterException {
		TweetV1 result = getTweet1(TWEET_UNRETWEET + id + JSON, new ArrayList<>());
		result.setRetweet(false);
		return result;
	}


	@Override
	public void muteConversation(long id) throws TwitterException {
		muteStatus(id, true);
	}


	@Override
	public void unmuteConversation(long id) throws ConnectionException {
		muteStatus(id, false);
	}


	@Override
	public void deleteStatus(long id) throws TwitterException {
		getTweet1(TWEET_DELETE + id + JSON, new ArrayList<>());
	}


	@Override
	public void uploadStatus(StatusUpdate update, long[] mediaIds) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("status=" + StringTools.encode(update.getText()));
		if (update.getReplyId() > 0)
			params.add("in_reply_to_status_id=" + update.getReplyId());
		if (mediaIds != null && mediaIds.length > 0) {
			StringBuilder buf = new StringBuilder();
			for (long id : mediaIds)
				buf.append(id).append("%2C");
			String idStr = buf.substring(0, buf.length() - 3);
			params.add("media_ids=" + idStr);
		}
		if (update.hasLocation()) {
			String lat = Double.toString(update.getLatitude());
			String lon = Double.toString(update.getLongitude());
			params.add("lat=" + StringTools.encode(lat));
			params.add("long=" + StringTools.encode(lon));
		}
		getTweet1(TWEET_UPLOAD, params);
	}


	@Override
	public UserList createUserlist(UserListUpdate update) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("name=" + StringTools.encode(update.getTitle()));
		params.add("description=" + StringTools.encode(update.getDescription()));
		if (update.isPublic())
			params.add("mode=public");
		else
			params.add("mode=private");
		return getUserlist1(USERLIST_CREATE, params);
	}


	@Override
	public UserList updateUserlist(UserListUpdate update) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + update.getId());
		params.add("name=" + StringTools.encode(update.getTitle()));
		params.add("description=" + StringTools.encode(update.getDescription()));
		if (update.isPublic())
			params.add("mode=public");
		else
			params.add("mode=private");
		return getUserlist1(USERLIST_UPDATE, params);
	}


	@Override
	public UserList getUserlist(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + id);
		return getUserlist1(USERLIST_SHOW, params);
	}


	@Override
	public UserList followUserlist(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + id);
		UserListV1 result = getUserlist1(USERLIST_FOLLOW, params);
		result.setFollowing(true);
		return result;
	}


	@Override
	public UserList unfollowUserlist(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + id);
		UserListV1 result = getUserlist1(USERLIST_UNFOLLOW, params);
		result.setFollowing(false);
		return result;
	}


	@Override
	public UserList deleteUserlist(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("list_id=" + id);
		return getUserlist1(USERLIST_DESTROY, params);
	}


	@Override
	public UserLists getUserlistOwnerships(long id, String name, long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (id > 0)
			params.add("user_id=" + id);
		else
			params.add("screen_name=" + StringTools.encode(name));
		return getUserlists(USERLIST_OWNERSHIP, params);
	}


	@Override
	public UserLists getUserlistMemberships(long id, String name, long cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (id > 0)
			params.add("user_id=" + id);
		else
			params.add("screen_name=" + StringTools.encode(name));
		params.add("count=" + settings.getListSize());
		params.add("cursor=" + cursor);
		return getUserlists(USERLIST_MEMBERSHIP, params);
	}


	@Override
	public void addUserToList(long id, String name) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (name.startsWith("@"))
			name = name.substring(1);
		params.add("list_id=" + id);
		params.add("screen_name=" + StringTools.encode(name));
		sendPost(USERLIST_ADD_USER, params);
	}


	@Override
	public void removeUserFromList(long id, String name) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (name.startsWith("@"))
			name = name.substring(1);
		params.add("list_id=" + id);
		params.add("screen_name=" + StringTools.encode(name));
		sendPost(USERLIST_DEL_USER, params);
	}


	@Override
	public void sendDirectmessage(long id, String message, long mediaId) throws TwitterException {
		try {
			// directmessage endpoint uses JSON structure
			JSONObject data = new JSONObject();
			JSONObject root = new JSONObject();
			JSONObject target = new JSONObject();
			JSONObject msg_create = new JSONObject();
			JSONObject event = new JSONObject();
			target.put("recipient_id", Long.toString(id));
			msg_create.put("target", target);
			msg_create.put("message_data", data);
			event.put("type", "message_create");
			event.put("message_create", msg_create);
			root.put("event", event);
			if (!message.isEmpty())
				data.put("text", message);
			if (mediaId > 0) {
				JSONObject attachment = new JSONObject();
				JSONObject media = new JSONObject();
				attachment.put("type", "media");
				attachment.put("media", media);
				media.put("id", Long.toString(mediaId));
				data.put("attachment", attachment);
			}
			Response response = post(DIRECTMESSAGE_CREATE, new ArrayList<>(0), root);
			if (response.code() != 200) {
				throw new TwitterException(response);
			}
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}


	@Override
	public void deleteDirectmessage(long id) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("id=" + id);
		try {
			Response response = delete(DIRECTMESSAGE_DELETE, params);
			if (response.code() < 200 || response.code() >= 300) {
				throw new TwitterException(response);
			}
		} catch (IOException err) {
			throw new TwitterException(err);
		}
	}


	@Override
	public Messages getDirectmessages(String cursor) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("count=" + settings.getListSize());
		if (cursor != null && !cursor.isEmpty())
			params.add("cursor=" + cursor);
		try {
			Response response = get(DIRECTMESSAGE, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				// init user cache to re-use instances
				Map<Long, User> userCache = new TreeMap<>();
				JSONObject json = new JSONObject(body.string());
				String nextCursor = json.optString("next_cursor", "");
				JSONArray array = json.getJSONArray("events");
				Messages result = new Messages(cursor, nextCursor);
				for (int pos = 0; pos < array.length(); pos++) {
					JSONObject item = array.getJSONObject(pos);
					try {
						MessageV1 message = new MessageV1(item);
						long senderId = message.getSenderId();
						// cache user instances to reduce API calls
						if (userCache.containsKey(senderId)) {
							message.addSender(userCache.get(senderId));
						} else {
							User user = showUser(senderId);
							userCache.put(senderId, user);
							message.addSender(user);
						}
						result.add(message);
					} catch (JSONException e) {
						if (BuildConfig.DEBUG) {
							Log.w("directmessage", e);
						}
					}
				}
				return result;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}


	@Override
	public long uploadMedia(MediaStatus mediaUpdate) throws TwitterException {
		List<String> params = new ArrayList<>();
		boolean enableChunk;
		final long mediaId;
		String state;
		int retries = 0;
		try {
			// step 1 INIT
			params.add("command=INIT");
			params.add("media_type=" + mediaUpdate.getMimeType());
			params.add("total_bytes=" + mediaUpdate.available());
			if (mediaUpdate.getMimeType().startsWith("video/")) {
				params.add("media_category=tweet_video");
				enableChunk = true;
			} else if (mediaUpdate.getMimeType().startsWith("image/gif")) {
				params.add("media_category=tweet_gif");
				enableChunk = true;
			} else {
				// disable chunking for images
				enableChunk = false;
			}
			Response response = post(MEDIA_UPLOAD, params);
			ResponseBody body = response.body();
			if (response.code() < 200 || response.code() >= 300 || body == null)
				throw new TwitterException(response);
			JSONObject jsonResponse = new JSONObject(body.string());
			mediaId = Long.parseLong(jsonResponse.getString("media_id_string"));

			// step 2 APPEND
			int segmentIndex = 0;
			while (mediaUpdate.available() > 0) {
				params.clear();
				params.add("command=APPEND");
				params.add("segment_index=" + segmentIndex++);
				params.add("media_id=" + mediaId);
				response = post(MEDIA_UPLOAD, params, mediaUpdate.getStream(), "media", enableChunk);
				if (response.code() < 200 || response.code() >= 300) {
					throw new TwitterException(response);
				}
			}

			// step 3 FINALIZE
			params.clear();
			params.add("command=FINALIZE");
			params.add("media_id=" + mediaId);
			response = post(MEDIA_UPLOAD, params);
			if (response.code() < 200 || response.code() >= 300)
				throw new TwitterException(response);
			// skip step 4 if chunking isn't enabled
			if (!enableChunk) {
				return mediaId;
			}
			// step 4 STATUS
			params.clear();
			params.add("command=STATUS");
			params.add("media_id=" + mediaId);
			// poll media processing information frequently
			do {
				response = get(MEDIA_UPLOAD, params);
				body = response.body();
				if (response.code() < 200 || response.code() >= 300 || body == null)
					throw new TwitterException(response);
				jsonResponse = new JSONObject(body.string());
				JSONObject processingInfo = jsonResponse.getJSONObject("processing_info");
				long retryAfter = processingInfo.optLong("check_after_secs");
				state = processingInfo.optString("state", "");
				// wait until next polling
				Thread.sleep(retryAfter * 1000L);
			} while (state.equals("in_progress") && ++retries <= POLLING_MAX_RETRIES);
			// check if media processing was successfully
			if (!state.equals("succeeded")) {
				JSONObject jsonError = jsonResponse.getJSONObject("processing_info").getJSONObject("error");
				String message = jsonError.getString("message");
				throw new TwitterException(message);
			}
			return mediaId;
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		} catch (InterruptedException e) {
			return -1L; //ignore
		}
	}


	@Override
	public MediaStatus downloadImage(String link) throws TwitterException {
		try {
			// this type of link requires authentication
			if (link.startsWith(DOWNLOAD)) {
				Response response = get(link, new ArrayList<>(0));
				ResponseBody body = response.body();
				if (response.code() == 200 && body != null) {
					MediaType type = body.contentType();
					if (type != null) {
						String mime = type.toString();
						InputStream stream = body.byteStream();
						return new MediaStatus(stream, mime);
					}
				}
				throw new TwitterException(response);
			}
			// public link, no authentication required
			else {
				Request request = new Request.Builder().url(link).get().build();
				Response response = client.newCall(request).execute();
				ResponseBody body = response.body();
				if (response.code() == 200 && body != null) {
					MediaType type = body.contentType();
					if (type != null) {
						String mime = type.toString();
						InputStream stream = body.byteStream();
						return new MediaStatus(stream, mime);
					}
				}
				throw new TwitterException(response);
			}
		} catch (Exception e) {
			throw new TwitterException(e);
		}
	}


	@Override
	public User updateProfile(ProfileUpdate update) throws TwitterException {
		List<String> params = new ArrayList<>();
		params.add("name=" + StringTools.encode(update.getName()));
		params.add("url=" + StringTools.encode(update.getUrl()));
		params.add("location=" + StringTools.encode(update.getLocation()));
		params.add("description=" + StringTools.encode(update.getDescription()));
		if (update.getProfileImageStream() != null) {
			updateImage(PROFILE_UPDATE_IMAGE, update.getProfileImageStream(), "image");
		}
		if (update.getBannerImageStream() != null) {
			updateImage(PROFILE_UPDATE_BANNER, update.getBannerImageStream(), "banner");
		}
		return getUser1(PROFILE_UPDATE, params);
	}


	@Override
	public List<Long> getIdBlocklist() throws TwitterException {
		// Note: the API returns up to 5000 user IDs
		// but for bigger lists, we have to parse the whole list
		Set<Long> result = new TreeSet<>();
		// add blocked user IDs
		long cursor = -1;
		for (int i = 0; i < 10 && cursor != 0; i++) {
			long[] ids = getUserIDs(IDS_BLOCKED_USERS, cursor);
			for (int pos = 0; pos < ids.length - 2; pos++) {
				result.add(ids[pos]);
			}
			cursor = ids[ids.length - 1];
		}
		// add muted user IDs
		cursor = -1;
		for (int i = 0; i < 10 && cursor != 0; i++) {
			long[] ids = getUserIDs(IDS_MUTED_USERS, cursor);
			for (int pos = 0; pos < ids.length - 2; pos++) {
				result.add(ids[pos]);
			}
			cursor = ids[ids.length - 1];
		}
		return new ArrayList<>(result);
	}


	@Override
	public List<Notification> getNotifications(long minId, long maxId) throws ConnectionException {
		List<String> params = new ArrayList<>();
		if (minId > 0)
			params.add("since_id=" + minId);
		if (maxId > 1)
			params.add("max_id=" + maxId);
		List<Status> mentions = getTweets1(TWEETS_MENTIONS, params);
		List<Notification> result = new ArrayList<>(mentions.size());
		for (Status status : mentions) {
			result.add(new TwitterNotification(status));
		}
		return result;
	}

	/**
	 * get tweets using an endpoint
	 *
	 * @param endpoint endpoint url to fetch the tweets
	 * @param params   additional parameters
	 * @return list of tweets
	 */
	private List<Status> getTweets1(String endpoint, List<String> params) throws TwitterException {
		try {
			params.add(TweetV1.PARAM_EXT_MODE);
			params.add(TweetV1.PARAM_INCL_RETWEET);
			params.add(TweetV1.PARAM_ENTITIES);
			params.add("count=" + settings.getListSize());
			Response response = get(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONArray array;
				if (endpoint.equals(TWEET_SEARCH)) // twitter search uses another structure
					array = new JSONObject(body.string()).getJSONArray("statuses");
				else
					array = new JSONArray(body.string());
				long homeId = settings.getLogin().getId();
				List<Status> tweets = new ArrayList<>(array.length() + 1);
				for (int i = 0; i < array.length(); i++) {
					try {
						JSONObject tweetJson = array.getJSONObject(i);
						tweets.add(new TweetV1(tweetJson, homeId));
					} catch (JSONException e) {
						if (BuildConfig.DEBUG) {
							Log.w("tweet", e);
						}
					}
				}
				return tweets;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * get tweets using an endpoint
	 *
	 * @param endpoint endpoint url to fetch the tweets
	 * @param params   additional parameters
	 * @return list of tweets
	 */
	private List<Status> getTweets2(String endpoint, List<String> params) throws TwitterException {
		try {
			params.add(TweetV2.FIELDS_TWEET);
			params.add(TweetV2.FIELDS_EXPANSION);
			params.add(UserV2.USER_FIELDS);
			params.add(MediaV2.FIELDS_MEDIA);
			params.add(PollV2.FIELDS_POLL);
			params.add(LocationV2.FIELDS_PLACE);
			params.add("max_results=" + settings.getListSize());
			Response response = get(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				JSONArray data = json.optJSONArray("data");
				if (data != null && data.length() > 0) {
					List<Status> tweets = new ArrayList<>(data.length() + 1);
					UserV2Map userMap = new UserV2Map(json, settings.getLogin().getId());
					for (int i = 0; i < data.length(); i++) {
						try {
							Status item = new TweetV2(data.getJSONObject(i), userMap);
							tweets.add(item);
						} catch (JSONException e) {
							if (BuildConfig.DEBUG) {
								Log.w("tweet", e);
							}
						}
					}
					return tweets;
				}
				return new ArrayList<>(0);
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * return tweet from API 1.1 endpoint
	 *
	 * @param endpoint to use
	 * @param params   additional parameter
	 */
	private TweetV1 getTweet1(String endpoint, List<String> params) throws TwitterException {
		try {
			params.add(TweetV1.PARAM_EXT_MODE);
			params.add(TweetV1.PARAM_INCL_RETWEET);
			params.add(TweetV1.PARAM_ENTITIES);
			Response response;
			if (endpoint.equals(TWEET_LOOKUP)) {
				response = get(endpoint, params);
			} else {
				response = post(endpoint, params);
			}
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				TweetV1 result = new TweetV1(json, settings.getLogin().getId());
				// fix: embedded tweet information doesn't match with the parent tweet
				//      re-downloading embedded tweet information
				if (result.getEmbeddedStatus() != null) {
					params.clear();
					params.add("id=" + result.getEmbeddedStatus().getId());
					Status status = getTweet1(TWEET_LOOKUP, params);
					result.setEmbeddedTweet(status);
				}
				return result;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * return tweet from API 2.0 endpoint
	 *
	 * @param endpoint to use
	 * @param params   additional parameter
	 */
	private TweetV2 getTweet2(String endpoint, List<String> params, Status statusCompat) throws TwitterException {
		try {
			Response response;
			// add metrics information if tweet is from current user and the tweet is not older than 30 days and not a retweet
			if (statusCompat.getAuthor().isCurrentUser() && new Date().getTime() - statusCompat.getTimestamp() < 2419200000L
					&& (statusCompat.getEmbeddedStatus() == null || statusCompat.getEmbeddedStatus().getRepostId() <= 0))
				params.add(TweetV2.FIELDS_TWEET_PRIVATE);
			else
				params.add(TweetV2.FIELDS_TWEET);
			params.add(TweetV2.FIELDS_EXPANSION);
			params.add(UserV2.USER_FIELDS);
			params.add(MediaV2.FIELDS_MEDIA);
			params.add(PollV2.FIELDS_POLL);
			params.add(LocationV2.FIELDS_PLACE);
			if (endpoint.startsWith(TWEET2_LOOKUP)) {
				response = get(endpoint, params);
			} else {
				response = post(endpoint, params);
			}
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				UserV2Map userMap = new UserV2Map(json, settings.getLogin().getId());
				MediaV2Map mediaMap = new MediaV2Map(json);
				PollV2Map pollMap = new PollV2Map(json);
				LocationV2Map locationMap = new LocationV2Map(json);
				return new TweetV2(json, userMap, mediaMap, pollMap, locationMap, statusCompat);
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * returns an array of user IDs from a given endpoint
	 *
	 * @param endpoint Endpoint where to get the user IDs
	 * @param cursor   cursor value to parse the ID pages
	 * @return an array of user IDs + the list cursor on the last array index
	 */
	private long[] getUserIDs(String endpoint, long cursor) throws TwitterException {
		try {
			List<String> params = new ArrayList<>();
			params.add("cursor=" + cursor);
			Response response = get(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				JSONArray idArray = json.getJSONArray("ids");
				cursor = Long.parseLong(json.optString("next_cursor_str", "0"));
				long[] result = new long[idArray.length() + 1];
				for (int pos = 0; pos < idArray.length(); pos++) {
					result[pos] = idArray.getLong(pos);
				}
				result[result.length - 1] = cursor;
				return result;
			} else {
				throw new TwitterException(response);
			}
		} catch (IOException | JSONException | NumberFormatException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * lookup a list of user IDs
	 *
	 * @param ids User IDs
	 * @return a list of users
	 */
	private Users getUsers1(long[] ids) throws TwitterException {
		List<String> params = new ArrayList<>();
		if (ids.length > 0) {
			StringBuilder idBuf = new StringBuilder("user_id=");
			for (long id : ids) {
				idBuf.append(id).append("%2C");
			}
			params.add(idBuf.substring(0, idBuf.length() - 3));
			return getUsers1(USERS_LOOKUP, params);
		}
		return new Users(0L, 0L);
	}

	/**
	 * create a list of users using API v 1.1
	 *
	 * @param endpoint endpoint url to get the user data from
	 * @param params   additional parameters
	 * @return user list
	 */
	private Users getUsers1(String endpoint, List<String> params) throws TwitterException {
		try {
			Response response;
			if (USERS_LOOKUP.equals(endpoint)) {
				response = post(endpoint, params);
			} else {
				params.add("skip_status=true");
				params.add("count=" + settings.getListSize());
				response = get(endpoint, params);
			}
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				String jsonResult = body.string();
				if (!jsonResult.startsWith("{\"users\":")) // convert to users JSON object
					jsonResult = "{\"users\":" + jsonResult + '}';
				JSONObject json = new JSONObject(jsonResult);
				JSONArray array = json.getJSONArray("users");
				long prevCursor = Long.parseLong(json.optString("previous_cursor_str", "0"));
				long nextCursor = Long.parseLong(json.optString("next_cursor_str", "0"));
				Users users = new Users(prevCursor, nextCursor);
				long homeId = settings.getLogin().getId();
				for (int i = 0; i < array.length(); i++) {
					try {
						users.add(new UserV1(array.getJSONObject(i), homeId));
					} catch (JSONException e) {
						if (BuildConfig.DEBUG) {
							Log.w("user", e);
						}
					}
				}
				return users;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException | NumberFormatException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * create a list of users using API v 2
	 *
	 * @param endpoint endpoint url to get the user data from
	 * @return user list
	 */
	private Users getUsers2(String endpoint) throws TwitterException {
		try {
			List<String> params = new ArrayList<>();
			params.add("user.fields=" + UserV2.USER_FIELDS);
			Response response = get(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				Users users = new Users(0L, 0L);
				// check if result is not empty
				if (json.has("data")) {
					JSONArray array = json.getJSONArray("data");
					long homeId = settings.getLogin().getId();
					for (int i = 0; i < array.length(); i++) {
						try {
							users.add(new UserV2(array.getJSONObject(i), homeId));
						} catch (JSONException err) {
							if (BuildConfig.DEBUG) {
								Log.w("user-v2", err);
							}
						}
					}
				}
				return users;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * send POST request and return updated user information
	 *
	 * @param endpoint POST endpoint
	 * @param params   additional parameters
	 * @return user information
	 */
	private User getUser1(String endpoint, List<String> params) throws TwitterException {
		try {
			params.add("skip_status=true");
			params.add("include_entities=true");
			Response response;
			if (endpoint.equals(USER_LOOKUP))
				response = get(endpoint, params);
			else
				response = post(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				return new UserV1(json, settings.getLogin().getId());
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * execute userlist action and return userlist information
	 *
	 * @param endpoint userlist endpoint to use
	 * @param params   additional parameters
	 * @return userlist information
	 */
	private UserListV1 getUserlist1(String endpoint, List<String> params) throws TwitterException {
		try {
			Response response;
			if (endpoint.equals(USERLIST_SHOW))
				response = get(endpoint, params);
			else
				response = post(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				return new UserListV1(json, settings.getLogin().getId());
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * get a list of userlists
	 *
	 * @param endpoint endpoint to get the userlists from
	 * @param params   additional parameter
	 * @return list of userlists
	 */
	private UserLists getUserlists(String endpoint, List<String> params) throws TwitterException {
		params.add("include_entities=true");
		try {
			Response response = get(endpoint, params);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONArray array;
				UserLists result;
				String bodyStr = body.string();
				// add cursors if available
				if (bodyStr.startsWith("{")) {
					JSONObject json = new JSONObject(bodyStr);
					array = json.getJSONArray("lists");
					long prevCursor = Long.parseLong(json.optString("previous_cursor_str", "0"));
					long nextCursor = Long.parseLong(json.optString("next_cursor_str", "0"));
					result = new UserLists(prevCursor, nextCursor);
				} else {
					array = new JSONArray(bodyStr);
					result = new UserLists(0L, 0L);
				}
				long currentId = settings.getLogin().getId();
				for (int pos = 0; pos < array.length(); pos++) {
					try {
						result.add(new UserListV1(array.getJSONObject(pos), currentId));
					} catch (JSONException e) {
						if (BuildConfig.DEBUG) {
							Log.w("userlist", e);
						}
					}
				}
				return result;
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException | NumberFormatException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * update profile images
	 *
	 * @param endpoint endpoint to use
	 * @param input    inputstream of the image file
	 * @param key      key name used to identify the type of image
	 */
	private void updateImage(String endpoint, InputStream input, String key) throws TwitterException {
		try {
			List<String> params = new ArrayList<>();
			params.add("skip_status=true");
			params.add("include_entities=false");
			Response response = post(endpoint, params, input, key, false);
			if (response.code() < 200 || response.code() >= 300) {
				throw new TwitterException(response);
			}
		} catch (IOException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * mute a status from conversation
	 *
	 * @param id   ID of the status
	 * @param hide true to hide the status
	 */
	private void muteStatus(long id, boolean hide) throws TwitterException {
		try {
			RequestBody request = RequestBody.create("{\"hidden\":" + hide + "}", TYPE_JSON);
			Response response = put(TWEET_UNI + id + "/hidden", new ArrayList<>(), request);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				if (json.getJSONObject("data").getBoolean("hidden") == hide) {
					return; // successfull if result equals request
				}
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * send post without return value
	 *
	 * @param endpoint endpoint to use
	 * @param params   endpoint parameters
	 */
	private void sendPost(String endpoint, List<String> params) throws TwitterException {
		try {
			Response response = post(endpoint, params);
			if (response.code() < 200 || response.code() >= 300) {
				throw new TwitterException(response);
			}
		} catch (IOException e) {
			throw new TwitterException(e);
		}
	}

	/**
	 * filter tweets from blocked users
	 */
	private void filterTweets(List<Status> tweets) {
		Set<Long> excludedIds = filterDatabase.getFilteredUserIds();
		for (int pos = tweets.size() - 1; pos >= 0; pos--) {
			long authorId = tweets.get(pos).getAuthor().getId();
			Status embeddedTweet = tweets.get(pos).getEmbeddedStatus();
			if (excludedIds.contains(authorId)) {
				tweets.remove(pos);
			} else if (embeddedTweet != null) {
				authorId = embeddedTweet.getAuthor().getId();
				if (excludedIds.contains(authorId)) {
					tweets.remove(pos);
				}
			}
		}
	}

	/**
	 * remove blocked users from list
	 */
	private void filterUsers(List<User> users) {
		Set<Long> exclude = filterDatabase.getFilteredUserIds();
		for (int pos = users.size() - 1; pos >= 0; pos--) {
			if (exclude.contains(users.get(pos).getId())) {
				users.remove(pos);
			}
		}
	}

	/**
	 * get credentials of the current user
	 *
	 * @param keys oauth key parameters
	 * @return current user
	 */
	private User getCredentials(String... keys) throws TwitterException {
		try {
			Response response = get(CREDENTIALS, new ArrayList<>(), keys);
			ResponseBody body = response.body();
			if (body != null && response.code() == 200) {
				JSONObject json = new JSONObject(body.string());
				return new UserV1(json);
			}
			throw new TwitterException(response);
		} catch (IOException | JSONException err) {
			throw new TwitterException(err);
		}
	}

	/**
	 * send POST request with empty body and create response
	 *
	 * @param endpoint endpoint url
	 * @param params   additional http parameters
	 * @param keys     optional API keys
	 * @return http response
	 */
	private Response post(String endpoint, List<String> params, String... keys) throws IOException {
		RequestBody body = RequestBody.create("", TYPE_TEXT);
		return post(endpoint, params, body, keys);
	}

	/**
	 * send POST request with JSON object and create response
	 *
	 * @param endpoint endpoint url
	 * @param params   additional http parameters
	 * @return http response
	 */
	@SuppressWarnings("SameParameterValue")
	private Response post(String endpoint, List<String> params, JSONObject json) throws IOException {
		RequestBody body = RequestBody.create(json.toString(), TYPE_JSON);
		return post(endpoint, params, body);
	}

	/**
	 * send POST request with file and create response
	 *
	 * @param endpoint    endpoint url
	 * @param params      additional http parameters
	 * @param enableChunk true to enable file chunk
	 * @param addToKey    key to add the file
	 * @return http response
	 */
	private Response post(String endpoint, List<String> params, InputStream is, String addToKey, boolean enableChunk) throws IOException {
		RequestBody data = new RequestBody() {
			@Override
			public MediaType contentType() {
				return TYPE_STREAM;
			}

			@Override
			public void writeTo(@NonNull BufferedSink sink) throws IOException {
				if (enableChunk && is.available() > CHUNK_MAX_BYTES) {
					sink.write(Okio.buffer(Okio.source(is)), CHUNK_MAX_BYTES);
				} else {
					sink.writeAll(Okio.buffer(Okio.source(is)));
				}
			}
		};
		RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(addToKey, "", data).build();
		return post(endpoint, params, body);
	}

	/**
	 * send POST request with request body
	 *
	 * @param endpoint endpoint url
	 * @param params   additional http parameters
	 * @param body     custom body
	 * @param keys     optional oauth keys (consumer key & secret, optional oauth tokens)
	 * @return http response
	 */
	private Response post(String endpoint, List<String> params, RequestBody body, String... keys) throws IOException {
		String authHeader;
		if (keys.length == 2)
			authHeader = buildHeader("POST", endpoint, params, keys);
		else
			authHeader = buildHeader("POST", endpoint, params);
		String url = appendParams(endpoint, params);
		Request request = new Request.Builder().url(url).addHeader("Authorization", authHeader).post(body).build();
		return client.newCall(request).execute();
	}

	/**
	 * create and call GET endpoint
	 *
	 * @param endpoint endpoint url
	 * @param params   url parameter
	 * @param keys     optional oauth keys (consumer key, secret & optional oauth tokens)
	 * @return http response
	 */
	private Response get(String endpoint, List<String> params, String... keys) throws IOException {
		String authHeader = buildHeader("GET", endpoint, params, keys);
		String url = appendParams(endpoint, params);
		Request request = new Request.Builder().url(url).addHeader("Authorization", authHeader).get().build();
		return client.newCall(request).execute();
	}

	/**
	 * create and call PUT endpoint
	 *
	 * @param endpoint endpoint url
	 * @return http response
	 */
	private Response put(String endpoint, List<String> params, RequestBody body) throws IOException {
		String authHeader = buildHeader("PUT", endpoint, params);
		String url = appendParams(endpoint, params);
		Request request = new Request.Builder().url(url).addHeader("Authorization", authHeader).put(body).build();
		return client.newCall(request).execute();
	}

	/**
	 * create and call GET endpoint
	 *
	 * @param endpoint endpoint url
	 * @return http response
	 */
	@SuppressWarnings("SameParameterValue")
	private Response delete(String endpoint, List<String> params) throws IOException {
		String authHeader = buildHeader("DELETE", endpoint, params);
		String url = appendParams(endpoint, params);
		Request request = new Request.Builder().url(url).addHeader("Authorization", authHeader).delete().build();
		return client.newCall(request).execute();
	}

	/**
	 * create http header with credentials and signature
	 *
	 * @param method   endpoint method to call
	 * @param endpoint endpoint url
	 * @param params   parameter to add to signature
	 * @param keys     keys for oauth access (consumer key, consumer secret, optional oauth token, optional oauth token secret)
	 * @return header string
	 */
	private String buildHeader(String method, String endpoint, List<String> params, String... keys) throws IOException {
		String oauthToken, tokenSecret, consumerKey, consumerSecret;
		String timeStamp = StringTools.getTimestamp();
		String random = StringTools.getRandomString();
		String oauth_token_param = "";

		if (keys.length >= 2) {
			consumerKey = keys[0];
			consumerSecret = keys[1];
		} else {
			consumerKey = tokens.getConsumerKey(false);
			consumerSecret = tokens.getConsumerSecret(false);
		}
		if (keys.length == 4) {
			oauthToken = keys[2];
			tokenSecret = keys[3];
		} else {
			oauthToken = settings.getLogin().getOauthToken();
			tokenSecret = settings.getLogin().getOauthSecret();
		}
		String signkey = consumerSecret + "&";
		// init default parameters
		TreeSet<String> sortedParams = new TreeSet<>();
		sortedParams.add("oauth_callback=oob");
		sortedParams.add("oauth_consumer_key=" + consumerKey);
		sortedParams.add("oauth_nonce=" + random);
		sortedParams.add("oauth_signature_method=" + StringTools.SIGNATURE_ALG);
		sortedParams.add("oauth_timestamp=" + timeStamp);
		sortedParams.add("oauth_version=" + OAUTH);
		// add custom parameters
		sortedParams.addAll(params);

		// only add tokens if there is no login process
		if (!REQUEST_TOKEN.equals(endpoint) && !OAUTH_VERIFIER.equals(endpoint)) {
			sortedParams.add("oauth_token=" + oauthToken);
			oauth_token_param = ", oauth_token=\"" + oauthToken + "\"";
			signkey += tokenSecret;
		}

		// build string with sorted parameters
		StringBuilder paramStr = new StringBuilder();
		for (String param : sortedParams)
			paramStr.append(param).append('&');
		paramStr.deleteCharAt(paramStr.length() - 1);

		// calculate oauth signature
		String signature = StringTools.sign(method, endpoint, paramStr.toString(), signkey);

		// create header string
		return "OAuth oauth_callback=\"oob\"" +
				", oauth_consumer_key=\"" + consumerKey + "\"" +
				", oauth_nonce=\"" + random + "\"" +
				", oauth_signature=\"" + signature + "\"" +
				", oauth_signature_method=\"" + StringTools.SIGNATURE_ALG + "\"" +
				", oauth_timestamp=\"" + timeStamp + "\""
				+ oauth_token_param +
				", oauth_version=\"" + OAUTH + "\"";
	}

	/**
	 * build url with parameters
	 *
	 * @param url    url without parameters
	 * @param params parameters
	 * @return url with parameters
	 */
	private String appendParams(String url, List<String> params) {
		if (!params.isEmpty()) {
			StringBuilder result = new StringBuilder(url);
			result.append('?');
			for (String param : params)
				result.append(param).append('&');
			result.deleteCharAt(result.length() - 1);
			return result.toString();
		}
		return url;
	}
}