package com.berat.gpt;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** Mod listesindeki "Config" dugmesi icin ayar ekranini kaydeder. */
public final class ClientInit {
    private ClientInit() {}

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new GptConfigScreen(parent)));
    }
}
