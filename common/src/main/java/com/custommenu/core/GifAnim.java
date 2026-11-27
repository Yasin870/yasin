package com.custommenu.core;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

/** GIF'i kare kare okur. Minecraft'a bağımlı değil. Pikseller ABGR (NativeImage formatı). */
public class GifAnim {
    public int w, h;
    public int[][] frames;
    public long[] delays;      // ms
    public long total;

    public static GifAnim load(File f, int maxWidth, long byteBudget) throws Exception {
        try (ImageInputStream in = ImageIO.createImageInputStream(f)) {
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("gif");
            if (!it.hasNext()) throw new IllegalStateException("GIF okuyucu yok");
            ImageReader r = it.next();
            r.setInput(in, false);
            int n = r.getNumImages(true);
            int W = 0, H = 0;
            try {
                IIOMetadataNode sn = (IIOMetadataNode) r.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                IIOMetadataNode lsd = child(sn, "LogicalScreenDescriptor");
                if (lsd != null) {
                    W = Integer.parseInt(lsd.getAttribute("logicalScreenWidth"));
                    H = Integer.parseInt(lsd.getAttribute("logicalScreenHeight"));
                }
            } catch (Throwable ignored) { }
            if (W <= 0 || H <= 0) { BufferedImage b0 = r.read(0); W = b0.getWidth(); H = b0.getHeight(); }

            float scale = Math.min(1f, (float) maxWidth / W);
            int ow = Math.max(1, Math.round(W * scale)), oh = Math.max(1, Math.round(H * scale));
            int maxFrames = (int) Math.max(2, byteBudget / ((long) ow * oh * 4));
            n = Math.min(n, maxFrames);

            GifAnim a = new GifAnim();
            a.w = ow; a.h = oh; a.frames = new int[n][]; a.delays = new long[n];

            BufferedImage canvas = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            BufferedImage snapshot = null;
            String lastDisposal = "none"; Rectangle lastRect = new Rectangle(0, 0, W, H);

            for (int i = 0; i < n; i++) {
                BufferedImage fr = r.read(i);
                IIOMetadata md = r.getImageMetadata(i);
                IIOMetadataNode root = (IIOMetadataNode) md.getAsTree("javax_imageio_gif_image_1.0");
                IIOMetadataNode desc = child(root, "ImageDescriptor"), gce = child(root, "GraphicControlExtension");
                int left = desc == null ? 0 : Integer.parseInt(desc.getAttribute("imageLeftPosition"));
                int top = desc == null ? 0 : Integer.parseInt(desc.getAttribute("imageTopPosition"));
                long delay = 100; String disposal = "none";
                if (gce != null) {
                    delay = Integer.parseInt(gce.getAttribute("delayTime")) * 10L;
                    disposal = gce.getAttribute("disposalMethod");
                }
                if (delay < 20) delay = 100;

                Graphics2D g = canvas.createGraphics();
                if (lastDisposal.equals("restoreToBackgroundColor")) {
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(lastRect.x, lastRect.y, lastRect.width, lastRect.height);
                    g.setComposite(AlphaComposite.SrcOver);
                } else if (lastDisposal.equals("restoreToPrevious") && snapshot != null) {
                    g.setComposite(AlphaComposite.Src);
                    g.drawImage(snapshot, 0, 0, null);
                    g.setComposite(AlphaComposite.SrcOver);
                }
                if (disposal.equals("restoreToPrevious")) {
                    snapshot = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D sg = snapshot.createGraphics(); sg.drawImage(canvas, 0, 0, null); sg.dispose();
                }
                g.drawImage(fr, left, top, null);
                g.dispose();
                lastDisposal = disposal; lastRect = new Rectangle(left, top, fr.getWidth(), fr.getHeight());

                BufferedImage out = canvas;
                if (ow != W || oh != H) {
                    out = new BufferedImage(ow, oh, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D og = out.createGraphics();
                    og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    og.drawImage(canvas, 0, 0, ow, oh, null); og.dispose();
                }
                int[] px = out.getRGB(0, 0, ow, oh, null, 0, ow);
                for (int k = 0; k < px.length; k++) {
                    int p = px[k];
                    px[k] = (p & 0xFF000000) | ((p & 0xFF) << 16) | (p & 0xFF00) | ((p >> 16) & 0xFF);
                }
                a.frames[i] = px; a.delays[i] = delay; a.total += delay;
            }
            return a;
        }
    }

    /** Verilen zamana (ms) denk gelen kare. */
    public int frameAt(long ms, boolean loop) {
        if (frames.length <= 1 || total <= 0) return 0;
        long t = loop ? ms % total : Math.min(ms, total - 1);
        for (int i = 0; i < delays.length; i++) { if (t < delays[i]) return i; t -= delays[i]; }
        return frames.length - 1;
    }

    private static IIOMetadataNode child(IIOMetadataNode p, String name) {
        for (org.w3c.dom.Node n = p.getFirstChild(); n != null; n = n.getNextSibling())
            if (n.getNodeName().equals(name)) return (IIOMetadataNode) n;
        return null;
    }
}
