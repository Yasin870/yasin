package com.custommenu.core;
import java.io.File;
import java.util.*;
public class MediaScanner {
    /** Ekranın klasöründeki dosyaları bulur. Klasör yoksa boş liste döner. */
    public static List<File> scan(File folder) {
        List<File> out = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) return out;
        for (File f : files)
            if (f.isFile() && MediaKind.detect(f.getName()) != MediaKind.UNKNOWN) out.add(f);
        out.sort(Comparator.comparing(File::getName));
        return out;
    }
    /** Dosyayı Custom Menu klasörüne kopyalar; orijinale dokunmaz. */
    public static File copyInto(File src, File folder) throws java.io.IOException {
        folder.mkdirs();
        File dst = new File(folder, src.getName());
        java.nio.file.Files.copy(src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return dst;
    }
}
