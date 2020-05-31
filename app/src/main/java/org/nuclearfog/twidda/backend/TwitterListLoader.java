package org.nuclearfog.twidda.backend;

import android.os.AsyncTask;

import androidx.annotation.Nullable;

import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.engine.TwitterEngine;
import org.nuclearfog.twidda.backend.items.TwitterList;
import org.nuclearfog.twidda.fragment.ListFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * Background task for downloading twitter lists created by a user
 *
 * @see ListFragment
 */
public class TwitterListLoader extends AsyncTask<Long, Void, List<TwitterList>> {

    public enum Action {
        LOAD,
        FOLLOW,
        DELETE
    }

    @Nullable
    private EngineException twException;
    private WeakReference<ListFragment> callback;
    private TwitterEngine mTwitter;
    private final Action action;

    public TwitterListLoader(ListFragment callback, Action action) {
        mTwitter = TwitterEngine.getInstance(callback.getContext());
        this.callback = new WeakReference<>(callback);
        this.action = action;
    }

    @Override
    protected void onPreExecute() {
        if (callback.get() != null) {
            callback.get().setRefresh(true);
        }
    }

    @Override
    protected List<TwitterList> doInBackground(Long[] param) {
        List<TwitterList> result;
        try {
            switch (action) {
                case LOAD:
                    result = mTwitter.getUserList(param[0]);
                    return result;

                case FOLLOW:
                    result = new ArrayList<>(1);
                    result.add(mTwitter.followUserList(param[0]));
                    return result;

                case DELETE:
                    result = new ArrayList<>(1);
                    result.add(mTwitter.deleteUserList(param[0]));
                    return result;
            }
        } catch (EngineException twException) {
            this.twException = twException;
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<TwitterList> result) {
        if (callback.get() != null) {
            callback.get().setRefresh(false);
            if (result != null) {
                switch (action) {
                    case LOAD:
                        callback.get().setData(result);
                        break;

                    case FOLLOW:
                        TwitterList list = result.get(0);
                        callback.get().updateItem(list);
                        break;

                    case DELETE:
                        list = result.get(0);
                        callback.get().removeItem(list.getId());
                        break;
                }
            } else if (twException != null) {
                callback.get().onError(twException);
            }
        }
    }
}