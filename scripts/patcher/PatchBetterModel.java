import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Patches BetterModel's Spigot jar so it does not touch the protected EntityHuman.bW field directly. */
public class PatchBetterModel {
    static final String PKG = "kr/toxicity/model/bukkit/nms/v1_21_R3/";
    static final String TARGET = PKG + "FunctionsKt.class";
    static final String HELPER = PKG + "FmodAccess.class";

    static final String HELPER_SRC = """
        package kr.toxicity.model.bukkit.nms.v1_21_R3;
        public final class FmodAccess {
            private static Object cached;
            private FmodAccess() {}
            public static Object playerModeCustomisation() {
                Object c = cached;
                if (c == null) {
                    try {
                        Class<?> k = Class.forName("net.minecraft.world.entity.player.EntityHuman", false, FmodAccess.class.getClassLoader());
                        java.lang.reflect.Field f = k.getDeclaredField("bW");
                        f.setAccessible(true);
                        c = f.get(null);
                        cached = c;
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        throw new RuntimeException("forzammilmod patch: cannot read EntityHuman.bW", e);
                    }
                }
                return c;
            }
        }
        """;

    public static void main(String[] a) throws Exception {
        if (a.length != 2) { System.err.println("usage: PatchBetterModel <in.jar> <out.jar>"); System.exit(2); }
        byte[] helper = compileHelper();
        int replaced = 0;
        try (ZipFile in = new ZipFile(a[0]);
             ZipOutputStream out = new ZipOutputStream(new FileOutputStream(a[1]))) {
            for (Enumeration<? extends ZipEntry> en = in.entries(); en.hasMoreElements();) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (n.startsWith("META-INF/") && (n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA"))) continue;
                if (n.equals(HELPER)) continue;
                byte[] data = e.isDirectory() ? new byte[0] : in.getInputStream(e).readAllBytes();
                if (n.equals(TARGET)) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(data).accept(cn, 0);
                    for (MethodNode m : cn.methods) {
                        for (AbstractInsnNode i : m.instructions.toArray()) {
                            if (i instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC
                                && f.owner.equals("net/minecraft/world/entity/player/EntityHuman")
                                && f.name.equals("bW")
                                && f.desc.equals("Lnet/minecraft/network/syncher/DataWatcherObject;")) {
                                InsnList l = new InsnList();
                                l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PKG + "FmodAccess",
                                    "playerModeCustomisation", "()Ljava/lang/Object;", false));
                                l.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/network/syncher/DataWatcherObject"));
                                m.instructions.insertBefore(i, l);
                                m.instructions.remove(i);
                                replaced++;
                            }
                        }
                    }
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    cn.accept(cw);
                    data = cw.toByteArray();
                }
                ZipEntry ne = new ZipEntry(n);
                out.putNextEntry(ne);
                out.write(data);
                out.closeEntry();
            }
            out.putNextEntry(new ZipEntry(HELPER));
            out.write(helper);
            out.closeEntry();
        }
        System.out.println("Replaced " + replaced + " access(es) to EntityHuman.bW");
        if (replaced == 0) { System.err.println("ERROR: nothing replaced"); new File(a[1]).delete(); System.exit(1); }
    }

    static byte[] compileHelper() throws Exception {
        Path dir = Files.createTempDirectory("fmodpatch");
        Path src = dir.resolve("FmodAccess.java");
        Files.writeString(src, HELPER_SRC);
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        if (jc == null) throw new IllegalStateException("Run with a JDK (needs javac)");
        int rc = jc.run(null, null, null, "--release", "21", "-d", dir.toString(), src.toString());
        if (rc != 0) throw new IllegalStateException("javac failed");
        return Files.readAllBytes(dir.resolve(HELPER));
    }
}
