package com.berat.gpt;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Hikaye olaylari. Asama 1: 1-3. gun dost yapay zeka, 4. gun ince ses, 5. gun APISIZ yol, 6. gun orta ses.
 * Tum metinler Lang uzerinden gelir; dil degisince ilerleme sifirlanmaz (StoryData ayridir).
 */
@Mod.EventBusSubscriber(modid = MinecraftGpt.MODID)
public final class StoryEvents {
    private static final UUID SPEED_ID = UUID.fromString("7d6b1c52-3f0e-4a11-9a50-0c3a9f1e2b44");
    private static final String BLOCKS = "█▓▒░#@%&¤µ¶";

    private static final List<Runnable> DEFERRED = new ArrayList<>();
    private static double nightAcc = 0.0D;
    private static boolean autoValidateStarted = false;

    /** APISIZ yolun 5. gun sahnesi: -1 = kapali, aksi halde gecen tik. */
    private static int rageTick = -1;
    private static boolean rageResumeChecked = false;

    private StoryEvents() {}

    // ------------------------------------------------------------------ yardimcilar

    public static int dayOf(ServerLevel level) {
        return (int) (level.getDayTime() / 24000L) + 1;
    }

    private static boolean hasBit(int mask, int bit) {
        return (mask & bit) != 0;
    }

    public static void say(ServerPlayer p, String text) {
        int day = dayOf(p.serverLevel());
        ChatFormatting color = day <= 3 ? ChatFormatting.AQUA : (day <= 5 ? ChatFormatting.GRAY : ChatFormatting.RED);
        MutableComponent c = Component.literal("<" + Lang.get("chat.name") + "> ").withStyle(color);
        c.append(Component.literal(text).withStyle(ChatFormatting.WHITE));
        p.sendSystemMessage(c);
    }

    static void sayAll(MinecraftServer server, String text) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) say(p, text);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    static void sound(MinecraftServer server, String id, float volume) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CommandSourceStack src = p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            server.getCommands().performPrefixedCommand(src, "playsound " + id + " master @s ~ ~ ~ " + volume);
        }
    }

    static void title(ServerPlayer p, String title, String sub, int fadeIn, int stay, int fadeOut) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(ChatFormatting.RED)));
    }

    static void glitchOverlay(long ms, float intensity) {
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHooks.glitch(ms, intensity);
    }

    // ------------------------------------------------------------------ komut ve sohbet

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        LiteralCommandNode<CommandSourceStack> node = e.getDispatcher().register(
                Commands.literal("ai").then(
                        Commands.argument("mesaj", StringArgumentType.greedyString()).executes(ctx -> {
                            AiService.ask(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "mesaj"));
                            return 1;
                        })));
        e.getDispatcher().register(Commands.literal("gpt").redirect(node));
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent e) {
        String raw = e.getRawText();
        String low = raw.toLowerCase(Locale.ROOT);
        String rest = null;
        if (low.startsWith("!ai ")) rest = raw.substring(4);
        else if (low.startsWith("!gpt ")) rest = raw.substring(5);
        if (rest != null) AiService.ask(e.getPlayer(), rest);
    }

    // ------------------------------------------------------------------ ana dongu

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) return;
        if (!(e.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        MinecraftServer server = level.getServer();

        if (!DEFERRED.isEmpty()) {
            List<Runnable> copy = new ArrayList<>(DEFERRED);
            DEFERRED.clear();
            for (Runnable r : copy) r.run();
        }

        StoryData d = StoryData.get(level);
        long time = level.getDayTime();
        int day = (int) (time / 24000L) + 1;
        long tod = time % 24000L;
        boolean anyone = !server.getPlayerList().getPlayers().isEmpty();

        // Elle girilen anahtari oyun acilisinda bir kez arka planda dogrula
        if (!autoValidateStarted && GptConfig.hasKey() && !GptConfig.isValidated()) {
            autoValidateStarted = true;
            AiService.validateAsync(res -> server.execute(() -> {
                if (res.ok()) GptConfig.markValidated();
            }));
        }

        // Hikaye yolu: APISIZ -> API gecisi yalnizca anahtar DOGRULANINCA olur
        int want = GptConfig.isValidated() ? 2 : 1;
        if (d.route == 0) {
            d.route = want;
            d.setDirty();
        } else if (d.route == 1 && want == 2 && anyone) {
            d.route = 2;
            d.setDirty();
            sayAll(server, Lang.get("ai.route.api"));
        }

        // Yeni gun
        if (anyone && day > d.lastDay && tod >= 200L) {
            d.lastDay = day;
            d.setDirty();
            onNewDay(server, d, day);
        }

        // 4. gun: ince ses
        if (anyone && d.stage < 1 && day >= 4 && tod >= 13000L) {
            d.stage = 1;
            d.setDirty();
            sound(server, "korku:event.thin", 0.6F);
            sayAll(server, Lang.get("ai.stage1"));
            if (hasBit(d.ideasDone, 4)) delayedConsequence(server, level);
        }

        // 6. gun: orta ses
        else if (anyone && d.stage < 2 && day >= 6 && tod >= 13000L) {
            d.stage = 2;
            d.setDirty();
            sound(server, "korku:event.medium", 1.0F);
            sayAll(server, Lang.get("ai.stage2"));
        }

        // 10. gunun yarisi: en guclu ses, dunya kalici degisir (13. bolum)
        else if (anyone && d.stage < 3 && day >= 10 && tod >= 12000L) {
            ReturnEvent.start(server, level, d);
        }

        // Geceler yaklasik 7 dakika uzar
        if (d.stage >= 1 && tod >= 13000L && tod < 23000L
                && level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            nightAcc += 1.0D - (10000.0D / 18400.0D);
            if (nightAcc >= 1.0D) {
                nightAcc -= 1.0D;
                level.setDayTime(level.getDayTime() - 1L);
            }
        }

        // 5. gun APISIZ sahne
        if (anyone && d.route == 1 && rageTick < 0) {
            if (!hasBit(d.noApiDone, 1) && day >= 5 && tod >= 1000L) {
                d.noApiDone |= 1;
                d.setDirty();
                rageTick = 0;
            } else if (hasBit(d.noApiDone, 1) && !hasBit(d.noApiDone, 2) && !rageResumeChecked) {
                // Sahne yarida kesildiyse (oyuncu cikti): dogrudan kapanisa gec
                rageResumeChecked = true;
                rageTick = 300;
            }
        }
        ReturnEvent.tick(level, d);
        if (d.stage >= 3 && level.getGameTime() % 100L == 0L) ReturnEvent.spawnWave(level);
        tickRage(server, d);
    }

    private static void onNewDay(MinecraftServer server, StoryData d, int day) {
        if (day >= 1 && day <= 3) {
            int bit = 1 << (day - 1);
            if (!hasBit(d.ideasGiven, bit)) {
                d.ideasGiven |= bit;
                d.setDirty();
                sayAll(server, Lang.get("ai.idea" + day));
            }
        } else if (day == 4) {
            sayAll(server, Lang.get("ai.day4.dawn"));
        } else if (day >= 7) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.getRandom().nextInt(2) == 0) say(p, Lang.pick("ai.hostile", p.getRandom()));
            }
        }
    }

    /** 3. gundeki "yardimci" fikrin gec gelen sonucu: 4. gun gecesinde belli olur. */
    private static void delayedConsequence(MinecraftServer server, ServerLevel level) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 0, false, false));
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(p.getX() + 12.0D, p.getY(), p.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
    }

    // ------------------------------------------------------------------ APISIZ yol: 5. gun sahnesi

    private static Component glitchLine(RandomSource r) {
        List<String> words = Lang.list("ai.glitch.words");
        MutableComponent c = Component.literal("<" + Lang.get("chat.name") + "> ").withStyle(ChatFormatting.RED);
        int parts = 2 + r.nextInt(4);
        for (int i = 0; i < parts; i++) {
            String s;
            if (words.isEmpty() || r.nextInt(3) == 0) {
                StringBuilder sb = new StringBuilder();
                int n = 2 + r.nextInt(8);
                for (int k = 0; k < n; k++) sb.append(BLOCKS.charAt(r.nextInt(BLOCKS.length())));
                s = sb.toString();
            } else {
                s = words.get(r.nextInt(words.size()));
            }
            Style st = Style.EMPTY
                    .withColor(r.nextBoolean() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY)
                    .withObfuscated(r.nextInt(3) == 0);
            c.append(Component.literal(s + " ").withStyle(st));
        }
        return c;
    }

    private static void tickRage(MinecraftServer server, StoryData d) {
        if (rageTick < 0) return;
        rageTick++;
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (rageTick <= 300) {
            // Mesajlar giderek hizlanir; FPS dususu gercek degil, ekrandaki efektle taklit edilir
            int interval = Math.max(1, 16 - rageTick / 18);
            if (rageTick == 1) glitchOverlay(15_000L, 0.4F);
            if (rageTick == 150) glitchOverlay(8_000L, 0.8F);
            if (rageTick % interval == 0) {
                for (ServerPlayer p : players) p.sendSystemMessage(glitchLine(p.getRandom()));
            }
            if (rageTick % 60 == 0) {
                for (ServerPlayer p : players) {
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
                }
            }
        } else if (rageTick == 301) {
            glitchOverlay(6_000L, 1.0F);
            for (ServerPlayer p : players) title(p, Lang.get("event.noapi.final"), "", 5, 70, 10);
        } else if (rageTick == 391) {
            for (ServerPlayer p : players) {
                title(p, Lang.get("event.noapi.closing.title"), Lang.get("event.noapi.closing.sub"), 5, 60, 10);
            }
        } else if (rageTick == 461) {
            // Guvenli kapanis: once her seyi kaydet, sonra kapat
            d.noApiDone |= 2;
            d.setDirty();
            server.saveEverything(false, true, true);
        } else if (rageTick == 470) {
            rageTick = -1;
            closeGame(server);
        }
    }

    private static void closeGame(MinecraftServer server) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientHooks.closeGame();
        } else {
            for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
                p.connection.disconnect(Component.literal(Lang.get("ai.disconnect")));
            }
        }
    }

    // ------------------------------------------------------------------ 1-3. gun fikirleri

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        StoryData d = StoryData.get(level);
        BlockState placed = e.getPlacedBlock();
        BlockState below = level.getBlockState(e.getPos().below());

        // Fikir 1: calisma masasinin ustune mesale
        if (hasBit(d.ideasGiven, 1) && !hasBit(d.ideasDone, 1)
                && placed.is(Blocks.TORCH) && below.is(Blocks.CRAFTING_TABLE)) {
            d.ideasDone |= 1;
            d.setDirty();
            give(p, new ItemStack(Items.GOLDEN_APPLE, 2));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 1));
            say(p, Lang.get("ai.idea1.done"));
        }
        // Fikir 2: yatagin ustune hali
        else if (hasBit(d.ideasGiven, 2) && !hasBit(d.ideasDone, 2)
                && placed.is(BlockTags.WOOL_CARPETS) && below.is(BlockTags.BEDS)) {
            d.ideasDone |= 2;
            d.setDirty();
            give(p, new ItemStack(Items.DIAMOND, 2));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0));
            say(p, Lang.get("ai.idea2.done"));
        }
        // Fikir 3: firinin ustune oyulmus kabak (sonucu 4. gun gecesine kadar belli olmaz)
        else if (hasBit(d.ideasGiven, 4) && !hasBit(d.ideasDone, 4)
                && placed.is(Blocks.CARVED_PUMPKIN) && below.is(Blocks.FURNACE)) {
            d.ideasDone |= 4;
            d.setDirty();
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1));
            say(p, Lang.get("ai.idea3.done"));
        }
    }

    // ------------------------------------------------------------------ dusman canavarlar

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        if (!(e.getLevel() instanceof ServerLevel sl)) return;
        int stage = StoryData.get(sl).stage;
        if (stage < 1) return;
        if (e.getEntity() instanceof IronGolem golem) {
            if (stage >= 3) ReturnEvent.hostileGolem(golem);
            return;
        }
        if (!(e.getEntity() instanceof Monster mob)) return;
        if (mob instanceof Warden || mob instanceof WitherBoss) return;

        buff(mob, stage);

        if (stage >= 2 && !e.loadedFromDisk() && !mob.getTags().contains("gpt_extra")
                && (mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof Spider || mob instanceof Creeper)
                && sl.random.nextFloat() < (stage >= 3 ? 0.6F : 0.35F)) {
            DEFERRED.add(() -> spawnExtra(sl, mob));
        }
    }

    private static void buff(Monster mob, int stage) {
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(100.0D);

        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
            speed.addPermanentModifier(new AttributeModifier(SPEED_ID, "gpt_speed",
                    stage >= 3 ? 0.28D : (stage >= 2 ? 0.20D : 0.08D), AttributeModifier.Operation.MULTIPLY_BASE));
        }

        if (mob instanceof AbstractSkeleton sk) {
            boolean already = sk.goalSelector.getAvailableGoals().stream()
                    .anyMatch(w -> w.getGoal() instanceof LongRangeBowGoal<?>);
            if (!already) {
                sk.goalSelector.getAvailableGoals().removeIf(w -> w.getGoal() instanceof RangedBowAttackGoal<?>);
                sk.goalSelector.addGoal(4, new LongRangeBowGoal<>(sk, 1.0D, 20, stage >= 2 ? 100.0F : 50.0F));
            }
        }
    }

    private static void spawnExtra(ServerLevel sl, Monster src) {
        if (!src.isAlive()) return;
        if (sl.getEntitiesOfClass(Monster.class, src.getBoundingBox().inflate(24.0D)).size() > 25) return;
        Entity ent = src.getType().create(sl);
        if (ent instanceof Mob extra) {
            extra.moveTo(src.getX() + 1.0D, src.getY(), src.getZ(), src.getYRot(), 0.0F);
            extra.addTag("gpt_extra");
            extra.finalizeSpawn(sl, sl.getCurrentDifficultyAt(src.blockPosition()), MobSpawnType.EVENT, null, null);
            sl.addFreshEntity(extra);
        }
    }
}
