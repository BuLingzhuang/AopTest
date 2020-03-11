package com.blz.plugin;

import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class AopTransform extends Transform {
    private Map<String, File> modifyMap = new HashMap<>();
    private Project project;

    public AopTransform(Project project) {
        this.project = project;
    }

    @Override
    public String getName() {
        return AopTransform.class.getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        if (!isIncremental()) {
            transformInvocation.getOutputProvider().deleteAll();
        }
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                transformJar(transformInvocation, jarInput);
            }
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                transformDirectory(transformInvocation, directoryInput);
            }
        }
    }

    private void transformJar(TransformInvocation invocation, JarInput input) throws IOException {
        File tempDir = invocation.getContext().getTemporaryDir();
        String destName = input.getFile().getName();
        String hexName = DigestUtils.md5Hex(input.getFile().getAbsolutePath()).substring(0, 8);
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4);
        }
        // 获取输出路径
        File dest = invocation.getOutputProvider()
                .getContentLocation(destName + "_" + hexName, input.getContentTypes(), input.getScopes(), Format.JAR);

        JarFile originJar = new JarFile(input.getFile());
        File outputJar = new File(tempDir, "temp_" + input.getFile().getName());
        JarOutputStream output = new JarOutputStream(new FileOutputStream(outputJar));

        // 遍历原jar文件寻找class文件
        Enumeration<JarEntry> enumeration = originJar.entries();
        while (enumeration.hasMoreElements()) {
            JarEntry originEntry = enumeration.nextElement();
            InputStream inputStream = originJar.getInputStream(originEntry);

            String entryName = originEntry.getName();
            if (entryName.endsWith(".class")) {
                JarEntry destEntry = new JarEntry(entryName);
                output.putNextEntry(destEntry);

//                byte[] sourceBytes = IOUtils.toByteArray(inputStream);
                // 修改class文件内容
                byte[] modifiedBytes = modifyClass(inputStream);
                if (modifiedBytes == null) {
                    modifiedBytes = IOUtils.toByteArray(inputStream);
                }
                output.write(modifiedBytes);
                output.closeEntry();
            }
        }
        output.close();
        originJar.close();

        FileUtils.copyFile(outputJar, dest);
    }

    private void transformDirectory(TransformInvocation invocation, DirectoryInput input) throws IOException {
        File tempDir = invocation.getContext().getTemporaryDir();
        File dest = invocation.getOutputProvider()
                .getContentLocation(input.getName(), input.getContentTypes(), input.getScopes(), Format.DIRECTORY);
        File dir = input.getFile();
        if (dir != null && dir.exists()) {
            traverseDirectory(tempDir, dir);
            FileUtils.copyDirectory(input.getFile(), dest);
            for (Map.Entry<String, File> entry : modifyMap.entrySet()) {
                File target = new File(dest.getAbsolutePath() + entry.getKey());
                if (target.exists()) {
                    target.delete();
                }
                FileUtils.copyFile(entry.getValue(), target);
                entry.getValue().delete();
            }
        }
    }

    private void traverseDirectory(File tempDir, File dir) throws IOException {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
//            System.out.println("解析到 file = " + file.getAbsolutePath());
            if (file.isDirectory()) {
                traverseDirectory(tempDir, file);
            } else if (file.getAbsolutePath().endsWith(".class")) {
                String className = path2ClassName(file.getAbsolutePath()
                        .replace(dir.getAbsolutePath() + File.separator, ""));
//                byte[] sourceBytes = IOUtils.toByteArray(new FileInputStream(file));
//                byte[] modifiedBytes = modifyClass(sourceBytes);
                byte[] modifiedBytes = modifyClass(new FileInputStream(file));
                File modified = new File(tempDir, className.replace(".", "") + ".class");
                if (modified.exists()) {
                    modified.delete();
                }
                modified.createNewFile();
                new FileOutputStream(modified).write(modifiedBytes);
                String key = file.getAbsolutePath().replace(dir.getAbsolutePath(), "");
                modifyMap.put(key, modified);
            }
        }
    }

    private byte[] modifyClass(InputStream inputStream) {
        try {
            ClassReader classReader = new ClassReader(inputStream);
            ClassNode classNode = new ClassNode(Opcodes.ASM6);
            classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
            Iterator<MethodNode> iterator = classNode.methods.iterator();
            while (iterator.hasNext()) {
                MethodNode next = iterator.next();
                if (next.name.startsWith("test")) {
//                    List<LocalVariableNode> localVariables = next.localVariables;
//                    if (localVariables != null && localVariables.size() > 0) {
//                        for (LocalVariableNode localVariable : localVariables) {
//                            System.out.println("localVariables name = " + localVariable.name);
//                            System.out.println("localVariables desc = " + localVariable.desc);
//                            System.out.println("localVariables signature = " + localVariable.signature);
//                            System.out.println("localVariables index = " + localVariable.index);
//                            System.out.println("localVariables start = " + localVariable.start);
//                            System.out.println("localVariables end = " + localVariable.end + "\n");
//                        }
//                    } else {
//                        System.out.println("localVariables isEmpty");
//                    }
//                    iterator.remove();

                    InsnList instructions = next.instructions;
                    if (instructions != null) {
                        ListIterator<AbstractInsnNode> instructionsIterator = instructions.iterator();
                        while (instructionsIterator.hasNext()) {
                            AbstractInsnNode insnNode = instructionsIterator.next();
                            if (insnNode instanceof MethodInsnNode) {
                                showFinishData((MethodInsnNode) insnNode);
                            }
                        }
                    }
                }
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor classVisitor = new AopClassVisitor(classWriter);
            classNode.accept(classVisitor);
            return classWriter.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showFinishData(MethodInsnNode methodInsnNode) {
        System.out.println("MethodInsnNode getOpcode = " + methodInsnNode.getOpcode()
                + " name = " + methodInsnNode.name
                + " desc = " + methodInsnNode.desc
                + " owner = " + methodInsnNode.owner
                + " itf = " + methodInsnNode.itf + "\n");
        if (methodInsnNode.owner.equals("android/net/Uri")) {
            methodInsnNode.owner = "com/blz/print/TestUriPrint";
        }
    }

    private static String path2ClassName(String pathName) {
        return pathName.replace(File.separator, ".").replace(".class", "");
    }
}
