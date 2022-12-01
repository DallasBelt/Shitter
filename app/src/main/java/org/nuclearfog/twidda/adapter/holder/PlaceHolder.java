package org.nuclearfog.twidda.adapter.holder;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.database.GlobalSettings;

/**
 * ViewHolder class for a placeholder view
 *
 * @author nuclearfog
 */
public class PlaceHolder extends ViewHolder implements OnClickListener {

	public final ProgressBar loadCircle;
	public final Button loadBtn;

	private OnHolderClickListener listener;

	/**
	 * @param parent     Parent view from adapter
	 * @param horizontal true if placeholder orientation is horizontal
	 */
	public PlaceHolder(ViewGroup parent, GlobalSettings settings, boolean horizontal) {
		super(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_placeholder, parent, false));
		// get views
		CardView background = (CardView) itemView;
		loadCircle = itemView.findViewById(R.id.placeholder_loading);
		loadBtn = itemView.findViewById(R.id.placeholder_button);
		// theme views
		background.setCardBackgroundColor(settings.getCardColor());
		loadBtn.setTextColor(settings.getFontColor());
		loadBtn.setTypeface(settings.getTypeFace());
		AppStyles.setButtonColor(loadBtn, settings.getFontColor());
		AppStyles.setProgressColor(loadCircle, settings.getHighlightColor());
		// enable extra views
		if (horizontal) {
			loadBtn.setVisibility(View.INVISIBLE);
			loadCircle.setVisibility(View.VISIBLE);
			background.getLayoutParams().height = MATCH_PARENT;
			background.getLayoutParams().width = WRAP_CONTENT;
		}
		loadBtn.setOnClickListener(this);
	}


	@Override
	public void onClick(View v) {
		if (v == loadBtn) {
			int position = getLayoutPosition();
			if (position != NO_POSITION && listener != null) {
				boolean enableLoading = listener.onHolderClick(position);
				setLoading(enableLoading);
			}
		}
	}

	/**
	 * enable or disable progress circle
	 *
	 * @param enable true to enable progress, false to disable
	 */
	public void setLoading(boolean enable) {
		if (enable) {
			loadCircle.setVisibility(View.VISIBLE);
			loadBtn.setVisibility(View.INVISIBLE);
		} else {
			loadCircle.setVisibility(View.INVISIBLE);
			loadBtn.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * set click listener for this item
	 */
	public void setOnHolderClickListener(OnHolderClickListener listener) {
		this.listener = listener;
	}

	/**
	 * listener used to call after item click
	 */
	public interface OnHolderClickListener {

		/**
		 *
		 * @param position position of the item
		 * @return true to enable loading animation
		 */
		boolean onHolderClick(int position);
	}
}