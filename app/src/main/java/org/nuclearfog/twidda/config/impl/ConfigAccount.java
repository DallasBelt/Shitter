package org.nuclearfog.twidda.config.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.config.Configuration;
import org.nuclearfog.twidda.model.Account;
import org.nuclearfog.twidda.model.User;

/**
 * {link Account} implementation used by app settings
 *
 * @author nuclearfog
 */
public class ConfigAccount implements Account {

	private static final long serialVersionUID = -7526554312489208096L;

	private long id;
	private long timestamp;
	private int apiType;
	private String oauthToken, tokenSecret, bearerToken;
	private String consumerToken, consumerSecret, hostname;

	/**
	 *
	 */
	public ConfigAccount(@NonNull Account account) {
		id = account.getId();
		oauthToken = account.getOauthToken();
		tokenSecret = account.getOauthSecret();
		consumerToken = account.getConsumerToken();
		consumerSecret = account.getConsumerSecret();
		bearerToken = account.getBearerToken();
		hostname = account.getHostname();

		switch (account.getConfiguration()) {
			case TWITTER1:
				apiType = API_TWITTER_1;
				break;

			case TWITTER2:
				apiType = API_TWITTER_2;
				break;

			case MASTODON:
				apiType = API_MASTODON;
				break;
		}
	}

	/**
	 *
	 */
	public ConfigAccount(long id, String oauthToken, String tokenSecret, String consumerToken, String consumerSecret, String bearerToken, String hostname, int apiType) {
		this.id = id;
		this.oauthToken = oauthToken;
		this.tokenSecret = tokenSecret;
		this.consumerToken = consumerToken;
		this.consumerSecret = consumerSecret;
		this.bearerToken = bearerToken;
		this.hostname = hostname;
		this.apiType = apiType;
		timestamp = System.currentTimeMillis();
	}


	@Override
	public long getId() {
		return id;
	}


	@Override
	public long getTimestamp() {
		return timestamp;
	}


	@Nullable
	@Override
	public User getUser() {
		return null;
	}


	@Override
	public String getConsumerToken() {
		return consumerToken;
	}


	@Override
	public String getConsumerSecret() {
		return consumerSecret;
	}


	@Override
	public String getOauthToken() {
		return oauthToken;
	}


	@Override
	public String getOauthSecret() {
		return tokenSecret;
	}


	@Override
	public String getBearerToken() {
		return bearerToken;
	}


	@Override
	public String getHostname() {
		return hostname;
	}


	@Override
	public Configuration getConfiguration() {
		switch (apiType) {
			case API_TWITTER_1:
				return Configuration.TWITTER1;

			case API_TWITTER_2:
				return Configuration.TWITTER2;

			default:
			case API_MASTODON:
				return Configuration.MASTODON;
		}
	}


	@Override
	public boolean usingDefaultTokens() {
		return false;
	}


	@NonNull
	@Override
	public String toString() {
		return "hostname=\"" + getHostname() + "\" configuration=\"" + getConfiguration().getName() + "\"";
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof Account))
			return false;
		Account account = (Account) obj;
		if (account.getUser() != null && getUser() != null)
			return getUser().equals(account.getUser());
		return false;
	}

	/**
	 *
	 */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
}