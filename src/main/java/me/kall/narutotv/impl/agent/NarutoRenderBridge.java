package me.kall.narutotv.impl.agent;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.logging.Logger;

@SuppressWarnings({"resource", "unused"})
public class NarutoRenderBridge {
    public static final Path NARUTO_JAR;

    private static final Logger LOGGER = Logger.getLogger("NarutoEarlyRenderer");

    static {
        Path narutoJar = null;
        for (String jvmArgument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (jvmArgument.startsWith("-javaagent:")) {
                String agentPath = jvmArgument.substring("-javaagent:".length());
                if (agentPath.contains("narutotv")) {
                    narutoJar = Path.of(agentPath);
                    break;
                }
            }
        }
        NARUTO_JAR = narutoJar;
    }

    private static final Class<?> DISPLAY_WINDOW;
    private static final Method RENDER;

    static {
        try {
            NarutoClassLoader narutoClassLoader = new NarutoClassLoader(NARUTO_JAR.toUri().toURL(), Thread.currentThread().getContextClassLoader());
            DISPLAY_WINDOW = narutoClassLoader.loadClass("me.kall.narutotv.impl.agent.NarutoEarlyRenderer");
            RENDER = DISPLAY_WINDOW.getMethod("bridge");
        } catch (Exception exception) {
            LOGGER.severe(exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    public static synchronized void render()  {
        try {
            RENDER.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            LOGGER.severe(exception.getMessage());
            throw new RuntimeException(exception);
        }
    }
}
