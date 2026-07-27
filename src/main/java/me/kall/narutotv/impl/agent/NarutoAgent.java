package me.kall.narutotv.impl.agent;

import me.kall.narutotv.impl.NarutoProperties;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class NarutoAgent {
    private static final String[] BOOTSTRAP_ENTRIES = {"me/kall/narutotv/impl/agent/NarutoClassLoader.class", "me/kall/narutotv/impl/agent/NarutoRenderBridge.class"};

    public static void premain(String agentArgs, @NotNull Instrumentation instrumentation) {
        System.setProperty(NarutoProperties.GPU_ACCEL, "");

        instrumentation.addTransformer(new NarutoTransformer(), false);
        try {
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(NarutoAgent.createBootstrapJar().toFile()));
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            throw new RuntimeException(exception);
        }
    }

    private static @NotNull Path createBootstrapJar() {
        try {
            Path narutoBootstrap = NarutoRenderBridge.NARUTO_JAR.getParent().resolve("naruto-bootstrap.jar");
            if (narutoBootstrap.toFile().exists() && narutoBootstrap.toFile().delete()) System.out.println("Deleting existing naruto-bootstrap.jar for update");
            Files.createFile(narutoBootstrap);

            try (ZipFile source = new ZipFile(NarutoRenderBridge.NARUTO_JAR.toFile());
                 ZipOutputStream output = new ZipOutputStream(new FileOutputStream(narutoBootstrap.toFile()))) {

                for (String entryName : BOOTSTRAP_ENTRIES) {
                    ZipEntry zipEntry = source.getEntry(entryName);
                    if (zipEntry == null) continue;

                    output.putNextEntry(new ZipEntry(entryName));

                    try (InputStream inputStream = source.getInputStream(zipEntry)) {
                        inputStream.transferTo(output);
                    }

                    output.closeEntry();
                }
            }

            return narutoBootstrap;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
