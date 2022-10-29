package org.nuclearfog.twidda.backend.async;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import org.nuclearfog.twidda.backend.api.Connection;
import org.nuclearfog.twidda.backend.api.ConnectionException;
import org.nuclearfog.twidda.backend.api.twitter.Twitter;
import org.nuclearfog.twidda.database.AppDatabase;
import org.nuclearfog.twidda.model.Tweet;
import org.nuclearfog.twidda.ui.activities.TweetActivity;

import java.lang.ref.WeakReference;

/**
 * Background task to download tweet informations and to take actions
 *
 * @author nuclearfog
 * @see TweetActivity
 */
public class TweetAction extends AsyncTask<Long, Tweet, Void> {

	/**
	 * Load tweet
	 */
	public static final int LOAD = 1;

	/**
	 * load tweet from database first
	 */
	public static final int LD_DB = 2;

	/**
	 * retweet tweet
	 */
	public static final int RETWEET = 3;

	/**
	 * remove retweet
	 * (delete operation, "retweet ID" required)
	 */
	public static final int UNRETWEET = 4;

	/**
	 * favorite tweet
	 */
	public static final int FAVORITE = 5;

	/**
	 * remove tweet from favorites
	 */
	public static final int UNFAVORITE = 6;

	/**
	 * hide reply
	 */
	public static final int HIDE = 7;

	/**
	 * unhide reply
	 */
	public static final int UNHIDE = 8;

	/**
	 * delete own tweet
	 * (delete operation, "retweet ID" required)
	 */
	public static final int DELETE = 9;

	private Connection connection;
	private WeakReference<TweetActivity> weakRef;
	private AppDatabase db;

	@Nullable
	private ConnectionException exception;
	private int action;

	/**
	 * @param action action for a given tweet
	 */
	public TweetAction(TweetActivity activity, int action) {
		super();
		weakRef = new WeakReference<>(activity);
		db = new AppDatabase(activity);
		connection = Twitter.get(activity);

		this.action = action;
	}

	/**
	 * @param ids first value is the tweet ID. The second value is the retweet ID. Required for delete operations
	 */
	@Override
	protected Void doInBackground(Long... ids) {
		try {
			switch (action) {
				case LD_DB:
					Tweet newTweet = db.getTweet(ids[0]);
					if (newTweet != null) {
						publishProgress(newTweet);
					}
					// fall through

				case LOAD:
					newTweet = connection.showTweet(ids[0]);
					//tweet = mTwitter.getStatus(tweetId);
					publishProgress(newTweet);
					if (db.containsTweet(ids[0])) {
						// update tweet if there is a database entry
						db.updateTweet(newTweet);
					}
					break;

				case DELETE:
					connection.deleteTweet(ids[0]);
					db.removeTweet(ids[0]);
					// removing retweet reference to this tweet
					db.removeTweet(ids[1]);
					break;

				case RETWEET:
					newTweet = connection.retweetTweet(ids[0]);
					if (newTweet.getEmbeddedTweet() != null)
						publishProgress(newTweet.getEmbeddedTweet());
					db.updateTweet(newTweet);
					break;

				case UNRETWEET:
					newTweet = connection.unretweetTweet(ids[0]);
					publishProgress(newTweet);
					db.updateTweet(newTweet);
					// removing retweet reference to this tweet
					if (ids.length == 2)
						db.removeTweet(ids[1]);
					break;

				case FAVORITE:
					newTweet = connection.favoriteTweet(ids[0]);
					publishProgress(newTweet);
					db.storeFavorite(newTweet);
					break;

				case UNFAVORITE:
					newTweet = connection.unfavoriteTweet(ids[0]);
					publishProgress(newTweet);
					db.removeFavorite(newTweet);
					break;

				case HIDE:
					connection.hideReply(ids[0], true);
					db.hideReply(ids[0], true);
					break;

				case UNHIDE:
					connection.hideReply(ids[0], false);
					db.hideReply(ids[0], false);
					break;
			}
		} catch (ConnectionException exception) {
			this.exception = exception;
			if (exception.getErrorCode() == ConnectionException.RESOURCE_NOT_FOUND) {
				// delete database entry if tweet was not found
				db.removeTweet(ids[0]);
				if (ids.length > 1) {
					// also remove reference to this tweet
					db.removeTweet(ids[1]);
				}
			}
		}
		return null;
	}


	@Override
	protected void onProgressUpdate(Tweet... tweets) {
		TweetActivity activity = weakRef.get();
		if (activity != null && tweets.length > 0 && tweets[0] != null) {
			activity.setTweet(tweets[0]);
		}
	}


	@Override
	protected void onPostExecute(Void v) {
		TweetActivity activity = weakRef.get();
		if (activity != null) {
			if (exception == null) {
				activity.OnSuccess(action);
			} else {
				activity.onError(exception);
			}
		}
	}
}