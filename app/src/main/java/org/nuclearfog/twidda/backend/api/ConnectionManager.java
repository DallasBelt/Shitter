package org.nuclearfog.twidda.backend.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.backend.api.mastodon.Mastodon;
import org.nuclearfog.twidda.backend.api.twitter.v1.TwitterV1;
import org.nuclearfog.twidda.backend.api.twitter.v2.TwitterV2;
import org.nuclearfog.twidda.config.Configuration;
import org.nuclearfog.twidda.config.GlobalSettings;
import org.nuclearfog.twidda.config.GlobalSettings.OnSettingsChangeListener;

/**
 * this class manages multiple API implementations depending on settings
 *
 * @author nuclearfog
 */
public class ConnectionManager implements OnSettingsChangeListener {

	private static final int IDX_MASTODON = 0;
	private static final int IDX_TWITTER1 = 1;
	private static final int IDX_TWITTER2 = 2;

	private static ConnectionManager instance;

	private Connection[] connections;
	private GlobalSettings settings;
	private boolean notifyChanged = false;

	/**
	 *
	 */
	private ConnectionManager(Context context) {
		connections = new Connection[3];
		connections[IDX_MASTODON] = new Mastodon(context);
		connections[IDX_TWITTER1] = new TwitterV1(context);
		connections[IDX_TWITTER2] = new TwitterV2(context);

		settings = GlobalSettings.getInstance(context);
		settings.addSettingsChangeListener(this);
	}


	@Override
	public void onSettingsChange() {
		notifyChanged = true;
	}

	/**
	 * creates a connection to an online service
	 *
	 * @return connection
	 */
	@NonNull
	public static Connection getConnection(Context context) {
		return getConnection(context, null);
	}

	/**
	 * creates a connection to an online service
	 *
	 * @param config configuration to use, null to choose automatically
	 * @return connection
	 */
	@NonNull
	public static Connection getConnection(Context context, @Nullable Configuration config) {
		ConnectionManager manager = ConnectionManager.getInstance(context);
		return manager.getConnection(config);
	}

	/**
	 * @return singleton instance of this class
	 */
	@NonNull
	public static ConnectionManager getInstance(Context context) {
		if (instance == null || instance.notifyChanged) {
			instance = new ConnectionManager(context);
		}
		return instance;
	}

	/**
	 * get singleton class of a connection
	 *
	 * @param config Network selection or null to choose automatically
	 * @return singleton instance
	 */
	@NonNull
	public Connection getConnection(@Nullable Configuration config) {
		// create new singleton instance if there is none or if settings change
		if (config == null) {
			config = instance.settings.getLogin().getConfiguration();
		}
		switch (config) {
			default:
			case MASTODON:
				return instance.connections[IDX_MASTODON];

			case TWITTER1:
				return instance.connections[IDX_TWITTER1];

			case TWITTER2:
				return instance.connections[IDX_TWITTER2];
		}
	}
}