// Copyright 2016 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codelabs.cosu;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class LockedActivity extends Activity {

    //private ImageView imageView;
    private Button stopLockButton;
    private String mCurrentPhotoPath;
    private PDFView pdf;
    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;

    private static final String PREFS_FILE_NAME = "MyPrefsFile";
    private static final String PHOTO_PATH = "Photo Path";
    public static final String LOCK_ACTIVITY_KEY = "lock_activity";
    public static final int FROM_LOCK_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_locked);

        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Setup stop lock task button
        stopLockButton = (Button) findViewById(R.id.stop_lock_button);
        pdf = (PDFView) findViewById(R.id.pdfView);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                final File file = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath(), "spiritspdf/spirits.pdf");
                pdf.fromFile(file)
                        // .pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                        .enableSwipe(true) // allows to block changing pages using swipe
                        //.sw
                        .swipeHorizontal(true)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        // allows to draw something on the current page, usually visible in the middle of the screen
                        //.onDraw(onDrawListener)
                        // allows to draw something on all pages, separately for every page. Called only for visible pages
                        // .onDrawAll(onDrawListener)
                        // .onLoad(onLoadCompleteListener) // called after document is loaded and starts to be rendered
                        // .onPageChange(onPageChangeListener)
                        // .onPageScroll(onPageScrollListener)
                        // .onError(onErrorListener)
                        /// .onRender(onRenderListener) // called after document is rendered for the first time
                        // called on single tap, return true if handled, false to toggle scroll handle visibility
                        // .onTap(onTapListener)
                        .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                        .password(null)
                        .scrollHandle(null)
                        //.
                        .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                        // spacing between pages in dp. To define spacing color, set view background
                        .spacing(0)
                        .load();
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


        stopLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityManager am = (ActivityManager) getSystemService(
                        Context.ACTIVITY_SERVICE);

                if (am.getLockTaskModeState() ==
                            ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                }

                setDefaultCosuPolicies(false);

                Intent intent = new Intent(
                        getApplicationContext(), MainActivity.class);

                intent.putExtra(LOCK_ACTIVITY_KEY,FROM_LOCK_ACTIVITY);
                startActivity(intent);
                finish();
            }
        });

        // set image to View
       // setImageToView();

        // Set Default COSU policy
        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getPackageManager();
        if(mDevicePolicyManager.isDeviceOwnerApp(getPackageName())){
            setDefaultCosuPolicies(true);
        }
        else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_device_owner,Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // start lock task mode if its not already active
        if(mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())){
            ActivityManager am = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE);
            if(am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
    }

    @Override
    protected void onStop(){
        super.onStop();

        // get editor object and make preference changes to save photo filepath
        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PHOTO_PATH, mCurrentPhotoPath);
        editor.commit();
    }



    private void setDefaultCosuPolicies(boolean active){
        // set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

        // disable keyguard and status bar
        //mDevicePolicyManager.set(mAdminComponentName, active);
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        // enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);

        // set system update policy
        if (active){
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    null);
        }

        // set this Activity as a lock task package

        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName,
                active ? new String[]{getPackageName()} : new String[]{});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        if (active) {
            // set Cosu activity as home intent receiver so that it is started
            // on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(
                    mAdminComponentName, intentFilter, new ComponentName(
                            getPackageName(), LockedActivity.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getPackageName());
        }
    }

    private void setUserRestriction(String restriction, boolean disallow){
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName,
                    restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName,
                    restriction);
        }
    }

    private void enableStayOnWhilePluggedIn(boolean enabled){
        if (enabled) {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                        | BatteryManager.BATTERY_PLUGGED_USB
                        | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    "0"
                    );
        }
    }
}
