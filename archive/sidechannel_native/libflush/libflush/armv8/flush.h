/* See LICENSE file for license and copyright information */
//#include "../logoutput.h"

#ifndef ARM_V8_FLUSH_H
#define ARM_V8_FLUSH_H

static inline void arm_v8_flush(void* address)
{
  __asm__ volatile ("DC CIVAC, %0" :: "r"(address));
  __asm__ volatile ("DSB ISH");
  __asm__ volatile ("ISB");
}

#endif /* ARM_V8_FLUSH_H */
