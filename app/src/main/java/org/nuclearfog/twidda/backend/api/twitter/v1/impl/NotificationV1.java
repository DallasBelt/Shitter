package org.nuclearfog.twidda.backend.api.twitter.v1.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.model.Notification;
import org.nuclearfog.twidda.model.Status;
import org.nuclearfog.twidda.model.User;

/**
 * Twitter implementation of a notification
 * Twitter currently only supports mentions as notification
 *
 * @author nuclearfog
 */
public class NotificationV1 implements Notification {

	private static final long serialVersionUID = -2434138376220697796L;

	private Status status;


	public NotificationV1(Status status) {
		this.status = status;
	}


	@Override
	public long getId() {
		return status.getId();
	}


	@Override
	public int getType() {
		// Twitte rcurrently only supports mentions as notification
		return TYPE_MENTION;
	}


	@Override
	public long getTimestamp() {
		return status.getTimestamp();
	}


	@Override
	public User getUser() {
		return status.getAuthor();
	}


	@Override
	public Status getStatus() {
		return status;
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		if (!(obj instanceof Notification))
			return false;
		return ((Notification) obj).getId() == getId();
	}


	@NonNull
	@Override
	public String toString() {
		return "id=" + getId() + " " + getUser();
	}
}