/* See LICENSE file for license and copyright information */

#ifndef ARM_v8_MEMORY_H
#define ARM_v8_MEMORY_H
static inline void
arm_v8_access_memory(void* pointer)
{
  volatile uint32_t value;
  __asm__ volatile ("LDR %0, [%1]\n\t"
      : "=r" (value)
      : "r" (pointer)
      );//loading data into register from memory
}

static inline void
arm_v8_memory_barrier(void)
{
  __asm__ volatile ("DSB SY");
  __asm__ volatile ("ISB");
}

static inline void
arm_v8_prefetch(void* pointer)
{
  __asm__ volatile ("PRFM PLDL3KEEP, [%x0]" :: "p" (pointer));
  __asm__ volatile ("PRFM PLDL2KEEP, [%x0]" :: "p" (pointer));
  __asm__ volatile ("PRFM PLDL1KEEP, [%x0]" :: "p" (pointer));
}

#endif  /*ARM_v8_MEMORY_H*/
