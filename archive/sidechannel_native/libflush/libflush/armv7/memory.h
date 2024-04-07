/* See LICENSE file for license and copyright information */

#ifndef ARM_V7_MEMORY_H
#define ARM_V7_MEMORY_H

inline void
arm_v7_access_memory(void* pointer)
{
  volatile uint32_t value;
  __asm__ volatile ("LDR %0, [%1]\n\t"
      : "=r" (value)
      : "r" (pointer)
      );
}

inline void
arm_v7_memory_barrier(void)
{
  __asm__ volatile ("DSB");
  __asm__ volatile ("ISB");
}

inline void
arm_v7_prefetch(void* pointer)
{
  __asm__ volatile ("pld [%0]" :: "r" (pointer));
}

#endif  /*ARM_V7_MEMORY_H*/
