package com.brandon3055.brandonscore.handlers.contributor;

import codechicken.lib.math.MathHelper;
import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.client.ClientOnly;
import com.brandon3055.brandonscore.handlers.contributor.ContributorConfig.WingBehavior;
import com.brandon3055.brandonscore.utils.Utils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Created by brandon3055 on 23/11/2022
 */
public class Animations {
    private ContributorProperties props;
    //Colours (can not use last tick vals because of rollover)
    private float wingBoneCol = 0;
    private float wingBoneColSpeed = 0;
    private float wingWebCol = 0;
    private float wingWebColSpeed = 0;
    private float shieldCol = 0;
    private float shieldColSpeed = 0;

    private float wingExt = 0;
    private float lastWingExt = 0;
    private float targetWingExt = 0;
    private float extAprSpeed = 0;

    private float wingFold = 0;
    private float lastWingFold = 0;
    private float targetWingFold = 0;
    private float foldAprSpeed = 0;

    private float wingFlap = 0;
    private float wingFlap2 = 0;
    private float lastWingFlap = 0;
    private float lastWingFlap2 = 0;
    private float targetWingFlap = 0;
    private float targetWingFlap2 = 0;
    private float flapAprSpeed = 0;
    private float flap2AprSpeed = 0;
    private float flapDir = 0; //1 up, -1 down, 0 neutral
    private float lastVelocity = 0;
    private float flapSpeed = 0;

    private float wingPitch = 0;
    private float lastWingPitch = 0;
    private float targetWingPitch = 0;
    private float pitchAprSpeed = 0;

    //If > 0 overrides all other logic to do the hide animation
    private boolean hideWings = true;
    private float hideAnim = 2;

    private int playerExpiry = 0;
    private Player player = null;

    public Animations(ContributorProperties props) {
        this.props = props;
    }

    public void setPlayer(Player player) {
        this.player = player;
        playerExpiry = 0;
    }

    @OnlyIn (Dist.CLIENT)
    public void tick() {
        Boolean paused = Utils.unsafeCallWhenOn(Dist.CLIENT, () -> ClientOnly::isClientPaused);
        if (paused != null && paused) return;
        ContributorConfig config = props.getConfig();
        if (config.getWingRGBBoneColour()) {
            float s = ContributorConfig.unpack(config.getWingsOverrideBoneColour())[0];
            wingBoneColSpeed = s * s * 0.05f;
            wingBoneCol += wingBoneColSpeed;
            wingBoneCol %= 1;
        }
        if (config.getWingRGBWebColour()) {
            float s = ContributorConfig.unpack(config.getWingsOverrideWebColour())[0];
            wingWebColSpeed = s * s * 0.05F;
            wingWebCol += wingWebColSpeed;
            wingWebCol %= 1;
        }
        if (config.getShieldRGB()) {
            float s = ContributorConfig.unpack(config.getShieldOverride())[0];
            shieldColSpeed = s * s * 0.05F;
            shieldCol += shieldColSpeed;
            shieldCol %= 1;
        }

        if (props.hasWings() && player != null) {
            tickWings(player, config);
        }

        if (player != null && playerExpiry++ > 20*60) {
            player = null;
        }
    }

    private void tickWings(Player player, ContributorConfig config) {
        lastWingExt = wingExt;
        lastWingFold = wingFold;
        lastWingFlap = wingFlap;
        lastWingFlap2 = wingFlap2;
        lastWingPitch = wingPitch;
        extAprSpeed = foldAprSpeed = pitchAprSpeed = 0.1F;
        flapAprSpeed = 0.05F;
        flap2AprSpeed = 0.1F;
        targetWingExt = targetWingFold = 0;
        hideWings = false;
        float hideSpeed = 0.1F;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasElytra = chest.getItem().canElytraFly(chest, player);
        if (!hasElytra && BrandonsCore.equipmentManager != null) {
            hasElytra = !BrandonsCore.equipmentManager.findMatchingItem(e -> e.getItem().canElytraFly(e, player), player).isEmpty();
        }

        if (config.getWingsTier() == null || (hasElytra && config.getWingsElytra() == ContributorConfig.WingElytraCompat.HIDE_WINGS)) {
            hideWings = true;
        }
        else if (!hasElytra && config.getWingsElytra() == ContributorConfig.WingElytraCompat.REPLACE) {
            hideWings = true;
        }
        //Elytra flight overrides all other wing behavior logic.
        else if (player.isFallFlying()) {
            //Retract and fold wings when diving.
            float diveInv = 1.0F; //0 means full speed dive, 1 means no dive
            Vec3 speed = player.getDeltaMovement();
            float velocity = (float) speed.length();
            float velDelta = velocity - lastVelocity;
            lastVelocity = velocity;
            if (speed.y < 0.0D) {
                Vec3 direction = speed.normalize();
                diveInv = 1.0F - (float) Math.pow(-direction.y, 1.5D);
            }
            float dive = 1 - diveInv;
            targetWingExt = diveInv;
            targetWingFold = 1 - (dive * 0.5F);

            //Wing Flap (To put it simply, Wings flap when accelerating against gravity)
            float vertical = (float) speed.y + 0.1F; //Because you need energy to maintain 0 altitude change
            //If positive then player is being boosted by something like a firework or DE flight
            float newFlapSpeed = MathHelper.clip((MathHelper.clip(velDelta, 0, 1) - MathHelper.clip(-vertical, 0, 1)) * 10, 0, 0.1F);
            if (newFlapSpeed > flapSpeed) flapSpeed = newFlapSpeed;
            else flapSpeed = MathHelper.approachLinear(flapSpeed, newFlapSpeed, 0.005F);
            doFlapAnimation(flapSpeed, 4, 2, 0.25F, -0.5F, 2.5F);
            lastVelocity = velocity;
            hideSpeed = 0.25F;
        } else {
            if (player.getAbilities().flying) {
                doWingBehavior(config.getWingsCreative());
            } else{
                doWingBehavior(config.getWingsGround());
            }
        }

        wingExt = MathHelper.approachLinear(wingExt, targetWingExt, extAprSpeed);
        wingFold = MathHelper.approachLinear(wingFold, targetWingFold, foldAprSpeed);
        wingFlap = MathHelper.approachLinear(wingFlap, targetWingFlap, flapAprSpeed);
        wingFlap2 = MathHelper.approachLinear(wingFlap2, targetWingFlap2, flap2AprSpeed);
        wingPitch = MathHelper.approachLinear(wingPitch, targetWingPitch, pitchAprSpeed);
        hideAnim = MathHelper.approachLinear(hideAnim, hideWings ? 2 : 0, hideSpeed);
        if (hideAnim > 0) {
            wingExt = MathHelper.clip(Math.min(wingExt, 1 - hideAnim), 0, 1);
            wingFold = MathHelper.clip(Math.min(wingFold, 1 - hideAnim), 0, 1);
            wingFlap = MathHelper.clip(Math.min(wingFlap, 1 - hideAnim), 0, 1);
            wingFlap2 = MathHelper.clip(Math.min(wingFlap2, 1 - hideAnim), 0, 1);
            wingPitch = MathHelper.clip(Math.min(wingPitch, 1 - hideAnim), 0, 1);
        }
    }

    private void doWingBehavior(WingBehavior behavior) {
        switch (behavior){
            case HIDE -> hideWings = true;
            case RETRACT -> targetWingExt = targetWingFold = targetWingFlap = targetWingFlap2 = 0;
            case EXTEND -> {
                targetWingFold = targetWingExt = 1;
                targetWingFlap = 0;
                targetWingFlap2 = 1F;
            }
            case EXTEND_AND_FLAP -> {
                targetWingFold = targetWingExt = 1;
                doFlapAnimation(0.08F, 2.5F, 2, 0.25F, -0.5F, 2.5F);
            }
        }
    }

    private void doFlapAnimation(float flapSpeed, float upSpeed, float downSpeed, float upPitch, float downPitch, float pitchMod) {
        if (flapSpeed > 0) {
            float minSpeed = Math.max(flapSpeed, 0.01F);
            flapAprSpeed = flap2AprSpeed = minSpeed * 20;
            if (flapDir == 0) flapDir = 1;
            if (flapDir > 0) { //Up
                targetWingFlap = MathHelper.approachLinear(targetWingFlap, 10, minSpeed * upSpeed);
                targetWingPitch = upPitch;
                if (targetWingFlap > 0.5) targetWingFlap2 = 1 - ((targetWingFlap - 0.5F) * 2F * 1.1F);
                if (targetWingFlap >= 1) flapDir = -1;
            } else { //Down
                targetWingFlap = MathHelper.approachLinear(targetWingFlap, -10, minSpeed * downSpeed);
                targetWingPitch = downPitch;
                if (targetWingFlap < 0.5) targetWingFlap2 = 1 - (targetWingFlap * 2F * 1.1F);
                if (targetWingFlap <= -0.25) flapDir = 1;
            }
            pitchAprSpeed = minSpeed * pitchMod;
        } else {
            flapDir = 0;
            targetWingPitch = 0;
            targetWingFlap = 0;
            targetWingFlap2 = 0.5F;
        }
    }

    public float getWingBoneCol(float partialTick) {
        return wingBoneCol + (wingBoneColSpeed * partialTick);
    }

    public float getWingWebCol(float partialTick) {
        return wingWebCol + (wingWebColSpeed * partialTick);
    }

    public float getShieldCol(float partialTick) {
        return shieldCol + (shieldColSpeed * partialTick);
    }

    public float getWingExt(float partialTick) {
        return MathHelper.interpolate(lastWingExt, wingExt, partialTick);
    }

    //1 is glide pos, 0 is 'stowed' angle
    public float getWingFold(float partialTick) {
        return MathHelper.interpolate(lastWingFold, wingFold, partialTick);
    }

    //0 is neutral/glide pos
    public float getWingFlap(float partialTick) {
        return MathHelper.interpolate(lastWingFlap, wingFlap, partialTick);
    }

    public float getWingFlap2(float partialTick) {
        return MathHelper.interpolate(lastWingFlap2, wingFlap2, partialTick);
    }

    public float getWingPitch(float partialTick) {
        return MathHelper.interpolate(lastWingPitch, wingPitch, partialTick);
    }

    public float hideDecay() {
        return Math.max(hideAnim - 1, 0);
    }
}
