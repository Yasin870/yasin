package com.berat.gpt;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/** Kaynak yenilenince (F3+T) Minecraft GPT dil dosyalarini da yeniler. */
@Mod.EventBusSubscriber(modid = MinecraftGpt.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((ResourceManagerReloadListener) manager -> Lang.reload());
    }
}
