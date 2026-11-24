package com.berat.gpt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Dunyaya kayitli hikaye ilerlemesi. Dil degisince bunlar sifirlanmaz. */
public class StoryData extends SavedData {
    /** 0 = normal, 1 = 4. gun sesinden sonra, 2 = 6. gun sesinden sonra. */
    public int stage;
    public int lastDay;
    /** 0 = belirsiz, 1 = APISIZ yol, 2 = API'li yol. */
    public int route;
    /** APISIZ yolun olaylari (bit 1 = 5. gun basladi, bit 2 = kapanis tamam). */
    public int noApiDone;
    /** API'li yolun olaylari (ayri tutulur, sonraki asamalar icin). */
    public int apiDone;
    /** Fikir bitleri: 1, 2, 4 = 1., 2., 3. fikir. */
    public int ideasGiven;
    public int ideasDone;

    public static StoryData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(StoryData::load, StoryData::new, "minecraftgpt_story");
    }

    public static StoryData load(CompoundTag t) {
        StoryData d = new StoryData();
        d.stage = t.getInt("stage");
        d.lastDay = t.getInt("lastDay");
        d.route = t.getInt("route");
        d.noApiDone = t.getInt("noApiDone");
        d.apiDone = t.getInt("apiDone");
        d.ideasGiven = t.getInt("ideasGiven");
        d.ideasDone = t.getInt("ideasDone");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putInt("stage", stage);
        t.putInt("lastDay", lastDay);
        t.putInt("route", route);
        t.putInt("noApiDone", noApiDone);
        t.putInt("apiDone", apiDone);
        t.putInt("ideasGiven", ideasGiven);
        t.putInt("ideasDone", ideasDone);
        return t;
    }
}
