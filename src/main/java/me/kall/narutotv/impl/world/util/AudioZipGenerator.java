package me.kall.narutotv.impl.world.util;

import me.kall.narutotv.NarutoTV;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AudioZipGenerator {
    private static final Logger LOGGER = LogManager.getLogger(AudioZipGenerator.class);

    private final String absAudioPath;
    private final String zipName;

    public final String id;

    private AudioZipGenerator(String absAudioPath) {
        this.absAudioPath = absAudioPath;
        this.id = "audio_" + Path.of(absAudioPath).getParent().getFileName().hashCode();
        this.zipName = "NarutoTVAudioSource-" + this.id + ".zip";
    }

    @Contract("_ -> new")
    public static @NotNull AudioZipGenerator get(String absAudioPath) {
        return new AudioZipGenerator(absAudioPath);
    }

    public void generate(Runnable validation) {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.runAsync(() -> {
            try {
                Path resourcepacks = FMLLoader.getGamePath().resolve("resourcepacks");
                if (!Files.exists(resourcepacks)) Files.createDirectories(resourcepacks);
                File zip = new File(resourcepacks.toString(), this.zipName);
                if (zip.exists()) return;
                try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip))) {
                    this.mcmeta(output);
                    this.audioFile(output);
                    this.soundJson(output);
                }
            } catch (Exception exception) {
                LOGGER.error("Error generating audio zip.", exception);
                throw new RuntimeException(exception);
            }
        }, NarutoTV.io()).whenCompleteAsync((unused0, zipError) -> {
            PackRepository repository = minecraft.getResourcePackRepository();
            repository.reload();

            String packID = "file/" + this.zipName;
            Collection<String> selected = repository.getSelectedIds();

            if (selected.contains(packID)) {
                validation.run();
                return;
            }

            if (repository.getPack(packID) == null) return;

            Collection<String> newSelected = new ArrayList<>(selected);
            newSelected.add(packID);

            repository.setSelected(newSelected);

            minecraft.options.resourcePacks = new ArrayList<>(newSelected);
            minecraft.options.save();

            minecraft.reloadResourcePacks().whenCompleteAsync((unused1, throwable1) -> validation.run(), minecraft);
        }, minecraft);
    }

    private void mcmeta(@NotNull ZipOutputStream output) throws Exception {
        output.putNextEntry(new ZipEntry("pack.mcmeta"));

        String mcmeta = String.format("""
                {
                  "pack": {
                    "pack_format": %s,
                    "description": "NarutoTV Audio Sources"
                  }
                }""", SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        output.write(mcmeta.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private void audioFile(@NotNull ZipOutputStream output) throws Exception {
        output.putNextEntry(new ZipEntry(String.format("assets/%s/sounds/%s.ogg", NarutoTV.MOD_ID, this.id)));

        try (FileInputStream inputStream = new FileInputStream(this.absAudioPath)){
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) output.write(buffer, 0, length);
        }

        output.closeEntry();
    }

    private void soundJson(@NotNull ZipOutputStream output) throws Exception {
        output.putNextEntry(new ZipEntry(String.format("assets/%s/sounds.json", NarutoTV.MOD_ID)));

        String json = String.format("""
                {
                  "%s": {
                    "sounds": [
                      "%s:%s"
                    ]
                  }
                }""", this.id, NarutoTV.MOD_ID, this.id);

        output.write(json.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }
}
