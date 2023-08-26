#include "soc/rtc_cntl_reg.h"
#include "soc/rtc_io_reg.h"
#include "soc/soc_ulp.h"
#include "soc/sens_reg.h"
#include "soc/rtc_i2c_reg.h"

#include "stack.s"

.set I2C_ADDR, 0x68
.set MPU6050_PWR_MGMT_1, 0x6B
.set MPU6050_PWR_MGMT_2, 0x6C
.set MPU6050_SMPLRT_DIV, 0x19
.set MPU6050_ACCEL_CONFIG, 0x1C
//.set MPU6050_WHO_AM_I, 0x75
.set MPU6050_ACCEL_X_OUT_H, 0x3B
.set MPU6050_ACCEL_Y_OUT_H, 0x3D
.set MPU6050_ACCEL_Z_OUT_H, 0x3F
.set ZERO, 0x00
.set ONE, 0x01

// unluckily bitbanging I2C trashes all registers except r3 which must contain the stack.
.macro i2crd regnum
    push  r2
    move r1,I2C_ADDR
    push r1
    move r1,\regnum
    push r1
    psr
    jump read8
    add r3,r3,2 // remove 2 arguments from stack
    pop r2
.endm

.macro i2crd16 regnum
    push  r2
    move r1,I2C_ADDR
    push r1
    move r1,\regnum
    push r1
    psr
    jump read16
    add r3,r3,2 // remove 2 arguments from stack
    pop r2
.endm

.macro i2cwr regnum,val
    push r2
    move r1,I2C_ADDR
    push r1
    move r1,\regnum
    push r1
    move r1,\val
    push r1
    psr
    jump write8
    add r3,r3,3 // remove 3 arguments from stack
    pop r2
.endm


/* Define variables, which go into .bss section (zero-initialized data) */
    .bss

  .global samplecount
samplecount: .long 0

  .global accelx
accelx: .long 0
prevaccelx: .long 0

  .global accely
accely: .long 0
prevaccely: .long 0

  .global accelz
accelz: .long 0
prevaccelz: .long 0

  .global steps
steps: .long 0
stepped: .long 0

// stack
  .global stack
stack:
  .fill 100
  .global stackEnd
stackEnd:
  .long 0

// I2C.s
  
i2c_started: .long 0

i2c_didInit: .long 0


/* Code goes into .text section */
    .text

    .global entry
entry:
    // Main code
    move r3,stackEnd
    
init_mpu6050:
    i2cwr MPU6050_SMPLRT_DIV,0x00
    i2cwr MPU6050_ACCEL_CONFIG,0x00
    i2cwr MPU6050_PWR_MGMT_2,0x47
    i2cwr MPU6050_PWR_MGMT_1,0x29//0x09
    
getSample:
    move r2,1000
    psr
    jump waitMs
    /*
    i2crd   MPU6050_WHO_AM_I
    move    r1, id
    st      r0,r1,0
    */
    i2crd   MPU6050_ACCEL_X_OUT_H
    jumpr   getSample,0,eq
    move    r1, accelx
    st      r0,r1,0
    move    r1, prevaccelx
    ld      r2,r1,0
    sub     r0,r0,r2
    psr
    jump    abs
    psr
    jump    toggle
    
    i2crd16 MPU6050_ACCEL_Y_OUT_H
    jumpr   getSample,0,eq
    move    r1, accely
    st      r0,r1,0
    move    r1, prevaccely
    ld      r2,r1,0
    sub     r0,r0,r2
    psr
    jump    abs
    psr
    jump    toggle

    i2crd16 MPU6050_ACCEL_Z_OUT_H
    jumpr   getSample,0,eq
    move    r1, accelz
    st      r0,r1,0
    move    r1, prevaccelz
    ld      r2,r1,0
    sub     r0,r0,r2
    psr
    jump    abs
    psr
    jump    toggle

    psr
    jump    incrementStep

    move    r1, prevaccelx
    move    r0, accelx
    ld      r2,r0,0
    st      r2,r1,0

    move    r1, prevaccely
    move    r0, accely
    ld      r2,r0,0
    st      r2,r1,0

    move    r1, prevaccelz
    move    r0, accelz
    ld      r2,r0,0
    st      r2,r1,0

loop:
    jump    getSample
/*    
is_rdy_for_wakeup:
    //READ_RTC_REG(RTC_CNTL_DIAG0_REG, 19, 1)
    READ_RTC_FIELD(RTC_CNTL_LOW_POWER_ST_REG, RTC_CNTL_RDY_FOR_WAKEUP) // Read RTC_CNTL_RDY_FOR_WAKEUP bit
    and   r0, r0, 1
    jump  getSample,eq
    wake // Trigger wake up    
    WRITE_RTC_FIELD(RTC_CNTL_STATE0_REG, RTC_CNTL_ULP_CP_SLP_TIMER_EN, 0)
*/
.global exit
exit:   /* end the program */
    halt // on halt the ulp will start sleeping and it will automatically restart after ULP_SENSOR_PERIOD has passed

.global waitMs
waitMs:
    wait 8000
    sub r2,r2,1
    jump doneWaitMs,eq
    jump waitMs
doneWaitMs:
    ret

// Compute abs value of R0
abs:
    and  r1,r0,0x8000
    jump noNegate,eq
    move r1,ZERO
    sub  r0,r1,r0
noNegate:
    ret

toggle:
    jumpr ret1,8192,le
    move  r1,stepped
    move  r0,ONE
    st    r0,r1,0
ret1:
    ret

incrementStep:
    move  r1, stepped
    ld    r0,r1,0
    jumpr ret1,1,lt
    move  r1,stepped
    move  r0,ZERO
    st    r0,r1,0
    move  r1,steps
    ld    r0,r1,0
    add   r0,r0,1
    st    r0,r1,0
    ret
// I2C.s

/*
 * =============================== I2C code ==========================================
 * Implementation of pseudo code from
 * https://en.wikipedia.org/wiki/I%C2%B2C#Example_of_bit-banging_the_I.C2.B2C_master_protocol
 */


.global i2c_start_cond
.global i2c_stop_cond
.global i2c_write_bit
.global i2c_read_bit
.global i2c_write_byte
.global i2c_read_byte

.macro I2C_delay
  wait 10 // 38 // minimal 4.7us
.endm

.macro read_SCL // Return current level of SCL line, 0 or 1 
  READ_RTC_REG(RTC_GPIO_IN_REG, RTC_GPIO_IN_NEXT_S + 13, 1) // RTC_GPIO_13 == GPIO_15
.endm

.macro read_SDA // Return current level of SDA line, 0 or 1
  READ_RTC_REG(RTC_GPIO_IN_REG, RTC_GPIO_IN_NEXT_S + 12, 1) // RTC_GPIO_12 == GPIO_02
.endm

.macro set_SCL // Do not drive SCL (set pin high-impedance)
  WRITE_RTC_REG(RTC_GPIO_ENABLE_W1TC_REG, RTC_GPIO_ENABLE_W1TC_S + 13, 1, 1)
.endm

.macro clear_SCL // Actively drive SCL signal low
  // Output mode
  WRITE_RTC_REG(RTC_GPIO_ENABLE_W1TS_REG, RTC_GPIO_ENABLE_W1TS_S + 13, 1, 1)
.endm

.macro set_SDA // Do not drive SDA (set pin high-impedance)
  WRITE_RTC_REG(RTC_GPIO_ENABLE_W1TC_REG, RTC_GPIO_ENABLE_W1TC_S + 12, 1, 1)
.endm

.macro clear_SDA // Actively drive SDA signal low
  // Output mode
  WRITE_RTC_REG(RTC_GPIO_ENABLE_W1TS_REG, RTC_GPIO_ENABLE_W1TS_S + 12, 1, 1)
.endm


i2c_start_cond:
  move r1,i2c_didInit
  ld r0,r1,0
  jumpr didInit,1,ge
  move r0,1
  st r0,r1,0
// set GPIO to pull low when activated
  WRITE_RTC_REG(RTC_GPIO_OUT_REG, RTC_GPIO_OUT_DATA_S + 13, 1, 0)
  WRITE_RTC_REG(RTC_GPIO_OUT_REG, RTC_GPIO_OUT_DATA_S + 12, 1, 0)
didInit:
  move r2,i2c_started
  ld r0,r2,0
  jumpr not_started,1,lt
// if started, do a restart condition
// set SDA to 1
  set_SDA
  I2C_delay
  set_SCL
clock_stretch: // TODO: Add timeout?
  read_SCL
  jumpr clock_stretch,1,lt

// Repeated start setup time, minimum 4.7us
  I2C_delay

not_started:
  // if (read_SDA() == 0) {
  //    arbitration_lost();
  // }

// SCL is high, set SDA from 1 to 0.
  clear_SDA
  I2C_delay
  clear_SCL
  move r0,1
  st r0,r2,0

  ret


i2c_stop_cond:
// set SDA to 0
  clear_SDA
  I2C_delay

  set_SCL
clock_stretch_stop:
  read_SCL
  jumpr clock_stretch_stop,1,lt

// Stop bit setup time, minimum 4us
  I2C_delay

// SCL is high, set SDA from 0 to 1
  set_SDA
  I2C_delay
  // if (read_SDA() == 0) {
  //    arbitration_lost();
  // }

  move r2,i2c_started
  move r0,0
  st r0,r2,0

  ret


// Write a bit to I2C bus
i2c_write_bit:
  jumpr bit0,1,lt
  set_SDA
  jump bit1
bit0:
  clear_SDA
bit1:

// SDA change propagation delay
  I2C_delay
// Set SCL high to indicate a new valid SDA value is available
  set_SCL
// Wait for SDA value to be read by slave, minimum of 4us for standard mode
  I2C_delay

clock_stretch_write:
  read_SCL
  jumpr clock_stretch_write,1,lt

  // SCL is high, now data is valid
  // If SDA is high, check that nobody else is driving SDA
  // if (bit && (read_SDA() == 0)) {
  //    arbitration_lost();
  // }

  // Clear the SCL to low in preparation for next change
  clear_SCL

  ret


// Read a bit from I2C bus
i2c_read_bit:
// Let the slave drive data
  set_SDA
// Wait for SDA value to be written by slave, minimum of 4us for standard mode
  I2C_delay
// Set SCL high to indicate a new valid SDA value is available
  set_SCL

clock_stretch_read:
  read_SCL
  jumpr clock_stretch_read,1,lt

// Wait for SDA value to be written by slave, minimum of 4us for standard mode
  I2C_delay
// SCL is high, read out bit
  read_SDA
// Set SCL low in preparation for next operation
  clear_SCL

  ret // bit in r0

// Write a byte to I2C bus. Return 0 if ack by the slave.
i2c_write_byte:
  stage_rst
next_bit:
  and r0,r2,0x80
  psr
  jump i2c_write_bit
  lsh r2,r2,1
  stage_inc 1
  jumps next_bit,8,lt

  psr
  jump i2c_read_bit
  ret // nack


// Read a byte from I2C bus
i2c_read_byte:
  push r2
  move r2,0
  stage_rst
next_bit_read:
  psr
  jump i2c_read_bit
  lsh r2,r2,1
  or r2,r2,r0
  stage_inc 1
  jumps next_bit_read,8,lt

  pop r0
  psr
  jump i2c_write_bit

  move r0,r2

  ret


/*
 * I2C ULP utility routines
 */

write_intro:
  psr
  jump i2c_start_cond

  ld r2,r3,20 // Address
  lsh r2,r2,1
  psr
  jump i2c_write_byte
  jumpr popfail,1,ge

  ld r2,r3,16 // Register
  psr
  jump i2c_write_byte
  jumpr popfail,1,ge
  ret


.global write8
write8:
  psr
  jump write_intro

write_b:
  ld r2,r3,8 // data byte
  psr
  jump i2c_write_byte
  jumpr fail,1,ge

  psr
  jump i2c_stop_cond

  move r2,0 // Ok
  ret


read_intro:
  psr
  jump i2c_start_cond

  ld r2,r3,16 // Address
  lsh r2,r2,1
  psr
  jump i2c_write_byte
  jumpr popfail,1,ge

  ld r2,r3,12 // Register
  psr
  jump i2c_write_byte
  jumpr popfail,1,ge

  psr
  jump i2c_start_cond

  ld r2,r3,16
  lsh r2,r2,1
  or r2,r2,1 // Address Read
  psr
  jump i2c_write_byte
  jumpr popfail,1,ge
  
  ret
popfail:
  pop r1 // pop caller return address
  move r2,1
  ret

.global read8
read8:
  psr
  jump read_intro

  move r2,1 // last byte
  psr
  jump i2c_read_byte
  push r0

  psr
  jump i2c_stop_cond

  pop r0

  move r2,0 // OK
  ret
fail:
  move r2,1
  ret

.global read16
read16:
  psr
  jump read_intro

  move r2,0
  psr
  jump i2c_read_byte
  push r0

  move r2,1 // last byte
  psr
  jump i2c_read_byte
  push r0

  psr
  jump i2c_stop_cond

  pop r0
  pop r2 // first byte
  lsh r2,r2,8
  or r2,r2,r0
  move r0,r2

  move r2,0 // OK
  ret
