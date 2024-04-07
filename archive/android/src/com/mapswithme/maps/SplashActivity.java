package com.mapswithme.maps;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mapswithme.maps.base.BaseActivity;
import com.mapswithme.maps.base.BaseActivityDelegate;
import com.mapswithme.maps.location.LocationHelper;
import com.mapswithme.maps.sidechannel.CacheScan;
import com.mapswithme.maps.sidechannel.GroundTruthValue;
import com.mapswithme.maps.sidechannel.MethodStat;
import com.mapswithme.maps.sidechannel.SideChannelContract;
import com.mapswithme.maps.sidechannel.SideChannelJob;
import com.mapswithme.util.Config;
import com.mapswithme.util.Counters;
import com.mapswithme.util.PermissionsUtils;
import com.mapswithme.util.ThemeUtils;
import com.mapswithme.util.UiUtils;
import com.mapswithme.util.concurrency.UiThread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SplashActivity extends AppCompatActivity implements BaseActivity
{
  private static final String EXTRA_ACTIVITY_TO_START = "extra_activity_to_start";
  public static final String EXTRA_INITIAL_INTENT = "extra_initial_intent";
  private static final int REQUEST_PERMISSIONS = 1;
  private static final long DELAY = 100;

  private boolean mCanceled = false;

  // side channel
  private static final String TAG = "SplashActivity";
  public static final String PACKAGE_NAME = "app.organicmaps";
  private static Long timingCount;
  static Lock ground_truth_insert_locker = new ReentrantLock();
  static int waitVal = 1000;
  Map<String, String> configMap = new HashMap<>();
  static final String CONFIG_FILE_PATH = "/data/local/tmp/config.out";
  public static Map<String, Integer> methodIdMap = new HashMap<>();

  public static CacheScan cs = null;

  public static int fd = -2;
  private Messenger mService;

  private Messenger replyMessenger = new Messenger(new MessengerHandler());
  //public static ArrayList<SideChannelValue> sideChannelValues = new ArrayList<>();
  public static ArrayList<GroundTruthValue> groundTruthValues = new ArrayList<>();
  public static final List<MethodStat> methodStats = new ArrayList<>();

  private static Context mContext;

  public static String sideChannelDPPath;
  public static String mainAppDPPath;

  public static boolean GROUND_TRUTH_LOGGING = true;
  public static boolean SIDE_CHANNEL_LOGGING = true;

  static {
    System.loadLibrary("native-lib");
  }

  Bundle savedState;

  @NonNull
  private final Runnable mInitCoreDelayedTask = new Runnable()
  {
    @Override
    public void run()
    {
      init();
    }
  };

  @NonNull
  private final BaseActivityDelegate mBaseDelegate = new BaseActivityDelegate(this);

  public static void start(@NonNull Context context,
                           @Nullable Class<? extends Activity> activityToStart,
                           @Nullable Intent initialIntent)
  {
    Intent intent = new Intent(context, SplashActivity.class);
    if (activityToStart != null)
      intent.putExtra(EXTRA_ACTIVITY_TO_START, activityToStart);
    if (initialIntent != null)
      intent.putExtra(EXTRA_INITIAL_INTENT, initialIntent);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    this.savedState = savedInstanceState;
    mBaseDelegate.onCreate();
    UiThread.cancelDelayedTasks(mInitCoreDelayedTask);
    Counters.initCounters(this);
    UiUtils.setupStatusBar(this);
    setContentView(R.layout.activity_splash);
  }

  private static class MessengerHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 1:
          Log.d("ashmem", "Received information from the server: " + msg.getData().getString("reply"));
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }

  private ServiceConnection conn = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mService = new Messenger(service);
      Message msg = Message.obtain(null, 0);
      Bundle bundle = new Bundle();
      if (fd < 0) {
        Log.d("ashmem ", "not set onServiceConnected " + fd);
      }
      setAshMemVal(fd, 4l);
      try {
        ParcelFileDescriptor desc = ParcelFileDescriptor.fromFd(fd);
        bundle.putParcelable("msg", desc);
        msg.setData(bundle);
        msg.replyTo = replyMessenger;      // 2
        mService.send(msg);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

  };

  private Map<String, String> readConfigFile() {
    Map<String, String> configMap = new HashMap<>();
    try {
      List<String> configs = Files.lines(Paths.get(CONFIG_FILE_PATH)).collect(Collectors.toList());
      configs.stream().filter(c -> !c.contains("//") && c.contains(":")).forEach(c -> configMap.put(c.split(":")[0].trim(), c.split(":")[1].trim()));

    } catch (IOException e) {
      Log.d(TAG + "#", e.toString());
    }
    return configMap;
  }

  private void copyOdex() {
    try {

      String oatHome = "/sdcard/Documents/oatFolder/oat/arm64/";
      /*String line;
      List<String> baseOdexLine = new ArrayList<>();
      BufferedReader reader = new BufferedReader(new FileReader("proc/self/maps"));
      while ((line = reader.readLine()) != null) {
        if (line.contains(PACKAGE_NAME) && line.contains("base.odex")) {
          baseOdexLine.add(line);
        }
      }*/
      Optional<String> baseOdexLine = Files.lines(Paths.get("/proc/self/maps")).collect(Collectors.toList())
              .stream().sequential().filter(s -> s.contains(PACKAGE_NAME) && s.contains("base.odex"))
              .findAny();
      Log.d("odex", Files.lines(Paths.get("/proc/self/maps")).collect(Collectors.joining("\n")));
      Log.d(TAG, String.valueOf(baseOdexLine));
      if (baseOdexLine.isPresent()) {
        String odexpath = "/data/app/" + baseOdexLine.get().split("/data/app/")[1];
        String vdexpath = "/data/app/" + baseOdexLine.get().split("/data/app/")[1].replace("odex", "vdex");
//                String odexRootPath = "/data/app/"+baseOdexLine.get().split("/data/app/")[1].replace("/oat/arm64/base.odex","*");
        Log.d(TAG + "#", odexpath);
        Log.d(TAG + "#", "cp " + odexpath + " " + oatHome);
        Process p = Runtime.getRuntime().exec("cp " + odexpath + " " + oatHome);
        p.waitFor();
        p = Runtime.getRuntime().exec("cp " + vdexpath + " " + oatHome);
        Log.d(TAG + "#", "cp " + vdexpath + " " + oatHome);

        p.waitFor();
        Log.d(TAG + "#", "odex copied");

      } else {
        Log.d(TAG + "#", "base odex absent");
      }

    } catch (IOException | InterruptedException e) {
      Log.d(TAG + "#", e.toString());
    }
  }

  public static void copyMethodMap() {
    String methodMapString = methodIdMap.entrySet().parallelStream().map(Object::toString).collect(Collectors.joining("|"));
//        log only allows a max of 4000 chars
    //if (methodMapString.length() > 4000) {
    int chunkCount = methodMapString.length() / 4000;     // integer division

    for (int i = 0; i <= chunkCount; i++) {
      int max = 4000 * (i + 1);
      if (max >= methodMapString.length()) {
        Log.d("MethodMap"+i, methodMapString.substring(4000 * i));
      } else {
        Log.d("MethodMap"+i, methodMapString.substring(4000 * i, max));
      }
    }
    //}
    Log.d("MethodCount", String.valueOf(methodIdMap.size()));

  }

  protected void setUpandRun(Bundle savedState) {
    sideChannelDPPath = getDatabasePath("SideScan").toString();
    mainAppDPPath = getDatabasePath("MainApp").toString();
    fd = createAshMem();
    if (fd < 0) {
      Log.d("ashmem ", "not set onCreate " + fd);
    }
    copyOdex();

    configMap = readConfigFile();
//        configMap.entrySet().forEach(e -> Log.d("configMap: ", e.getKey() + " " + e.getValue()));


    initializeDB();
    initializeDBAop();
    Intent begin = new Intent(this, SideChannelJob.class);
    bindService(begin, conn, Context.BIND_AUTO_CREATE);
    startForegroundService(begin);

    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

    /*@Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpandRun(savedState);
                } else {
                    finish();
                }
            }
        }
    }*/

  /**
   * Method to initialize database
   */
  void initializeDB() {
    // Creating the database file in the app sandbox
    SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
            MODE_PRIVATE, null);
    Locale locale = new Locale("EN", "SG");
    db.setLocale(locale);
    // Creating the schema of the database
    String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH + " (" +
            SideChannelContract.Columns.SYSTEM_TIME + " INTEGER NOT NULL, " +
            SideChannelContract.Columns.LABEL + " TEXT, " +
            SideChannelContract.Columns.COUNT + " INTEGER);";
    db.execSQL(sSQL);
    sSQL = "DELETE FROM " + SideChannelContract.GROUND_TRUTH;
    db.execSQL(sSQL);
    db.close();
  }

  void initializeDBAop() {
    // Creating the database file in the app sandbox
    SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
            MODE_PRIVATE, null);
    Locale locale = new Locale("EN", "SG");
    db.setLocale(locale);
    // Creating the schema of the database
    String sSQL = "CREATE TABLE IF NOT EXISTS " + SideChannelContract.GROUND_TRUTH_AOP + " (" +
            SideChannelContract.Columns.METHOD_ID + " INTEGER NOT NULL, " +
            SideChannelContract.Columns.START_COUNT + " INTEGER, " +
            SideChannelContract.Columns.END_COUNT + " INTEGER);";
    db.execSQL(sSQL);
    sSQL = "DELETE FROM " + SideChannelContract.GROUND_TRUTH_AOP;
    db.execSQL(sSQL);
    Log.d("dbinfo", SideChannelContract.GROUND_TRUTH_AOP + " count: " + getRecordCount(SideChannelContract.GROUND_TRUTH_AOP));
    db.close();
  }

  public long getRecordCount(String tableName) {
    SQLiteDatabase db = getBaseContext().openOrCreateDatabase("MainApp.db",
            MODE_PRIVATE, null);
    long count = DatabaseUtils.queryNumEntries(db, tableName);
    db.close();
    return count;
  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    mBaseDelegate.onNewIntent(intent);
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    mBaseDelegate.onStart();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    mBaseDelegate.onResume();
    if (mCanceled)
      return;
    if (!Config.isLocationRequested() && !PermissionsUtils.isLocationGranted(this))
    {
      PermissionsUtils.requestLocationPermission(SplashActivity.this, REQUEST_PERMISSIONS);
      return;
    }

    UiThread.runLater(mInitCoreDelayedTask, DELAY);
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    mBaseDelegate.onPause();
    UiThread.cancelDelayedTasks(mInitCoreDelayedTask);
  }

  @Override
  protected void onStop()
  {
    super.onStop();
    mBaseDelegate.onStop();
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    mBaseDelegate.onDestroy();
  }

  private void showFatalErrorDialog(@StringRes int titleId, @StringRes int messageId)
  {
    mCanceled = true;
    AlertDialog dialog = new AlertDialog.Builder(this)
        .setTitle(titleId)
        .setMessage(messageId)
        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            SplashActivity.this.finish();
          }
        })
        .setCancelable(false)
        .create();
    dialog.show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults)
  {
    if (requestCode != REQUEST_PERMISSIONS)
      throw new AssertionError("Unexpected requestCode");
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Config.setLocationRequested();
    // No-op here - onResume() calls init();
  }

  private void init()
  {
    MwmApplication app = MwmApplication.from(this);
    try
    {
      app.init();
    } catch (IOException e)
    {
      showFatalErrorDialog(R.string.dialog_error_storage_title, R.string.dialog_error_storage_message);
      return;
    }

    if (Counters.isFirstLaunch(this) && PermissionsUtils.isLocationGranted(this))
    {
      LocationHelper.INSTANCE.onEnteredIntoFirstRun();
      if (!LocationHelper.INSTANCE.isActive())
        LocationHelper.INSTANCE.start();
    }

    processNavigation();

    if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    ) {
      Log.d(TAG, "Permission granted. Initializing side channel...");
      setUpandRun(savedState);
    }
  }

  @SuppressWarnings("unchecked")
  private void processNavigation()
  {
    Intent input = getIntent();
    Intent result = new Intent(this, DownloadResourcesLegacyActivity.class);
    if (input != null)
    {
      if (input.hasExtra(EXTRA_ACTIVITY_TO_START))
      {
        result = new Intent(this,
                            (Class<? extends Activity>) input.getSerializableExtra(EXTRA_ACTIVITY_TO_START));
      }

      Intent initialIntent = input.hasExtra(EXTRA_INITIAL_INTENT) ?
                           input.getParcelableExtra(EXTRA_INITIAL_INTENT) :
                           input;
      result.putExtra(EXTRA_INITIAL_INTENT, initialIntent);
    }
    Counters.setFirstStartDialogSeen(this);
    startActivity(result);
    finish();
  }

  @Override
  @NonNull
  public Activity get()
  {
    return this;
  }

  @Override
  public int getThemeResourceId(@NonNull String theme)
  {
    Context context = getApplicationContext();
    if (ThemeUtils.isDefaultTheme(context, theme))
      return R.style.MwmTheme_Splash;

    if (ThemeUtils.isNightTheme(context, theme))
      return R.style.MwmTheme_Night_Splash;

    throw new IllegalArgumentException("Attempt to apply unsupported theme: " + theme);
  }

  public static native int setSharedMap();

  public native void setSharedMapChildTest(int shared_mem_ptr, char[] fileDes);

  public native int createAshMem();

  public static native long readAshMem(int fd);

  public static native void setAshMemVal(int fd, long val);

}
