# Phantom

A packet-based NPC library for Paper servers. NPCs are completely client-side so they dont affect server performance the way real entities do.

Built on top of PacketEvents and EntityLib.

## Features

- Player NPCs, mob NPCs, armor stands, and display entities
- Physics simulation with gravity, collisions, knockback
- A* pathfinding with navigation goals
- Component system for health, combat, and custom behaviors
- Click handlers and proximity triggers
- Skin management with caching
- Async visibility calculations

## Getting Started

### Dependencies

You'll need PacketEvents and EntityLib. Add them to your plugin.yml or paper-plugin.yml as dependencies.

### Maven/Gradle

The library is hosted on GitHub packages

```kotlin
repositories {
    maven("https://maven.pkg.github.com/solarprisons/phantomnpcs")
}

dependencies {
    implementation("prisons.solar:api:1.0.0-SNAPSHOT")
    implementation("prisons.solar:platform-paper:1.0.0-SNAPSHOT")
}
```

You might need to authenticate with github packages, check their docs if you run into issues.

### Basic Usage

First create a Phantom instance in your plugin. Usually you do this in onEnable:

```java
public class MyPlugin extends JavaPlugin {

    private Phantom phantom;

    @Override
    public void onEnable() {
        phantom = PaperPhantom.builder(this)
            .build();

        phantom.enable();
    }

    @Override
    public void onDisable() {
        if (phantom != null) {
            phantom.disable();
        }
    }
}
```

### Spawning NPCs

Creating and spawning an NPC is pretty straightforward:

```java
// Create position
Position pos = Position.of(
    world.getUID().toString(),
    x, y, z,
    yaw, pitch
);

// Create the NPC
NPC<?> npc = phantom.npcs().create(EntityType.PLAYER, pos);

// Customize appearance
npc.appearance().setCustomName("Steve");
npc.appearance().setCustomNameVisible(true);

// Register and spawn
phantom.npcs().register(npc);
npc.spawn();
```

For player NPCs you can set skins and stuff:

```java
NPC<?> npc = phantom.npcs().create(EntityType.PLAYER, pos);
// npc.appearance() returns PlayerAppearance for player types
```

### Click Handlers

You can listen for when players interact with NPCs:

```java
npc.onClick(ctx -> {
    Player player = ctx.viewer().platformPlayer();
    player.sendMessage("You clicked the NPC!");

    // ctx.clickType() tells you if it was left or right click
});
```

### Physics

Physics is opt-in. Enable it per NPC:

```java
npc.enablePhysics();

// Apply velocity
npc.setVelocity(new Vector3d(0, 10, 0)); // jump

// Or apply impulse
npc.applyImpulse(new Vector3d(5, 2, 0)); // knockback
```

### AI and Pathfinding

NPCs have a goal selector for AI behaviors:

```java
GoalSelector goals = npc.getGoalSelector();

// Make NPC navigate to a position
NavigateToPositionGoal navGoal = new NavigateToPositionGoal(
    targetPosition,
    speed,
    MovementCapabilities.DEFAULT,
    pathfinder,
    worldProvider
);

goals.addGoal(navGoal);
```

You can get the pathfinder and world provider from services:

```java
PathfindingComponent pathfinder = phantom.services()
    .get(PathfindingComponent.class)
    .orElseThrow();

WorldProvider worldProvider = phantom.services()
    .get(WorldProvider.class)
    .orElseThrow();
```

### Components

NPCs use a component system for optional features like combat:

```java
// Add health/combat to an NPC
npc.addComponent(new DefaultCombatantComponent(20.0f)); // 20 HP

// Check if NPC has a component
if (npc.hasComponent(CombatantComponent.class)) {
    CombatantComponent combat = npc.getComponent(CombatantComponent.class).get();
    // do stuff with it
}
```

### Events

Subscribe to NPC events through the event bus:

```java
phantom.events().subscribe(NPCInteractEvent.class, event -> {
    // handle interaction
});

phantom.events().subscribe(DamageEvent.class, event -> {
    // NPC took damage
});
```

### Mob NPCs

Works the same way as player NPCs just with different entity types:

```java
NPC<?> zombie = phantom.npcs().create(EntityType.ZOMBIE, pos);
NPC<?> villager = phantom.npcs().create(EntityType.VILLAGER, pos);
NPC<?> creeper = phantom.npcs().create(EntityType.CREEPER, pos);

// etc
```

### Display Entities

You can also create text displays, block displays, item displays:

```java
NPC<?> textDisplay = phantom.npcs().create(EntityType.TEXT_DISPLAY, pos);
// textDisplay.appearance() returns TextDisplayAppearance

NPC<?> blockDisplay = phantom.npcs().create(EntityType.BLOCK_DISPLAY, pos);
NPC<?> itemDisplay = phantom.npcs().create(EntityType.ITEM_DISPLAY, pos);
```

## Configuration

You can pass a config path to the builder:

```java
Path configPath = getDataFolder().toPath().resolve("phantom.yml");

phantom = PaperPhantom.builder(this)
    .configPath(configPath)
    .debug(true) // enables debug logging
    .build();
```

## Block Change Notifications

If you want pathfinding to respond to block changes, call invalidatePathfindingCache when blocks are placed/broken:

```java
@EventHandler
public void onBlockPlace(BlockPlaceEvent event) {
    Block b = event.getBlock();
    phantom.invalidatePathfindingCache(
        b.getWorld().getUID().toString(),
        b.getX(), b.getY(), b.getZ()
    );
}
```

## Support

Having issues or got questions? Join our Discord: https://discord.gg/kKqH9HdjKu

## License

MIT
