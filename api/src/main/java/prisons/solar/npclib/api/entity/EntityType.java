package prisons.solar.npclib.api.entity;

/**
 * Represents the type of entity an NPC can be.
 */
public enum EntityType {
    // Player types
    PLAYER(EntityCategory.PLAYER, true),

    // Living mobs
    ZOMBIE(EntityCategory.MOB, true),
    SKELETON(EntityCategory.MOB, true),
    CREEPER(EntityCategory.MOB, true),
    VILLAGER(EntityCategory.MOB, true),
    IRON_GOLEM(EntityCategory.MOB, true),
    SNOW_GOLEM(EntityCategory.MOB, true),
    WITCH(EntityCategory.MOB, true),
    PILLAGER(EntityCategory.MOB, true),
    VINDICATOR(EntityCategory.MOB, true),
    EVOKER(EntityCategory.MOB, true),
    ENDERMAN(EntityCategory.MOB, true),
    PIGLIN(EntityCategory.MOB, true),
    PIGLIN_BRUTE(EntityCategory.MOB, true),
    ZOMBIFIED_PIGLIN(EntityCategory.MOB, true),
    BLAZE(EntityCategory.MOB, true),
    WITHER_SKELETON(EntityCategory.MOB, true),
    GUARDIAN(EntityCategory.MOB, true),
    ELDER_GUARDIAN(EntityCategory.MOB, true),
    SHULKER(EntityCategory.MOB, true),
    WARDEN(EntityCategory.MOB, true),
    ALLAY(EntityCategory.MOB, true),
    VEX(EntityCategory.MOB, true),

    // Passive mobs
    COW(EntityCategory.MOB, true),
    PIG(EntityCategory.MOB, true),
    SHEEP(EntityCategory.MOB, true),
    CHICKEN(EntityCategory.MOB, true),
    WOLF(EntityCategory.MOB, true),
    CAT(EntityCategory.MOB, true),
    HORSE(EntityCategory.MOB, true),
    DONKEY(EntityCategory.MOB, true),
    MULE(EntityCategory.MOB, true),
    LLAMA(EntityCategory.MOB, true),
    FOX(EntityCategory.MOB, true),
    PANDA(EntityCategory.MOB, true),
    BEE(EntityCategory.MOB, true),
    FROG(EntityCategory.MOB, true),
    AXOLOTL(EntityCategory.MOB, true),
    GOAT(EntityCategory.MOB, true),
    CAMEL(EntityCategory.MOB, true),
    SNIFFER(EntityCategory.MOB, true),
    ARMADILLO(EntityCategory.MOB, true),

    // Display entities (1.19.4+)
    TEXT_DISPLAY(EntityCategory.DISPLAY, false),
    BLOCK_DISPLAY(EntityCategory.DISPLAY, false),
    ITEM_DISPLAY(EntityCategory.DISPLAY, false),

    // Object entities
    ARMOR_STAND(EntityCategory.OBJECT, false),
    ITEM_FRAME(EntityCategory.OBJECT, false),
    GLOW_ITEM_FRAME(EntityCategory.OBJECT, false),
    INTERACTION(EntityCategory.OBJECT, false);

    private final EntityCategory category;
    private final boolean living;

    EntityType(EntityCategory category, boolean living) {
        this.category = category;
        this.living = living;
    }

    /**
     * Gets the category this entity type belongs to.
     *
     * @return the entity category
     */
    public EntityCategory category() {
        return category;
    }

    /**
     * Returns whether this entity type represents a living entity.
     *
     * @return true if living entity
     */
    public boolean isLiving() {
        return living;
    }

    /**
     * Returns whether this is a player entity type.
     *
     * @return true if player
     */
    public boolean isPlayer() {
        return category == EntityCategory.PLAYER;
    }

    /**
     * Returns whether this is a display entity type.
     *
     * @return true if display entity
     */
    public boolean isDisplay() {
        return category == EntityCategory.DISPLAY;
    }
}
