package org.nuclearfog.twidda.model;

import java.io.Serializable;

/**
 * This interface represents a poll integrated in a status.
 *
 * @author nuclearfog
 */
public interface Poll extends Serializable, Comparable<Poll> {

	/**
	 * @return ID of the poll
	 */
	long getId();

	/**
	 * @return true if current user has voted
	 */
	boolean voted();

	/**
	 * @return true if vote is finished
	 */
	boolean closed();

	/**
	 * @return true if multiple choice is allowed
	 */
	boolean multipleChoice();

	/**
	 * @return time where the poll expires
	 */
	long expirationTime();

	/**
	 * @return total number of votes
	 */
	int voteCount();

	/**
	 * @return option
	 */
	Option[] getOptions();


	/**
	 * represents a vote option
	 */
	interface Option extends Serializable {

		/**
		 * @return title of the option
		 */
		String getTitle();

		/**
		 * @return vote count of the option
		 */
		int getVotes();

		/**
		 * @return true if option is selected
		 */
		boolean selected();
	}
}