package org.nuclearfog.twidda.adapter;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.nuclearfog.twidda.database.GlobalSettings;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

/**
 * Spinner Adapter for font settings
 *
 * @author nuclearfog
 * @see org.nuclearfog.twidda.activity.AppSettings
 */
public class FontAdapter extends BaseAdapter {

    private static final int TEXT_PADDING = 20;
    private static final float FONT_SIZE = 24.0f;
    private static final Typeface[] fonts = GlobalSettings.FONTS;
    private static final String[] names = GlobalSettings.FONT_NAMES;

    private GlobalSettings settings;

    public FontAdapter(GlobalSettings settings) {
        this.settings = settings;
    }


    @Override
    public int getCount() {
        return fonts.length;
    }


    @Override
    public long getItemId(int pos) {
        return getItem(pos).hashCode();
    }


    @Override
    public Typeface getItem(int pos) {
        return fonts[pos];
    }


    @Override
    public View getView(int pos, View view, ViewGroup parent) {
        TextView tv;
        String name = names[pos];
        Typeface font = fonts[pos];
        if (view instanceof TextView)
            tv = (TextView) view;
        else {
            tv = new TextView(parent.getContext());
            tv.setTextSize(COMPLEX_UNIT_SP, FONT_SIZE);
            tv.setPadding(TEXT_PADDING, 0, TEXT_PADDING, 0);
            tv.setTextColor(settings.getFontColor());
        }
        tv.setText(name);
        tv.setTypeface(font);
        return tv;
    }
}