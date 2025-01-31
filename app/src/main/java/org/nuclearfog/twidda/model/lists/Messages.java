package org.nuclearfog.twidda.model.lists;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.model.Message;

import java.util.LinkedList;

/**
 * Custom message list implementation containing cursor information
 *
 * @author nuclearfog
 */
public class Messages extends LinkedList<Message> {

	private static final long serialVersionUID = 7877548659917419256L;

	private String prevCursor, nextCursor;

	/**
	 *
	 */
	public Messages() {
		this(null, null);
	}

	/**
	 * @param prevCursor cursor to a previous list
	 * @param nextCursor cursor to a next list
	 */
	public Messages(String prevCursor, String nextCursor) {
		super();
		this.prevCursor = prevCursor;
		this.nextCursor = nextCursor;
	}

	/**
	 *
	 */
	public Messages(Messages messages) {
		super.addAll(messages);
		prevCursor = messages.prevCursor;
		nextCursor = messages.nextCursor;
	}


	@Override
	@Nullable
	public Message get(int index) {
		return super.get(index);
	}

	/**
	 * replace old list with a new one
	 *
	 * @param list new list
	 */
	public void replaceAll(Messages list) {
		super.clear();
		super.addAll(list);
		prevCursor = list.prevCursor;
		nextCursor = list.nextCursor;
	}

	/**
	 * add a new list to the bottom of this list
	 *
	 * @param list  new list
	 * @param index Index of the sub list
	 */
	public void addAll(int index, Messages list) {
		super.addAll(index, list);
		nextCursor = list.nextCursor;
	}

	/**
	 * remove message item matching with a given ID
	 *
	 * @param id message ID
	 * @return index of the removed message or -1 if not found
	 */
	public int removeItem(long id) {
		for (int index = 0; index < size(); index++) {
			Message item = get(index);
			if (item != null && item.getId() == id) {
				remove(index);
				return index;
			}
		}
		return -1;
	}

	/**
	 * get previous cursor
	 *
	 * @return cursor string
	 */
	public String getPrevCursor() {
		return prevCursor;
	}

	/**
	 * get next cursor string
	 *
	 * @return cursor string
	 */
	public String getNextCursor() {
		return nextCursor;
	}


	@Override
	@NonNull
	public String toString() {
		return "size=" + size() + " previous=" + getPrevCursor() + " next=" + getNextCursor();
	}
}