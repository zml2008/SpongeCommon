/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.launch.transformer;

import com.google.common.collect.Sets;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.ClassVisitor;
import org.spongepowered.asm.lib.ClassWriter;
import org.spongepowered.asm.lib.MethodVisitor;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.util.PrettyPrinter;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Resource;

/**
 * A transformer which prints a warning for any non-whitelisted
 * classes which contain a reference to the provided method calls.
 *
 * <p>This transformer is annotated with @Resource to prevent Mixin from using
 * it in the initial pass. Since it doesn't modify any classes, it's pointless to
 * run it there.</p>
 *
 * Based on Forge's TerminalTransformer
 */
@Resource
public class SpongeMethodWarningTranformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }

        Set<SpongeMethodWarningRegistry.TransformData> needsCheck = getNonWhitelisted(transformedName.replace(".", "/"));

        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        ClassVisitor visitor = writer;
        visitor = new WarningVisitor(visitor, needsCheck);

        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    private static class WarningVisitor extends ClassVisitor {

        private String className;
        private Set<SpongeMethodWarningRegistry.TransformData> needsCheck;

        public WarningVisitor(ClassVisitor cv, Set<SpongeMethodWarningRegistry.TransformData> needsCheck) {
            super(Opcodes.ASM5, cv);
            this.needsCheck = needsCheck;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, methodName, methodDesc, signature, exceptions)) {

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);

                    if (owner.equals(WarningVisitor.this.className)) {
                        return;
                    }

                    for (SpongeMethodWarningRegistry.TransformData data: needsCheck) {
                        if (data.owner.equals(owner) && data.method.equals(name + desc)) {
                            new PrettyPrinter(60)
                                    .add("A mod/plugin has a direct reference to %s.%s! %s", data.owner, data.method, data.warningMessage)
                                    .add("Method containing the reference: %s.%s%s", WarningVisitor.this.className, methodName, methodDesc)
                                    .trace(System.err);
                        }
                    }
                }

            };
        }
    }

    protected static Set<SpongeMethodWarningRegistry.TransformData> getNonWhitelisted(String className) {
        try {
            ClassNode node = readClass(Launch.classLoader.getClassBytes(className));
            Set<SpongeMethodWarningRegistry.TransformData> needsTransform = Sets.newHashSet(SpongeMethodWarningRegistry.warningMethods);

            while (!className.equals("java/lang/Object")) {
                for (SpongeMethodWarningRegistry.TransformData data: SpongeMethodWarningRegistry.warningMethods) {
                    if (data.whitelistedClasses.contains(className)) {
                        needsTransform.remove(data);
                    }
                }

                node = readClass(Launch.classLoader.getClassBytes(node.superName));
                className = node.name.replace(".", "/");
            }
            return needsTransform;
        } catch (IOException e) {
            throw new RuntimeException("Error when determining superclass", e);
        }
    }

    private static ClassNode readClass(byte[] basicClass) {
        ClassReader classReader = new ClassReader(basicClass);

        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        return classNode;
    }
}
