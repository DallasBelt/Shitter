package org.nuclearfog.twidda.adapter.holder;

/**
 * Click listener for adapter view holder
 *
 * @author nuclearfog
 */
public interface OnHolderClickListener {

	int NO_TYPE = -1;

	int LIST_CLICK = 1;

	int USER_CLICK = 2;

	int USER_REMOVE = 3;

	int STATUS_CLICK = 4;

	int STATUS_LABEL = 5;

	int LIST_PROFILE = 6;

	int MESSAGE_VIEW = 7;

	int MESSAGE_ANSWER = 8;

	int MESSAGE_PROFILE = 9;

	int MESSAGE_MEDIA = 10;

	int MESSAGE_DELETE = 11;

	int ACCOUNT_SELECT = 12;

	int ACCOUNT_REMOVE = 13;

	int IMAGE_CLICK = 14;

	int IMAGE_SAVE = 15;

	int PREVIEW_CLICK = 16;

	int CARD_IMAGE = 17;

	int CARD_LINK = 18;

	int POLL_ITEM = 19;

	int POLL_OPTION = 20;

	/**
	 * called when an item was clicked
	 *
	 * @param position adapter position of the item
	 * @param type     type of click
	 * @param extras   extra information
	 */
	void onItemClick(int position, int type, int... extras);

	/**
	 * called when a placeholder item was clicked
	 *
	 * @param position position of the item
	 * @return true to enable loading animation
	 */
	boolean onPlaceholderClick(int position);
}