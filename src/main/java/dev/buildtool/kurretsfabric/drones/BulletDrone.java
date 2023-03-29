package dev.buildtool.kurretsfabric.drones;

import dev.buildtool.kurretsfabric.Drone;
import dev.buildtool.kurretsfabric.KTurrets;
import dev.buildtool.kurretsfabric.goals.AttackTask;
import dev.buildtool.kurretsfabric.projectiles.Bullet;
import dev.buildtool.kurretsfabric.screenhandlers.BulletDroneScreenHandler;
import dev.buildtool.satako.DefaultInventory;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class BulletDrone extends Drone {
    public DefaultInventory ammo = new DefaultInventory(18) {
        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return stack.isOf(Items.GOLD_NUGGET) || stack.isOf(Items.IRON_NUGGET);
        }
    };

    public BulletDrone(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean isArmed() {
        return !ammo.isEmpty();
    }

    @Override
    protected List<DefaultInventory> getContainedItems() {
        return Collections.singletonList(ammo);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeInt(getId());
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        if (target.isAlive()) {
            for (ItemStack item : ammo.getItems()) {
                if (!item.isEmpty()) {
                    double d0 = target.getX() - this.getX();
                    double d1 = target.getEyeY() - getEyeY();
                    double d2 = target.getZ() - this.getZ();
                    Bullet bullet = new Bullet(this, d0, d1, d2, world);
                    bullet.setDamage(item.getItem() == Items.GOLD_NUGGET ? KTurrets.CONFIGURATION.goldBulletDamage() : KTurrets.CONFIGURATION.ironBulletDamage());
                    world.spawnEntity(bullet);
                    world.playSound(null, getBlockPos(), KTurrets.BULLET_FIRE, SoundCategory.NEUTRAL, 1, 1);
                    item.decrement(1);
                    break;
                }
            }
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        PacketByteBuf byteBuf = new PacketByteBuf(Unpooled.buffer());
        byteBuf.writeInt(getId());
        return new BulletDroneScreenHandler(syncId, inv, byteBuf);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.put("Ammo", ammo.writeToTag());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        ammo.readFromTag(nbt.getCompound("Ammo"));
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        goalSelector.add(5, new ProjectileAttackGoal(this, 1, KTurrets.CONFIGURATION.bulletTurretDelay(), (float) getRange()));
        targetSelector.add(5, new AttackTask(this));
    }
}
