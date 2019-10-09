package org.nuclearfog.twidda;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.Tab;

import org.nuclearfog.twidda.adapter.FragmentAdapter;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.window.AppSettings;
import org.nuclearfog.twidda.window.LoginPage;
import org.nuclearfog.twidda.window.SearchPage;
import org.nuclearfog.twidda.window.TweetPopup;
import org.nuclearfog.twidda.window.UserProfile;

import static org.nuclearfog.twidda.window.SearchPage.KEY_SEARCH;
import static org.nuclearfog.twidda.window.UserProfile.KEY_PROFILE_ID;
import static org.nuclearfog.twidda.window.UserProfile.KEY_PROFILE_NAME;

/**
 * Main Activity
 */
public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {

    public static final int DB_CLEARED = 3;
    public static final int APP_LOGOUT = 4;
    private static final int LOGIN = 1;
    private static final int SETTING = 2;

    private GlobalSettings settings;
    private FragmentAdapter adapter;
    private TabLayout tablayout;
    private ViewPager pager;
    private View root;
    private int tabIndex = 0;

    static {
        // Enable vector drawable support
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.page_main);
        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        pager = findViewById(R.id.home_pager);
        tablayout = findViewById(R.id.home_tab);
        root = findViewById(R.id.main_layout);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        settings = GlobalSettings.getInstance(this);
        adapter = new FragmentAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setOffscreenPageLimit(3);
        tablayout.setupWithViewPager(pager);
        tablayout.addOnTabSelectedListener(this);

        Tab tlTab = tablayout.getTabAt(0);
        Tab trTab = tablayout.getTabAt(1);
        Tab mnTab = tablayout.getTabAt(2);

        if (tlTab != null && trTab != null && mnTab != null) {
            tlTab.setIcon(R.drawable.home);
            trTab.setIcon(R.drawable.hash);
            mnTab.setIcon(R.drawable.mention);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (!settings.getLogin()) {
            Intent loginIntent = new Intent(this, LoginPage.class);
            startActivityForResult(loginIntent, LOGIN);
        }
        root.setBackgroundColor(settings.getBackgroundColor());
        tablayout.setSelectedTabIndicatorColor(settings.getHighlightColor());
    }


    @Override
    protected void onActivityResult(int reqCode, int returnCode, Intent intent) {
        switch (reqCode) {
            case LOGIN:
                if (returnCode == RESULT_CANCELED)
                    finish();
                break;

            case SETTING:
                if (returnCode == DB_CLEARED)
                    adapter.clearData();
                else
                    adapter.notifySettingsChanged();
                break;
        }
        super.onActivityResult(reqCode, returnCode, intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.home, m);
        MenuItem search = m.findItem(R.id.action_search);
        SearchView searchQuery = (SearchView) search.getActionView();
        searchQuery.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Intent search = new Intent(MainActivity.this, SearchPage.class);
                search.putExtra(KEY_SEARCH, s);
                startActivity(search);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        MenuItem profile = m.findItem(R.id.action_profile);
        MenuItem tweet = m.findItem(R.id.action_tweet);
        MenuItem search = m.findItem(R.id.action_search);
        MenuItem setting = m.findItem(R.id.action_settings);

        switch (tabIndex) {
            case 0:
                profile.setVisible(true);
                search.setVisible(false);
                tweet.setVisible(true);
                setting.setVisible(false);
                search.collapseActionView();
                break;

            case 1:
                profile.setVisible(false);
                search.setVisible(true);
                tweet.setVisible(false);
                setting.setVisible(true);
                break;

            case 2:
                profile.setVisible(false);
                search.setVisible(false);
                tweet.setVisible(false);
                setting.setVisible(true);
                search.collapseActionView();
                break;
        }
        return super.onPrepareOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_profile:
                long homeId = settings.getUserId();
                Intent user = new Intent(this, UserProfile.class);
                user.putExtra(KEY_PROFILE_ID, homeId);
                user.putExtra(KEY_PROFILE_NAME, "");
                startActivity(user);
                break;

            case R.id.action_tweet:
                Intent tweet = new Intent(this, TweetPopup.class);
                startActivity(tweet);
                break;

            case R.id.action_settings:
                Intent settings = new Intent(this, AppSettings.class);
                startActivityForResult(settings, SETTING);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (tabIndex == 0) {
            super.onBackPressed();
        } else {
            pager.setCurrentItem(0);
        }
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        tabIndex = tab.getPosition();
        invalidateOptionsMenu();
    }


    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        adapter.scrollToTop(tab.getPosition());
    }


    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }
}