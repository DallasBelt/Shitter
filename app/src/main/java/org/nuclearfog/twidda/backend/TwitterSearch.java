package org.nuclearfog.twidda.backend;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.listitems.Tweet;
import org.nuclearfog.twidda.backend.listitems.TwitterUser;
import org.nuclearfog.twidda.database.ErrorLog;
import org.nuclearfog.twidda.viewadapter.TimelineRecycler;
import org.nuclearfog.twidda.viewadapter.UserRecycler;
import org.nuclearfog.twidda.window.SearchPage;

import java.lang.ref.WeakReference;
import java.util.List;

import twitter4j.TwitterException;

public class TwitterSearch extends AsyncTask<String, Void, Boolean> {

    private TimelineRecycler searchAdapter;
    private UserRecycler userAdapter;
    private TwitterEngine mTwitter;
    private ErrorLog errorLog;
    private WeakReference<SearchPage> ui;
    private int highlight, font_color;
    private boolean imageLoad;
    private String errorMessage = "E: Twitter search, ";
    private int returnCode = 0;

    public TwitterSearch(Context context) {
        ui = new WeakReference<>((SearchPage)context);
        mTwitter = TwitterEngine.getInstance(context);
        errorLog = new ErrorLog(context);

        GlobalSettings settings = GlobalSettings.getInstance(context);
        font_color = settings.getFontColor();
        highlight = settings.getHighlightColor();
        imageLoad = settings.loadImages();

        RecyclerView tweetSearch = ui.get().findViewById(R.id.tweet_result);
        RecyclerView userSearch = ui.get().findViewById(R.id.user_result);
        searchAdapter = (TimelineRecycler) tweetSearch.getAdapter();
        userAdapter = (UserRecycler) userSearch.getAdapter();

        if(searchAdapter == null) {
            searchAdapter = new TimelineRecycler(ui.get());
            tweetSearch.setAdapter(searchAdapter);
        }
        if(userAdapter == null) {
            userAdapter = new UserRecycler(ui.get());
            userSearch.setAdapter(userAdapter);
        }
    }


    @Override
    protected Boolean doInBackground(String... search) {
        String strSearch = search[0];
        long id = 1L;
        try {
            if(searchAdapter.getItemCount() > 0) {
                id = searchAdapter.getItemId(0);
                List<Tweet> tweets = mTwitter.searchTweets(strSearch,id);
                tweets.addAll(searchAdapter.getData());
                searchAdapter.setData(tweets);
            } else {
                List<Tweet> tweets = mTwitter.searchTweets(strSearch,id);
                searchAdapter.setData(tweets);
            }
            if(userAdapter.getItemCount() == 0) {
                List<TwitterUser> user = mTwitter.searchUsers(strSearch);
                userAdapter.setData(user);
            }

            searchAdapter.setColor(highlight,font_color);
            searchAdapter.toggleImage(imageLoad);
            userAdapter.toggleImage(imageLoad);
            return true;

        } catch (TwitterException err) {
            returnCode = err.getErrorCode();
            if (returnCode != 420) {
                errorMessage += err.getMessage();
                errorLog.add(errorMessage);
            }
        } catch(Exception err) {
            errorMessage += err.getMessage();
            errorLog.add(errorMessage);
        }
        return false;
    }


    @Override
    protected void onPostExecute(Boolean success) {
        SearchPage connect = ui.get();
        if(connect == null)
            return;
        if (!success) {
            if (returnCode == 420) {
                Toast.makeText(connect, R.string.rate_limit_exceeded, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(connect, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
        SwipeRefreshLayout tweetReload = connect.findViewById(R.id.searchtweets);
        searchAdapter.notifyDataSetChanged();
        userAdapter.notifyDataSetChanged();
        tweetReload.setRefreshing(false);
        connect.dismiss();
    }

    public interface OnDismiss {
        void dismiss();
    }
}