# KeepInventory Totem

A NeoForge 1.21.1 mod that adds a KeepInventory Totem.

If a player dies with one or more totems anywhere in their inventory (including
armor or offhand slots), exactly one totem is consumed and their full inventory is restored after
respawning. The effect applies only to that player and does not change the
server's `keepInventory` gamerule.

On death, the normal death screen is replaced by a short animation: the totem
flies into view and cracks, an End Portal opens, and the camera appears to fly
through it before the player automatically respawns. The transition hides the
HUD and briefly applies blindness.

## Crafting

Craft it with a Totem of Undying in the center, Ender Chests on the four cardinal
sides, and Diamonds in the corners.

## Building

Requires Java 21.

```sh
./gradlew build
```

The built mod JAR will be placed in `build/libs`.
