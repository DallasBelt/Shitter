package org.nuclearfog.twidda.backend.async;

import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import org.nuclearfog.twidda.backend.api.Connection;
import org.nuclearfog.twidda.backend.api.ConnectionException;
import org.nuclearfog.twidda.backend.api.twitter.Twitter;
import org.nuclearfog.twidda.backend.api.twitter.update.MediaUpdate;
import org.nuclearfog.twidda.backend.utils.StringTools;
import org.nuclearfog.twidda.ui.activities.ImageViewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

/**
 * This AsyncTask class downloads images to a local cache folder
 * and creates Uri of the images.
 *
 * @author nuclearfog
 * @see ImageViewer
 */
public class ImageLoader extends AsyncTask<Uri, Uri, Boolean> {

	private WeakReference<ImageViewer> weakRef;
	private Connection connection;

	@Nullable
	private ConnectionException exception;
	private File cacheFolder;

	/**
	 * @param activity    Activity context
	 * @param cacheFolder cache folder where to store image files
	 */
	public ImageLoader(ImageViewer activity, File cacheFolder) {
		super();
		weakRef = new WeakReference<>(activity);
		connection = Twitter.get(activity);
		this.cacheFolder = cacheFolder;
	}


	@Override
	protected Boolean doInBackground(Uri[] links) {
		try {
			// download imaged to a local cache folder
			for (Uri link : links) {
				// get input stream
				MediaUpdate mediaUpdate = connection.downloadImage(link.toString());
				InputStream input = mediaUpdate.getStream();
				String mimeType = mediaUpdate.getMimeType();

				// create file
				String ext = '.' + mimeType.substring(mimeType.indexOf('/') + 1);
				File imageFile = new File(cacheFolder, StringTools.getRandomString() + ext);
				imageFile.createNewFile();

				// copy image to cache folder
				FileOutputStream output = new FileOutputStream(imageFile);
				int length;
				byte[] buffer = new byte[4096];
				while ((length = input.read(buffer)) > 0)
					output.write(buffer, 0, length);
				input.close();
				output.close();

				// create a new uri
				publishProgress(Uri.fromFile(imageFile));
			}
			return true;
		} catch (ConnectionException exception) {
			this.exception = exception;
		} catch (IOException exception) {
			exception.printStackTrace();
		}
		return false;
	}


	@Override
	protected void onProgressUpdate(Uri[] uris) {
		ImageViewer activity = weakRef.get();
		if (activity != null) {
			activity.setImage(uris[0]);
		}
	}


	@Override
	protected void onPostExecute(Boolean success) {
		ImageViewer activity = weakRef.get();
		if (activity != null) {
			if (success) {
				activity.onSuccess();
			} else {
				activity.onError(exception);
			}
		}
	}
}