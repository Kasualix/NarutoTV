package me.kall.narutotv.app;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Executable {
    public static final AtomicBoolean PRINT = new AtomicBoolean();

    public static @Nullable String runCommand(@NotNull List<String> command) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException exception) {
            System.err.println("Exception building process for command " + command);
            exception.printStackTrace(System.err);
            return null;
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (PRINT.get()) System.out.println(line);
                output.append(line.trim()).append("\n");
            }
        } catch (IOException exception) {
            System.err.println("Exception reading input stream for command " + command);
            exception.printStackTrace(System.err);
            return null;
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) System.err.println("Command exited with code " + exitCode + ": " + command);
        } catch (InterruptedException exception) {
            System.err.println("Exception finalizing process for command " + command);
            return null;
        }

        return output.toString().trim();
    }

    public static @Nullable String runCommand(@NotNull String... command) {
        return runCommand(List.of(command));
    }
}
