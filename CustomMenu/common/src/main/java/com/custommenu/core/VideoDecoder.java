package com.custommenu.core;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/** MP4'ü (H.264) arka planda çözer. Orijinal çözünürlük ve FPS korunur. Ses çalmaz. */
public class VideoDecoder implements Runnable {
    public static final class Frame {
        public final int w, h; public final int[] abgr;
        Frame(int w, int h, int[] p) { this.w = w; this.h = h; this.abgr = p; }
    }
    public final AtomicReference<Frame> latest = new AtomicReference<>();
    public volatile boolean finished, failed;
    public volatile String error = "";
    public volatile double fpsInfo = 0;
    private volatile boolean stop;
    private final File file; private final boolean loop; private final int maxW; // maxW <= 0: küçültme yok

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
                    fpsInfo = fps;
                    long frameNs = (long) (1e9 / fps);
                    long start = System.nanoTime(), lastShown = 0; long n = 0; Picture pic; boolean any = false;
                    while (!stop && (pic = grab.getNativeFrame()) != null) {
                        any = true; n++;
                        long due = start + n * frameNs, now = System.nanoTime();
                        boolean late = now - due > frameNs;
                        // geride kaldıysak dönüştürmeyi atla (çözme zaten gerekli); en az ~8 kare/sn göster
                        if (!late || now - lastShown > 120_000_000L) {
                            latest.set(convert(AWTUtil.toBufferedImage(pic)));
                            lastShown = System.nanoTime();
                            long wait = (due - lastShown) / 1_000_000L;
                            if (wait > 0) Thread.sleep(wait);
                        }
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
        boolean scale = maxW > 0 && W > maxW;
        if (!scale && bi.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            byte[] d = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            if (d.length == W * H * 3) { // hızlı yol: ham B,G,R baytları
                int[] px = new int[W * H];
                for (int i = 0, k = 0; i < px.length; i++, k += 3)
                    px[i] = 0xFF000000 | ((d[k] & 0xFF) << 16) | ((d[k + 1] & 0xFF) << 8) | (d[k + 2] & 0xFF);
                return new Frame(W, H, px);
            }
        }
        float sc = scale ? (float) maxW / W : 1f;
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
