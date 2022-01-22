package org.nuclearfog.twidda.backend.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.model.User;
import org.nuclearfog.twidda.model.UserList;

/**
 * API v 1.1 user list implementation
 *
 * @author nuclearfog
 */
class UserListV1 implements UserList {

    private static final long serialVersionUID = 4121925943880606236L;

    private long id;
    private long time;
    private String title;
    private String description;
    private int memberCount;
    private int subscriberCount;
    private boolean isPrivate;
    private boolean following;
    private boolean isOwner;
    private User owner;


    UserListV1(JSONObject json, long currentId) throws JSONException {
        id = Long.parseLong(json.optString("id_str", "-1"));
        title = json.optString("name");
        description = json.optString("description");
        memberCount = json.optInt("member_count");
        subscriberCount = json.optInt("subscriber_count");
        isPrivate = json.optString("mode").equals("private");
        following = json.optBoolean("following");
        time = StringTools.getTime1(json.optString("created_at"));
        owner = new UserV1(json.getJSONObject("user"));
        isOwner = currentId == owner.getId();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getTimestamp() {
        return time;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public User getListOwner() {
        return owner;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }

    @Override
    public boolean isFollowing() {
        return following;
    }

    @Override
    public int getMemberCount() {
        return memberCount;
    }

    @Override
    public int getSubscriberCount() {
        return subscriberCount;
    }

    @Override
    public boolean isListOwner() {
        return isOwner;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof UserList))
            return false;
        return ((UserList) obj).getId() == id;
    }

    @NonNull
    @Override
    public String toString() {
        return title + " / " + description;
    }

    /**
     * set manually follow status
     *
     * @param following following status
     */
    void setFollowing(boolean following) {
        this.following = following;
    }
}