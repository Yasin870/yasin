package com.berat.gpt;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(MinecraftGpt.MODID)
public class MinecraftGpt {
    public static final String MODID = "minecraftgpt";

    public MinecraftGpt() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GptConfig.SPEC, "minecraftgpt.toml");
        Lang.reload();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.register();
        }
    }
}
