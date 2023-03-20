package org.nuclearfog.twidda.database.impl;

import static org.nuclearfog.twidda.database.AppDatabase.MASK_USER_DEFAULT_IMAGE;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_USER_FOLLOW_REQUESTED;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_USER_PRIVATE;
import static org.nuclearfog.twidda.database.AppDatabase.MASK_USER_VERIFIED;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.database.DatabaseAdapter.UserRegisterTable;
import org.nuclearfog.twidda.database.DatabaseAdapter.UserTable;
import org.nuclearfog.twidda.model.Account;
import org.nuclearfog.twidda.model.Emoji;
import org.nuclearfog.twidda.model.User;

/**
 * database implementation of an user
 *
 * @author nuclearfog
 */
public class DatabaseUser implements User {

	private static final long serialVersionUID = 2367055336838212570L;

	private long id;
	private long createdAt;
	private int following;
	private int follower;
	private int statusCount;
	private int favorCount;
	private boolean isCurrentUser;
	private boolean isVerified;
	private boolean isLocked;
	private boolean followReqSent;
	private boolean defaultImage;
	private String username = "";
	private String screen_name = "";
	private String bio = "";
	private String location = "";
	private String link = "";
	private String profileImageSmall = "";
	private String profileImageOrig = "";
	private String profileBannerSmall = "";
	private String profileBannerOrig = "";

	/**
	 * @param cursor  database cursor containing user column
	 * @param account current user login
	 */
	public DatabaseUser(Cursor cursor, Account account) {
		id = cursor.getLong(cursor.getColumnIndexOrThrow(UserTable.ID));
		String username = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.USERNAME));
		String screen_name = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.SCREENNAME));
		String profileImageOrig = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.IMAGE));
		String bio = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.DESCRIPTION));
		String link = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.LINK));
		String location = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.LOCATION));
		String profileBannerOrig = cursor.getString(cursor.getColumnIndexOrThrow(UserTable.BANNER));
		createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(UserTable.SINCE));
		following = cursor.getInt(cursor.getColumnIndexOrThrow(UserTable.FRIENDS));
		follower = cursor.getInt(cursor.getColumnIndexOrThrow(UserTable.FOLLOWER));
		statusCount = cursor.getInt(cursor.getColumnIndexOrThrow(UserTable.STATUSES));
		favorCount = cursor.getInt(cursor.getColumnIndexOrThrow(UserTable.FAVORITS));
		int register = cursor.getInt(cursor.getColumnIndexOrThrow(UserRegisterTable.REGISTER));
		isVerified = (register & MASK_USER_VERIFIED) != 0;
		isLocked = (register & MASK_USER_PRIVATE) != 0;
		followReqSent = (register & MASK_USER_FOLLOW_REQUESTED) != 0;
		defaultImage = (register & MASK_USER_DEFAULT_IMAGE) != 0;
		isCurrentUser = account.getId() == id;

		if (username != null)
			this.username = username;
		if (screen_name != null)
			this.screen_name = screen_name;
		if (bio != null)
			this.bio = bio;
		if (link != null)
			this.link = link;
		if (location != null)
			this.location = location;
		if (profileImageOrig != null)
			this.profileImageOrig = profileImageOrig;
		if (profileBannerOrig != null)
			this.profileBannerOrig = profileBannerOrig;

		switch (account.getConfiguration()) {
			case TWITTER1:
			case TWITTER2:
				if (profileImageOrig != null && !profileImageOrig.isEmpty()) {
					if (defaultImage) {
						profileImageSmall = profileImageOrig;
					} else {
						profileImageSmall = profileImageOrig + "_bigger";
					}
				}
				if (profileBannerOrig != null && !profileBannerOrig.isEmpty()) {
					if (profileBannerOrig.endsWith("/1500x500")) {
						profileBannerSmall = profileBannerOrig.substring(0, profileBannerOrig.length() - 9) + "/600x200";
					} else {
						profileBannerSmall = profileBannerOrig + "/600x200";
					}
				}
				break;

			case MASTODON:
				profileImageSmall = profileImageOrig;
				profileBannerSmall = profileBannerOrig;
				break;

			default:
				profileImageSmall = "";
				profileBannerSmall = "";
				break;
		}
	}


	@Override
	public long getId() {
		return id;
	}


	@Override
	public String getUsername() {
		return username;
	}


	@Override
	public String getScreenname() {
		return screen_name;
	}


	@Override
	public long getTimestamp() {
		return createdAt;
	}


	@Override
	public String getOriginalProfileImageUrl() {
		return profileImageOrig;
	}


	@Override
	public String getProfileImageThumbnailUrl() {
		return profileImageSmall;
	}


	@Override
	public String getOriginalBannerImageUrl() {
		return profileBannerOrig;
	}


	@Override
	public String getBannerImageThumbnailUrl() {
		return profileBannerSmall;
	}


	@Override
	public boolean hasDefaultProfileImage() {
		return defaultImage;
	}


	@Override
	public String getDescription() {
		return bio;
	}


	@Override
	public String getLocation() {
		return location;
	}


	@Override
	public String getProfileUrl() {
		return link;
	}


	@Override
	public boolean isVerified() {
		return isVerified;
	}


	@Override
	public boolean isProtected() {
		return isLocked;
	}


	@Override
	public boolean followRequested() {
		return followReqSent;
	}


	@Override
	public int getFollowing() {
		return following;
	}


	@Override
	public int getFollower() {
		return follower;
	}


	@Override
	public int getStatusCount() {
		return statusCount;
	}


	@Override
	public int getFavoriteCount() {
		return favorCount;
	}


	@Override
	public boolean isCurrentUser() {
		return isCurrentUser;
	}


	@Override
	public Emoji[] getEmojis() {
		return new Emoji[0];
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof User))
			return false;
		return ((User) obj).getId() == id;
	}


	@Override
	public int compareTo(User o) {
		return Long.compare(o.getTimestamp(), createdAt);
	}


	@NonNull
	@Override
	public String toString() {
		return "name=\"" + screen_name + "\"";
	}

	/**
	 * set this user as current user
	 */
	public void setAsCurrentUser() {
		isCurrentUser = true;
	}
}