#pragma once

#define TAG "GROUND_TRUTH_DEBUG"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)

namespace sidechannel
{
    struct node {
        long systemTime;
        int methodId;
        struct node *next;
    };

    int divideNum(int g, int h);

    int multiplyNum(int e, int f);

    int subNum(int c, int d);

    int addNum(int a, int b);

    long getTimeStamp();

    void logMethodCall(char* methodName);
}