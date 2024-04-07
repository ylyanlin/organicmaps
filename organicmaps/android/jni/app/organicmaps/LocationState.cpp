#include "Framework.hpp"
#include "map/gps_tracker.hpp"

#include "app/organicmaps/core/jni_helper.hpp"

#include "app/organicmaps/platform/AndroidPlatform.hpp"

extern "C"
{

static void LocationStateModeChanged(location::EMyPositionMode mode,
                                     std::shared_ptr<jobject> const & listener)
{
  JNIEnv * env = jni::GetEnv();
  env->CallVoidMethod(*listener, jni::GetMethodID(env, *listener.get(),
                      "onMyPositionModeChanged", "(I)V"), static_cast<jint>(mode));
}

static void LocationPendingTimeout(std::shared_ptr<jobject> const & listener)
{
  JNIEnv * env = jni::GetEnv();
  env->CallVoidMethod(*listener, jni::GetMethodID(env, *listener.get(),
                      "onLocationPendingTimeout", "()V"));
}

//  public static void nativeSwitchToNextMode();
JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeSwitchToNextMode(JNIEnv * env, jclass clazz)
{
  timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  __android_log_print(ANDROID_LOG_INFO, "SIDECHANNEL_DEBUG", "[MethodId_7][%ld] nativeSwitchToNextMode called", (long)(ts.tv_sec * 1000000) + (ts.tv_nsec / 1000));
  ASSERT(g_framework, ());
  g_framework->SwitchMyPositionNextMode();
}

// private static int nativeGetMode();
JNIEXPORT jint JNICALL
Java_app_organicmaps_location_LocationState_nativeGetMode(JNIEnv * env, jclass clazz)
{
  ASSERT(g_framework, ());
  return g_framework->GetMyPositionMode();
}

//  public static void nativeSetListener(ModeChangeListener listener);
JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeSetListener(JNIEnv * env, jclass clazz,
                                                                  jobject listener)
{
  ASSERT(g_framework, ());
  g_framework->SetMyPositionModeListener(std::bind(&LocationStateModeChanged, std::placeholders::_1,
                                                   jni::make_global_ref(listener)));
}

//  public static void nativeRemoveListener();
JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeRemoveListener(JNIEnv * env, jclass clazz)
{
  ASSERT(g_framework, ());
  g_framework->SetMyPositionModeListener(location::TMyPositionModeChanged());
}

JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeSetLocationPendingTimeoutListener(
  JNIEnv * env, jclass clazz, jobject listener)
{
  ASSERT(g_framework, ());
  g_framework->NativeFramework()->SetMyPositionPendingTimeoutListener(
    std::bind(&LocationPendingTimeout, jni::make_global_ref(listener)));
}

JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeRemoveLocationPendingTimeoutListener(
  JNIEnv * env, jclass)
{
  ASSERT(g_framework, ());
  g_framework->NativeFramework()->SetMyPositionPendingTimeoutListener(nullptr);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeOnLocationError(JNIEnv * env, jclass clazz, int errorCode)
{
  ASSERT(g_framework, ());
  g_framework->OnLocationError(errorCode);
}

JNIEXPORT void JNICALL
Java_app_organicmaps_location_LocationState_nativeLocationUpdated(JNIEnv * env, jclass clazz, jlong time,
                                                                  jdouble lat, jdouble lon, jfloat accuracy,
                                                                  jdouble altitude, jfloat speed, jfloat bearing)
{
  ASSERT(g_framework, ());
  location::GpsInfo info;
  info.m_source = location::EAndroidNative;

  info.m_timestamp = static_cast<double>(time) / 1000.0;
  info.m_latitude = lat;
  info.m_longitude = lon;

  if (accuracy > 0.0)
    info.m_horizontalAccuracy = accuracy;

  if (altitude != 0.0)
  {
    info.m_altitude = altitude;
    info.m_verticalAccuracy = accuracy;
  }

  if (bearing > 0.0)
    info.m_bearing = bearing;

  if (speed > 0.0)
    info.m_speedMpS = speed;

  g_framework->OnLocationUpdated(info);
  GpsTracker::Instance().OnLocationUpdated(info);
}
} // extern "C"
