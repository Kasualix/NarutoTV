package me.kall.narutotv;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class JavaFileCollector {
    public static void mergeJavaFilesToTxt(File sourceFolder, File targetTxt) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetTxt), StandardCharsets.UTF_8))) {
            appendJavaFiles(sourceFolder, bw);
        }
    }

    private static void appendJavaFiles(@NotNull File sourceFolder, BufferedWriter bw) throws IOException {
        File[] files = sourceFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    appendJavaFiles(file, bw);
                } else if (file.getName().endsWith(".java")) {
                    bw.write("// ===== File: " + file.getAbsolutePath() + " =====\n");
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            bw.write(line);
                            bw.newLine();
                        }
                    }
                    bw.newLine();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            Path path = Path.of("D:\\ModDev\\NarutoTV\\src\\main\\java\\me\\kall\\narutotv");
            mergeJavaFilesToTxt(path.toFile(), path.resolve("code" + ".txt").toFile());
            System.out.println("Merged!");
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}