package org.nuclearfog.twidda.ui.activities;

import static org.nuclearfog.twidda.ui.activities.StatusEditor.KEY_STATUS_EDITOR_TEXT;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.config.Configuration;
import org.nuclearfog.twidda.config.GlobalSettings;
import org.nuclearfog.twidda.ui.adapter.FragmentAdapter;
import org.nuclearfog.twidda.ui.views.TabSelector;
import org.nuclearfog.twidda.ui.views.TabSelector.OnTabSelectedListener;

/**
 * search Activity for statuses and users
 *
 * @author nuclearfog
 */
public class SearchActivity extends AppCompatActivity implements OnTabSelectedListener, OnQueryTextListener {

	/**
	 * Key for the search query, required
	 * value type is String
	 */
	public static final String KEY_SEARCH_QUERY = "search_query";

	public static final int SEARCH_STR_MAX_LEN = 128;

	private FragmentAdapter adapter;
	private GlobalSettings settings;
	private ViewPager2 viewPager;
	private Toolbar toolbar;

	private String search = "";


	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(AppStyles.setFontScale(newBase));
	}


	@Override
	protected void onCreate(@Nullable Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.page_search);
		ViewGroup root = findViewById(R.id.search_layout);
		TabSelector tabSelector = findViewById(R.id.search_tab);
		toolbar = findViewById(R.id.search_toolbar);
		viewPager = findViewById(R.id.search_pager);

		toolbar.setTitle("");
		setSupportActionBar(toolbar);

		settings = GlobalSettings.getInstance(this);
		adapter = new FragmentAdapter(this);
		tabSelector.addViewPager(viewPager);
		tabSelector.addOnTabSelectedListener(this);
		viewPager.setAdapter(adapter);
		viewPager.setOffscreenPageLimit(2);

		String search = getIntent().getStringExtra(KEY_SEARCH_QUERY);
		if (search != null) {
			this.search = search;
			boolean enableHashtags = !search.startsWith("#") && settings.getLogin().getConfiguration() == Configuration.MASTODON;
			adapter.setupSearchPage(search, enableHashtags);
			tabSelector.addTabIcons(R.array.search_tab_icons);
		}
		AppStyles.setTheme(root);
	}


	@Override
	public void onBackPressed() {
		if (viewPager.getCurrentItem() > 0) {
			viewPager.setCurrentItem(0);
		} else {
			super.onBackPressed();
		}
	}


	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu m) {
		getMenuInflater().inflate(R.menu.search, m);
		MenuItem searchItem = m.findItem(R.id.new_search);
		MenuItem searchFilter = m.findItem(R.id.search_filter);
		SearchView searchView = (SearchView) searchItem.getActionView();

		boolean enableSearchFilter = settings.getLogin().getConfiguration().filterEnabled();
		searchFilter.setChecked(settings.filterResults() & enableSearchFilter);
		searchView.setQueryHint(search);
		searchView.setOnQueryTextListener(this);
		// set theme
		AppStyles.setTheme(searchView, Color.TRANSPARENT);
		AppStyles.setMenuIconColor(m, settings.getIconColor());
		AppStyles.setOverflowIcon(toolbar, settings.getIconColor());
		return super.onCreateOptionsMenu(m);
	}


	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		// create new status
		if (item.getItemId() == R.id.search_status) {
			Intent intent = new Intent(this, StatusEditor.class);
			if (search.startsWith("#"))
				intent.putExtra(KEY_STATUS_EDITOR_TEXT, search + " ");
			startActivity(intent);
		}
		// theme expanded search view
		else if (item.getItemId() == R.id.new_search) {
			SearchView searchView = (SearchView) item.getActionView();
			AppStyles.setTheme(searchView, Color.TRANSPARENT);
		}
		// enable/disable search filter
		else if (item.getItemId() == R.id.search_filter) {
			boolean enable = !settings.filterResults();
			settings.setFilterResults(enable);
			item.setChecked(enable);
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onQueryTextSubmit(String s) {
		if (s.length() <= SearchActivity.SEARCH_STR_MAX_LEN && !s.contains(":") && !s.contains("$")) {
			Intent search = new Intent(this, SearchActivity.class);
			search.putExtra(KEY_SEARCH_QUERY, s);
			startActivity(search);
		} else {
			Toast.makeText(getApplicationContext(), R.string.error_twitter_search, Toast.LENGTH_SHORT).show();
		}
		return true;
	}


	@Override
	public boolean onQueryTextChange(String s) {
		return false;
	}


	@Override
	public void onTabSelected(int oldPosition, int position) {
		invalidateOptionsMenu();
		adapter.scrollToTop(oldPosition);
	}
}