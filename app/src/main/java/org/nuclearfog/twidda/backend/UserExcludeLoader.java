package org.nuclearfog.twidda.backend;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import org.nuclearfog.twidda.activities.UserExclude;
import org.nuclearfog.twidda.backend.api.Twitter;
import org.nuclearfog.twidda.backend.api.TwitterException;
import org.nuclearfog.twidda.database.AppDatabase;
import org.nuclearfog.twidda.database.ExcludeDatabase;
import org.nuclearfog.twidda.model.User;

import java.lang.ref.WeakReference;

/**
 * Backend of {@link UserExclude}
 * performs user mute or block actions and exports block list to database
 *
 * @author nuclearfog
 */
public class UserExcludeLoader extends AsyncTask<String, Void, Void> {

    public enum Mode {
        REFRESH,
        MUTE_USER,
        BLOCK_USER
    }

    @Nullable
    private TwitterException err;
    private WeakReference<UserExclude> callback;
    private ExcludeDatabase excludeDatabase;
    private AppDatabase appDatabase;
    private Twitter twitter;
    private Mode mode;


    public UserExcludeLoader(UserExclude activity, Mode mode) {
        super();
        twitter = Twitter.get(activity);
        appDatabase = new AppDatabase(activity);
        excludeDatabase = new ExcludeDatabase(activity);
        callback = new WeakReference<>(activity);
        this.mode = mode;
    }


    @Override
    protected Void doInBackground(String[] names) {
        try {
            if (mode == Mode.REFRESH) { // fixme
                //List<Long> ids = mTwitter.getExcludedUserIDs();
                //excludeDatabase.setExcludeList(ids);
            } else if (mode == Mode.MUTE_USER) {
                User user = twitter.muteUser(names[0]);
                appDatabase.storeUser(user);
            } else if (mode == Mode.BLOCK_USER) {
                User user = twitter.blockUser(names[0]);
                appDatabase.storeUser(user);
            }
        } catch (TwitterException err) {
            this.err = err;
        } catch (Exception err) {
            // ignore
        }
        return null;
    }


    @Override
    protected void onPostExecute(Void v) {
        UserExclude activity = callback.get();
        if (activity != null) {
            if (err == null) {
                activity.onSuccess(mode);
            } else {
                activity.onError(err);
            }
        }
    }
}