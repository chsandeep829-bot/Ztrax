// Join For More @PRIVATE_SRC_FILES
package com.bgmi.utils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downtwo extends AsyncTask<String, String, String> {
    private static final String TAG = "Downtwo";

    public static native String Link();

    public interface Callback {
        void onComplete(boolean success);
    }

    public interface ProgressListener {
        void onProgress(int percent);
    }

    private final Context context;
    private final Callback callback;
    private ProgressListener progressListener;
    private static final String HOSTOP_ZIP = "sigma.zip";
    private static final String LOADER_DIR_NAME = "loader";

    public Downtwo(Context context) {
        this(context, null);
    }

    public Downtwo(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            if (params.length == 0 || params[0] == null || params[0].trim().isEmpty()) {
                return "Invalid URL provided";
            }

            String downloadUrl = params[0].trim();
            publishProgress("Downloading files...");
            return downloadAndExtract(downloadUrl);
        } catch (Exception e) {
            Log.e(TAG, "doInBackground error: ", e);
            return "Background error: " + e.getMessage();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        boolean success = (result == null);

        try {
            if (callback != null) {
                callback.onComplete(success);
            }
        } catch (Exception e) {
            Log.e(TAG, "Callback threw exception: ", e);
        }

        if (!success) {
            showFinalMessage("Error: " + result);
        }
    }

    private HttpURLConnection openConnection(String urlString) throws IOException {
        String currentUrl = urlString;
        for (int i = 0; i < 6; i++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile)");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl != null && !redirectUrl.isEmpty()) {
                    if (!redirectUrl.startsWith("http://") && !redirectUrl.startsWith("https://")) {
                        redirectUrl = new URL(url, redirectUrl).toString();
                    }
                    conn.disconnect();
                    currentUrl = redirectUrl;
                    continue;
                }
            }
            return conn;
        }
        URL url = new URL(currentUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile)");
        return conn;
    }

    private void deleteRecursive(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists()) return;
        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDir.delete();
    }

    private String downloadAndExtract(String urlString) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(urlString);

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return "Download failed: Server returned HTTP " + code;
            }

            long totalSize = connection.getContentLength();
            long downloaded = 0;

            InputStream input = connection.getInputStream();
            File pathBase = new File(context.getFilesDir().getPath());
            if (!pathBase.exists()) {
                pathBase.mkdirs();
            }

            File pathOutput = new File(pathBase, HOSTOP_ZIP);
            if (pathOutput.exists()) {
                pathOutput.delete();
            }

            try (FileOutputStream output = new FileOutputStream(pathOutput)) {
                byte[] data = new byte[8192];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                    downloaded += count;
                    if (totalSize > 0 && progressListener != null) {
                        int percent = (int) ((downloaded * 100L) / totalSize);
                        progressListener.onProgress(percent);
                    }
                }
                output.flush();
            }
            input.close();

            File loaderDirectory = new File(pathBase, LOADER_DIR_NAME);
            if (loaderDirectory.exists()) {
                deleteRecursive(loaderDirectory);
            }
            loaderDirectory.mkdirs();

            ZipFile zipFile = new ZipFile(pathOutput);
            if (zipFile.isEncrypted()) {
                zipFile.setPassword("PRIVATE_SRC_FILES".toCharArray());
            }
            zipFile.extractAll(loaderDirectory.getAbsolutePath());

            setPermissions(loaderDirectory);

            if (pathOutput.exists()) {
                pathOutput.delete();
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading or extracting file: ", e);
            return "Download failed: " + e.getMessage();
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {}
            }
        }
    }

    private void setPermissions(File directory) {
        if (directory == null) return;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    setPermissions(file);
                }
            }
        } else {
            directory.setReadable(true, false);
            directory.setWritable(true, false);
            directory.setExecutable(true, false);
        }
    }

    private void showFinalMessage(String message) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        try {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Download Status")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> {
                        try {
                            dialog.dismiss();
                        } catch (Exception ignored) {}
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show final message dialog: ", e);
        }
    }
}
