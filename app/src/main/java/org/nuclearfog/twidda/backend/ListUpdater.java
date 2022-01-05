package org.nuclearfog.twidda.backend;

import android.os.AsyncTask;

import org.nuclearfog.twidda.activities.ListEditor;
import org.nuclearfog.twidda.backend.apiold.EngineException;
import org.nuclearfog.twidda.backend.apiold.TwitterEngine;
import org.nuclearfog.twidda.backend.holder.ListHolder;
import org.nuclearfog.twidda.model.UserList;

import java.lang.ref.WeakReference;

/**
 * This class creates and updates user lists
 * Backend for {@link ListEditor}
 *
 * @author nuclearfog
 */
public class ListUpdater extends AsyncTask<ListHolder, Void, UserList> {


    private EngineException err;
    private final TwitterEngine mTwitter;
    private final WeakReference<ListEditor> callback;


    public ListUpdater(ListEditor activity) {
        super();
        callback = new WeakReference<>(activity);
        mTwitter = TwitterEngine.getInstance(activity);
    }


    @Override
    protected UserList doInBackground(ListHolder... listHolders) {
        try {
            ListHolder mList = listHolders[0];
            return mTwitter.updateUserList(mList);
        } catch (EngineException err) {
            this.err = err;
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(UserList result) {
        if (callback.get() != null) {
            if (result != null) {
                callback.get().onSuccess(result);
            } else {
                callback.get().onError(err);
            }
        }
    }
}