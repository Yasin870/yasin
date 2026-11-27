package dev.droidfps;

import dev.droidfps.adaptive.AdaptiveController;
import dev.droidfps.config.DroidConfig;
import dev.droidfps.util.AndroidEnv;
import dev.droidfps.util.ThermalProbe;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DroidFps implements ClientModInitializer {
    public static final String MOD_ID = "droidfps";
    public static final Logger LOG = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOG.info("DroidFPS baslatiliyor! Android Ortami: {}, Renderer: {}", AndroidEnv.isAndroid(), AndroidEnv.renderer());
        DroidConfig.load();
        ThermalProbe.init();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> AdaptiveController.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AdaptiveController.reset());
        ClientTickEvents.END_CLIENT_TICK.register(AdaptiveController::tick);
    }
}
