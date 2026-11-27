package com.custommenu.forge;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicReference;

/** MP3'ü JLayer ile PCM'e çevirir, Minecraft'ın OpenAL'ında çalar. Ana (render) thread'den çağrılmalı. */
public final class AudioPlayer {
    private static final int MAX_SHORTS = 24_000_000; // ~3-4 dk stereo; fazlası kesilir

    private static final class Decoded {
        final String key; final short[] pcm; final int rate, ch;
        Decoded(String k, short[] p, int r, int c) { key = k; pcm = p; rate = r; ch = c; }
    }
    private static final AtomicReference<Decoded> ready = new AtomicReference<>();
    private static volatile int generation = 0;
    private static String key = "";
    private static int source = 0, buffer = 0;

    private AudioPlayer() { }

    /** path boşsa durdurur. Aynı yol tekrar gelirse sadece ses seviyesini günceller. */
    public static void update(String path, float volume, boolean loop) {
        try {
            if (path.isEmpty()) { if (!key.isEmpty()) stop(); return; }
            if (!path.equals(key)) { stop(); key = path; startDecode(path); }
            if (source != 0) {
                AL10.alSourcef(source, AL10.AL_GAIN, volume);
                AL10.alSourcei(source, AL10.AL_LOOPING, loop ? 1 : 0);
            } else {
                Decoded d = ready.get();
                if (d != null && d.key.equals(key)) play(d, volume, loop);
            }
        } catch (Throwable t) { key = ""; }
    }

    private static void play(Decoded d, float volume, boolean loop) {
        ShortBuffer sb = BufferUtils.createShortBuffer(d.pcm.length);
        sb.put(d.pcm).flip();
        buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, d.ch == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, sb, d.rate);
        source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, 1);
        AL10.alSourcef(source, AL10.AL_GAIN, volume);
        AL10.alSourcei(source, AL10.AL_LOOPING, loop ? 1 : 0);
        AL10.alSourcePlay(source);
    }

    public static void stop() {
        generation++;
        ready.set(null);
        key = "";
        try {
            if (source != 0) { AL10.alSourceStop(source); AL10.alDeleteSources(source); }
            if (buffer != 0) AL10.alDeleteBuffers(buffer);
        } catch (Throwable ignored) { }
        source = 0; buffer = 0;
    }

    private static void startDecode(String path) {
        final int gen = generation; final String k = path;
        Thread t = new Thread(() -> {
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
                Bitstream bs = new Bitstream(in);
                Decoder dec = new Decoder();
                short[] out = new short[1 << 20]; int len = 0; Header h;
                while ((h = bs.readFrame()) != null && gen == generation && len < MAX_SHORTS) {
                    SampleBuffer sb = (SampleBuffer) dec.decodeFrame(h, bs);
                    int n = sb.getBufferLength();
                    if (len + n > out.length) out = java.util.Arrays.copyOf(out, Math.max(out.length * 2, len + n));
                    System.arraycopy(sb.getBuffer(), 0, out, len, n); len += n;
                    bs.closeFrame();
                }
                if (gen == generation && len > 0)
                    ready.set(new Decoded(k, java.util.Arrays.copyOf(out, len), dec.getOutputFrequency(), dec.getOutputChannels()));
            } catch (Throwable ignored) { /* bozuk MP3 oyunu çökertmez */ }
        }, "CustomMenu-Mp3");
        t.setDaemon(true); t.start();
    }
}
