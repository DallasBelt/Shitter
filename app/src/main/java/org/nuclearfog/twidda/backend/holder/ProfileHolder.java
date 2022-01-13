package org.nuclearfog.twidda.backend.holder;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * holder class for profile update information
 *
 * @author nuclearfog
 */
public class ProfileHolder {

    private String name;
    private String url;
    private String description;
    private String location;

    private InputStream profileImgStream;
    private InputStream bannerImgStream;


    public ProfileHolder(String name, String url, String description, String location) {
        this.name = name;
        this.url = url;
        this.description = description;
        this.location = location;
    }

    /**
     * add profile image Uri
     *
     * @param context context used to resolve Uri
     * @param profileImgUri Uri of the local image file
     */
    public void addImageUri(Context context, @Nullable Uri profileImgUri) {
        try {
            profileImgStream = context.getContentResolver().openInputStream(profileImgUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * add banner image Uri
     *
     * @param context context used to resolve Uri
     * @param bannerImgUri Uri of the local image file
     */
    public void addBannerUri(Context context, @Nullable Uri bannerImgUri) {
        try {
            bannerImgStream = context.getContentResolver().openInputStream(bannerImgUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return screen name of the user
     */
    public String getName() {
        return name;
    }

    /**
     * @return profile description (bio)
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return location name
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return profile url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return filestream of the profile image
     */
    public InputStream getProfileImageStream() {
        return profileImgStream;
    }

    /**
     * @return filestream of the banner image
     */
    public InputStream getBannerImageStream() {
        return bannerImgStream;
    }
}