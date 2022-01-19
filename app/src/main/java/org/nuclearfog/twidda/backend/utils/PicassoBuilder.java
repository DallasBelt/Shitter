package org.nuclearfog.twidda.backend.utils;

import android.content.Context;
import android.os.Build;

import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import org.nuclearfog.twidda.backend.proxy.ProxyAuthenticator;
import org.nuclearfog.twidda.backend.proxy.UserProxy;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.security.KeyStore;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Create Picasso instance with TLS 1.2 support for pre Lollipo devices
 *
 * @author nuclearfog
 */
public class PicassoBuilder implements GlobalSettings.SettingsListener {

    private static PicassoBuilder instance;
    private static boolean notifySettingsChange = false;

    private OkHttp3Downloader downloader;


    private PicassoBuilder(Context context) {
        GlobalSettings settings = GlobalSettings.getInstance(context);
        settings.registerObserver(this);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // setup proxy
        if (settings.isProxyEnabled()) {
            builder.proxy(UserProxy.get(settings));
            if (settings.isProxyAuthSet()) {
                builder.proxyAuthenticator(new ProxyAuthenticator(settings));
            }
        }
        // setup TLS 1.2 support if needed
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                factory.init((KeyStore) null);
                X509TrustManager manager = (X509TrustManager) factory.getTrustManagers()[0];
                builder.sslSocketFactory(new TLSSocketFactory(), manager);
                downloader = new OkHttp3Downloader(builder.build());
            } catch (Exception e) {
                // ignore, try without TLS 1.2 support
            }
        }
        downloader = new OkHttp3Downloader(builder.build());
        notifySettingsChange = false;
    }

    /**
     * @return instance of Picasso with custom downloader
     */
    public static Picasso get(Context context) {
        if (instance == null || notifySettingsChange) {
            instance = new PicassoBuilder(context);
        }
        return new Picasso.Builder(context).downloader(instance.downloader).build();
    }


    @Override
    public void onSettingsChange() {
        notifySettingsChange = true;
    }
}