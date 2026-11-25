package com.berat.gpt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 13. Bolum: 10. gun. Dunya kalici olarak degisir.
 * En guclu ses, daha cok dusman, dusman demir golemler ve kesfedilecek bir yapi (icinde lanetli kitap).
 * Soil World henuz yok; bu olay su an gun sayisina baglidir. Soil World gelince donus bu sinifin start() metodunu cagiracak.
 */
final class ReturnEvent {
    private record BuildOp(int kind, BlockPos pos, BlockState state) {}

    private static final int SET = 0;
    private static final int SUPPORT = 2;
    private static final int CHEST = 3;
    private static final int PER_TICK = 220;

    private static final UUID SPEED_ID = UUID.fromString("7d6b1c52-3f0e-4a11-9a50-0c3a9f1e2b44");

    private static final EntityType<?>[] WAVE = {
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON, EntityType.STRAY,
            EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.WITCH,
            EntityType.ENDERMAN, EntityType.VINDICATOR, EntityType.PILLAGER, EntityType.SILVERFISH,
            EntityType.IRON_GOLEM, EntityType.IRON_GOLEM
    };

    private static List<BuildOp> queue = null;
    private static int index = 0;

    private ReturnEvent() {}

    // ------------------------------------------------------------------ baslangic

    static void start(MinecraftServer server, ServerLevel level, StoryData d) {
        d.stage = 3;
        d.setDirty();
        StoryEvents.sound(server, "korku:event.thick", 1.0F);
        StoryEvents.glitchOverlay(4000L, 0.7F);

        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ServerPlayer p : players) {
            StoryEvents.title(p, Lang.get("event.day10.title"), Lang.get("event.day10.sub"), 10, 80, 20);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0, false, false));
        }
        StoryEvents.sayAll(server, Lang.get("ai.stage3"));

        if (!players.isEmpty() && d.structState == 0) {
            BlockPos from = players.get(0).blockPosition();
            BlockPos site = pickSite(level, from);
            d.structX = site.getX();
            d.structY = site.getY();
            d.structZ = site.getZ();
            d.structState = 1;
            d.setDirty();
            StoryEvents.sayAll(server, hint(from, site));
        }
    }

    private static String hint(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "dir.e" : "dir.w") : (dz > 0 ? "dir.s" : "dir.n");
        int dist = (int) (Math.round(Math.sqrt((double) dx * dx + (double) dz * dz) / 10.0D) * 10L);
        return Lang.get("ai.structure.hint", Lang.get(dir), String.valueOf(dist));
    }

    /** Yapi icin yer secer: once yuklu bolgede (70-120 blok), olmazsa daha yakinda. */
    private static BlockPos pickSite(ServerLevel level, BlockPos origin) {
        RandomSource r = level.random;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < 30; i++) {
                double ang = r.nextDouble() * Math.PI * 2.0D;
                int dist = pass == 0 ? 70 + r.nextInt(50) : 55 + r.nextInt(25);
                int x = origin.getX() + (int) Math.round(Math.cos(ang) * dist);
                int z = origin.getZ() + (int) Math.round(Math.sin(ang) * dist);
                if (pass == 0 && !level.isLoaded(new BlockPos(x, origin.getY(), z))) continue;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (y < level.getMinBuildHeight() + 8 || y > level.getMaxBuildHeight() - 40) continue;
                if (!level.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()) continue;
                return new BlockPos(x, y, z);
            }
        }
        return new BlockPos(origin.getX() + 60, origin.getY(), origin.getZ());
    }

    // ------------------------------------------------------------------ her tik

    static void tick(ServerLevel level, StoryData d) {
        if (d.stage < 3 || d.structState == 0) return;

        if (d.structState == 1) {
            // Yari kalan insa (oyun kapandiysa) bastan, ayni yerde yapilir
            if (queue == null) {
                queue = buildQueue(d);
                index = 0;
            }
            int n = 0;
            while (index < queue.size() && n < PER_TICK) {
                run(level, queue.get(index++));
                n++;
            }
            if (index >= queue.size()) {
                queue = null;
                d.structState = 2;
                d.setDirty();
            }
        }

        // Yapinin ustunde hafif mor parcaciklar (yalnizca yakindaki oyunculara gonderilir)
        if (level.getGameTime() % 20L == 0L) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, d.structX + 0.5D, d.structY + 16.0D, d.structZ + 0.5D,
                    14, 1.5D, 3.0D, 1.5D, 0.05D);
        }
    }

    // ------------------------------------------------------------------ yapi

    private static List<BuildOp> buildQueue(StoryData d) {
        List<BuildOp> ops = new ArrayList<>(8000);
        final int cx = d.structX;
        final int cy = d.structY;
        final int cz = d.structZ;

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState black = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState purple = Blocks.PURPLE_CONCRETE.defaultBlockState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        BlockState glass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();

        // 1) Alani temizle (agaclar, tepeler)
        for (int dy = 0; dy <= 23; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    ops.add(new BuildOp(SET, new BlockPos(cx + dx, cy + dy, cz + dz), air));
                }
            }
        }

        // 2) Platform: siyah beton, mor "devre" cizgileri, koselerde aglayan obsidyen; altina destek
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                int ax = Math.abs(dx);
                int az = Math.abs(dz);
                BlockState s = black;
                if (ax == 7 || az == 7) s = polished;
                if (dx == 0 || dz == 0) s = purple;
                if (ax == az && ax >= 3) s = purple;
                if (ax == 7 && az == 7) s = crying;
                BlockPos floor = new BlockPos(cx + dx, cy - 1, cz + dz);
                ops.add(new BuildOp(SET, floor, s));
                ops.add(new BuildOp(SUPPORT, floor, null));
            }
        }

        // 3) Kule: 5x5 govde, 12 blok yuksek, mor seritler, isik veren obsidyen
        for (int dy = 0; dy <= 11; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean ring = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    if (!ring) continue;
                    boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    BlockState s = black;
                    if (corner || dy == 4 || dy == 8) s = purple;
                    if (dy == 2 && ((Math.abs(dx) == 2 && dz == 0) || (dx == 0 && dz == -2))) s = crying;
                    ops.add(new BuildOp(SET, new BlockPos(cx + dx, cy + dy, cz + dz), s));
                }
            }
        }
        // Kapi (guney yuz, 2 blok yuksek)
        ops.add(new BuildOp(SET, new BlockPos(cx, cy, cz + 2), air));
        ops.add(new BuildOp(SET, new BlockPos(cx, cy + 1, cz + 2), air));

        // 4) Cati ("islemci"): 7x7 siyah plaka, 5x5 cilali plaka, ustte 3x3x3 mor cam cekirdek
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                ops.add(new BuildOp(SET, new BlockPos(cx + dx, cy + 12, cz + dz), black));
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ops.add(new BuildOp(SET, new BlockPos(cx + dx, cy + 13, cz + dz), polished));
            }
        }
        for (int dy = 14; dy <= 16; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean center = dx == 0 && dz == 0 && dy == 15;
                    ops.add(new BuildOp(SET, new BlockPos(cx + dx, cy + dy, cz + dz), center ? crying : glass));
                }
            }
        }
        // Uzaktan gorunen ince kule ucu
        for (int dy = 17; dy <= 21; dy++) {
            ops.add(new BuildOp(SET, new BlockPos(cx, cy + dy, cz), black));
        }
        ops.add(new BuildOp(SET, new BlockPos(cx, cy + 22, cz), crying));

        // 5) Kose sutunlari (anten)
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                for (int dy = 0; dy <= 6; dy++) {
                    ops.add(new BuildOp(SET, new BlockPos(cx + 7 * sx, cy + dy, cz + 7 * sz), black));
                }
                ops.add(new BuildOp(SET, new BlockPos(cx + 7 * sx, cy + 7, cz + 7 * sz), crying));
            }
        }

        // 6) Kulenin icinde sandik: lanetli kitap burada (kapiya donuk)
        ops.add(new BuildOp(CHEST, new BlockPos(cx, cy, cz),
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH)));
        return ops;
    }

    private static void run(ServerLevel level, BuildOp op) {
        switch (op.kind()) {
            case SET -> {
                if (!level.getBlockState(op.pos()).equals(op.state())) {
                    level.setBlock(op.pos(), op.state(), 2);
                }
            }
            case SUPPORT -> {
                // Platformun altindaki bosluklari siyah tasla doldur (havada asili kalmasin)
                for (int i = 1; i <= 12; i++) {
                    BlockPos q = op.pos().below(i);
                    BlockState bs = level.getBlockState(q);
                    if (bs.isAir() || !bs.getFluidState().isEmpty() || bs.canBeReplaced()) {
                        level.setBlock(q, Blocks.BLACKSTONE.defaultBlockState(), 2);
                    } else {
                        break;
                    }
                }
            }
            case CHEST -> {
                level.setBlock(op.pos(), op.state(), 2);
                BlockEntity be = level.getBlockEntity(op.pos());
                if (be instanceof ChestBlockEntity chest) {
                    chest.setItem(13, makeBook());
                    chest.setChanged();
                }
            }
            default -> { }
        }
    }

    /** Lanetli kitap: ismi okunamayacak kadar glitchli. Kullanim mantigi 14. bolumde eklenecek. */
    private static ItemStack makeBook() {
        ItemStack b = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = b.getOrCreateTag();
        tag.putString("title", "???");
        tag.putString("author", Lang.get("chat.name"));
        ListTag pages = new ListTag();
        for (String key : new String[] {"book.page1", "book.page2"}) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(Lang.get(key)))));
        }
        tag.put("pages", pages);
        tag.putBoolean("gpt_cursed_book", true);
        b.setHoverName(Component.literal(Lang.get("book.name"))
                .withStyle(Style.EMPTY.withObfuscated(true).withColor(ChatFormatting.DARK_PURPLE)));
        return b;
    }

    // ------------------------------------------------------------------ dusmanlar

    /** Demir golem oyuncuyu hedefler; kendi yaptigin golemler de saldirir. */
    static void hostileGolem(IronGolem g) {
        g.setPlayerCreated(false);
        boolean has = g.targetSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof GolemTargetGoal);
        if (!has) g.targetSelector.addGoal(2, new GolemTargetGoal(g));
        AttributeInstance follow = g.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(64.0D);
        AttributeInstance speed = g.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_ID);
            speed.addPermanentModifier(new AttributeModifier(SPEED_ID, "gpt_speed", 0.25D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    /** Oyuncularin etrafinda surekli dusman dalgasi: golem dahil her dusman, gece gunduz. */
    static void spawnWave(ServerLevel level) {
        RandomSource r = level.random;
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            int near = level.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(48.0D)).size();
            if (near >= 28) continue;
            int tries = 1 + r.nextInt(2);
            for (int i = 0; i < tries; i++) spawnOne(level, p, r);
        }
    }

    private static void spawnOne(ServerLevel level, ServerPlayer p, RandomSource r) {
        double ang = r.nextDouble() * Math.PI * 2.0D;
        int dist = 16 + r.nextInt(18);
        int x = Mth.floor(p.getX() + Math.cos(ang) * dist);
        int z = Mth.floor(p.getZ() + Math.sin(ang) * dist);
        if (!level.isLoaded(new BlockPos(x, p.getBlockY(), z))) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        if (!level.getBlockState(pos).isAir()) return;
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return;

        EntityType<?> type = WAVE[r.nextInt(WAVE.length)];
        Entity ent = type.create(level);
        if (!(ent instanceof Mob mob)) return;
        mob.moveTo(x + 0.5D, y, z + 0.5D, r.nextFloat() * 360.0F, 0.0F);
        mob.addTag("gpt_extra");
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 72000, 0, false, false));
        mob.setTarget(p);
        level.addFreshEntity(mob);
    }
}
