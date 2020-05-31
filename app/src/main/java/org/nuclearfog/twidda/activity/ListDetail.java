package org.nuclearfog.twidda.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.FragmentAdapter;
import org.nuclearfog.twidda.backend.helper.FontTool;
import org.nuclearfog.twidda.database.GlobalSettings;


public class ListDetail extends AppCompatActivity implements OnTabSelectedListener {

    public static final String KEY_LISTDETAIL_ID = "list-id";
    public static final String KEY_LISTDETAIL_NAME = "list-name";

    private FragmentAdapter adapter;
    private TabLayout tablayout;
    private ViewPager pager;

    private int tabIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_listdetail);
        View root = findViewById(R.id.listdetail_root);
        Toolbar toolbar = findViewById(R.id.listdetail_toolbar);
        tablayout = findViewById(R.id.listdetail_tab);
        pager = findViewById(R.id.listdetail_pager);

        GlobalSettings settings = GlobalSettings.getInstance(this);
        FontTool.setViewFontAndColor(settings, root);
        root.setBackgroundColor(settings.getBackgroundColor());

        pager.setOffscreenPageLimit(2);
        tablayout.setupWithViewPager(pager);
        tablayout.setSelectedTabIndicatorColor(settings.getHighlightColor());
        setSupportActionBar(toolbar);
        tablayout.addOnTabSelectedListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Bundle param = getIntent().getExtras();

        if (adapter == null && param != null && param.containsKey(KEY_LISTDETAIL_ID)) {
            long id = param.getLong(KEY_LISTDETAIL_ID);
            String name = param.getString(KEY_LISTDETAIL_NAME, "");
            adapter = new FragmentAdapter(getSupportFragmentManager());
            adapter.setupListContentPage(id);
            pager.setAdapter(adapter);

            Tab tlTab = tablayout.getTabAt(0);
            Tab trTab = tablayout.getTabAt(1);
            if (tlTab != null && trTab != null) {
                tlTab.setIcon(R.drawable.list);
                trTab.setIcon(R.drawable.user);
            }
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(name);
            }
        }
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
    public void onTabSelected(Tab tab) {
        tabIndex = tab.getPosition();
    }


    @Override
    public void onTabUnselected(Tab tab) {
        if (adapter != null)
            adapter.scrollToTop(tab.getPosition());
    }


    @Override
    public void onTabReselected(Tab tab) {
    }
}