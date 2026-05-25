package com.sphxiw.elitezombies;

import java.util.Optional;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EliteZombiesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EliteZombieEvents {
    private static final double ISOLATED_ZOMBIE_SPAWN_KEEP_CHANCE = 0.38D;
    private static final double SMALL_GROUP_ZOMBIE_SPAWN_KEEP_CHANCE = 0.68D;
    private static final double LARGE_GROUP_ZOMBIE_SPAWN_KEEP_CHANCE = 0.84D;
    private static final double SHIELD_CHANCE = 0.30D;
    private static final double RANDOM_ARMOR_CHANCE = 0.50D;
    private static final double ZOMBIE_KNIGHT_CHANCE = 0.10D;
    private static final double ELITE_ZOMBIE_KNIGHT_CHANCE = 0.02D;
    private static final double HELL_KNIGHT_CHANCE = 0.01D;
    private static final double NETHER_HELL_KNIGHT_CHANCE = 0.10D;
    private static final double POTION_USE_CHANCE = 0.28D;
    private static final double TARGET_ZOMBIE_SPEED = 0.31D;
    private static final double DODGE_NAVIGATION_SPEED = 1.25D;
    private static final double ATTACK_NAVIGATION_SPEED = 1.35D;
    private static final double REGROUP_NAVIGATION_SPEED = 1.05D;
    private static final int POTION_DRINK_TICKS = 32;
    private static final int BLOCK_BREAK_TICKS = 48;
    private static final int BLOCK_BREAK_SCAN_INTERVAL = 5;
    private static final int CLUSTER_SCAN_INTERVAL = 40;
    private static final int ROD_CHECK_INTERVAL = 10;
    private static final int FISHING_ROD_MIN_COOLDOWN = 110;
    private static final int FISHING_ROD_RANDOM_COOLDOWN = 70;
    private static final double SPAWN_CLUSTER_RADIUS = 24.0D;
    private static final double ACTION_CLUSTER_RADIUS = 22.0D;
    private static final int MAX_ALLIES_TO_COMMAND = 8;

    private static final String NBT_INITIALIZED = "EliteZombiesInitialized";
    private static final String NBT_DEATH_ZOMBIE = "EliteZombiesDeathZombie";
    private static final String NBT_POTION_DECIDED = "EliteZombiesPotionDecided";
    private static final String NBT_POTION_DRINK_TICKS = "EliteZombiesPotionDrinkTicks";
    private static final String NBT_POTION_KIND = "EliteZombiesPotionKind";
    private static final String NBT_POTION_DISPLAY = "EliteZombiesPotionDisplay";
    private static final String NBT_SHIELD_UP_TICKS = "EliteZombiesShieldUpTicks";
    private static final String NBT_ROD_COOLDOWN = "EliteZombiesRodCooldown";
    private static final String NBT_ROD_DISPLAY_TICKS = "EliteZombiesRodDisplayTicks";
    private static final String NBT_BREAK_X = "EliteZombiesBreakX";
    private static final String NBT_BREAK_Y = "EliteZombiesBreakY";
    private static final String NBT_BREAK_Z = "EliteZombiesBreakZ";
    private static final String NBT_BREAK_TICKS = "EliteZombiesBreakTicks";

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final Item[] LEATHER_ARMOR = { Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS };
    private static final Item[] GOLD_ARMOR = { Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS };
    private static final Item[] CHAINMAIL_ARMOR = { Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS };
    private static final Item[] IRON_ARMOR = { Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS };
    private static final Item[] DIAMOND_ARMOR = { Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS };
    private static final Item[] NETHERITE_ARMOR = { Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS };
    private static final Item[][] RANDOM_ARMOR_SETS = {
            LEATHER_ARMOR,
            GOLD_ARMOR,
            CHAINMAIL_ARMOR,
            IRON_ARMOR,
            DIAMOND_ARMOR,
            NETHERITE_ARMOR
    };
    private static final Enchantment[] HELL_SWORD_EXTRA_ENCHANTMENTS = {
            Enchantments.UNBREAKING,
            Enchantments.KNOCKBACK,
            Enchantments.MOB_LOOTING
    };

    private EliteZombieEvents() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.getType() != EntityType.ZOMBIE) {
            return;
        }

        if (shouldReduceSpawn(event.getSpawnType()) && shouldCancelForSpawnConcentration(zombie)) {
            event.setSpawnCancelled(true);
            return;
        }

        initializeEliteZombie(zombie, true);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.getType() != EntityType.ZOMBIE || zombie.level().isClientSide()) {
            return;
        }

        if (!zombie.getPersistentData().getBoolean(NBT_INITIALIZED)) {
            initializeEliteZombie(zombie, true);
        }

        tickCooldownsAndDisplays(zombie);
        LivingEntity target = zombie.getTarget();
        if (!(target instanceof Player player) || !target.isAlive()) {
            resetBlockBreaking(zombie);
            if (isStaggeredTick(zombie, CLUSTER_SCAN_INTERVAL)) {
                maybeRegroupWithNearbyZombies(zombie, null);
            }
            return;
        }

        if (isStaggeredTick(zombie, CLUSTER_SCAN_INTERVAL)) {
            maybeRegroupWithNearbyZombies(zombie, player);
        }
        maybeDrinkPotionBeforeAttack(zombie, player);
        maybeRaiseShield(zombie, player);
        maybeDodgeRangedPlayer(zombie, player);
        if (hasBreakTarget(zombie) || isStaggeredTick(zombie, BLOCK_BREAK_SCAN_INTERVAL)) {
            maybeBreakInteractiveBlock(zombie, player);
        }
        if (isStaggeredTick(zombie, ROD_CHECK_INTERVAL)) {
            maybePullPlayerWithFishingRod(zombie, player);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.getType() != EntityType.ZOMBIE || zombie.level().isClientSide()) {
            return;
        }

        if (!hasShield(zombie)) {
            return;
        }

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player) || !isInFrontOfZombie(zombie, attacker)) {
            return;
        }

        if (zombie.getRandom().nextDouble() > 0.72D) {
            return;
        }

        CompoundTag data = zombie.getPersistentData();
        data.putInt(NBT_SHIELD_UP_TICKS, 24);
        zombie.startUsingItem(InteractionHand.OFF_HAND);
        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 0.9F + zombie.getRandom().nextFloat() * 0.2F);
        zombie.getOffhandItem().hurtAndBreak(Math.max(1, (int) event.getAmount()), zombie, brokenZombie -> brokenZombie.broadcastBreakEvent(EquipmentSlot.OFFHAND));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Zombie zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            return;
        }

        zombie.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        zombie.setBaby(false);
        zombie.setCanPickUpLoot(false);
        zombie.setPersistenceRequired();
        zombie.getPersistentData().putBoolean(NBT_DEATH_ZOMBIE, true);
        zombie.getPersistentData().putBoolean(NBT_INITIALIZED, true);

        boolean keepInventory = level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        copyArmorAndWeapons(player, zombie, !keepInventory);
        applyEliteAttributes(zombie);
        level.addFreshEntity(zombie);
    }

    private static boolean shouldReduceSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL
                || spawnType == MobSpawnType.CHUNK_GENERATION
                || spawnType == MobSpawnType.REINFORCEMENT
                || spawnType == MobSpawnType.PATROL;
    }

    private static boolean shouldCancelForSpawnConcentration(Zombie zombie) {
        int nearbyZombies = countNearbyZombies(zombie, SPAWN_CLUSTER_RADIUS, 8.0D);
        double keepChance = ISOLATED_ZOMBIE_SPAWN_KEEP_CHANCE;
        if (nearbyZombies >= 3) {
            keepChance = LARGE_GROUP_ZOMBIE_SPAWN_KEEP_CHANCE;
        } else if (nearbyZombies > 0) {
            keepChance = SMALL_GROUP_ZOMBIE_SPAWN_KEEP_CHANCE;
        }
        return zombie.getRandom().nextDouble() > keepChance;
    }

    private static int countNearbyZombies(Zombie zombie, double horizontalRadius, double verticalRadius) {
        AABB area = zombie.getBoundingBox().inflate(horizontalRadius, verticalRadius, horizontalRadius);
        return zombie.level().getEntitiesOfClass(Zombie.class, area, other -> other != zombie && other.isAlive() && other.getType() == EntityType.ZOMBIE).size();
    }

    private static boolean isInNether(Entity entity) {
        return entity.level().dimension() == Level.NETHER;
    }

    private static void initializeEliteZombie(Zombie zombie, boolean randomGear) {
        CompoundTag data = zombie.getPersistentData();
        data.putBoolean(NBT_INITIALIZED, true);
        applyEliteAttributes(zombie);

        if (randomGear && !data.getBoolean(NBT_DEATH_ZOMBIE)) {
            equipSpawnVariant(zombie);
        }
    }

    private static void applyEliteAttributes(Zombie zombie) {
        AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getBaseValue() < TARGET_ZOMBIE_SPEED) {
            speed.setBaseValue(TARGET_ZOMBIE_SPEED);
        }
    }

    private static void equipSpawnVariant(Zombie zombie) {
        RandomSource random = zombie.getRandom();
        double hellKnightChance = isInNether(zombie) ? NETHER_HELL_KNIGHT_CHANCE : HELL_KNIGHT_CHANCE;
        double roll = random.nextDouble();

        if (roll < hellKnightChance) {
            equipHellKnight(zombie);
            return;
        }

        roll -= hellKnightChance;
        if (roll < ELITE_ZOMBIE_KNIGHT_CHANCE) {
            equipEliteZombieKnight(zombie);
            return;
        }

        roll -= ELITE_ZOMBIE_KNIGHT_CHANCE;
        if (roll < ZOMBIE_KNIGHT_CHANCE) {
            equipZombieKnight(zombie);
            return;
        }

        equipRandomWeapon(zombie);
        maybeEquipRandomArmor(zombie);
        maybeEquipShield(zombie);
    }

    private static void equipZombieKnight(Zombie zombie) {
        Item[] armor = zombie.getRandom().nextBoolean() ? IRON_ARMOR : CHAINMAIL_ARMOR;
        equipArmorSet(zombie, armor, false, false);
        equipGuaranteedShield(zombie);
        equipEnchantedWeapon(zombie, Items.IRON_SWORD, 18, 18, 0.18F);
    }

    private static void equipEliteZombieKnight(Zombie zombie) {
        equipArmorSet(zombie, DIAMOND_ARMOR, true, false);
        equipGuaranteedShield(zombie);
        equipEnchantedWeapon(zombie, Items.DIAMOND_SWORD, 24, 18, 0.22F);
    }

    private static void equipHellKnight(Zombie zombie) {
        equipArmorSet(zombie, NETHERITE_ARMOR, false, true);
        equipGuaranteedShield(zombie);

        ItemStack sword = EnchantmentHelper.enchantItem(zombie.getRandom(), new ItemStack(Items.NETHERITE_SWORD), 28 + zombie.getRandom().nextInt(14), true);
        ensureEnchantment(sword, Enchantments.FIRE_ASPECT, 1);
        Enchantment extra = HELL_SWORD_EXTRA_ENCHANTMENTS[zombie.getRandom().nextInt(HELL_SWORD_EXTRA_ENCHANTMENTS.length)];
        ensureEnchantment(sword, extra, 1 + zombie.getRandom().nextInt(Math.min(2, extra.getMaxLevel())));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.25F);
    }

    private static void equipRandomWeapon(Zombie zombie) {
        RandomSource random = zombie.getRandom();
        ItemStack weapon = random.nextBoolean() ? new ItemStack(Items.IRON_SWORD) : new ItemStack(Items.IRON_AXE);
        int enchantLevel = 12 + random.nextInt(18);
        EnchantmentHelper.enchantItem(random, weapon, enchantLevel, true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.12F);
    }

    private static void equipEnchantedWeapon(Zombie zombie, Item item, int basePower, int randomPower, float dropChance) {
        int enchantLevel = basePower + zombie.getRandom().nextInt(randomPower + 1);
        ItemStack weapon = EnchantmentHelper.enchantItem(zombie.getRandom(), new ItemStack(item), enchantLevel, true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        zombie.setDropChance(EquipmentSlot.MAINHAND, dropChance);
    }

    private static void maybeEquipRandomArmor(Zombie zombie) {
        RandomSource random = zombie.getRandom();
        if (random.nextDouble() > RANDOM_ARMOR_CHANCE) {
            return;
        }

        int slotIndex = random.nextInt(ARMOR_SLOTS.length);
        Item[] armorSet = RANDOM_ARMOR_SETS[random.nextInt(RANDOM_ARMOR_SETS.length)];
        ItemStack armor = new ItemStack(armorSet[slotIndex]);
        zombie.setItemSlot(ARMOR_SLOTS[slotIndex], armor);
        zombie.setDropChance(ARMOR_SLOTS[slotIndex], 0.07F);
    }

    private static void equipArmorSet(Zombie zombie, Item[] armorItems, boolean randomEnchantments, boolean protectionOne) {
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack armor = new ItemStack(armorItems[i]);
            if (randomEnchantments) {
                armor = EnchantmentHelper.enchantItem(zombie.getRandom(), armor, 18 + zombie.getRandom().nextInt(18), true);
            }
            if (protectionOne) {
                armor.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 1);
            }

            zombie.setItemSlot(ARMOR_SLOTS[i], armor);
            zombie.setDropChance(ARMOR_SLOTS[i], 0.12F);
        }
    }

    private static void maybeEquipShield(Zombie zombie) {
        if (zombie.getRandom().nextDouble() > SHIELD_CHANCE) {
            return;
        }

        equipGuaranteedShield(zombie);
    }

    private static void equipGuaranteedShield(Zombie zombie) {
        ItemStack shield = new ItemStack(Items.SHIELD);
        zombie.setItemSlot(EquipmentSlot.OFFHAND, shield);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.08F);
    }

    private static void ensureEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        if (EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) <= 0) {
            stack.enchant(enchantment, level);
        }
    }

    private static void maybeDrinkPotionBeforeAttack(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        if (data.getBoolean(NBT_POTION_DECIDED)) {
            int drinkTicks = data.getInt(NBT_POTION_DRINK_TICKS);
            if (drinkTicks > 0) {
                continuePotionDrink(zombie, player, drinkTicks);
            }
            return;
        }

        data.putBoolean(NBT_POTION_DECIDED, true);
        if (zombie.getRandom().nextDouble() > POTION_USE_CHANCE) {
            return;
        }

        String kind = zombie.getRandom().nextBoolean() ? "strength" : "speed";
        data.putString(NBT_POTION_KIND, kind);
        data.putInt(NBT_POTION_DRINK_TICKS, POTION_DRINK_TICKS);

        if (zombie.getOffhandItem().isEmpty()) {
            ItemStack potion = PotionUtils.setPotion(new ItemStack(Items.POTION), "strength".equals(kind) ? Potions.STRENGTH : Potions.SWIFTNESS);
            zombie.setItemSlot(EquipmentSlot.OFFHAND, potion);
            zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            data.putBoolean(NBT_POTION_DISPLAY, true);
        }

        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.8F, 0.85F + zombie.getRandom().nextFloat() * 0.25F);
    }

    private static void continuePotionDrink(Zombie zombie, Player player, int drinkTicks) {
        CompoundTag data = zombie.getPersistentData();
        zombie.getNavigation().stop();
        zombie.getLookControl().setLookAt(player, 30.0F, 30.0F);
        data.putInt(NBT_POTION_DRINK_TICKS, drinkTicks - 1);

        if (zombie.level() instanceof ServerLevel level && drinkTicks % 6 == 0) {
            level.sendParticles(ParticleTypes.WITCH, zombie.getX(), zombie.getY() + 1.0D, zombie.getZ(), 8, 0.25D, 0.45D, 0.25D, 0.02D);
        }

        if (drinkTicks > 1) {
            return;
        }

        if ("strength".equals(data.getString(NBT_POTION_KIND))) {
            zombie.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 90, 0));
        } else {
            zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 70, 0));
        }

        if (data.getBoolean(NBT_POTION_DISPLAY) && zombie.getOffhandItem().is(Items.POTION)) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        data.remove(NBT_POTION_DISPLAY);
        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.8F, 1.1F);
    }

    private static void maybeRaiseShield(Zombie zombie, Player player) {
        if (!hasShield(zombie) || zombie.isUsingItem()) {
            return;
        }

        double distanceSqr = zombie.distanceToSqr(player);
        if (distanceSqr > 36.0D || !isInFrontOfZombie(zombie, player)) {
            return;
        }

        if (zombie.getRandom().nextInt(18) == 0) {
            zombie.getPersistentData().putInt(NBT_SHIELD_UP_TICKS, 18 + zombie.getRandom().nextInt(16));
            zombie.startUsingItem(InteractionHand.OFF_HAND);
        }
    }

    private static void maybeDodgeRangedPlayer(Zombie zombie, Player player) {
        if (!playerIsHoldingRangedWeapon(player) || zombie.tickCount % 16 != 0 || !zombie.hasLineOfSight(player)) {
            return;
        }

        double distanceSqr = zombie.distanceToSqr(player);
        if (distanceSqr < 36.0D || distanceSqr > 484.0D) {
            return;
        }

        Vec3 awayFromPlayer = zombie.position().subtract(player.position());
        Vec3 horizontalAway = new Vec3(awayFromPlayer.x, 0.0D, awayFromPlayer.z);
        if (horizontalAway.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 side = new Vec3(horizontalAway.z, 0.0D, -horizontalAway.x).normalize();
        if (zombie.getRandom().nextBoolean()) {
            side = side.scale(-1.0D);
        }
        Vec3 retreat = horizontalAway.normalize().scale(0.65D);
        Vec3 destination = zombie.position().add(side.scale(3.0D)).add(retreat.scale(2.0D));
        zombie.getNavigation().moveTo(destination.x, zombie.getY(), destination.z, DODGE_NAVIGATION_SPEED);
    }

    private static void maybeRegroupWithNearbyZombies(Zombie zombie, Player sharedTarget) {
        if (!(zombie.level() instanceof ServerLevel level)) {
            return;
        }

        AABB area = zombie.getBoundingBox().inflate(ACTION_CLUSTER_RADIUS, 8.0D, ACTION_CLUSTER_RADIUS);
        List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, area, other -> other != zombie && other.isAlive() && other.getType() == EntityType.ZOMBIE);
        if (nearbyZombies.isEmpty()) {
            return;
        }

        int usedAllies = 0;
        double x = zombie.getX();
        double y = zombie.getY();
        double z = zombie.getZ();
        for (Zombie ally : nearbyZombies) {
            if (usedAllies >= MAX_ALLIES_TO_COMMAND) {
                break;
            }

            usedAllies++;
            x += ally.getX();
            y += ally.getY();
            z += ally.getZ();
            if (sharedTarget != null && ally.getTarget() != sharedTarget && ally.hasLineOfSight(sharedTarget)) {
                ally.setTarget(sharedTarget);
                ally.getNavigation().moveTo(sharedTarget, ATTACK_NAVIGATION_SPEED);
            }
        }

        double divisor = usedAllies + 1.0D;
        Vec3 center = new Vec3(x / divisor, y / divisor, z / divisor);
        double distanceToCenter = zombie.position().distanceToSqr(center);
        boolean hasTarget = sharedTarget != null;
        if (!hasTarget && distanceToCenter > 25.0D) {
            zombie.getNavigation().moveTo(center.x, center.y, center.z, REGROUP_NAVIGATION_SPEED);
        } else if (hasTarget && distanceToCenter > 81.0D && zombie.distanceToSqr(sharedTarget) > 36.0D) {
            zombie.getNavigation().moveTo(center.x, center.y, center.z, REGROUP_NAVIGATION_SPEED);
        }
    }

    private static void maybeBreakInteractiveBlock(Zombie zombie, Player player) {
        if (!(zombie.level() instanceof ServerLevel level) || zombie.distanceToSqr(player) > 100.0D) {
            resetBlockBreaking(zombie);
            return;
        }

        Optional<BlockPos> targetPos = findBreakableBlockNearZombie(zombie);
        if (targetPos.isEmpty()) {
            resetBlockBreaking(zombie);
            return;
        }

        BlockPos pos = targetPos.get();
        CompoundTag data = zombie.getPersistentData();
        if (!isSameBreakTarget(data, pos)) {
            resetBlockBreaking(zombie);
            data.putInt(NBT_BREAK_X, pos.getX());
            data.putInt(NBT_BREAK_Y, pos.getY());
            data.putInt(NBT_BREAK_Z, pos.getZ());
            data.putInt(NBT_BREAK_TICKS, 0);
        }

        int ticks = data.getInt(NBT_BREAK_TICKS) + 1;
        data.putInt(NBT_BREAK_TICKS, ticks);
        int stage = Math.min(9, ticks * 10 / BLOCK_BREAK_TICKS);
        level.destroyBlockProgress(zombie.getId(), pos, stage);

        if (ticks >= BLOCK_BREAK_TICKS) {
            BlockState state = level.getBlockState(pos);
            if (canZombieBreak(state)) {
                level.destroyBlock(pos, true, zombie);
                level.levelEvent(2001, pos, Block.getId(state));
            }
            resetBlockBreaking(zombie);
        }
    }

    private static Optional<BlockPos> findBreakableBlockNearZombie(Zombie zombie) {
        BlockPos base = zombie.blockPosition();
        BlockPos forward = base.relative(zombie.getDirection());
        BlockPos[] candidates = {
                forward,
                forward.above(),
                base,
                base.above()
        };

        for (BlockPos pos : candidates) {
            if (canZombieBreak(zombie.level().getBlockState(pos))) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private static boolean canZombieBreak(BlockState state) {
        return state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS);
    }

    private static boolean isSameBreakTarget(CompoundTag data, BlockPos pos) {
        return data.contains(NBT_BREAK_X)
                && data.getInt(NBT_BREAK_X) == pos.getX()
                && data.getInt(NBT_BREAK_Y) == pos.getY()
                && data.getInt(NBT_BREAK_Z) == pos.getZ();
    }

    private static boolean hasBreakTarget(Zombie zombie) {
        return zombie.getPersistentData().contains(NBT_BREAK_X);
    }

    private static void resetBlockBreaking(Zombie zombie) {
        CompoundTag data = zombie.getPersistentData();
        if (data.contains(NBT_BREAK_X) && zombie.level() instanceof ServerLevel level) {
            BlockPos pos = new BlockPos(data.getInt(NBT_BREAK_X), data.getInt(NBT_BREAK_Y), data.getInt(NBT_BREAK_Z));
            level.destroyBlockProgress(zombie.getId(), pos, -1);
        }
        data.remove(NBT_BREAK_X);
        data.remove(NBT_BREAK_Y);
        data.remove(NBT_BREAK_Z);
        data.remove(NBT_BREAK_TICKS);
    }

    private static void maybePullPlayerWithFishingRod(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        double distanceSqr = zombie.distanceToSqr(player);
        if (data.getInt(NBT_ROD_COOLDOWN) > 0 || distanceSqr < 100.0D || distanceSqr > 576.0D || !zombie.hasLineOfSight(player)) {
            return;
        }

        data.putInt(NBT_ROD_COOLDOWN, FISHING_ROD_MIN_COOLDOWN + zombie.getRandom().nextInt(FISHING_ROD_RANDOM_COOLDOWN));
        zombie.getLookControl().setLookAt(player, 30.0F, 30.0F);

        if (zombie.getOffhandItem().isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.FISHING_ROD));
            zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            data.putInt(NBT_ROD_DISPLAY_TICKS, 22);
            zombie.swing(InteractionHand.OFF_HAND);
        } else {
            zombie.swing(InteractionHand.MAIN_HAND);
        }

        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.HOSTILE, 1.0F, 0.8F + zombie.getRandom().nextFloat() * 0.2F);
        pullPlayerTowardZombie(zombie, player);
        zombie.getNavigation().moveTo(player, ATTACK_NAVIGATION_SPEED);
    }

    private static void pullPlayerTowardZombie(Zombie zombie, Player player) {
        Vec3 pull = zombie.position().subtract(player.position());
        Vec3 horizontal = new Vec3(pull.x, 0.0D, pull.z);
        if (horizontal.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 direction = horizontal.normalize();
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x + direction.x * 1.15D, Math.min(current.y + 0.28D, 0.42D), current.z + direction.z * 1.15D);
        player.hasImpulse = true;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    private static void tickCooldownsAndDisplays(Zombie zombie) {
        CompoundTag data = zombie.getPersistentData();
        decrement(data, NBT_ROD_COOLDOWN);

        int shieldTicks = data.getInt(NBT_SHIELD_UP_TICKS);
        if (shieldTicks > 0) {
            data.putInt(NBT_SHIELD_UP_TICKS, shieldTicks - 1);
            if (hasShield(zombie) && !zombie.isUsingItem()) {
                zombie.startUsingItem(InteractionHand.OFF_HAND);
            }
        } else if (zombie.isUsingItem() && zombie.getUsedItemHand() == InteractionHand.OFF_HAND && hasShield(zombie)) {
            zombie.stopUsingItem();
        }

        int rodTicks = data.getInt(NBT_ROD_DISPLAY_TICKS);
        if (rodTicks > 0) {
            data.putInt(NBT_ROD_DISPLAY_TICKS, rodTicks - 1);
            if (rodTicks == 1 && zombie.getOffhandItem().is(Items.FISHING_ROD)) {
                zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }
    }

    private static void decrement(CompoundTag data, String key) {
        int value = data.getInt(key);
        if (value > 0) {
            data.putInt(key, value - 1);
        }
    }

    private static boolean isStaggeredTick(Zombie zombie, int interval) {
        return interval <= 1 || (zombie.tickCount + zombie.getId()) % interval == 0;
    }

    private static boolean hasShield(Zombie zombie) {
        return zombie.getOffhandItem().is(Items.SHIELD);
    }

    private static boolean isInFrontOfZombie(Zombie zombie, Entity entity) {
        Vec3 look = zombie.getViewVector(1.0F).normalize();
        Vec3 toEntity = entity.position().subtract(zombie.position()).normalize();
        return look.dot(toEntity) > 0.2D;
    }

    private static boolean playerIsHoldingRangedWeapon(Player player) {
        return isRangedWeapon(player.getMainHandItem()) || isRangedWeapon(player.getOffhandItem());
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;
    }

    private static void copyArmorAndWeapons(ServerPlayer player, Zombie zombie, boolean clearCopiedSlots) {
        copyEquipmentSlot(player, zombie, EquipmentSlot.HEAD, true, clearCopiedSlots);
        copyEquipmentSlot(player, zombie, EquipmentSlot.CHEST, true, clearCopiedSlots);
        copyEquipmentSlot(player, zombie, EquipmentSlot.LEGS, true, clearCopiedSlots);
        copyEquipmentSlot(player, zombie, EquipmentSlot.FEET, true, clearCopiedSlots);
        copyEquipmentSlot(player, zombie, EquipmentSlot.MAINHAND, false, clearCopiedSlots);
        copyEquipmentSlot(player, zombie, EquipmentSlot.OFFHAND, false, clearCopiedSlots);
    }

    private static void copyEquipmentSlot(ServerPlayer player, Zombie zombie, EquipmentSlot slot, boolean armorSlot, boolean clearCopiedSlots) {
        ItemStack original = player.getItemBySlot(slot);
        if (original.isEmpty()) {
            return;
        }

        if (!armorSlot && !isWeaponOrShield(original)) {
            return;
        }

        zombie.setItemSlot(slot, original.copy());
        zombie.setDropChance(slot, 1.0F);

        if (clearCopiedSlots) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static boolean isWeaponOrShield(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof AxeItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem
                || item instanceof ShieldItem;
    }
}
