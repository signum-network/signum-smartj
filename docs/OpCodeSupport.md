# OpCode Support

[OpCode Explanations](https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings)

| Support type | Meaning |
| - | - |
| Supported | BlockTalk can compile the instruction. |
| Not Implemented | Compiling this instruction has not been implemented in BlockTalk yet. |
| Not in ASM | The ASM Library does not include this instruction. |
| Unsupported | BlockTalk does not support this instruction and/or related features

| OpCode | Supported? | Reason (if unsupported) |
| - | - | - | - |
| aaload | Not Implemented |  |
| aastore | Not Implemented |  |
| aconst_null | Supported |  |
| aload | Supported |  |
| aload_0 | Not Implemented |  |
| aload_1 | Not Implemented |  |
| aload_2 | Not Implemented |  |
| aload_3 | Not Implemented |  |
| anewarray | Not Implemented |  |
| areturn | Supported |  |
| arraylength | Not Implemented |  |
| astore | Supported |  |
| astore_0 | Not Implemented |  |
| astore_1 | Not Implemented |  |
| astore_2 | Not Implemented |  |
| astore_3 | Not Implemented |  |
| athrow | Unsupported | ATs do not have exceptions. |
| baload | Not Implemented |  |
| bastore | Not Implemented |  |
| bipush | Not Implemented |  |
| breakpoint | Not in ASM |  |
| caload | Not Implemented |  |
| castore | Not Implemented |  |
| checkcast | Unsupported | ATs do not have reflection. |
| d2f | Not Implemented |  |
| d2i | Not Implemented |  |
| d2l | Not Implemented |  |
| dadd | Not Implemented |  |
| daload | Not Implemented |  |
| dastore | Not Implemented |  |
| dcmpg | Not Implemented |  |
| dcmpl | Not Implemented |  |
| dconst_0 | Not Implemented |  |
| dconst_1 | Not Implemented |  |
| ddiv | Not Implemented |  |
| dload | Not Implemented |  |
| dload_0 | Not in ASM |  |
| dload_1 | Not in ASM |  |
| dload_2 | Not in ASM |  |
| dload_3 | Not in ASM |  |
| dmul | Not Implemented |  |
| dneg | Not Implemented |  |
| drem | Not Implemented |  |
| dreturn | Not Implemented |  |
| dstore | Not Implemented |  |
| dstore_0 | Not in ASM |  |
| dstore_1 | Not in ASM |  |
| dstore_2 | Not in ASM |  |
| dstore_3 | Not in ASM |  |
| dsub | Not Implemented |  |
| dup | Supported |  |
| dup_x1 | Not Implemented |  |
| dup_x2 | Not Implemented |  |
| dup2 | Not Implemented |  |
| dup2_x1 | Not Implemented |  |
| dup2_x2 | Not Implemented |  |
| f2d | Not Implemented |  |
| f2i | Not Implemented |  |
| f2l | Not Implemented |  |
| fadd | Not Implemented |  |
| faload | Not Implemented |  |
| fastore | Not Implemented |  |
| fcmpg | Not Implemented |  |
| fcmpl | Not Implemented |  |
| fconst_0 | Not Implemented |  |
| fconst_1 | Not Implemented |  |
| fconst_2 | Not Implemented |  |
| fdiv | Not Implemented |  |
| fload | Not Implemented |  |
| fload_0 | Not in ASM |  |
| fload_1 | Not in ASM |  |
| fload_2 | Not in ASM |  |
| fload_3 | Not in ASM |  |
| fmul | Not Implemented |  |
| fneg | Not Implemented |  |
| frem | Not Implemented |  |
| freturn | Not Implemented |  |
| fstore | Not Implemented |  |
| fstore_0 | Not in ASM |  |
| fstore_1 | Not in ASM |  |
| fstore_2 | Not in ASM |  |
| fstore_3 | Not in ASM |  |
| fsub | Not Implemented |  |
| getfield | Supported |  |
| getstatic | Not Implemented |  |
| goto | Supported |  |
| goto_w | Not in ASM |  |
| i2b | Supported |  |
| i2c | Supported |  |
| i2d | Not Implemented |  |
| i2f | Not Implemented |  |
| i2l | Supported |  |
| i2s | Supported |  |
| iadd | Supported |  |
| iaload | Not Implemented |  |
| iand | Supported |  |
| iastore | Not Implemented |  |
| iconst_m1 | Supported |  |
| iconst_0 | Supported |  |
| iconst_1 | Supported |  |
| iconst_2 | Supported |  |
| iconst_3 | Supported |  |
| iconst_4 | Supported |  |
| iconst_5 | Supported |  |
| idiv | Supported |  |
| if_acmpeq | Supported |  |
| if_acmpne | Supported |  |
| if_icmpeq | Supported |  |
| if_icmpge | Not Implemented |  |
| if_icmpgt | Not Implemented |  |
| if_icmple | Not Implemented |  |
| if_icmplt | Not Implemented |  |
| if_icmpne | Supported |  |
| ifeq | Supported |  |
| ifge | Supported |  |
| ifgt | Supported |  |
| ifle | Supported |  |
| iflt | Supported |  |
| ifne | Supported |  |
| ifnonnull | Supported |  |
| ifnull | Supported |  |
| iinc | Not Implemented |  |
| iload | Supported |  |
| iload_0 | Not in ASM |  |
| iload_1 | Not in ASM |  |
| iload_2 | Not in ASM |  |
| iload_3 | Not in ASM |  |
| impdep1 | Not in ASM |  |
| impdep2 | Not in ASM |  |
| imul | Supported |  |
| ineg | Supported |  |
| instanceof | Unsupported | ATs do not have reflection. |
| invokedynamic | Supported |  |
| invokeinterface | Supported |  |
| invokespecial | Supported |  |
| invokestatic | Supported |  |
| invokevirtual | Supported |  |
| ior | Supported |  |
| irem | Supported |  |
| ireturn | Supported |  |
| ishl | Not Implemented |  |
| ishr | Not Implemented |  |
| istore | Supported |  |
| istore_0 | Not in ASM |  |
| istore_1 | Not in ASM |  |
| istore_2 | Not in ASM |  |
| istore_3 | Not in ASM |  |
| isub | Supported |  |
| iushr | Not Implemented |  |
| ixor | Supported |  |
| jsr | Not Implemented |  |
| jsr_w | Not in ASM |  |
| l2d | Not Implemented |  |
| l2f | Not Implemented |  |
| l2i | Supported |  |
| ladd | Supported |  |
| laload | Not Implemented |  |
| land | Supported |  |
| lastore | Not Implemented |  |
| lcmp | Supported |  |
| lconst_0 | Supported |  |
| lconst_1 | Supported |  |
| ldc | Supported |  |
| ldc_w | Not in ASM |  |
| ldc2_w | Not in ASM |  |
| ldiv | Supported |  |
| lload | Supported |  |
| lload_0 | Not in ASM |  |
| lload_1 | Not in ASM |  |
| lload_2 | Not in ASM |  |
| lload_3 | Not in ASM |  |
| lmul | Supported |  |
| lneg | Supported |  |
| lookupswitch | Not Implemented |  |
| lor | Supported |  |
| lrem | Supported |  |
| lreturn | Supported |  |
| lshl | Not Implemented |  |
| lshr | Not Implemented |  |
| lstore | Supported |  |
| lstore_0 | Not in ASM |  |
| lstore_1 | Not in ASM |  |
| lstore_2 | Not in ASM |  |
| lstore_3 | Not in ASM |  |
| lsub | Supported |  |
| lushr | Not Implemented |  |
| lxor | Supported |  |
| monitorenter | Unsupported | ATs are a single thread. Multithreading is not supported. |
| monitorexit | Unsupported | ATs are a single thread. Multithreading is not supported. |
| multianewarray | Not Implemented |  |
| new | Not Implemented |  |
| newarray | Not Implemented |  |
| nop | Supported |  |
| pop | Supported |  |
| pop2 | Supported |  |
| putfield | Supported |  |
| putstatic | Not Implemented |  |
| ret | Not Implemented |  |
| return | Supported |  |
| saload | Not Implemented |  |
| sastore | Not Implemented |  |
| sipush | Not Implemented |  |
| swap | Not Implemented |  |
| tableswitch | Not Implemented |  |
| wide | Not in ASM |  |
