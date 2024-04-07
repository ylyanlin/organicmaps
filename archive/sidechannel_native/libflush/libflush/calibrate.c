/* See LICENSE file for license and copyright information */

#include <inttypes.h>
#include <sched.h>
#include <unistd.h>
#include "libflush.h"

#include "calibrate.h"
#include "logoutput.h"

#define MIN(a, b) ((a) > (b)) ? (b) : (a)

#define log_err(fmt,args...) __android_log_print(ANDROID_LOG_ERROR, TAG_NAME, (const char *) fmt, ##args)
int calibrate(libflush_session_t* libflush_session)
{
    size_t threshold;
    size_t reload_time = 0, flush_reload_time = 0, i, count = 1000000;
    size_t dummy[16];
    size_t *ptr = dummy + 8;
    uint64_t start = 0, end = 0;

    libflush_reload_address(libflush_session, ptr);
    for (i = 0; i < count; i++) {
        reload_time += libflush_reload_address(libflush_session, ptr);
    }

    for (i = 0; i < count; i++) {
        flush_reload_time += libflush_reload_address_and_flush(libflush_session, ptr);
    }

    reload_time /= count;
    flush_reload_time /= count;

    LOGD("INFO", "Flush+Reload: %zd cycles, Reload only: %zd cycles\n",
         flush_reload_time, reload_time);
    threshold = (flush_reload_time + reload_time * 2) / 3;
    LOGD("SUCCESS", "Flush+Reload threshold: %zd cycles\n",
         threshold);
    return threshold;
}