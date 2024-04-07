package com.mapswithme.util;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class PermissionsUtils
{
  private PermissionsUtils() {}

  public static boolean isFineLocationGranted(@NonNull Context context)
  {
    return ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  public static boolean isLocationGranted(@NonNull Context context)
  {
    return isFineLocationGranted(context) ||
        ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  public static void requestLocationPermission(@NonNull Activity activity, int code)
  {
    ActivityCompat.requestPermissions(activity, new String[]{
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION,
        READ_EXTERNAL_STORAGE,
        WRITE_EXTERNAL_STORAGE
    }, code);
  }
}
