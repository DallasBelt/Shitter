package org.nuclearfog.twidda.ui.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import org.nuclearfog.twidda.model.Media;
import org.nuclearfog.twidda.model.Message;
import org.nuclearfog.twidda.model.Status;
import org.nuclearfog.twidda.ui.adapter.holder.IconHolder;
import org.nuclearfog.twidda.ui.adapter.holder.OnHolderClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter sued to show status icons
 *
 * @author nuclearfog
 */
public class IconAdapter extends Adapter<IconHolder> implements OnHolderClickListener {

	private OnMediaClickListener listener;

	private List<Integer> items = new ArrayList<>();
	private boolean invert;

	/**
	 * @param invert true to invert item order
	 */
	public IconAdapter(OnMediaClickListener listener, boolean invert) {
		this.listener = listener;
		this.invert = invert;
	}


	@NonNull
	@Override
	public IconHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new IconHolder(parent, this);
	}


	@Override
	public void onBindViewHolder(@NonNull IconHolder holder, int position) {
		holder.setIconType(items.get(position));
	}


	@Override
	public int getItemCount() {
		return items.size();
	}


	@Override
	public void onItemClick(int position, int type, int... extras) {
		if (listener != null) {
			Integer item = items.get(position);
			if (item == IconHolder.TYPE_IMAGE || item == IconHolder.TYPE_GIF || item == IconHolder.TYPE_VIDEO || item == IconHolder.TYPE_AUDIO) {
				if (invert) {
					listener.onMediaClick(items.size() - position - 1);
				} else {
					listener.onMediaClick(position);
				}
			}
		}
	}


	@Override
	public boolean onPlaceholderClick(int index) {
		return false;
	}

	/**
	 * @return true if adapter does not contain any elements
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/**
	 * add icons using status information
	 */
	public void addItems(Status status) {
		items.clear();
		if (status.getMedia().length > 0) {
			addMediaIcons(status.getMedia());
		}
		if (status.getLocation() != null) {
			items.add(IconHolder.TYPE_LOCATION);
		}
		if (status.getPoll() != null) {
			items.add(IconHolder.TYPE_POLL);
		}
		notifyDataSetChanged();
	}

	/**
	 * add icons using message information
	 */
	public void addItems(Message message) {
		items.clear();
		if (message.getMedia().length > 0) {
			addMediaIcons(message.getMedia());
		}
		notifyDataSetChanged();
	}

	/**
	 * append image icon at the end
	 */
	public void addImageItem() {
		appendItem(IconHolder.TYPE_IMAGE);
	}

	/**
	 * append video icon at the end
	 */
	public void addVideoItem() {
		appendItem(IconHolder.TYPE_VIDEO);
	}

	/**
	 * append GIF icon at the end
	 */
	public void addGifItem() {
		appendItem(IconHolder.TYPE_GIF);
	}

	/**
	 * append audio icon to the end
	 */
	public void addAudioItem() {
		appendItem(IconHolder.TYPE_AUDIO);
	}

	/**
	 *
	 */
	private void appendItem(int itemType) {
		if (invert) {
			items.add(0, itemType);
			notifyItemInserted(0);
		} else {
			items.add(itemType);
			notifyItemInserted(items.size() - 1);
		}
	}

	/**
	 * add media icons depending on type
	 */
	private void addMediaIcons(Media[] medias) {
		for (Media media : medias) {
			switch (media.getMediaType()) {
				case Media.PHOTO:
					items.add(IconHolder.TYPE_IMAGE);
					break;

				case Media.GIF:
					items.add(IconHolder.TYPE_GIF);
					break;

				case Media.VIDEO:
					items.add(IconHolder.TYPE_VIDEO);
					break;

				case Media.AUDIO:
					items.add(IconHolder.TYPE_AUDIO);
					break;
			}
		}
	}

	/**
	 * item click lsitener for media icons
	 */
	public interface OnMediaClickListener {

		/**
		 * called on media item click
		 */
		void onMediaClick(int index);
	}
}