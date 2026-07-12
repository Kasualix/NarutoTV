package me.kall.narutotv.base.env;

import me.kall.narutotv.app.data.MediaArgs;
import me.kall.narutotv.base.data.Sources;

public class TVConfig {
    private static final String VIDEO_PATH = "tv.video";
    private static final String AUDIO_PATH = "tv.audio";

    static {
        if (TVConfig.video() == null || TVConfig.audio() == null) {
            MediaArgs mediaArgs = Sources.roll();
            System.setProperty(VIDEO_PATH, mediaArgs.absVideoPath());
            System.setProperty(AUDIO_PATH, mediaArgs.absAudioPath());
        }
    }

    public static String video() {
        return System.getProperty(VIDEO_PATH);
    }

    public static String audio() {
        return System.getProperty(AUDIO_PATH);
    }
}
