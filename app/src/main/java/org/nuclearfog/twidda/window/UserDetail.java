package org.nuclearfog.twidda.window;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.UserLists;
import org.nuclearfog.twidda.backend.listitems.TwitterUser;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.viewadapter.UserAdapter;
import org.nuclearfog.twidda.viewadapter.UserAdapter.OnItemClicked;

import static android.os.AsyncTask.Status.RUNNING;

public class UserDetail extends AppCompatActivity implements OnItemClicked, OnRefreshListener {

    private RecyclerView userList;
    private UserLists uList;
    private int mode = -1;
    private long id = 0;


    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        b = getIntent().getExtras();
        if (b != null) {
            mode = b.getInt("mode");
            if (b.containsKey("tweetID"))
                id = b.getLong("tweetID");
            else if (b.containsKey("userID"))
                id = b.getLong("userID");
        }
        setContentView(R.layout.userpage);

        SwipeRefreshLayout refresh = findViewById(R.id.user_refresh);
        userList = findViewById(R.id.userlist);
        userList.setLayoutManager(new LinearLayoutManager(this));
        Toolbar toolbar = findViewById(R.id.user_toolbar);
        setSupportActionBar(toolbar);

        GlobalSettings settings = GlobalSettings.getInstance(this);
        userList.setBackgroundColor(settings.getBackgroundColor());

        refresh.setRefreshing(true);
        refresh.setOnRefreshListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();

        if (uList == null) {
            uList = new UserLists(UserDetail.this);
            int titleId;
            switch (mode) {
                case 0:
                    titleId = R.string.following;
                    uList.execute(id, UserLists.FOLLOWING, -1L);
                    break;
                case 1:
                    titleId = R.string.follower;
                    uList.execute(id, UserLists.FOLLOWERS, -1L);
                    break;
                case 2:
                    titleId = R.string.retweet;
                    uList.execute(id, UserLists.RETWEETER, -1L);
                    break;
                case 3:
                default:
                    titleId = R.string.favorite;
                    uList.execute(id, UserLists.FAVORISER, -1L);
                    break;
            }
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(titleId);
            }
        }
    }


    @Override
    protected void onStop() {
        if (uList != null && uList.getStatus() == RUNNING)
            uList.cancel(true);
        super.onStop();
    }


    @Override
    public void onItemClick(ViewGroup parent, int position) {
        UserAdapter userListAdapter = (UserAdapter) userList.getAdapter();
        if (userListAdapter != null) {
            TwitterUser user = userListAdapter.getData().get(position);
            long userID = user.userID;
            String username = user.screenname;
            Intent intent = new Intent(this, UserProfile.class);
            intent.putExtra("userID", userID);
            intent.putExtra("username", username);
            startActivity(intent);
        }
    }


    @Override
    public void onRefresh() {
        if (uList != null && uList.getStatus() == RUNNING)
            uList.cancel(true);
        uList = new UserLists(UserDetail.this);
        if (mode == 0)
            uList.execute(id, UserLists.FOLLOWING, -1L);
        else if (mode == 1)
            uList.execute(id, UserLists.FOLLOWERS, -1L);
        else if (mode == 2)
            uList.execute(id, UserLists.RETWEETER, -1L);
        else
            uList.execute(id, UserLists.FAVORISER, -1L);
    }
}