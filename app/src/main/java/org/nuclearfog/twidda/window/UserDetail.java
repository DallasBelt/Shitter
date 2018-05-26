package org.nuclearfog.twidda.window;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.UserLists;
import org.nuclearfog.twidda.backend.listitems.TwitterUser;
import org.nuclearfog.twidda.viewadapter.UserRecycler;

/**
 * User List Activity
 * @see UserLists
 */
public class UserDetail extends AppCompatActivity implements UserRecycler.OnItemClicked {

    private long userID, tweetID;
    private long mode;
    private RecyclerView userList;
    private UserLists uList;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.userpage);
        if(getIntent().getExtras() != null)
            getExtras(getIntent().getExtras());

        userList = findViewById(R.id.userlist);
        userList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        Toolbar toolbar = findViewById(R.id.user_toolbar);
        setSupportActionBar(toolbar);
        SharedPreferences settings = getSharedPreferences("settings", 0);
        int background = settings.getInt("background_color", 0xff0f114a);

        userList.setBackgroundColor(background);
        getUsers();
    }

    @Override
    protected void onDestroy() {
        uList.cancel(true);
        super.onDestroy();
    }

    /**
     * Home Button
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        overridePendingTransition(0,0);
    }

    @Override
    public boolean onCreateOptionsMenu( Menu m ) {
        getMenuInflater().inflate(R.menu.user, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.user_back) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onItemClick(View view, ViewGroup parent, int position) {
        UserRecycler uAdp = (UserRecycler) userList.getAdapter();
        TwitterUser user = uAdp.getData().get(position);
        long userID = user.userID;
        String username = user.screenname;
        Intent intent = new Intent(getApplicationContext(), UserProfile.class);
        Bundle bundle = new Bundle();
        bundle.putLong("userID",userID);
        bundle.putString("username", username);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @SuppressWarnings("ConstantConditions")
    private void getUsers() {
        uList = new UserLists(UserDetail.this);
        if(mode == 0L){
            getSupportActionBar().setTitle(R.string.following);
            uList.execute(userID, UserLists.FOLLOWING, -1L);
        } else if(mode == 1L){
            getSupportActionBar().setTitle(R.string.follower);
            uList.execute(userID, UserLists.FOLLOWERS, -1L);
        } else if(mode == 2L){
            getSupportActionBar().setTitle(R.string.retweet);
            uList.execute(tweetID, UserLists.RETWEETER, -1L);
        } else if(mode == 3L){
            getSupportActionBar().setTitle(R.string.favorite);
            uList.execute(tweetID, UserLists.FAVORISER, -1L);
        }
    }

    @SuppressWarnings("ConstantCondidions")
    private void getExtras(Bundle b) {
        userID  = b.getLong("userID");
        tweetID = b.getLong("tweetID");
        mode = b.getLong("mode");
    }
}