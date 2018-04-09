/*
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package fr.matlink.deviceproperties;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class DeviceInfoBuilder {

    static private Map<String, String> staticProperties = new HashMap<>();
    private Context context;
    final private String filename;

    static {
        staticProperties.put("Client", "android-google");
        staticProperties.put("Roaming", "mobile-notroaming");
        staticProperties.put("TimeZone", TimeZone.getDefault().getID());
        staticProperties.put("GL.Extensions", TextUtils.join(",", EglExtensionRetriever.getEglExtensions()));
    }

    DeviceInfoBuilder(Context context) {
        this.context = context;
        this.filename = "device-" + Build.DEVICE + ".properties";
    }

    public String build() {
        Map<String, String> properties = getDeviceInfo();
        StringBuilder stringBuilder = new StringBuilder();
        for (String key: properties.keySet()) {
            stringBuilder
                    .append(key)
                    .append(" = ")
                    .append(String.valueOf(properties.get(key)))
                    .append("\n")
            ;
        }
        return stringBuilder.toString();
    }

    private Map<String, String> getDeviceInfo() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("UserReadableName", Build.MANUFACTURER + " " + Build.PRODUCT + " (api" + Integer.toString(Build.VERSION.SDK_INT) + ")");
        values.putAll(getBuildValues());
        values.putAll(getConfigurationValues());
        values.putAll(getDisplayMetricsValues());
        values.putAll(getPackageManagerValues());
        values.putAll(getOperatorValues());
        values.putAll(staticProperties);
        return values;
    }

    private Map<String, String> getBuildValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Build.HARDWARE", Build.HARDWARE);
        values.put("Build.RADIO", Build.RADIO);
        values.put("Build.BOOTLOADER", Build.BOOTLOADER);
        values.put("Build.FINGERPRINT", Build.FINGERPRINT);
        values.put("Build.BRAND", Build.BRAND);
        values.put("Build.DEVICE", Build.DEVICE);
        values.put("Build.VERSION.SDK_INT", Integer.toString(Build.VERSION.SDK_INT));
        values.put("Build.MODEL", Build.MODEL);
        values.put("Build.MANUFACTURER", Build.MANUFACTURER);
        values.put("Build.PRODUCT", Build.PRODUCT);
        values.put("Build.ID", Build.ID);
        values.put("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
        return values;
    }

    private Map<String, String> getConfigurationValues() {
        Map<String, String> values = new LinkedHashMap<>();
        Configuration config = context.getResources().getConfiguration();
        values.put("TouchScreen", Integer.toString(config.touchscreen));
        values.put("Keyboard", Integer.toString(config.keyboard));
        values.put("Navigation", Integer.toString(config.navigation));
        values.put("ScreenLayout", Integer.toString(config.screenLayout & 15));
        values.put("HasHardKeyboard", Boolean.toString(config.keyboard == Configuration.KEYBOARD_QWERTY));
        values.put("HasFiveWayNavigation", Boolean.toString(config.navigation == Configuration.NAVIGATIONHIDDEN_YES));
        values.put("GL.Version", Integer.toString(((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo().reqGlEsVersion));
        return values;
    }

    private Map<String, String> getDisplayMetricsValues() {
        Map<String, String> values = new LinkedHashMap<>();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        values.put("Screen.Density", Integer.toString((int) (metrics.density * 160f)));
        values.put("Screen.Width", Integer.toString(metrics.widthPixels));
        values.put("Screen.Height", Integer.toString(metrics.heightPixels));
        return values;
    }

    private Map<String, String> getPackageManagerValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Platforms", TextUtils.join(",", NativeDeviceInfoProvider.getPlatforms()));
        values.put("SharedLibraries", TextUtils.join(",", NativeDeviceInfoProvider.getSharedLibraries(context)));
        values.put("Features", TextUtils.join(",", NativeDeviceInfoProvider.getFeatures(context)));
        values.put("Locales", TextUtils.join(",", NativeDeviceInfoProvider.getLocales(context)));
        NativeGsfVersionProvider gsfVersionProvider = new NativeGsfVersionProvider(context);
        values.put("GSF.version", Integer.toString(gsfVersionProvider.getGsfVersionCode(false)));
        values.put("Vending.version", Integer.toString(gsfVersionProvider.getVendingVersionCode(false)));
        values.put("Vending.versionString", gsfVersionProvider.getVendingVersionString(false));
        return values;
    }

    private Map<String, String> getOperatorValues() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("CellOperator", tm.getNetworkOperator());
        values.put("SimOperator", tm.getSimOperator());
        return values;
    }

    public boolean hasPermission(){
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED);
    }

    public void write(String properties){
        File file = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath(),
                this.filename);
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(properties);
            writer.flush();
            writer.close();
            Toast.makeText(context, "Device properties has been written to " + filename, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Cannot write to " + filename, Toast.LENGTH_LONG).show();
        }
    }
}
