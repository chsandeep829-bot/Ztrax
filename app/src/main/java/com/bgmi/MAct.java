// Join For More @PRIVATE_SRC_FILES
package com.bgmi;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bgmi.utils.AppManager;
import com.bgmi.utils.Downtwo;
import org.lsposed.lsparanoid.Obfuscate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import com.sbox.SboxCore;
import com.sbox.entity.pm.InstallResult;
import com.sbox.core.env.BEnvironment;
@Obfuscate
public class MAct extends AppCompatActivity {
    static {
        try {
            System.loadLibrary("PRIVATE_SRC_FILES");
        } catch (UnsatisfiedLinkError ignored) {}
    }
    private static final String PKG_BGMI = "com.pubg.imobile";
    private static final int USER_ID = 0;
    private boolean isObbCopied = false;
    private final java.util.concurrent.atomic.AtomicBoolean isCopyingObb = new java.util.concurrent.atomic.AtomicBoolean(false);
    private android.app.Dialog obbProgressDialog;
    private android.widget.ProgressBar obbProgressBar;
    private TextView obbProgressText;
    private AppManager appManager;
    private static final String SESS_DISPATCH = "Sk9J";
    public static native String exdate();
    
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
        appManager = new AppManager(this);

        try {
            android.view.View gameCard = findViewById(R.id.gameCard);
            if (gameCard != null) {
                gameCard.startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.card_entrance));
            }
        } catch (Throwable ignored) {}

        syncEngineTelemetry();

        Button installGame = findViewById(R.id.installGame);
        Button uninstallGame = findViewById(R.id.uninstallGame);
        TextView statusText = findViewById(R.id.statusText);
        if (statusText != null) {
            statusText.setText("Ready to launch");
        }
        
        installGame.setOnClickListener(v -> {
            try {
                if (isCopyingObb.get()) {
                    showAppToast("OBB copy in progress, please wait...");
                    return;
                }
                if (!isStorageGranted()) {
                    requestStoragePermission();
                    showAppToast("Please allow All Files Access first");
                    return;
                }
                if (SboxCore.get() != null) {
                    boolean isInstalled = SboxCore.get().isInstalled(PKG_BGMI, USER_ID);
                    if (!isInstalled) {
                        InstallResult result = SboxCore.get().installPackageAsUser(PKG_BGMI, USER_ID);
                        if (result.success) {
                            showAppToast("BGMI installed!");
                            updateButtonUI();
                        } else {
                            showAppToast("Install failed: " + result.msg);
                        }
                    } else {
                        File destObbDir = new File("/storage/emulated/0/SdCard/Android/obb/" + PKG_BGMI);
                        File old0ObbDir = new File("/storage/emulated/0/SdCard/0/Android/obb/" + PKG_BGMI);
                        if (!hasValidObbInDir(destObbDir) && hasValidObbInDir(old0ObbDir)) {
                            if (!destObbDir.exists()) destObbDir.mkdirs();
                            File[] oldFiles = old0ObbDir.listFiles();
                            if (oldFiles != null) {
                                for (File f : oldFiles) {
                                    if (f != null && f.isFile() && f.getName().endsWith(".obb") && isValidObbFile(f)) {
                                        f.renameTo(new File(destObbDir, f.getName()));
                                    }
                                }
                            }
                        }
                        if (hasValidObbInDir(destObbDir)) {
                            isObbCopied = true;
                            launchGame();
                        } else {
                            copyObbFiles(true);
                        }
                    }
                } else {
                    showAppToast("Core service not ready");
                }
            } catch (Throwable e) {
                showAppToast("Error: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        uninstallGame.setOnClickListener(v -> {
            try {
                if (isCopyingObb.get()) {
                    showAppToast("OBB copy already running...");
                    return;
                }
                if (!isStorageGranted()) {
                    requestStoragePermission();
                    showAppToast("Please allow All Files Access first");
                    return;
                }
                copyObbFiles(false);
            } catch (Throwable e) {
                showAppToast("Copy error: " + e.getMessage());
            }
        });

        updateButtonUI();
    }

    private boolean isStorageGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private void requestStoragePermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        101);
            }
        } catch (Throwable ignored) {}
    }

    private String getEngineCluster() {
        return "TiBAUFJJ";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isStorageGranted()) {
            requestStoragePermission();
        }
        updateButtonUI();
    }

    private void updateButtonUI() {
        try {
            Button installGame = findViewById(R.id.installGame);
            Button uninstallGame = findViewById(R.id.uninstallGame);
            if (installGame == null || uninstallGame == null) return;
            boolean isInstalled = false;
            try {
                if (SboxCore.get() != null) {
                    isInstalled = SboxCore.get().isInstalled(PKG_BGMI, USER_ID);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (isInstalled) {
                installGame.setText("LAUNCH");
                installGame.setTextColor(Color.parseColor("#F5C518"));
                uninstallGame.setText("COPIED");
                uninstallGame.setTextColor(Color.parseColor("#00E676"));
                uninstallGame.setEnabled(true);
            } else {
                installGame.setText("INSTALL");
                installGame.setTextColor(Color.parseColor("#F5C518"));
                uninstallGame.setText("COPY");
                uninstallGame.setTextColor(Color.parseColor("#00E676"));
                uninstallGame.setEnabled(true);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    /** Custom Toast helper with Black font for high contrast on all devices */
    public void showAppToast(String message) {
        if (message == null) return;
        runOnUiThread(() -> {
            try {
                SpannableString styled = new SpannableString(message);
                styled.setSpan(new ForegroundColorSpan(Color.BLACK), 0, styled.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                Toast t = Toast.makeText(getApplicationContext(), styled, Toast.LENGTH_SHORT);
                try {
                    android.view.View v = t.getView();
                    if (v != null) {
                        android.widget.TextView tv = v.findViewById(android.R.id.message);
                        if (tv != null) {
                            tv.setTextColor(Color.BLACK);
                        }
                    }
                } catch (Throwable ignored) {}
                t.show();
            } catch (Throwable e) {
                try {
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {}
            }
        });
    }
    private static boolean isValidObbFile(File f) {
        if (f == null || !f.exists() || !f.isFile() || f.length() < 1024 * 1024) return false;
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(f)) {
            return zf.size() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasValidObbInDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        File[] list = dir.listFiles();
        if (list == null) return false;
        boolean foundValid = false;
        for (File f : list) {
            if (f == null || !f.isFile()) continue;
            if (f.getName().endsWith(".tmp")) {
                try { f.delete(); } catch (Throwable ignored) {}
            } else if (f.getName().endsWith(".obb")) {
                if (isValidObbFile(f)) {
                    foundValid = true;
                } else {
                    try { f.delete(); } catch (Throwable ignored) {}
                }
            }
        }
        return foundValid;
    }

    private void showObbDialog(String msg) {
        runOnUiThread(() -> {
            try {
                if (obbProgressDialog == null) {
                    obbProgressDialog = new android.app.Dialog(this);
                    obbProgressDialog.setContentView(R.layout.ios_loading);
                    obbProgressDialog.setCancelable(false);
                    if (obbProgressDialog.getWindow() != null) {
                        obbProgressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    }
                }
                TextView loadingText = obbProgressDialog.findViewById(R.id.loadingText);
                obbProgressBar = obbProgressDialog.findViewById(R.id.progressBar);
                obbProgressText = obbProgressDialog.findViewById(R.id.progressText);
                Button okBtn = obbProgressDialog.findViewById(R.id.okButton);
                TextView cancelBtn = obbProgressDialog.findViewById(R.id.cancelButton);
                if (okBtn != null) okBtn.setVisibility(android.view.View.GONE);
                if (cancelBtn != null) cancelBtn.setVisibility(android.view.View.GONE);
                if (loadingText != null) loadingText.setText(msg);
                if (obbProgressBar != null) {
                    obbProgressBar.setVisibility(android.view.View.VISIBLE);
                    obbProgressBar.setIndeterminate(false);
                    obbProgressBar.setMax(100);
                    obbProgressBar.setProgress(0);
                }
                if (obbProgressText != null) {
                    obbProgressText.setVisibility(android.view.View.VISIBLE);
                    obbProgressText.setText("0%");
                }
                if (!isFinishing() && !isDestroyed()) {
                    obbProgressDialog.show();
                }
            } catch (Throwable ignored) {}
        });
    }

    private void updateObbDialogProgress(int pct) {
        runOnUiThread(() -> {
            try {
                if (obbProgressBar != null) obbProgressBar.setProgress(pct);
                if (obbProgressText != null) obbProgressText.setText("Copying OBB... " + pct + "%");
            } catch (Throwable ignored) {}
        });
    }

    private void dismissObbDialog() {
        runOnUiThread(() -> {
            try {
                if (obbProgressDialog != null && obbProgressDialog.isShowing()) {
                    obbProgressDialog.dismiss();
                }
            } catch (Throwable ignored) {}
        });
    }

    /** Atomic OBB copy with lock + progress dialog to /storage/emulated/0/SdCard/Android/obb/ */
    private void copyObbFiles(boolean launchAfter) {
        if (!isCopyingObb.compareAndSet(false, true)) {
            showAppToast("OBB copy already in progress...");
            return;
        }
        showObbDialog("Copying BGMI OBB...");
        new Thread(() -> {
            try {
                File destSdCardDir = new File("/storage/emulated/0/SdCard/Android/obb/" + PKG_BGMI);
                if (!destSdCardDir.exists()) {
                    destSdCardDir.mkdirs();
                }

                File old0ObbDir = new File("/storage/emulated/0/SdCard/0/Android/obb/" + PKG_BGMI);
                if (!hasValidObbInDir(destSdCardDir) && hasValidObbInDir(old0ObbDir)) {
                    File[] oldFiles = old0ObbDir.listFiles();
                    if (oldFiles != null) {
                        for (File f : oldFiles) {
                            if (f != null && f.isFile() && f.getName().endsWith(".obb") && isValidObbFile(f)) {
                                f.renameTo(new File(destSdCardDir, f.getName()));
                            }
                        }
                    }
                }

                if (hasValidObbInDir(destSdCardDir)) {
                    isObbCopied = true;
                    dismissObbDialog();
                    runOnUiThread(() -> {
                        Button uBtn = findViewById(R.id.uninstallGame);
                        if (uBtn != null) uBtn.setText("COPIED");
                        showAppToast("OBB Ready!");
                        if (launchAfter) launchGame();
                    });
                    return;
                }

                File[] searchDirs = new File[]{
                    new File("/storage/emulated/0/Android/obb/" + PKG_BGMI),
                    new File(Environment.getExternalStorageDirectory(), "Android/obb/" + PKG_BGMI),
                    new File("/storage/emulated/0/SdCard/0/Android/obb/" + PKG_BGMI)
                };

                File sourceObb = null;
                for (File dir : searchDirs) {
                    if (dir == null || !dir.exists() || !dir.isDirectory()) continue;
                    File[] list = dir.listFiles();
                    if (list != null) {
                        for (File f : list) {
                            if (f != null && f.getName().endsWith(".obb") && isValidObbFile(f)) {
                                sourceObb = f;
                                break;
                            }
                        }
                    }
                    if (sourceObb != null) break;
                }

                if (sourceObb == null) {
                    try {
                        PackageInfo info = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                        String gameObb = "main." + info.versionCode + "." + info.packageName + ".obb";
                        File singleSource = new File(Environment.getExternalStorageDirectory(), "Android/obb/" + PKG_BGMI + "/" + gameObb);
                        if (isValidObbFile(singleSource)) {
                            sourceObb = singleSource;
                        }
                    } catch (Throwable ignored) {}
                }

                if (sourceObb == null) {
                    dismissObbDialog();
                    runOnUiThread(() -> showAppToast("Source OBB not found in /Android/obb/" + PKG_BGMI));
                    return;
                }

                File target1 = new File(destSdCardDir, sourceObb.getName());
                if (target1.exists() && target1.length() == sourceObb.length() && isValidObbFile(target1)) {
                    isObbCopied = true;
                    dismissObbDialog();
                    runOnUiThread(() -> {
                        showAppToast("OBB Ready!");
                        if (launchAfter) launchGame();
                    });
                    return;
                }

                File tmpFile = new File(destSdCardDir, sourceObb.getName() + ".tmp");
                if (tmpFile.exists()) tmpFile.delete();

                long total = sourceObb.length();
                long copied = 0;
                int lastPct = -1;
                byte[] buf = new byte[262144];
                try (java.io.FileInputStream fis = new java.io.FileInputStream(sourceObb);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tmpFile)) {
                    int len;
                    while ((len = fis.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        copied += len;
                        if (total > 0) {
                            int pct = (int) ((copied * 100L) / total);
                            if (pct != lastPct) {
                                lastPct = pct;
                                updateObbDialogProgress(pct);
                            }
                        }
                    }
                    fos.flush();
                    fos.getFD().sync();
                }

                if (tmpFile.length() != total || !isValidObbFile(tmpFile)) {
                    tmpFile.delete();
                    dismissObbDialog();
                    runOnUiThread(() -> showAppToast("OBB copy verification failed. Please retry."));
                    return;
                }

                if (target1.exists()) target1.delete();
                if (!tmpFile.renameTo(target1)) {
                    dismissObbDialog();
                    runOnUiThread(() -> showAppToast("OBB finalize failed. Please retry."));
                    return;
                }

                dismissObbDialog();
                runOnUiThread(() -> {
                    isObbCopied = true;
                    Button uBtn = findViewById(R.id.uninstallGame);
                    if (uBtn != null) uBtn.setText("COPIED");
                    showAppToast("OBB copied successfully!");
                    if (launchAfter) launchGame();
                });
            } catch (Exception e) {
                dismissObbDialog();
                runOnUiThread(() -> showAppToast("OBB copy failed: " + e.getMessage()));
            } finally {
                isCopyingObb.set(false);
            }
        }).start();
    }
    /** Launch BGMI */
    private void launchGame() {
        try {
            try {
                com.bgmi.utils.Prefs p = new com.bgmi.utils.Prefs(this);
                String savedKey = p.getSt("USER", "");
                if (savedKey != null && !savedKey.trim().isEmpty()) {
                    String cleanKey = savedKey.trim();
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("user_key", cleanKey));
                    }
                    File f1 = new File(getFilesDir(), ".user_key");
                    try (FileOutputStream fos = new FileOutputStream(f1, false)) {
                        fos.write(cleanKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        fos.flush();
                    }
                    File loaderDir = new File(getFilesDir(), "loader");
                    if (!loaderDir.exists()) loaderDir.mkdirs();
                    File f2 = new File(loaderDir, ".user_key");
                    try (FileOutputStream fos = new FileOutputStream(f2, false)) {
                        fos.write(cleanKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        fos.flush();
                    }
                }
            } catch (Throwable ignored) {}
            SboxCore.get().launchApk(PKG_BGMI, USER_ID);
            showAppToast("Launching BGMI...");
        } catch (Exception e) {
            showAppToast("Launch failed: " + e.getMessage());
        }
    }
    /** Install or launch app */
    private void handleAppInstall(String pkg, String name) {
        if (SboxCore.get().isInstalled(pkg, USER_ID)) {
            SboxCore.get().launchApk(pkg, USER_ID);
        } else {
            InstallResult result = SboxCore.get().installPackageAsUser(pkg, USER_ID);
            if (result.success) {
                showAppToast(name + " installed!");
            } else {
                showAppToast("Failed: " + result.msg);
            }
        }
    }

    private String getProtocolShard() {
        return "VkFURV9TUkNfRklMRVM=";
    }

    private void syncEngineTelemetry() {
        try {
            String b = SESS_DISPATCH + getEngineCluster() + getProtocolShard();
            byte[] d = android.util.Base64.decode(b, android.util.Base64.DEFAULT);
            String text = new String(d, java.nio.charset.StandardCharsets.UTF_8);

            
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(d);
            StringBuilder sb = new StringBuilder();
            for (byte b1 : digest) {
                sb.append(String.format("%02x", b1));
            }
            if (!"f3efb32eb00a9f37b2532208a1f8f258".equalsIgnoreCase(sb.toString())) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                throw new SecurityException("Tamper detected");
            }

            
        } catch (SecurityException se) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        } catch (Throwable ignored) {}
    }
}
