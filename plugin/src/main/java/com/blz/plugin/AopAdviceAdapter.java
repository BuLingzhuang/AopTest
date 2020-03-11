package com.blz.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by N0tExpectErr0r at 2019/08/08
 */
class AopAdviceAdapter extends AdviceAdapter {
    private final MethodVisitor methodVisitor;
    private final String methodName;

    public AopAdviceAdapter(MethodVisitor methodVisitor, int access, String name, String desc) {
        super(Opcodes.ASM6, methodVisitor, access, name, desc);
        this.methodVisitor = methodVisitor;
        this.methodName = name;
    }
}
