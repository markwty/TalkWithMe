/* ULP Example: Read temperautre in deep sleep

   This example code is in the Public Domain (or CC0 licensed, at your option.)

   Unless required by applicable law or agreed to in writing, this
   software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
   CONDITIONS OF ANY KIND, either express or implied.

   This file contains assembly code which runs on the ULP.

*/

/* ULP assembly files are passed through C preprocessor first, so include directives
   and C macros may be used in these files 
 */

.macro push rx
  st \rx,r3,0
  sub r3,r3,1
.endm

.macro pop rx
  add r3,r3,1
  ld \rx,r3,0
.endm

// Prepare subroutine jump, uses scratch register sr
.macro psr sr=r1 pos=.
  .set _next2,(\pos+16)
  move \sr,_next2
  push \sr
.endm

// Return from subroutine
.macro ret sr=r1
  pop  \sr
  jump \sr
.endm
