package org.nuclearfog.twidda.backend.async;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.twidda.BuildConfig;
import org.nuclearfog.twidda.backend.api.Connection;
import org.nuclearfog.twidda.backend.api.ConnectionException;
import org.nuclearfog.twidda.backend.api.ConnectionManager;
import org.nuclearfog.twidda.backend.helper.MediaStatus;
import org.nuclearfog.twidda.backend.utils.StringUtils;
import org.nuclearfog.twidda.ui.activities.ImageViewer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * This class downloads images to a local cache folder
 * and creates Uri of the images.
 *
 * @author nuclearfog
 * @see ImageViewer
 */
public class ImageLoader extends AsyncExecutor<ImageLoader.ImageParameter, ImageLoader.ImageResult> {

	private Connection connection;

	/**
	 * @param context Activity context
	 */
	public ImageLoader(Context context) {
		connection = ConnectionManager.getDefaultConnection(context);
	}


	@Override
	protected ImageResult doInBackground(@NonNull ImageParameter request) {
		try {
			// get input stream
			MediaStatus mediaUpdate = connection.downloadImage(request.uri.toString());
			InputStream input = mediaUpdate.getStream();
			String mimeType = mediaUpdate.getMimeType();
			if (input == null) {
				return new ImageResult(null, null);
			}

			// create file
			String ext = '.' + mimeType.substring(mimeType.indexOf('/') + 1);
			File imageFile = new File(request.cache, StringUtils.getRandomString() + ext);
			imageFile.createNewFile();

			// copy image to cache folder
			FileOutputStream output = new FileOutputStream(imageFile);
			int length;
			byte[] buffer = new byte[4096];
			while ((length = input.read(buffer)) > 0)
				output.write(buffer, 0, length);
			input.close();
			output.close();

			// create Uri from cached image
			return new ImageResult(Uri.fromFile(imageFile), null);
		} catch (ConnectionException exception) {
			return new ImageResult(null, exception);
		} catch (Exception exception) {
			if (BuildConfig.DEBUG) {
				exception.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Async request class to send information required to execute the task
	 */
	public static class ImageParameter {

		final File cache;
		final Uri uri;

		public ImageParameter(Uri uri, File cache) {
			this.cache = cache;
			this.uri = uri;
		}
	}

	/**
	 * Async result class
	 */
	public static class ImageResult {

		@Nullable
		public final Uri uri;
		@Nullable
		public final ConnectionException exception;

		ImageResult(@Nullable Uri uri, @Nullable ConnectionException exception) {
			this.exception = exception;
			this.uri = uri;
		}
	}
}