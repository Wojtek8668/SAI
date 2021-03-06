package com.aefyr.sai.installerx.resolver.appmeta.xapk;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.appmeta.AppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.utils.IOUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

//TODO handle manifest.json versions
public class XapkAppMetaExtractor implements AppMetaExtractor {
    private static final String TAG = "XapkMetaExtractor";

    private static final String META_FILE = "manifest.json";
    private static final String ICON_FILE = "icon.png";

    private Context mContext;
    private AppMeta mAppMeta;

    private boolean mSeenMetaFile = false;
    private boolean mSeenIconFile = false;

    public XapkAppMetaExtractor(Context context) {
        mContext = context.getApplicationContext();
        mAppMeta = new AppMeta();
    }

    @Override
    public boolean wantEntry(ApkSourceFile.Entry entry) {
        return entry.getLocalPath().equals(META_FILE) || entry.getLocalPath().equals(ICON_FILE);
    }

    @Override
    public void consumeEntry(ApkSourceFile.Entry entry, InputStream entryInputStream) {
        if (entry.getLocalPath().equals(META_FILE)) {
            try {
                JSONObject metaJson = new JSONObject(IOUtils.readStream(entryInputStream, StandardCharsets.UTF_8));
                mAppMeta.packageName = metaJson.optString("package_name");
                mAppMeta.appName = metaJson.optString("name");
                mAppMeta.versionName = metaJson.optString("version_name");
                mAppMeta.versionCode = metaJson.optLong("version_code");
                mSeenMetaFile = true;
            } catch (Exception e) {
                Log.w(TAG, "Unable to extract meta", e);
            }
        }

        if (entry.getLocalPath().equals(ICON_FILE)) {
            File iconFile = new File(getCacheDir(), UUID.randomUUID().toString() + ".png");
            try (InputStream in = entryInputStream; OutputStream out = new FileOutputStream(iconFile)) {
                IOUtils.copyStream(in, out);
                mAppMeta.iconUri = Uri.fromFile(iconFile);
                mSeenIconFile = true;
            } catch (IOException e) {
                Log.w(TAG, "Unable to extract icon", e);
            }
        }
    }

    private File getCacheDir() {
        File cacheDir = new File(mContext.getCacheDir(), "XapkZipAppMetaExtractor");
        if (!cacheDir.exists())
            cacheDir.mkdir();

        return cacheDir;
    }

    @Nullable
    @Override
    public AppMeta buildMeta() {
        if (mSeenMetaFile && mSeenIconFile)
            return mAppMeta;

        return null;
    }

}
