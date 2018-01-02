package org.nuclearfog.twidda.Window;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import org.nuclearfog.twidda.DataBase.TweetDatabase;
import org.nuclearfog.twidda.Backend.ProfileInfo;
import org.nuclearfog.twidda.Backend.ProfileTweets;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.ViewAdapter.TimelineAdapter;

public class UserProfile extends AppCompatActivity {

    private SwipeRefreshLayout homeReload, favoriteReload;
    private ListView homeTweets, homeFavorits;
    private TextView txtFollowing, txtFollower;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.profile);
        Toolbar tool = (Toolbar) findViewById(R.id.profile_toolbar);
        setSupportActionBar(tool);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        userId = getIntent().getExtras().getLong("userID");
        homeTweets = (ListView)findViewById(R.id.ht_list);
        homeFavorits = (ListView)findViewById(R.id.hf_list);
        txtFollowing = (TextView)findViewById(R.id.following);
        txtFollower  = (TextView)findViewById(R.id.follower);
        initElements();
        initTabs();
        initSwipe();
        getContent();
        setListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.home, m);
        m.findItem(R.id.action_profile).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch(item.getItemId()) {
            case R.id.action_tweet:
                intent = new Intent(this, TweetPopup.class);
                startActivity(intent);
                break;
            case R.id.action_settings:
                intent = new Intent(this,AppSettings.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    /**
     * Init Tab Listener
     */
    private void initTabs(){
        TabHost mTab = (TabHost)findViewById(R.id.profile_tab);
        mTab.setup();
        // Tab #1
        TabHost.TabSpec tab1 = mTab.newTabSpec("tweets");
        tab1.setContent(R.id.hometweets);
        tab1.setIndicator("",getResources().getDrawable(R.drawable.timeline_icon));
        mTab.addTab(tab1);
        // Tab #2
        TabHost.TabSpec tab2 = mTab.newTabSpec("favorites");
        tab2.setContent(R.id.homefavorits);
        tab2.setIndicator("",getResources().getDrawable(R.drawable.favorite_icon));
        mTab.addTab(tab2);
        // Listener
        mTab.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                homeReload.setRefreshing(false);
                favoriteReload.setRefreshing(false);
            }
        });
    }

    /**
     * Init SwipeRefresh
     */
    private void initSwipe(){
        homeReload = (SwipeRefreshLayout) findViewById(R.id.hometweets);
        homeReload.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTweets(0L);
            }
        });
        favoriteReload = (SwipeRefreshLayout) findViewById(R.id.homefavorits);
        favoriteReload.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getTweets(1L);
            }
        });
    }

    /**
     * Load Content from Database
     */
    private void getContent(){
        new Thread(){
            @Override
            public void run(){
                TweetDatabase mTweet = new TweetDatabase(UserProfile.this, TweetDatabase.USER_TL, userId);
                TimelineAdapter tl = new TimelineAdapter(UserProfile.this,mTweet);
                homeTweets.setAdapter(tl);
                TweetDatabase fTweet = new TweetDatabase(UserProfile.this, TweetDatabase.FAV_TL, userId);
                TimelineAdapter fl = new TimelineAdapter(UserProfile.this,fTweet);
                homeFavorits.setAdapter(fl);
            }
        }.run();
    }

    /**
     * UserProfile Contents
     */
    private void initElements() {
        ProfileInfo profile = new ProfileInfo(this);
        profile.execute(userId);
    }

    /**
     * Download Content
     * @param mode  0 = Home Tweets, 1 = Home Favorite Tweets
     */
    private void getTweets(long mode ){
        ProfileTweets mProfile = new ProfileTweets(this);
        mProfile.execute(userId, mode);
    }

    /**
     * Set On Item Click  Listener
     */
    private void setListener(){
        homeTweets.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!homeReload.isRefreshing()) {
                    TimelineAdapter tlAdp = (TimelineAdapter) homeTweets.getAdapter();
                    TweetDatabase twDB = tlAdp.getAdapter();
                    long tweetID = twDB.getTweetId(position);
                    long userID = twDB.getUserID(position);
                    Intent intent = new Intent(getApplicationContext(), TweetDetail.class);
                    Bundle bundle = new Bundle();
                    bundle.putLong("tweetID",tweetID);
                    bundle.putLong("userID",userID);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });
        homeFavorits.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!favoriteReload.isRefreshing()) {
                    TimelineAdapter tlAdp = (TimelineAdapter) homeFavorits.getAdapter();
                    TweetDatabase twDB = tlAdp.getAdapter();
                    long tweetID = twDB.getTweetId(position);
                    long userID = twDB.getUserID(position);
                    Intent intent = new Intent(getApplicationContext(), TweetDetail.class);
                    Bundle bundle = new Bundle();
                    bundle.putLong("tweetID",tweetID);
                    bundle.putLong("userID",userID);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });

        txtFollowing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFollows(0L);
            }
        });
        txtFollower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFollows(1L);
            }
        });
    }

    private void getFollows(long mode){
        Intent intent = new Intent(getApplicationContext(), Follower.class);
        Bundle bundle = new Bundle();
        bundle.putLong("userID",userId);
        bundle.putLong("mode",mode);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}