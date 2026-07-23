# KeepInventory Totem

A NeoForge 1.21.1 mod that adds a KeepInventory Totem.

If a player dies with one or more totems anywhere in their inventory (including
armor or offhand slots), exactly one totem is consumed and their full inventory is restored after
respawning. The effect applies only to that player and does not change the
server's `keepInventory` gamerule.

On death, the normal death screen is replaced by the vanilla item-activation
animation for the Totem of Keeping, composited above the portal effect. Radial fissures then leak animated End Portal
energy, and each fissure widens in staggered jumps until the fractured opening
rapidly bursts toward the camera. The portal completes and the player respawns
as the vanilla totem activation finishes. The
transition hides the HUD and briefly applies blindness. If the player respawns
beside their bed, the camera fades in with a short waking-up and getting-out motion.
It begins at mattress height looking down, sits upright, turns toward the side of
the bed, and rises into the normal standing camera position.

## Crafting

Craft it with a Totem of Undying in the center, Ender Chests on the four cardinal
sides, and Diamonds in the corners.

## Building

Requires Java 21.

```sh
./gradlew build
```

The built mod JAR will be placed in `build/libs`.
