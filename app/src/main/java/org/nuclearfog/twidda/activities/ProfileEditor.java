package org.nuclearfog.twidda.activities;

import static android.os.AsyncTask.Status.RUNNING;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.nuclearfog.twidda.activities.UserProfile.RETURN_PROFILE_CHANGED;
import static org.nuclearfog.twidda.activities.UserProfile.RETURN_PROFILE_DATA;
import static org.nuclearfog.twidda.activities.UserProfile.TOOLBAR_TRANSPARENCY;
import static org.nuclearfog.twidda.database.GlobalSettings.BANNER_IMG_MID_RES;
import static org.nuclearfog.twidda.database.GlobalSettings.PROFILE_IMG_HIGH_RES;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.UserUpdater;
import org.nuclearfog.twidda.backend.holder.ProfileHolder;
import org.nuclearfog.twidda.model.User;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.backend.utils.PicassoBuilder;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.dialog.ConfirmDialog;
import org.nuclearfog.twidda.dialog.ConfirmDialog.DialogType;
import org.nuclearfog.twidda.dialog.ConfirmDialog.OnConfirmListener;
import org.nuclearfog.twidda.dialog.ProgressDialog;
import org.nuclearfog.twidda.dialog.ProgressDialog.OnProgressStopListener;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

/**
 * Activity for Twitter profile editor
 *
 * @author nuclearfog
 */
public class ProfileEditor extends MediaActivity implements OnClickListener, OnProgressStopListener, OnConfirmListener, Callback {

    /**
     * key to preload user data
     */
    public static final String KEY_USER_DATA = "profile-editor-data";

    private UserUpdater editorAsync;
    private GlobalSettings settings;
    private Picasso picasso;

    private ImageView profile_image, profile_banner, toolbar_background, changeBannerBtn;
    private EditText name, link, loc, bio;
    private Button addBannerBtn;
    private Dialog loadingCircle;
    private ConfirmDialog errorDialog, closeDialog;

    private User user;
    private Uri profileLink, bannerLink;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppStyles.setFontScale(newBase));
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_editprofile);
        Toolbar toolbar = findViewById(R.id.editprofile_toolbar);
        ConstraintLayout root = findViewById(R.id.page_edit);
        ImageView changeImageBtn = findViewById(R.id.profile_change_image_btn);
        profile_image = findViewById(R.id.edit_pb);
        profile_banner = findViewById(R.id.edit_banner);
        addBannerBtn = findViewById(R.id.edit_add_banner);
        changeBannerBtn = findViewById(R.id.edit_change_banner);
        toolbar_background = findViewById(R.id.editprofile_toolbar_background);
        name = findViewById(R.id.edit_name);
        link = findViewById(R.id.edit_link);
        loc = findViewById(R.id.edit_location);
        bio = findViewById(R.id.edit_bio);

        loadingCircle = new ProgressDialog(this, this);
        closeDialog = new ConfirmDialog(this, DialogType.PROFILE_EDITOR_LEAVE, this);
        errorDialog = new ConfirmDialog(this, DialogType.PROFILE_EDITOR_ERROR, this);
        toolbar.setTitle(R.string.page_profile_editor);
        setSupportActionBar(toolbar);

        settings = GlobalSettings.getInstance(this);
        if (!settings.toolbarOverlapEnabled()) {
            ConstraintSet constraints = new ConstraintSet();
            constraints.clone(root);
            constraints.connect(R.id.edit_banner, ConstraintSet.TOP, R.id.editprofile_toolbar, ConstraintSet.BOTTOM);
            constraints.connect(R.id.edit_add_banner, ConstraintSet.TOP, R.id.edit_banner, ConstraintSet.TOP);
            constraints.applyTo(root);
        }
        toolbar.setBackgroundColor(settings.getBackgroundColor() & TOOLBAR_TRANSPARENCY);
        changeBannerBtn.setImageResource(R.drawable.add);
        changeImageBtn.setImageResource(R.drawable.add);
        profile_banner.setDrawingCacheEnabled(true);
        AppStyles.setTheme(root, settings.getBackgroundColor());
        picasso = PicassoBuilder.get(this);

        Object data = getIntent().getSerializableExtra(KEY_USER_DATA);
        if (data instanceof User) {
            user = (User) data;
            setUser();
        }
        profile_image.setOnClickListener(this);
        profile_banner.setOnClickListener(this);
        addBannerBtn.setOnClickListener(this);
    }


    @Override
    protected void onDestroy() {
        if (editorAsync != null && editorAsync.getStatus() == RUNNING)
            editorAsync.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
        String username = name.getText().toString();
        String userLink = link.getText().toString();
        String userLoc = loc.getText().toString();
        String userBio = bio.getText().toString();
        if (user != null && username.equals(user.getUsername()) && userLink.equals(user.getProfileUrl())
                && userLoc.equals(user.getLocation()) && userBio.equals(user.getDescription())
                && profileLink == null && bannerLink == null) {
            finish();
        } else if (username.isEmpty() && userLink.isEmpty() && userLoc.isEmpty() && userBio.isEmpty()) {
            finish();
        } else if (!closeDialog.isShowing()) {
            closeDialog.show();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        getMenuInflater().inflate(R.menu.edit, m);
        AppStyles.setMenuIconColor(m, settings.getIconColor());
        return super.onCreateOptionsMenu(m);
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            updateUser();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onAttachLocation(@Nullable Location location) {
    }


    @Override
    protected void onMediaFetched(int resultType, @NonNull Uri uri) {
        // Add image as profile image
        if (resultType == REQUEST_PROFILE) {
            profile_image.setImageURI(uri);
            profileLink = uri;
        }
        // Add image as banner image
        else if (resultType == REQUEST_BANNER) {
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            picasso.load(uri).resize(displaySize.x, displaySize.x / 3).centerCrop(Gravity.TOP).into(profile_banner, this);
            addBannerBtn.setVisibility(INVISIBLE);
            changeBannerBtn.setVisibility(VISIBLE);
            bannerLink = uri;
        }
    }


    @Override
    public void onClick(View v) {
        // select net profile image
        if (v.getId() == R.id.edit_pb) {
            getMedia(REQUEST_PROFILE);
        }
        // select new banner image
        else if (v.getId() == R.id.edit_add_banner || v.getId() == R.id.edit_banner) {
            getMedia(REQUEST_BANNER);
        }
    }


    @Override
    public void stopProgress() {
        if (editorAsync != null && editorAsync.getStatus() == RUNNING) {
            editorAsync.cancel(true);
        }
    }


    @Override
    public void onConfirm(DialogType type) {
        if (type == DialogType.PROFILE_EDITOR_LEAVE) {
            finish();
        } else if (type == DialogType.PROFILE_EDITOR_ERROR) {
            updateUser();
        }
    }


    @Override
    public void onSuccess() {
        // set toolbar background
        if (settings.toolbarOverlapEnabled()) {
            AppStyles.setToolbarBackground(ProfileEditor.this, profile_banner, toolbar_background);
        }
    }


    @Override
    public void onError(Exception e) {
    }

    /**
     * called after user profile was updated successfully
     */
    public void onSuccess(User user) {
        Intent data = new Intent();
        data.putExtra(RETURN_PROFILE_DATA, user);
        Toast.makeText(this, R.string.info_profile_updated, Toast.LENGTH_SHORT).show();
        setResult(RETURN_PROFILE_CHANGED, data);
        finish();
    }

    /**
     * called after an error occurs
     *
     * @param err Engine Exception
     */
    public void onError(ErrorHandler.TwitterError err) {
        if (!errorDialog.isShowing()) {
            String message = ErrorHandler.getErrorMessage(this, err);
            errorDialog.setMessage(message);
            errorDialog.show();
        }
        if (loadingCircle.isShowing()) {
            loadingCircle.dismiss();
        }
    }

    /**
     * update user information
     */
    private void updateUser() {
        if (editorAsync == null || editorAsync.getStatus() != RUNNING) {
            String username = name.getText().toString();
            String userLink = link.getText().toString();
            String userLoc = loc.getText().toString();
            String userBio = bio.getText().toString();
            if (username.trim().isEmpty()) {
                String errMsg = getString(R.string.error_empty_name);
                name.setError(errMsg);
            } else if (!userLink.isEmpty() && userLink.contains(" ")) {
                String errMsg = getString(R.string.error_invalid_link);
                link.setError(errMsg);
            } else if (editorAsync == null || editorAsync.getStatus() != RUNNING) {
                ProfileHolder holder = new ProfileHolder(username, userLink, userBio, userLoc);
                if (profileLink != null)
                    holder.addImageUri(getApplicationContext(), profileLink);
                if (bannerLink != null)
                    holder.addBannerUri(getApplicationContext(), bannerLink);
                editorAsync = new UserUpdater(this, holder);
                editorAsync.execute();
                if (!loadingCircle.isShowing()) {
                    loadingCircle.show();
                }
            }
        }
    }

    /**
     * Set current user's information
     */
    private void setUser() {
        if (!user.getImageUrl().isEmpty()) {
            String pbLink = user.getImageUrl();
            if (!user.hasDefaultProfileImage())
                pbLink += PROFILE_IMG_HIGH_RES;
            picasso.load(pbLink).transform(new RoundedCornersTransformation(5, 0)).into(profile_image);
        }
        if (!user.getBannerUrl().isEmpty()) {
            String bnLink = user.getBannerUrl() + BANNER_IMG_MID_RES;
            picasso.load(bnLink).into(profile_banner, this);
            addBannerBtn.setVisibility(INVISIBLE);
            changeBannerBtn.setVisibility(VISIBLE);
        } else {
            addBannerBtn.setVisibility(VISIBLE);
            changeBannerBtn.setVisibility(INVISIBLE);
        }
        name.setText(user.getUsername());
        link.setText(user.getProfileUrl());
        loc.setText(user.getLocation());
        bio.setText(user.getDescription());
    }
}