package org.nuclearfog.twidda.ui.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.nuclearfog.twidda.config.GlobalSettings;
import org.nuclearfog.twidda.lists.Trends;
import org.nuclearfog.twidda.model.Trend;
import org.nuclearfog.twidda.ui.adapter.holder.OnHolderClickListener;
import org.nuclearfog.twidda.ui.adapter.holder.PlaceHolder;
import org.nuclearfog.twidda.ui.adapter.holder.TrendHolder;

/**
 * custom {@link androidx.recyclerview.widget.RecyclerView} adapter implementation to show trends
 *
 * @author nuclearfog
 * @see org.nuclearfog.twidda.ui.fragments.TrendFragment
 */
public class TrendAdapter extends Adapter<ViewHolder> implements OnHolderClickListener {

	/**
	 * "index" used to replace the whole list with new items
	 */
	public static final int CLEAR_LIST = -1;

	private static final int TYPE_TREND = 0;

	private static final int TYPE_PLACEHOLDER = 1;

	private TrendClickListener itemClickListener;
	private GlobalSettings settings;

	private Trends items = new Trends();

	/**
	 * @param itemClickListener Listener for item click
	 */
	public TrendAdapter(GlobalSettings settings, TrendClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
		this.settings = settings;
	}


	@Override
	public int getItemCount() {
		return items.size();
	}


	@Override
	public int getItemViewType(int position) {
		if (items.get(position) != null)
			return TYPE_TREND;
		return TYPE_PLACEHOLDER;
	}


	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (viewType == TYPE_TREND) {
			return new TrendHolder(parent, settings, this);
		} else {
			return new PlaceHolder(parent, settings, false, this);
		}
	}


	@Override
	public void onBindViewHolder(@NonNull ViewHolder vh, int index) {
		if (vh instanceof TrendHolder) {
			TrendHolder holder = (TrendHolder) vh;
			Trend trend = items.get(index);
			if (trend != null) {
				holder.setContent(trend, index);
			}
		} else if (vh instanceof PlaceHolder) {

		}
	}


	@Override
	public void onItemClick(int position, int type, int... extras) {
		itemClickListener.onTrendClick(items.get(position));
	}


	@Override
	public boolean onPlaceholderClick(int index) {
		return false;
	}

	/**
	 * get adapter items
	 *
	 * @return a copy of the items
	 */
	public Trends getItems() {
		return new Trends(items);
	}

	/**
	 * replace data from list
	 *
	 * @param newItems array of trend items
	 */
	public void addItems(Trends newItems, int index) {
		if (index < 0) {
			this.items.replaceAll(newItems);
			notifyDataSetChanged();
		} else {
			items.addAll(index, newItems);
			if (items.getMaxId() != 0L && items.peekLast() != null) {
				items.add(null);
				notifyItemRangeInserted(index, newItems.size() + 1);
			} else if (items.getMaxId() == 0L && items.peekLast() == null) {
				items.pollLast();
				notifyItemRangeInserted(index, newItems.size() - 1);
			} else if (!newItems.isEmpty()) {
				notifyItemRangeInserted(index, newItems.size());
			}
		}
	}

	/**
	 * clear adapter data
	 */
	public void clear() {
		items.clear();
		notifyDataSetChanged();
	}

	/**
	 * check if adapter is empty
	 *
	 * @return true if adapter is empty
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/**
	 * Listener for trend list
	 */
	public interface TrendClickListener {

		/**
		 * called when a trend item is clicked
		 *
		 * @param trend trend name
		 */
		void onTrendClick(Trend trend);


		boolean onPlaceholderClick(long cursor, int index);
	}
}