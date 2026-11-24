// Join For More @PRIVATE_SRC_FILES
package com.bgmi;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.sbox.SboxCore;
import com.sbox.app.configuration.AppLifecycleCallback;
import com.sbox.app.configuration.ClientConfiguration;
import com.sbox.core.system.api.BoxActivate;
import java.io.File;
import com.bgmi.utils.Prefs;
import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class BoxApplication extends Application {

    static {
        try {
            System.loadLibrary("PRIVATE_SRC_FILES");
        } catch (Throwable t) {
            Log.e("BoxApplication", "loadLibrary PRIVATE_SRC_FILES failed: " + t.getMessage());
        }
    }
    public static native String getSdkKey();
    private static final String TAG = "BoxApplication";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "BoxApplication attachBaseContext started");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("");
            } catch (Throwable t) {
                Log.w(TAG, "HiddenApiBypass failed: " + t.getMessage());
            }
        }
        try {
            Prefs prefs = new Prefs(base);
            Log.d(TAG, "Initializing SboxCore...");
            SboxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

               @Override
                public boolean isHideRoot() {
                    return true;
                }

               @Override
                public boolean isHideXposed() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

               @Override
                public boolean requestInstallPackage(File file){
                    if (file != null && file.exists()) {
                        try {
                            base.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);
                        } catch (Throwable ignored) {}
                    }
                    return false;
                }
            });
            Log.d(TAG, "SboxCore initialization completed");
        } catch (Throwable e) {
            Log.e(TAG, "Error in attachBaseContext", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SboxCore.get().doCreate();
        } catch (Throwable e) {
            Log.e(TAG, "SboxCore doCreate error", e);
        }
        try {
            BoxActivate.activateBox(getSdkKey());
        } catch (Throwable e) {
            Log.e(TAG, "BoxActivate error", e);
        }
        try {
            SboxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {
                @Override
                public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
                }

                @Override
                public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                }

                @Override
                public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "addAppLifecycleCallback error", e);
        }
    }
}
