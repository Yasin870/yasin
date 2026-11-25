package com.berat.gpt;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/** 13. bolum: demir golemlerin oyuncuyu dusman olarak hedeflemesi. */
public class GolemTargetGoal extends NearestAttackableTargetGoal<Player> {
    public GolemTargetGoal(Mob mob) {
        super(mob, Player.class, false);
    }
}
