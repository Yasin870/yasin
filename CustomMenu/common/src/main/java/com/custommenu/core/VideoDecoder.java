package com.custommenu.core;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/** MP4'ü (H.264) arka planda kare kare çözer. Minecraft'a bağımlı değil. Sadece son kareyi tutar. Ses çalmaz. */
public class VideoDecoder implements Runnable {
    public static final class Frame {
        public final int w, h; public final int[] abgr;
        Frame(int w, int h, int[] p) { this.w = w; this.h = h; this.abgr = p; }
    }
    public final AtomicReference<Frame> latest = new AtomicReference<>();
    public volatile boolean finished, failed;
    public volatile String error = "";
    private volatile boolean stop;
    private final File file; private final boolean loop; private final int maxW;

    public VideoDecoder(File file, boolean loop, int maxW) { this.file = file; this.loop = loop; this.maxW = maxW; }
    public void stop() { stop = true; }

    @Override public void run() {
        try {
            do {
                try (FileChannelWrapper ch = NIOUtils.readableChannel(file)) {
                    FrameGrab grab = FrameGrab.createFrameGrab(ch);
                    double fps = 25;
                    try {
                        DemuxerTrackMeta m = grab.getVideoTrack().getMeta();
                        if (m.getTotalDuration() > 0 && m.getTotalFrames() > 0) fps = m.getTotalFrames() / m.getTotalDuration();
                    } catch (Throwable ignored) { }
                    if (fps < 1 || fps > 120) fps = 25;
                    long start = System.nanoTime(); long n = 0; Picture pic; boolean any = false;
                    while (!stop && (pic = grab.getNativeFrame()) != null) {
                        any = true;
                        latest.set(convert(AWTUtil.toBufferedImage(pic)));
                        n++;
                        long wait = (start + (long) (n * 1e9 / fps) - System.nanoTime()) / 1_000_000L;
                        if (wait > 0) Thread.sleep(wait);
                    }
                    if (!any) throw new IllegalStateException("Video karesi okunamadı (desteklenmeyen kodek?)");
                }
            } while (loop && !stop);
            finished = true;
        } catch (Throwable t) {
            failed = true; error = String.valueOf(t);
        }
    }

    private Frame convert(BufferedImage bi) {
        int W = bi.getWidth(), H = bi.getHeight();
        float sc = Math.min(1f, (float) maxW / W);
        int ow = Math.max(1, Math.round(W * sc)), oh = Math.max(1, Math.round(H * sc));
        BufferedImage out = bi;
        if (ow != W || oh != H) {
            out = new BufferedImage(ow, oh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(bi, 0, 0, ow, oh, null); g.dispose();
        }
        int[] px = out.getRGB(0, 0, ow, oh, null, 0, ow);
        for (int k = 0; k < px.length; k++) {
            int p = px[k];
            px[k] = 0xFF000000 | ((p & 0xFF) << 16) | (p & 0xFF00) | ((p >> 16) & 0xFF);
        }
        return new Frame(ow, oh, px);
    }
}
