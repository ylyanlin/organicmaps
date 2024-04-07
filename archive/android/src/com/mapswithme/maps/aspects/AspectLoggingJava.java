package com.mapswithme.maps.aspects;

import static com.mapswithme.maps.SplashActivity.GROUND_TRUTH_LOGGING;
import static com.mapswithme.maps.SplashActivity.fd;
import static com.mapswithme.maps.SplashActivity.methodIdMap;
import static com.mapswithme.maps.SplashActivity.methodStats;
import static com.mapswithme.maps.SplashActivity.readAshMem;
import static com.mapswithme.maps.sidechannel.JobMainAppInsertRunnable.insert_locker;

import com.mapswithme.maps.sidechannel.MethodStat;

import android.util.Log;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;


@Aspect
public class AspectLoggingJava {

    final String POINTCUT_METHOD_1 =
            "execution(* com.mapswithme.maps.search.SearchHistoryAdapter.getItemCount(..))";

    @Pointcut(POINTCUT_METHOD_1)
    public void executeCfgMethod1() {
    }

    final String POINTCUT_METHOD_2 =
            "execution(* com.mapswithme.maps.search.SearchAdapter.onBindViewHolder(.., int))";

    @Pointcut(POINTCUT_METHOD_2)
    public void executeCfgMethod2() {
    }

    final String POINTCUT_METHOD_3 =
            "execution(* com.mapswithme.maps.search.SearchAdapter.BaseResultViewHolder.bind(.., int))";

    @Pointcut(POINTCUT_METHOD_3)
    public void executeCfgMethod3() {
    }

    final String POINTCUT_METHOD_4 =
            "execution(* com.mapswithme.maps.MwmActivity.onTranslationChanged(float))";

    @Pointcut(POINTCUT_METHOD_4)
    public void executeCfgMethod4() {
    }

    final String POINTCUT_METHOD_5 =
            "execution(* com.mapswithme.maps.NavigationButtonsAnimationController.update(float))";

    @Pointcut(POINTCUT_METHOD_5)
    public void executeCfgMethod5() {
    }

    final String POINTCUT_METHOD_6 =
            "execution(* com.mapswithme.maps.routing.NavigationController.updateSearchButtonsTranslation(float))";

    @Pointcut(POINTCUT_METHOD_6)
    public void executeCfgMethod6() {
    }

    final String POINTCUT_METHOD_7 =
            "execution(* com.mapswithme.maps.NavigationButtonsAnimationController.move(float))";

    @Pointcut(POINTCUT_METHOD_7)
    public void executeCfgMethod7() {
    }

    final String POINTCUT_METHOD_8 =
            "execution(* com.mapswithme.maps.MwmActivity.onPlacePageSlide(int))";

    @Pointcut(POINTCUT_METHOD_8)
    public void executeCfgMethod8() {
    }


    @Around(""
            + "executeCfgMethod1()"
            + "|| executeCfgMethod2()"
            + "|| executeCfgMethod3()"
            + "|| executeCfgMethod4()"
            + "|| executeCfgMethod5()"
            + "|| executeCfgMethod6()"
            + "|| executeCfgMethod7()"
            + "|| executeCfgMethod8()"
    )
    public Object weaveJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            methodIdMap.putIfAbsent(methodSignature.toString(), methodIdMap.size());

            long startFd = fd > 0 ? readAshMem(fd) : -1;

            Object result = joinPoint.proceed();

            long endFd = fd > 0 ? readAshMem(fd) : -1;

            if (GROUND_TRUTH_LOGGING) {
                MethodStat methodStat = new MethodStat(methodIdMap.get(methodSignature.toString()), startFd, endFd);
//              Log.d("#Aspect ", methodStat.getId()+" "+startFd+" "+endFd);
                insert_locker.lock();
                if (methodStats.isEmpty()) {
                    methodStats.add(methodStat);

                } else if (!methodStats.get(methodStats.size() - 1).equals(methodStat)) {
                    methodStats.add(methodStat);
                }
                insert_locker.unlock();
            }
            //Log.d("LoggingVM", methodStat.toString());//
            //Log.d("LoggingVM", Thread.currentThread().getId() + ": " + methodSignature.toString());

            //Log.v("LoggingVM ",
            //        methodSignature.toString());
            return result;
        } catch (Exception e) {
            return joinPoint.proceed();
        }
    }
}

