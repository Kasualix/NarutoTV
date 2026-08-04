package me.kall.narutotv;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaFileCollector {
    public static void merge(File sourceFolder, File targetTxt) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetTxt), StandardCharsets.UTF_8))) {
            JavaFileCollector.append(sourceFolder, writer);
        }
    }

    private static void append(@NotNull File sourceFolder, BufferedWriter writer) throws IOException {
        File[] files = sourceFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    JavaFileCollector.append(file, writer);
                } else if (file.getName().endsWith(".java")) {
                    writer.write("// =====" + file.getAbsolutePath() + " =====\n");
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                    writer.newLine();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Path path = Path.of("D:\\ModDev\\NarutoTV\\src\\main\\java\\me\\kall\\narutotv");
            Path txt = path.resolve("code" + ".txt");
            if (Files.exists(txt)) Files.delete(txt);
            JavaFileCollector.merge(path.toFile(), txt.toFile());
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}