package com.ttclient.modules.client;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client-side music player.
 * Built-in tracks are original short melodies (not copyrighted songs).
 * Drop legal .wav files into .minecraft/ttclient/music/ and use Track=Custom.
 */
public class Music extends Module {
    public final ModeSetting track = addSetting(new ModeSetting(
            "Track", "Built-in melody or Custom folder", "Calm",
            "None", "Calm", "Bright", "Pulse", "Night", "Custom"));
    public final NumberSetting volume = addSetting(new NumberSetting(
            "Volume", "Playback volume", 0.45, 0.0, 1.0, 0.05));
    public final BoolSetting loop = addSetting(new BoolSetting(
            "Loop", "Repeat current track", true));
    public final NumberSetting customIndex = addSetting(new NumberSetting(
            "CustomIndex", "Which file in ttclient/music/ when Track=Custom (0 = first)", 0, 0, 64, 1));
    public final BoolSetting announce = addSetting(new BoolSetting(
            "Announce", "Chat the track name when it starts", true));

    private final AtomicBoolean stopFlag = new AtomicBoolean(false);
    private Thread playThread;
    private List<Path> customFiles = List.of();
    private float lastVol = -1f;
    private String lastTrack = "";

    public Music() {
        super("Music", "Play built-in melodies or your own .wav files from ttclient/music/", Category.CLIENT);
        setEnabled(false);
    }

    public static Music get() {
        try {
            var mods = com.ttclient.client.TTClientClient.modules;
            return mods == null ? null : mods.getModule(Music.class);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void onEnable() {
        stopFlag.set(false);
        refreshCustomFiles();
        startPlayback();
    }

    @Override
    public void onDisable() {
        stopFlag.set(true);
        stopPlayback();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        String t = track.get();
        float v = volume.getFloat();
        if (!t.equals(lastTrack) || Math.abs(v - lastVol) > 0.001f) {
            lastTrack = t;
            lastVol = v;
            stopFlag.set(true);
            stopPlayback();
            stopFlag.set(false);
            if (!"None".equals(t)) startPlayback();
        }
    }

    private void refreshCustomFiles() {
        try {
            Path dir = musicDir();
            Files.createDirectories(dir);
            List<Path> list = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.wav")) {
                for (Path p : ds) list.add(p);
            }
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.WAV")) {
                for (Path p : ds) list.add(p);
            }
            list.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));
            customFiles = list;
        } catch (Throwable t) {
            customFiles = List.of();
        }
    }

    private static Path musicDir() {
        try {
            Minecraft mc = Minecraft.getInstance();
            File gd = null;
            try {
                Object v = mc.getClass().getField("gameDirectory").get(mc);
                if (v instanceof File f) gd = f;
            } catch (Throwable ignored) {}
            if (gd == null) {
                try {
                    Object v = mc.getClass().getMethod("getGameDirectory").invoke(mc);
                    if (v instanceof File f) gd = f;
                } catch (Throwable ignored) {}
            }
            if (gd != null) return gd.toPath().resolve("ttclient").resolve("music");
        } catch (Throwable ignored) {}
        return Path.of(System.getProperty("user.home", "."), ".minecraft", "ttclient", "music");
    }

    private void startPlayback() {
        String mode = track.get();
        if ("None".equals(mode)) return;
        stopPlayback();
        stopFlag.set(false);
        playThread = new Thread(() -> runPlayLoop(mode), "tt-music");
        playThread.setDaemon(true);
        playThread.start();
    }

    private void stopPlayback() {
        stopFlag.set(true);
        if (playThread != null) {
            try { playThread.interrupt(); } catch (Throwable ignored) {}
            playThread = null;
        }
    }

    private void runPlayLoop(String mode) {
        do {
            if (stopFlag.get()) return;
            try {
                if ("Custom".equals(mode)) {
                    refreshCustomFiles();
                    if (customFiles.isEmpty()) {
                        chat("Music: no .wav files in ttclient/music/");
                        return;
                    }
                    int idx = Math.max(0, Math.min(customFiles.size() - 1, customIndex.getInt()));
                    Path file = customFiles.get(idx);
                    announce("Custom: " + file.getFileName());
                    playWavFile(file);
                } else {
                    announce(mode);
                    playBuiltin(mode);
                }
            } catch (Throwable t) {
                chat("Music error: " + t.getClass().getSimpleName());
                return;
            }
        } while (Boolean.TRUE.equals(loop.get()) && !stopFlag.get() && isEnabled());
    }

    private void announce(String name) {
        if (Boolean.TRUE.equals(announce.get())) chat("Now playing: " + name);
    }

    private void chat(String msg) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) return;
            Object player = mc.player;
            Object component = Class.forName("net.minecraft.network.chat.Component")
                    .getMethod("literal", String.class).invoke(null, msg);
            for (String name : new String[]{"displayClientMessage", "sendSystemMessage"}) {
                try {
                    for (var m : player.getClass().getMethods()) {
                        if (!m.getName().equals(name)) continue;
                        Class<?>[] pts = m.getParameterTypes();
                        if (pts.length == 2 && pts[1] == boolean.class) {
                            m.invoke(player, component, false);
                            return;
                        }
                        if (pts.length == 1) {
                            m.invoke(player, component);
                            return;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private void playWavFile(Path file) throws Exception {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file.toFile())) {
            AudioFormat base = in.getFormat();
            float rate = base.getSampleRate() <= 0 ? 44100f : base.getSampleRate();
            int ch = base.getChannels() <= 0 ? 2 : base.getChannels();
            AudioFormat target = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
            AudioInputStream din = AudioSystem.isConversionSupported(target, base)
                    ? AudioSystem.getAudioInputStream(target, in) : in;
            try (Clip clip = AudioSystem.getClip()) {
                clip.open(din);
                setClipVolume(clip, volume.getFloat());
                clip.start();
                while (clip.isRunning() && !stopFlag.get()) {
                    Thread.sleep(50);
                    setClipVolume(clip, volume.getFloat());
                }
                clip.stop();
            }
        }
    }

    private void setClipVolume(Clip clip, float vol) {
        try {
            FloatControl c = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = c.getMinimum();
            float max = Math.min(c.getMaximum(), 0f);
            float v = Math.max(0f, Math.min(1f, vol));
            c.setValue(min + (max - min) * v);
        } catch (Throwable ignored) {}
    }

    private void playBuiltin(String name) throws Exception {
        int[][] notes;
        switch (name) {
            case "Bright" -> notes = new int[][]{
                    {523, 180}, {659, 180}, {784, 180}, {1046, 320},
                    {784, 160}, {1046, 400}, {0, 120},
                    {659, 160}, {784, 160}, {880, 200}, {1046, 360}
            };
            case "Pulse" -> notes = new int[][]{
                    {110, 120}, {0, 40}, {110, 120}, {0, 40},
                    {130, 120}, {0, 40}, {110, 120}, {0, 40},
                    {165, 200}, {0, 80}, {110, 300}
            };
            case "Night" -> notes = new int[][]{
                    {196, 400}, {233, 400}, {261, 500}, {0, 150},
                    {233, 350}, {196, 450}, {174, 600}
            };
            default -> notes = new int[][]{
                    {261, 280}, {293, 280}, {329, 280}, {349, 360},
                    {329, 220}, {293, 280}, {261, 400}, {0, 100},
                    {293, 250}, {329, 250}, {392, 450}
            };
        }
        float vol = volume.getFloat();
        for (int[] n : notes) {
            if (stopFlag.get()) return;
            if (n[0] <= 0) Thread.sleep(n[1]);
            else playTone(n[0], n[1], vol);
        }
    }

    private static void playTone(int freq, int ms, float vol) throws LineUnavailableException, InterruptedException {
        float sampleRate = 22050f;
        int samples = (int) (sampleRate * ms / 1000f);
        byte[] buf = new byte[samples * 2];
        double amp = 0.25 * Math.max(0f, Math.min(1f, vol));
        for (int i = 0; i < samples; i++) {
            double t = i / sampleRate;
            double env = 1.0;
            int attack = Math.max(1, samples / 20);
            int release = Math.max(1, samples / 5);
            if (i < attack) env = i / (double) attack;
            if (i > samples - release) env = (samples - i) / (double) release;
            short s = (short) (Math.sin(2 * Math.PI * freq * t) * amp * env * 32767);
            buf[i * 2] = (byte) (s & 0xff);
            buf[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
            line.open(fmt);
            line.start();
            line.write(buf, 0, buf.length);
            line.drain();
        }
    }
}
