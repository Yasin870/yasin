package dev.droidfps.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Batarya sicakligini okumaya calisir (en iyi caba). Okunamazsa NaN doner ve kullanilmaz. */
public final class ThermalProbe {
    private ThermalProbe() {}

    private static Path path;

    public static void init() {
        Path battery = Path.of("/sys/class/power_supply/battery/temp");
        if (Files.isReadable(battery)) { path = battery; return; }
        try (var zones = Files.newDirectoryStream(Path.of("/sys/class/thermal"), "thermal_zone*")) {
            for (Path z : zones) {
                Path type = z.resolve("type");
                Path temp = z.resolve("temp");
                if (Files.isReadable(type) && Files.isReadable(temp)
                        && Files.readString(type).trim().toLowerCase().contains("battery")) {
                    path = temp;
                    return;
                }
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    public static double readCelsius() {
        if (path == null) return Double.NaN;
        try {
            double v = Double.parseDouble(Files.readString(path).trim());
            if (v > 1000) return v / 1000.0; // milli-derece
            if (v > 100) return v / 10.0;    // onda bir derece
            return v;
        } catch (IOException | RuntimeException e) {
            path = null;
            return Double.NaN;
        }
    }
}
