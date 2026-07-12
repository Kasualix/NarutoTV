package me.kall.narutotv.app;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public final class Executable {
    private static final Logger LOGGER = Logger.getLogger(Executable.class.getSimpleName());

    public static void runCommand(@NotNull List<String> command, boolean print) {
        String[] commandArray = command.toArray(String[]::new);
        if (Executable.runCommand(commandArray, print) == null) {
            LOGGER.severe("Error executing command " + Arrays.toString(commandArray));
        }
    }

    public static @Nullable String runCommand(@NotNull String[] command, boolean print) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException exception) {
            LOGGER.severe("Exception building process for command " + Arrays.toString(command));
            exception.printStackTrace(System.err);
            return null;
        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (print) System.out.println(line);
                output.append(line.trim()).append("\n");
            }
        } catch (IOException exception) {
            LOGGER.severe("Exception reading input stream for command " + Arrays.toString(command));
            exception.printStackTrace(System.err);
            return null;
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) LOGGER.severe("Command exited with code " + exitCode + ": " + Arrays.toString(command));
        } catch (InterruptedException exception) {
            LOGGER.severe("Exception finalizing process for command " + Arrays.toString(command));
            return null;
        }

        return output.toString().trim();
    }
}