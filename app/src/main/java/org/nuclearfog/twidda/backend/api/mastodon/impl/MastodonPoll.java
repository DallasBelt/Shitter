package org.nuclearfog.twidda.backend.api.mastodon.impl;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.model.Poll;

/**
 * Poll implementation for Mastodon
 *
 * @author nuclearfog
 */
public class MastodonPoll implements Poll {

	private static final long serialVersionUID = 1387541658586903903L;

	private long id;
	private long exTime;
	private boolean expired;
	private boolean voted;
	private boolean multipleChoice;
	private int voteCount;
	private MastodonOption[] options;

	/**
	 * @param json Mastodon poll jswon format
	 */
	public MastodonPoll(JSONObject json) throws JSONException {
		String idStr = json.getString("id");
		String exTimeStr = json.getString("expires_at");
		JSONArray optionsJson = json.getJSONArray("options");
		JSONArray votesJson = json.optJSONArray("own_votes");
		exTime = StringTools.getTime(exTimeStr, StringTools.TIME_MASTODON);
		expired = json.getBoolean("expired");
		voted = json.optBoolean("voted", false);
		voteCount = json.getInt("voters_count");
		multipleChoice = json.getBoolean("multiple");

		options = new MastodonOption[optionsJson.length()];
		for (int i = 0; i < optionsJson.length(); i++) {
			JSONObject option = optionsJson.getJSONObject(i);
			options[i] = new MastodonOption(option);
		}
		if (votesJson != null) {
			for (int i = 0; i < votesJson.length(); i++) {
				int index = votesJson.getInt(i);
				if (index >= 0 && index < options.length) {
					options[index].setSelected();
				}
			}
		}
		try {
			id = Long.parseLong(idStr);
		} catch (NumberFormatException e) {
			throw new JSONException("Bad ID: " + idStr);
		}
	}


	@Override
	public long getId() {
		return id;
	}


	@Override
	public boolean voted() {
		return voted;
	}


	@Override
	public boolean closed() {
		return expired;
	}


	@Override
	public int getLimit() {
		if (voted || expired) {
			return 0;
		}
		if (multipleChoice) {
			return options.length;
		}
		return 1;
	}


	@Override
	public long expirationTime() {
		return exTime;
	}


	@Override
	public int voteCount() {
		return voteCount;
	}


	@Override
	public Option[] getOptions() {
		return options;
	}


	@Override
	public int compareTo(Poll o) {
		return Long.compare(id, o.getId());
	}


	@NonNull
	@Override
	public String toString() {
		StringBuilder optionsBuf = new StringBuilder(" options=(");
		for (Option option : options) {
			optionsBuf.append(option).append(',');
		}
		optionsBuf.deleteCharAt(optionsBuf.length() - 1).append(')');
		return "id=" + id + " expired=" + expired + " options=" + optionsBuf;
	}

	/**
	 * Mastodon poll option implementation
	 */
	private static class MastodonOption implements Option {

		private static final long serialVersionUID = 4625032116285945452L;

		private String title;
		private int voteCount;
		private boolean selected = false;

		/**
		 * @param json mastodon poll json format
		 */
		private MastodonOption(JSONObject json) throws JSONException {
			voteCount = json.getInt("votes_count");
			title = json.getString("title");
		}


		@Override
		public String getTitle() {
			return title;
		}


		@Override
		public int getVotes() {
			return voteCount;
		}


		@Override
		public boolean selected() {
			return selected;
		}


		@NonNull
		@Override
		public String toString() {
			return "title=\"" + title + "\" votes=" + voteCount + " selected=" + selected;
		}

		/**
		 * mark this option as selected
		 */
		private void setSelected() {
			selected = true;
		}
	}
}