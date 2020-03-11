package com.blz.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by N0tExpectErr0r at 2019/08/08
 */
class AopClassVisitor extends ClassVisitor {
    public AopClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM6, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        return new AopAdviceAdapter(methodVisitor, access, name, desc);
    }
}
