package me.kall.narutotv.app.data;

public record MediaArgs(String absVideoPath, String absAudioPath, int channelCount, int sampleRate, int openALFormat, double fps, int width, int height, double duration) {
    @Override
    public int width() {
        int width = this.width;
        int widthCap = 7680;
        if (width > widthCap) width = widthCap;
        return width;
    }

    @Override
    public int height() {
        int height = this.height;
        int heightCap = 4320;
        if (height > heightCap) height = heightCap;
        return height;
    }
}