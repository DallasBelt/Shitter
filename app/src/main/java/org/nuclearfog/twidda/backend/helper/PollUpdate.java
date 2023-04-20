package org.nuclearfog.twidda.backend.helper;

import androidx.annotation.NonNull;

import org.nuclearfog.twidda.model.Poll;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is used to create a status poll
 *
 * @author nuclearfog
 * @see StatusUpdate
 */
public class PollUpdate {

	private int duration;
	private boolean multipleChoice;
	private boolean hideTotals;
	private List<String> options;

	/**
	 *
	 */
	public PollUpdate() {
		options = new LinkedList<>();
	}

	/**
	 * create poll using existing poll
	 *
	 * @param poll existing poll to update
	 */
	public PollUpdate(Poll poll) {
		options = new LinkedList<>();
		multipleChoice = poll.multipleChoiceEnabled();
		for (Poll.Option option : poll.getOptions()) {
			options.add(option.getTitle());
		}
	}

	/**
	 * set poll duration
	 *
	 * @return duration time in seconds
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * @return true if multiple choice is enabled
	 */
	public boolean multipleChoiceEnabled() {
		return multipleChoice;
	}

	/**
	 * @return true to hide total votes until poll is finnished
	 */
	public boolean hideTotalVotes() {
		return hideTotals;
	}

	/**
	 * @return an array of vote options
	 */
	public List<String> getOptions() {
		return options;
	}

	/**
	 * @param duration duration in seconds
	 */
	public void setDuration(int duration) {
		this.duration = duration;
	}


	public void hideVotes(boolean hideTotals) {
		this.hideTotals = hideTotals;
	}


	public void setMultipleChoice(boolean multipleChoice) {
		this.multipleChoice = multipleChoice;
	}


	public void setOptions(List<String> options) {
		this.options.clear();
		this.options.addAll(options);
	}


	@NonNull
	@Override
	public String toString() {
		return "valid=" + duration + " multiple=" + multipleChoice + "options=" + options.size();
	}
}