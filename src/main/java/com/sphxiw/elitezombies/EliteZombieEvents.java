package com.sphxiw.elitezombies;

import java.util.List;
import java.util.Optional;

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
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EliteZombiesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EliteZombieEvents {
    private static final double ISOLATED_ZOMBIE_SPAWN_KEEP_CHANCE = 0.38D;
    private static final double SMALL_GROUP_ZOMBIE_SPAWN_KEEP_CHANCE = 0.68D;
    private static final double LARGE_GROUP_ZOMBIE_SPAWN_KEEP_CHANCE = 0.84D;
    private static final double SHIELD_CHANCE = 0.30D;
    private static final double RANDOM_ARMOR_CHANCE = 0.50D;
    private static final double DIGGER_CHANCE = 0.10D;
    private static final double KNIGHT_TRAIT_CHANCE = 0.10D;
    private static final double SCOUT_CHANCE = 0.10D;
    private static final double CHASER_CHANCE = 0.20D;
    private static final double GUARD_CHANCE = 0.40D;
    private static final double ELITE_ZOMBIE_KNIGHT_CHANCE = 0.02D;
    private static final double HELL_KNIGHT_CHANCE = 0.01D;
    private static final double NETHER_HELL_KNIGHT_CHANCE = 0.10D;
    private static final double POTION_USE_CHANCE = 0.28D;
    private static final double TARGET_ZOMBIE_SPEED = 0.31D;
    private static final double VANILLA_ZOMBIE_SPEED = 0.23D;
    private static final double PLAYER_SPRINT_ZOMBIE_SPEED = 0.34D;
    private static final double DODGE_NAVIGATION_SPEED = 1.25D;
    private static final double ATTACK_NAVIGATION_SPEED = 1.35D;
    private static final double REGROUP_NAVIGATION_SPEED = 1.05D;
    private static final double FLEE_NAVIGATION_SPEED = 1.45D;
    private static final int POTION_DRINK_TICKS = 32;
    private static final int SPECIAL_POTION_DRINK_TICKS = 28;
    private static final int BLOCK_BREAK_TICKS = 48;
    private static final int BLOCK_BREAK_SCAN_INTERVAL = 5;
    private static final int CLUSTER_SCAN_INTERVAL = 40;
    private static final int ROD_CHECK_INTERVAL = 10;
    private static final int SPECIAL_ABILITY_INTERVAL = 8;
    private static final int PLAYER_NOISE_INTERVAL = 10;
    private static final int INVESTIGATION_REPATH_INTERVAL = 20;
    private static final int SEARCH_WANDER_INTERVAL = 28;
    private static final int TARGET_REPATH_CLOSE_TICKS = 8;
    private static final int TARGET_REPATH_MID_TICKS = 16;
    private static final int TARGET_REPATH_FAR_TICKS = 28;
    private static final int LOST_SIGHT_GRACE_TICKS = 18;
    private static final int SEARCH_MIN_TICKS = 60;
    private static final int SEARCH_RANDOM_TICKS = 100;
    private static final int FISHING_ROD_MIN_COOLDOWN = 110;
    private static final int FISHING_ROD_RANDOM_COOLDOWN = 70;
    private static final double SPAWN_CLUSTER_RADIUS = 24.0D;
    private static final double ACTION_CLUSTER_RADIUS = 22.0D;
    private static final double GROUP_PERSONAL_SPACE = 4.0D;
    private static final double GROUP_MAX_DRIFT = 13.0D;
    private static final double INVESTIGATION_CLOSE_DISTANCE_SQR = 6.25D;
    private static final double WALK_NOISE_RANGE = 14.0D;
    private static final double SPRINT_NOISE_RANGE = 26.0D;
    private static final double SWIM_NOISE_RANGE = 18.0D;
    private static final double ITEM_USE_NOISE_RANGE = 12.0D;
    private static final double LIGHT_CHANGE_NOISE_RANGE = 24.0D;
    private static final double ATTACK_NOISE_RANGE = 30.0D;
    private static final double KNIGHT_REPORT_RANGE = 18.0D;
    private static final double SCOUT_REPORT_RANGE = 34.0D;
    private static final double SURROUND_MIN_RADIUS = 3.0D;
    private static final double SURROUND_MAX_RADIUS = 5.0D;
    private static final int MAX_ALLIES_TO_COMMAND = 8;
    private static final int MAX_ZOMBIES_PER_STIMULUS = 24;
    private static final int TRAIT_REPORT_COOLDOWN = 60;
    private static final int ENDER_PEARL_COOLDOWN = 180;
    private static final int SPECIAL_POTION_COOLDOWN = 70;

    private static final String NBT_INITIALIZED = "EliteZombiesInitialized";
    private static final String NBT_DEATH_ZOMBIE = "EliteZombiesDeathZombie";
    private static final String NBT_TRAIT = "EliteZombiesTrait";
    private static final String TRAIT_DIGGER = "digger";
    private static final String TRAIT_KNIGHT = "knight";
    private static final String TRAIT_SCOUT = "scout";
    private static final String TRAIT_CHASER = "chaser";
    private static final String TRAIT_GUARD = "guard";
    private static final String TRAIT_COWARD = "coward";
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
    private static final String NBT_LAST_SEEN_X = "EliteZombiesLastSeenX";
    private static final String NBT_LAST_SEEN_Y = "EliteZombiesLastSeenY";
    private static final String NBT_LAST_SEEN_Z = "EliteZombiesLastSeenZ";
    private static final String NBT_LOST_SIGHT_TICKS = "EliteZombiesLostSightTicks";
    private static final String NBT_INVESTIGATE_X = "EliteZombiesInvestigateX";
    private static final String NBT_INVESTIGATE_Y = "EliteZombiesInvestigateY";
    private static final String NBT_INVESTIGATE_Z = "EliteZombiesInvestigateZ";
    private static final String NBT_SEARCH_TICKS = "EliteZombiesSearchTicks";
    private static final String NBT_ROUTE_COOLDOWN = "EliteZombiesRouteCooldown";
    private static final String NBT_NEARBY_ZOMBIES = "EliteZombiesNearbyZombies";
    private static final String NBT_REPORT_COOLDOWN = "EliteZombiesReportCooldown";
    private static final String NBT_ENDER_PEARL_COOLDOWN = "EliteZombiesEnderPearlCooldown";
    private static final String NBT_SPECIAL_POTION_TICKS = "EliteZombiesSpecialPotionTicks";
    private static final String NBT_SPECIAL_POTION_KIND = "EliteZombiesSpecialPotionKind";
    private static final String NBT_SPECIAL_POTION_DISPLAY = "EliteZombiesSpecialPotionDisplay";
    private static final String NBT_SPECIAL_POTION_COOLDOWN = "EliteZombiesSpecialPotionCooldown";
    private static final String SPECIAL_POTION_HARMING_HEAL = "harming_heal";
    private static final String SPECIAL_POTION_SPEED = "speed";
    private static final String SPECIAL_POTION_INVISIBILITY = "invisibility";

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
    private static final Item[] ANY_HOES = { Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE, Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE };
    private static final Item[] ANY_SHOVELS = { Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL, Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL };
    private static final Item[] ANY_AXES = { Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE };
    private static final Item[] ANY_PICKAXES = { Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE };
    private static final Item[] ANY_SWORDS = { Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD };
    private static final Item[][] DIGGER_TOOL_GROUPS = {
            ANY_HOES,
            ANY_SHOVELS,
            ANY_AXES,
            ANY_PICKAXES
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
                maybeMaintainLooseGroup(zombie, null);
            }
            tickInvestigation(zombie);
            handleTraitBehavior(zombie, null, false);
            return;
        }

        boolean canSeeTarget = zombie.hasLineOfSight(player);
        if (isStaggeredTick(zombie, CLUSTER_SCAN_INTERVAL)) {
            maybeMaintainLooseGroup(zombie, canSeeTarget ? player : null);
        }

        if (canSeeTarget) {
            rememberLastSeen(zombie, player);
            zombie.getPersistentData().remove(NBT_LOST_SIGHT_TICKS);
            clearInvestigation(zombie);
            improveTargetNavigation(zombie, player);
        } else if (handleLostSight(zombie, player)) {
            tickInvestigation(zombie);
            handleTraitBehavior(zombie, player, false);
            return;
        }

        if (handleTraitBehavior(zombie, player, canSeeTarget)) {
            return;
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

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player player)) {
            return;
        }

        receiveStimulus(zombie, player, player.position(), true);
        if (zombie.level() instanceof ServerLevel level) {
            emitNoise(level, player, player.position(), ATTACK_NOISE_RANGE, true);
        }

        if (!hasShield(zombie) || !isInFrontOfZombie(zombie, attacker)) {
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide() || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!player.isAlive() || player.isSpectator() || player.tickCount % PLAYER_NOISE_INTERVAL != 0) {
            return;
        }

        double noiseRange = getPlayerMovementNoiseRange(player);
        if (noiseRange > 0.0D) {
            emitNoise(player.serverLevel(), player, player.position(), noiseRange, false);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.getPlacedBlock().getLightEmission() > 0) {
            emitNoise(level, player, Vec3.atCenterOf(event.getPos()), LIGHT_CHANGE_NOISE_RANGE, false);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (event.getState().getLightEmission() > 0) {
            emitNoise(level, player, Vec3.atCenterOf(event.getPos()), LIGHT_CHANGE_NOISE_RANGE, false);
        }
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

    private static double getPlayerMovementNoiseRange(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        double horizontalSpeedSqr = movement.x * movement.x + movement.z * movement.z;
        double range = 0.0D;

        if (player.isSwimming() && horizontalSpeedSqr > 0.003D) {
            range = Math.max(range, SWIM_NOISE_RANGE);
        } else if (player.isSprinting() && horizontalSpeedSqr > 0.01D) {
            range = Math.max(range, SPRINT_NOISE_RANGE);
        } else if (horizontalSpeedSqr > 0.006D) {
            range = Math.max(range, player.isCrouching() ? 5.0D : WALK_NOISE_RANGE);
        }

        if (player.isUsingItem()) {
            range = Math.max(range, ITEM_USE_NOISE_RANGE);
        }

        return range;
    }

    private static void emitNoise(ServerLevel level, Player source, Vec3 noisePos, double range, boolean forceTarget) {
        AABB area = new AABB(noisePos, noisePos).inflate(range, 8.0D, range);
        double rangeSqr = range * range;
        int notified = 0;
        for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, area, other -> other.isAlive() && other.getType() == EntityType.ZOMBIE)) {
            if (notified >= MAX_ZOMBIES_PER_STIMULUS) {
                break;
            }

            if (zombie.position().distanceToSqr(noisePos) > rangeSqr) {
                continue;
            }

            receiveStimulus(zombie, source, noisePos, forceTarget);
            notified++;
        }
    }

    private static void receiveStimulus(Zombie zombie, Player source, Vec3 stimulusPos, boolean forceTarget) {
        if (!zombie.getPersistentData().getBoolean(NBT_INITIALIZED)) {
            initializeEliteZombie(zombie, true);
        }

        boolean canSeeSource = source != null && source.isAlive() && zombie.hasLineOfSight(source);
        if (forceTarget || canSeeSource) {
            zombie.setTarget(source);
            rememberLastSeen(zombie, source);
            clearInvestigation(zombie);
            zombie.getPersistentData().putInt(NBT_ROUTE_COOLDOWN, 0);
            return;
        }

        if (!hasVisiblePlayerTarget(zombie)) {
            startInvestigation(zombie, stimulusPos);
        }
    }

    private static boolean hasVisiblePlayerTarget(Zombie zombie) {
        LivingEntity target = zombie.getTarget();
        return target instanceof Player player && player.isAlive() && zombie.hasLineOfSight(player);
    }

    private static void initializeEliteZombie(Zombie zombie, boolean randomGear) {
        CompoundTag data = zombie.getPersistentData();
        data.putBoolean(NBT_INITIALIZED, true);

        if (randomGear && !data.getBoolean(NBT_DEATH_ZOMBIE)) {
            equipSpawnVariant(zombie);
        }
        applyEliteAttributes(zombie);
    }

    private static void applyEliteAttributes(Zombie zombie) {
        String trait = getTrait(zombie);
        double speed = TARGET_ZOMBIE_SPEED;
        double followRange = 35.0D;

        if (TRAIT_DIGGER.equals(trait) || TRAIT_GUARD.equals(trait)) {
            speed = VANILLA_ZOMBIE_SPEED;
        } else if (TRAIT_CHASER.equals(trait) || TRAIT_COWARD.equals(trait)) {
            speed = PLAYER_SPRINT_ZOMBIE_SPEED;
        }

        if (TRAIT_SCOUT.equals(trait)) {
            followRange = 56.0D;
        } else if (TRAIT_CHASER.equals(trait)) {
            followRange = 52.0D;
        } else if (TRAIT_KNIGHT.equals(trait)) {
            followRange = 42.0D;
            setAttributeBase(zombie, Attributes.MAX_HEALTH, 50.0D);
            if (zombie.getHealth() < zombie.getMaxHealth()) {
                zombie.setHealth(zombie.getMaxHealth());
            }
        }

        setAttributeBase(zombie, Attributes.MOVEMENT_SPEED, speed);
        setAttributeBase(zombie, Attributes.FOLLOW_RANGE, followRange);
    }

    private static void setAttributeBase(Zombie zombie, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        AttributeInstance instance = zombie.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value) {
            instance.setBaseValue(value);
        }
    }

    private static void equipSpawnVariant(Zombie zombie) {
        RandomSource random = zombie.getRandom();
        double roll = random.nextDouble();
        if (roll < DIGGER_CHANCE) {
            setTrait(zombie, TRAIT_DIGGER);
            equipDigger(zombie);
            return;
        }

        roll -= DIGGER_CHANCE;
        if (roll < KNIGHT_TRAIT_CHANCE) {
            setTrait(zombie, TRAIT_KNIGHT);
            equipKnightTrait(zombie);
            return;
        }

        roll -= KNIGHT_TRAIT_CHANCE;
        if (roll < SCOUT_CHANCE) {
            setTrait(zombie, TRAIT_SCOUT);
            clearEquipment(zombie);
            return;
        }

        roll -= SCOUT_CHANCE;
        if (roll < CHASER_CHANCE) {
            setTrait(zombie, TRAIT_CHASER);
            equipChaser(zombie);
            return;
        }

        roll -= CHASER_CHANCE;
        if (roll < GUARD_CHANCE) {
            setTrait(zombie, TRAIT_GUARD);
            equipGuard(zombie);
            return;
        }

        setTrait(zombie, TRAIT_COWARD);
        clearEquipment(zombie);
    }

    private static void setTrait(Zombie zombie, String trait) {
        zombie.getPersistentData().putString(NBT_TRAIT, trait);
    }

    private static String getTrait(Zombie zombie) {
        return zombie.getPersistentData().getString(NBT_TRAIT);
    }

    private static void equipDigger(Zombie zombie) {
        clearArmor(zombie);
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        Item[] toolGroup = DIGGER_TOOL_GROUPS[zombie.getRandom().nextInt(DIGGER_TOOL_GROUPS.length)];
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(randomItem(zombie, toolGroup)));
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.10F);
    }

    private static void equipKnightTrait(Zombie zombie) {
        double hellChanceWithinKnight = isInNether(zombie) ? NETHER_HELL_KNIGHT_CHANCE : HELL_KNIGHT_CHANCE;
        double eliteChanceWithinKnight = Math.max(ELITE_ZOMBIE_KNIGHT_CHANCE, 0.20D);
        double roll = zombie.getRandom().nextDouble();
        if (roll < hellChanceWithinKnight) {
            equipHellKnight(zombie);
        } else if (roll < hellChanceWithinKnight + eliteChanceWithinKnight) {
            equipEliteZombieKnight(zombie);
        } else {
            equipZombieKnight(zombie);
        }
    }

    private static void equipChaser(Zombie zombie) {
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(randomItem(zombie, ANY_AXES)));
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.12F);
        maybeEquipRandomArmor(zombie);
    }

    private static void equipGuard(Zombie zombie) {
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(randomItem(zombie, ANY_SWORDS)));
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.12F);
        maybeEquipRandomArmor(zombie);
        maybeEquipShield(zombie);
    }

    private static Item randomItem(Zombie zombie, Item[] items) {
        return items[zombie.getRandom().nextInt(items.length)];
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

    private static void equipEnchantedWeapon(Zombie zombie, Item item, int basePower, int randomPower, float dropChance) {
        int enchantLevel = basePower + zombie.getRandom().nextInt(randomPower + 1);
        ItemStack weapon = EnchantmentHelper.enchantItem(zombie.getRandom(), new ItemStack(item), enchantLevel, true);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        zombie.setDropChance(EquipmentSlot.MAINHAND, dropChance);
    }

    private static void clearEquipment(Zombie zombie) {
        clearArmor(zombie);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    private static void clearArmor(Zombie zombie) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            zombie.setItemSlot(slot, ItemStack.EMPTY);
            zombie.setDropChance(slot, 0.0F);
        }
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
        String trait = getTrait(zombie);
        if (TRAIT_DIGGER.equals(trait) || TRAIT_SCOUT.equals(trait) || TRAIT_COWARD.equals(trait)) {
            return;
        }

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

    private static boolean handleTraitBehavior(Zombie zombie, Player player, boolean canSeeTarget) {
        String trait = getTrait(zombie);
        if (tickSpecialPotion(zombie, player)) {
            return true;
        }

        if (player == null) {
            if (TRAIT_COWARD.equals(trait) && zombie.getHealth() < zombie.getMaxHealth() && !zombie.getPersistentData().contains(NBT_INVESTIGATE_X)) {
                return startSpecialPotion(zombie, SPECIAL_POTION_HARMING_HEAL);
            }
            return false;
        }

        if (canSeeTarget && isStaggeredTick(zombie, SPECIAL_ABILITY_INTERVAL)) {
            maybeReportTarget(zombie, player, trait);
        }

        if (TRAIT_COWARD.equals(trait)) {
            handleCowardBehavior(zombie, player, canSeeTarget);
            return true;
        }

        if (TRAIT_KNIGHT.equals(trait) && handleKnightSurvival(zombie, player)) {
            return true;
        }

        if (TRAIT_KNIGHT.equals(trait) && canSeeTarget && zombie.distanceToSqr(player) > 256.0D && !zombie.hasEffect(MobEffects.MOVEMENT_SPEED)) {
            startSpecialPotion(zombie, SPECIAL_POTION_SPEED);
        }

        if (TRAIT_CHASER.equals(trait) && canSeeTarget && isStaggeredTick(zombie, SPECIAL_ABILITY_INTERVAL)) {
            maybeUseEnderPearl(zombie, player);
        }

        return false;
    }

    private static void maybeReportTarget(Zombie zombie, Player player, String trait) {
        CompoundTag data = zombie.getPersistentData();
        if (data.getInt(NBT_REPORT_COOLDOWN) > 0) {
            return;
        }

        double range;
        if (TRAIT_SCOUT.equals(trait)) {
            range = SCOUT_REPORT_RANGE;
        } else if (TRAIT_KNIGHT.equals(trait)) {
            range = KNIGHT_REPORT_RANGE;
        } else {
            return;
        }

        data.putInt(NBT_REPORT_COOLDOWN, TRAIT_REPORT_COOLDOWN + zombie.getRandom().nextInt(20));
        if (zombie.level() instanceof ServerLevel level) {
            emitNoise(level, player, player.position(), range, true);
        }
    }

    private static boolean handleKnightSurvival(Zombie zombie, Player player) {
        if (zombie.getHealth() > zombie.getMaxHealth() * 0.35F) {
            return false;
        }

        fleeFromPlayer(zombie, player, 9.0D);
        if (zombie.distanceToSqr(player) > 144.0D && zombie.getHealth() < zombie.getMaxHealth()) {
            startSpecialPotion(zombie, SPECIAL_POTION_HARMING_HEAL);
        }
        return true;
    }

    private static void handleCowardBehavior(Zombie zombie, Player player, boolean canSeeTarget) {
        if (canSeeTarget) {
            if (zombie.getHealth() <= zombie.getMaxHealth() * 0.45F) {
                if (!zombie.hasEffect(MobEffects.MOVEMENT_SPEED) && zombie.getRandom().nextBoolean()) {
                    startSpecialPotion(zombie, SPECIAL_POTION_SPEED);
                } else if (!zombie.hasEffect(MobEffects.INVISIBILITY)) {
                    startSpecialPotion(zombie, SPECIAL_POTION_INVISIBILITY);
                }
            }
            fleeFromPlayer(zombie, player, 11.0D);
        }

        if (zombie.distanceToSqr(player) > 196.0D && zombie.getHealth() < zombie.getMaxHealth()) {
            startSpecialPotion(zombie, SPECIAL_POTION_HARMING_HEAL);
        }
    }

    private static void fleeFromPlayer(Zombie zombie, Player player, double distance) {
        if (!isStaggeredTick(zombie, TARGET_REPATH_CLOSE_TICKS) && !zombie.getNavigation().isDone()) {
            return;
        }

        Vec3 away = zombie.position().subtract(player.position());
        Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = new Vec3(zombie.getRandom().nextDouble() - 0.5D, 0.0D, zombie.getRandom().nextDouble() - 0.5D);
        }

        Vec3 destination = zombie.position().add(horizontal.normalize().scale(distance));
        zombie.getNavigation().moveTo(destination.x, zombie.getY(), destination.z, FLEE_NAVIGATION_SPEED);
    }

    private static boolean startSpecialPotion(Zombie zombie, String kind) {
        CompoundTag data = zombie.getPersistentData();
        if (data.getInt(NBT_SPECIAL_POTION_TICKS) > 0 || data.getInt(NBT_SPECIAL_POTION_COOLDOWN) > 0) {
            return false;
        }

        data.putString(NBT_SPECIAL_POTION_KIND, kind);
        data.putInt(NBT_SPECIAL_POTION_TICKS, SPECIAL_POTION_DRINK_TICKS);
        data.putInt(NBT_SPECIAL_POTION_COOLDOWN, SPECIAL_POTION_COOLDOWN);

        if (zombie.getOffhandItem().isEmpty()) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, getSpecialPotionStack(kind));
            zombie.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            data.putBoolean(NBT_SPECIAL_POTION_DISPLAY, true);
        }

        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.8F, 0.9F);
        return true;
    }

    private static boolean tickSpecialPotion(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        int ticks = data.getInt(NBT_SPECIAL_POTION_TICKS);
        if (ticks <= 0) {
            return false;
        }

        zombie.getNavigation().stop();
        if (player != null) {
            zombie.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
        data.putInt(NBT_SPECIAL_POTION_TICKS, ticks - 1);

        if (zombie.level() instanceof ServerLevel level && ticks % 7 == 0) {
            level.sendParticles(ParticleTypes.WITCH, zombie.getX(), zombie.getY() + 1.0D, zombie.getZ(), 6, 0.22D, 0.35D, 0.22D, 0.01D);
        }

        if (ticks > 1) {
            return true;
        }

        String kind = data.getString(NBT_SPECIAL_POTION_KIND);
        if (SPECIAL_POTION_HARMING_HEAL.equals(kind)) {
            zombie.heal(8.0F);
        } else if (SPECIAL_POTION_SPEED.equals(kind)) {
            zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 35, 0));
        } else if (SPECIAL_POTION_INVISIBILITY.equals(kind)) {
            zombie.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 20, 0));
        }

        if (data.getBoolean(NBT_SPECIAL_POTION_DISPLAY) && zombie.getOffhandItem().is(Items.POTION)) {
            zombie.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        data.remove(NBT_SPECIAL_POTION_TICKS);
        data.remove(NBT_SPECIAL_POTION_KIND);
        data.remove(NBT_SPECIAL_POTION_DISPLAY);
        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 0.8F, 1.1F);
        return false;
    }

    private static ItemStack getSpecialPotionStack(String kind) {
        if (SPECIAL_POTION_HARMING_HEAL.equals(kind)) {
            return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HARMING);
        }
        if (SPECIAL_POTION_INVISIBILITY.equals(kind)) {
            return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.INVISIBILITY);
        }
        return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);
    }

    private static void maybeUseEnderPearl(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        double distanceSqr = zombie.distanceToSqr(player);
        if (data.getInt(NBT_ENDER_PEARL_COOLDOWN) > 0 || distanceSqr < 144.0D || distanceSqr > 900.0D) {
            return;
        }

        Optional<Vec3> destination = findEnderPearlDestination(zombie, player);
        if (destination.isEmpty()) {
            return;
        }

        data.putInt(NBT_ENDER_PEARL_COOLDOWN, ENDER_PEARL_COOLDOWN + zombie.getRandom().nextInt(60));
        Vec3 pos = destination.get();
        zombie.swing(InteractionHand.MAIN_HAND);
        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.ENDER_PEARL_THROW, SoundSource.HOSTILE, 0.9F, 0.9F);
        zombie.teleportTo(pos.x, pos.y, pos.z);
        zombie.level().playSound(null, zombie.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.9F, 1.05F);
        if (zombie.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, zombie.getX(), zombie.getY() + 0.6D, zombie.getZ(), 24, 0.35D, 0.55D, 0.35D, 0.02D);
        }
    }

    private static Optional<Vec3> findEnderPearlDestination(Zombie zombie, Player player) {
        Vec3 fromPlayer = zombie.position().subtract(player.position());
        Vec3 horizontal = new Vec3(fromPlayer.x, 0.0D, fromPlayer.z);
        if (horizontal.lengthSqr() < 0.001D) {
            horizontal = new Vec3(1.0D, 0.0D, 0.0D);
        }

        Vec3 direction = horizontal.normalize();
        for (int i = 0; i < 5; i++) {
            double angle = (i - 2) * 0.55D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 rotated = new Vec3(direction.x * cos - direction.z * sin, 0.0D, direction.x * sin + direction.z * cos);
            Vec3 candidate = player.position().add(rotated.scale(3.0D + i * 0.6D));
            Optional<Vec3> safe = findSafeStandingPosition(zombie.level(), candidate);
            if (safe.isPresent()) {
                return safe;
            }
        }
        return Optional.empty();
    }

    private static Optional<Vec3> findSafeStandingPosition(Level level, Vec3 candidate) {
        BlockPos base = BlockPos.containing(candidate.x, candidate.y, candidate.z);
        for (int yOffset = 1; yOffset >= -2; yOffset--) {
            BlockPos pos = base.offset(0, yOffset, 0);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.below()).isAir()) {
                return Optional.of(Vec3.atBottomCenterOf(pos));
            }
        }
        return Optional.empty();
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

    private static void rememberLastSeen(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        data.putDouble(NBT_LAST_SEEN_X, player.getX());
        data.putDouble(NBT_LAST_SEEN_Y, player.getY());
        data.putDouble(NBT_LAST_SEEN_Z, player.getZ());
    }

    private static boolean handleLostSight(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        int lostSightTicks = data.getInt(NBT_LOST_SIGHT_TICKS) + 1;
        data.putInt(NBT_LOST_SIGHT_TICKS, lostSightTicks);

        if (data.contains(NBT_LAST_SEEN_X)) {
            startInvestigation(zombie, getLastSeenPosition(data));
        } else {
            startInvestigation(zombie, player.position());
        }

        if (lostSightTicks >= LOST_SIGHT_GRACE_TICKS) {
            zombie.setTarget(null);
            return true;
        }
        return false;
    }

    private static void improveTargetNavigation(Zombie zombie, Player player) {
        CompoundTag data = zombie.getPersistentData();
        int routeCooldown = data.getInt(NBT_ROUTE_COOLDOWN);
        if (routeCooldown > 0 && !zombie.getNavigation().isDone()) {
            return;
        }

        double distanceSqr = zombie.distanceToSqr(player);
        int nextCooldown;
        if (distanceSqr <= 36.0D) {
            nextCooldown = TARGET_REPATH_CLOSE_TICKS;
        } else if (distanceSqr <= 196.0D) {
            nextCooldown = TARGET_REPATH_MID_TICKS;
        } else {
            nextCooldown = TARGET_REPATH_FAR_TICKS;
        }
        data.putInt(NBT_ROUTE_COOLDOWN, nextCooldown + zombie.getRandom().nextInt(4));

        if (distanceSqr < 4.0D) {
            return;
        }

        Vec3 destination = player.position();
        if (data.getInt(NBT_NEARBY_ZOMBIES) > 0 && distanceSqr > 9.0D) {
            destination = getSurroundPosition(zombie, player, distanceSqr);
        }

        if (zombie.position().distanceToSqr(destination) > 2.25D) {
            zombie.getNavigation().moveTo(destination.x, destination.y, destination.z, ATTACK_NAVIGATION_SPEED);
        }
    }

    private static Vec3 getSurroundPosition(Zombie zombie, Player player, double distanceSqr) {
        int slot = Math.floorMod(zombie.getUUID().hashCode(), 8);
        double angle = slot * (Math.PI / 4.0D);
        double radius = distanceSqr < 100.0D ? SURROUND_MIN_RADIUS : SURROUND_MAX_RADIUS;
        return new Vec3(player.getX() + Math.cos(angle) * radius, player.getY(), player.getZ() + Math.sin(angle) * radius);
    }

    private static boolean tickInvestigation(Zombie zombie) {
        CompoundTag data = zombie.getPersistentData();
        if (!data.contains(NBT_INVESTIGATE_X)) {
            return false;
        }

        if (hasVisiblePlayerTarget(zombie)) {
            clearInvestigation(zombie);
            return false;
        }

        Vec3 investigationPos = getInvestigationPosition(data);
        double distanceSqr = zombie.position().distanceToSqr(investigationPos);
        if (distanceSqr > INVESTIGATION_CLOSE_DISTANCE_SQR) {
            data.remove(NBT_SEARCH_TICKS);
            if (isStaggeredTick(zombie, INVESTIGATION_REPATH_INTERVAL) || zombie.getNavigation().isDone()) {
                zombie.getNavigation().moveTo(investigationPos.x, investigationPos.y, investigationPos.z, REGROUP_NAVIGATION_SPEED);
            }
            return true;
        }

        int searchTicks = data.getInt(NBT_SEARCH_TICKS);
        if (searchTicks <= 0) {
            searchTicks = SEARCH_MIN_TICKS + zombie.getRandom().nextInt(SEARCH_RANDOM_TICKS + 1);
        }

        if (searchTicks <= 1) {
            clearInvestigation(zombie);
            return false;
        }

        data.putInt(NBT_SEARCH_TICKS, searchTicks - 1);
        if (isStaggeredTick(zombie, SEARCH_WANDER_INTERVAL) || zombie.getNavigation().isDone()) {
            Vec3 wanderPos = getSearchWanderPosition(zombie, investigationPos);
            zombie.getNavigation().moveTo(wanderPos.x, wanderPos.y, wanderPos.z, REGROUP_NAVIGATION_SPEED);
        }
        return true;
    }

    private static void startInvestigation(Zombie zombie, Vec3 position) {
        CompoundTag data = zombie.getPersistentData();
        if (data.contains(NBT_INVESTIGATE_X) && getInvestigationPosition(data).distanceToSqr(position) <= 9.0D) {
            return;
        }

        data.putDouble(NBT_INVESTIGATE_X, position.x);
        data.putDouble(NBT_INVESTIGATE_Y, position.y);
        data.putDouble(NBT_INVESTIGATE_Z, position.z);
        data.remove(NBT_SEARCH_TICKS);
        data.putInt(NBT_ROUTE_COOLDOWN, 0);
    }

    private static void clearInvestigation(Zombie zombie) {
        CompoundTag data = zombie.getPersistentData();
        data.remove(NBT_INVESTIGATE_X);
        data.remove(NBT_INVESTIGATE_Y);
        data.remove(NBT_INVESTIGATE_Z);
        data.remove(NBT_SEARCH_TICKS);
    }

    private static Vec3 getLastSeenPosition(CompoundTag data) {
        return new Vec3(data.getDouble(NBT_LAST_SEEN_X), data.getDouble(NBT_LAST_SEEN_Y), data.getDouble(NBT_LAST_SEEN_Z));
    }

    private static Vec3 getInvestigationPosition(CompoundTag data) {
        return new Vec3(data.getDouble(NBT_INVESTIGATE_X), data.getDouble(NBT_INVESTIGATE_Y), data.getDouble(NBT_INVESTIGATE_Z));
    }

    private static Vec3 getSearchWanderPosition(Zombie zombie, Vec3 center) {
        double angle = zombie.getRandom().nextDouble() * Math.PI * 2.0D;
        double radius = 2.0D + zombie.getRandom().nextDouble() * 5.0D;
        return new Vec3(center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius);
    }

    private static void maybeMaintainLooseGroup(Zombie zombie, Player sharedTarget) {
        if (!(zombie.level() instanceof ServerLevel level)) {
            return;
        }

        AABB area = zombie.getBoundingBox().inflate(ACTION_CLUSTER_RADIUS, 8.0D, ACTION_CLUSTER_RADIUS);
        List<Zombie> nearbyZombies = level.getEntitiesOfClass(Zombie.class, area, other -> other != zombie && other.isAlive() && other.getType() == EntityType.ZOMBIE);
        if (nearbyZombies.isEmpty()) {
            zombie.getPersistentData().putInt(NBT_NEARBY_ZOMBIES, 0);
            return;
        }

        int usedAllies = 0;
        double nearestAllyDistanceSqr = Double.MAX_VALUE;
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
            nearestAllyDistanceSqr = Math.min(nearestAllyDistanceSqr, zombie.distanceToSqr(ally));
            if (sharedTarget != null && ally.getTarget() != sharedTarget) {
                receiveStimulus(ally, sharedTarget, sharedTarget.position(), true);
            }
        }

        CompoundTag data = zombie.getPersistentData();
        data.putInt(NBT_NEARBY_ZOMBIES, usedAllies);
        double divisor = usedAllies + 1.0D;
        Vec3 center = new Vec3(x / divisor, y / divisor, z / divisor);
        double distanceToCenter = zombie.position().distanceToSqr(center);

        if (sharedTarget != null) {
            return;
        }

        boolean tooFarFromGroup = distanceToCenter > GROUP_MAX_DRIFT * GROUP_MAX_DRIFT;
        boolean tooCloseToAlly = nearestAllyDistanceSqr < GROUP_PERSONAL_SPACE * GROUP_PERSONAL_SPACE;
        if (tooFarFromGroup || tooCloseToAlly) {
            Vec3 loosePosition = getLooseGroupPosition(zombie, center);
            if (zombie.position().distanceToSqr(loosePosition) > 4.0D) {
                zombie.getNavigation().moveTo(loosePosition.x, loosePosition.y, loosePosition.z, REGROUP_NAVIGATION_SPEED);
            }
        }
    }

    private static Vec3 getLooseGroupPosition(Zombie zombie, Vec3 center) {
        int slot = Math.floorMod(zombie.getUUID().hashCode(), 12);
        double angle = slot * (Math.PI / 6.0D);
        double radius = GROUP_PERSONAL_SPACE + (slot % 3) * 1.5D;
        return new Vec3(center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius);
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
            if (canZombieBreak(zombie, state)) {
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
            if (canZombieBreak(zombie, zombie.level().getBlockState(pos))) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    private static boolean canZombieBreak(Zombie zombie, BlockState state) {
        if (state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS)) {
            return true;
        }

        return TRAIT_DIGGER.equals(getTrait(zombie)) && canDiggerToolBreak(zombie.getMainHandItem(), state);
    }

    private static boolean canDiggerToolBreak(ItemStack tool, BlockState state) {
        Item item = tool.getItem();
        if (item instanceof AxeItem) {
            return state.is(BlockTags.MINEABLE_WITH_AXE);
        }
        if (item instanceof PickaxeItem) {
            return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
        }
        if (item instanceof ShovelItem) {
            return state.is(BlockTags.MINEABLE_WITH_SHOVEL);
        }
        if (item instanceof HoeItem) {
            return state.is(BlockTags.MINEABLE_WITH_HOE);
        }
        return false;
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
        if (!canUseFishingRod(zombie)) {
            return;
        }

        CompoundTag data = zombie.getPersistentData();
        double distanceSqr = zombie.distanceToSqr(player);
        boolean guard = TRAIT_GUARD.equals(getTrait(zombie));
        double minDistanceSqr = guard ? 49.0D : 100.0D;
        double maxDistanceSqr = guard ? 784.0D : 576.0D;
        if (data.getInt(NBT_ROD_COOLDOWN) > 0 || distanceSqr < minDistanceSqr || distanceSqr > maxDistanceSqr || !zombie.hasLineOfSight(player)) {
            return;
        }

        int minCooldown = guard ? 55 : FISHING_ROD_MIN_COOLDOWN;
        int randomCooldown = guard ? 45 : FISHING_ROD_RANDOM_COOLDOWN;
        data.putInt(NBT_ROD_COOLDOWN, minCooldown + zombie.getRandom().nextInt(randomCooldown));
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

    private static boolean canUseFishingRod(Zombie zombie) {
        String trait = getTrait(zombie);
        return TRAIT_GUARD.equals(trait) || TRAIT_KNIGHT.equals(trait);
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
        decrement(data, NBT_ROUTE_COOLDOWN);
        decrement(data, NBT_REPORT_COOLDOWN);
        decrement(data, NBT_ENDER_PEARL_COOLDOWN);
        decrement(data, NBT_SPECIAL_POTION_COOLDOWN);

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
