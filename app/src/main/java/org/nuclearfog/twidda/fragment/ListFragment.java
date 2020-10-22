package org.nuclearfog.twidda.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.activity.ListDetail;
import org.nuclearfog.twidda.activity.UserDetail;
import org.nuclearfog.twidda.activity.UserProfile;
import org.nuclearfog.twidda.adapter.FragmentAdapter.FragmentChangeObserver;
import org.nuclearfog.twidda.adapter.ListAdapter;
import org.nuclearfog.twidda.adapter.ListAdapter.ListClickListener;
import org.nuclearfog.twidda.backend.TwitterListLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.items.TwitterList;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.util.List;

import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.os.AsyncTask.Status.FINISHED;
import static android.os.AsyncTask.Status.RUNNING;
import static org.nuclearfog.twidda.activity.ListDetail.KEY_CURRENT_USER_OWNS;
import static org.nuclearfog.twidda.activity.ListDetail.KEY_LISTDETAIL_DESCR;
import static org.nuclearfog.twidda.activity.ListDetail.KEY_LISTDETAIL_ID;
import static org.nuclearfog.twidda.activity.ListDetail.KEY_LISTDETAIL_TITLE;
import static org.nuclearfog.twidda.activity.ListDetail.KEY_LISTDETAIL_VISIB;
import static org.nuclearfog.twidda.activity.UserDetail.KEY_USERDETAIL_ID;
import static org.nuclearfog.twidda.activity.UserDetail.KEY_USERDETAIL_MODE;
import static org.nuclearfog.twidda.activity.UserDetail.USERLIST_SUBSCRBR;
import static org.nuclearfog.twidda.activity.UserProfile.KEY_PROFILE_ID;
import static org.nuclearfog.twidda.backend.TwitterListLoader.Action.DELETE;
import static org.nuclearfog.twidda.backend.TwitterListLoader.Action.FOLLOW;
import static org.nuclearfog.twidda.backend.TwitterListLoader.Action.LOAD;
import static org.nuclearfog.twidda.backend.TwitterListLoader.NO_CURSOR;

/**
 * Fragment class for user lists
 */
public class ListFragment extends Fragment implements OnRefreshListener, ListClickListener,
        FragmentChangeObserver, DialogInterface.OnClickListener {

    /**
     * Key for the owner ID
     */
    public static final String KEY_FRAG_LIST_OWNER_ID = "list_owner_id";

    /**
     * alternative key for the owner name
     */
    public static final String KEY_FRAG_LIST_OWNER_NAME = "list_owner_name";

    private TwitterListLoader listTask;
    private GlobalSettings settings;

    private SwipeRefreshLayout reloadLayout;
    private RecyclerView list;
    private ListAdapter adapter;

    private Dialog followDialog, deleteDialog;

    private long selectedList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle param) {
        Context context = inflater.getContext();

        settings = GlobalSettings.getInstance(context);
        adapter = new ListAdapter(this, settings);

        list = new RecyclerView(inflater.getContext());
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setHasFixedSize(true);
        list.setAdapter(adapter);

        reloadLayout = new SwipeRefreshLayout(context);
        reloadLayout.setProgressBackgroundColorSchemeColor(settings.getHighlightColor());
        reloadLayout.setOnRefreshListener(this);
        reloadLayout.addView(list);
        return reloadLayout;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (listTask == null) {
            setRefresh(true);
            load(NO_CURSOR);
        }
    }


    @Override
    public void onDestroy() {
        if (listTask != null && listTask.getStatus() == RUNNING)
            listTask.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onRefresh() {
        if (listTask != null && listTask.getStatus() != RUNNING) {
            load(NO_CURSOR);
        }
    }


    @Override
    public void onClick(final TwitterList listItem, Action action) {
        if (getContext() != null && !reloadLayout.isRefreshing()) {
            switch (action) {
                case PROFILE:
                    Intent profile = new Intent(getContext(), UserProfile.class);
                    profile.putExtra(KEY_PROFILE_ID, listItem.getListOwner().getId());
                    startActivity(profile);
                    break;

                case FOLLOW:
                    if (listItem.isFollowing()) {
                        if (followDialog == null) {
                            Builder confirmDialog = new Builder(getContext(), R.style.ConfirmDialog);
                            confirmDialog.setMessage(R.string.confirm_unfollow_list);
                            confirmDialog.setNegativeButton(R.string.confirm_no, null);
                            confirmDialog.setPositiveButton(R.string.confirm_yes, this);
                            followDialog = confirmDialog.create();
                        }
                        if (!followDialog.isShowing()) {
                            selectedList = listItem.getId();
                            followDialog.show();
                        }
                    } else {
                        listTask = new TwitterListLoader(this, FOLLOW, listItem.getId(), "");
                        listTask.execute(listItem.getId());
                    }
                    break;

                case SUBSCRIBER:
                    Intent following = new Intent(getContext(), UserDetail.class);
                    following.putExtra(KEY_USERDETAIL_ID, listItem.getId());
                    following.putExtra(KEY_USERDETAIL_MODE, USERLIST_SUBSCRBR);
                    startActivity(following);
                    break;

                case MEMBER:
                    Intent detailedList = new Intent(getContext(), ListDetail.class);
                    Bundle param = getArguments();
                    if (param != null && param.getLong(KEY_FRAG_LIST_OWNER_ID) == settings.getUserId())
                        detailedList.putExtra(KEY_CURRENT_USER_OWNS, true);
                    detailedList.putExtra(KEY_LISTDETAIL_ID, listItem.getId());
                    detailedList.putExtra(KEY_LISTDETAIL_TITLE, listItem.getTitle());
                    detailedList.putExtra(KEY_LISTDETAIL_DESCR, listItem.getDescription());
                    detailedList.putExtra(KEY_LISTDETAIL_VISIB, !listItem.isPrivate());
                    startActivity(detailedList);
                    break;

                case DELETE:
                    if (deleteDialog == null) {
                        Builder confirmDialog = new Builder(requireContext(), R.style.ConfirmDialog);
                        confirmDialog.setMessage(R.string.confirm_delete_list);
                        confirmDialog.setNegativeButton(R.string.confirm_no, null);
                        confirmDialog.setPositiveButton(R.string.confirm_yes, this);
                        deleteDialog = confirmDialog.create();
                    }
                    if (!deleteDialog.isShowing()) {
                        selectedList = listItem.getId();
                        deleteDialog.show();
                    }
                    break;
            }
        }
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == BUTTON_POSITIVE) {
            if (dialog == followDialog) {
                listTask = new TwitterListLoader(this, FOLLOW, selectedList, "");
                listTask.execute();
            } else if (dialog == deleteDialog) {
                listTask = new TwitterListLoader(this, DELETE, selectedList, "");
                listTask.execute();
            }
        }
    }


    @Override
    public void onReset() {
        if (list != null) {
            list.setAdapter(adapter);
            setRefresh(true);
            load(NO_CURSOR);
        }
    }


    @Override
    public void onTabChange() {
    }

    /**
     * set data to list
     *
     * @param data List of Twitter list data
     */
    public void setData(List<TwitterList> data) {
        adapter.setData(data);
        setRefresh(false);
    }

    /**
     * update item in list
     *
     * @param item Twitter list item
     */
    public void updateItem(TwitterList item) {
        adapter.updateItem(item);
    }

    /**
     * remove item with specific ID
     *
     * @param id ID of list to remove
     */
    public void removeItem(long id) {
        adapter.removeItem(id);
    }

    /**
     * called from {@link TwitterListLoader} to enable or disable RefreshLayout
     *
     * @param enable true to enable RefreshLayout with delay
     */
    private void setRefresh(boolean enable) {
        if (enable) {
            reloadLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (listTask != null && listTask.getStatus() != FINISHED
                            && !reloadLayout.isRefreshing())
                        reloadLayout.setRefreshing(true);
                }
            }, 500);
        } else {
            reloadLayout.setRefreshing(false);
        }
    }

    /**
     * called from {@link TwitterListLoader} if an error occurs
     *
     * @param error Twitter exception
     */
    public void onError(@Nullable EngineException error) {
        if (getContext() != null && error != null)
            ErrorHandler.handleFailure(getContext(), error);
        setRefresh(false);
    }

    /**
     * load content into the list
     */
    private void load(long cursor) {
        Bundle param = getArguments();
        if (param != null) {
            long id = param.getLong(KEY_FRAG_LIST_OWNER_ID, 0);
            String ownerName = param.getString(KEY_FRAG_LIST_OWNER_NAME, "");
            listTask = new TwitterListLoader(this, LOAD, id, ownerName);
            listTask.execute(cursor);
        }
    }
}