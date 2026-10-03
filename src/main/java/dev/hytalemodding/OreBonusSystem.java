package dev.hytalemodding;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class OreBonusSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    // TEST MODE: true = 100% bonus whenever the pickaxe outranks the ore (easy to see it work).
    // Set to false for your real 10% / 25% / 50% table.
    private static final boolean TEST_MODE = false;

    // Rank per material. Gold shares Iron's rank, Silver shares Cobalt's (matches your table)
    private static final Map<String, Integer> RANK = Map.of(
            "Copper", 1, "Iron", 2, "Gold", 2, "Thorium", 3,
            "Cobalt", 4, "Silver", 4, "Adamantite", 5, "Mithril", 6);

    public OreBonusSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> buffer,
                       @Nonnull BreakBlockEvent event) {

        // Block id looks like "Ore_Copper_Shale" -> material "Copper"
        String blockId = event.getBlockType().getId();
        if (blockId == null || !blockId.startsWith("Ore_")) return;
        String rest = blockId.substring(4);
        int cut = rest.indexOf('_');
        String ore = (cut == -1) ? rest : rest.substring(0, cut);
        Integer oreRank = RANK.get(ore);
        if (oreRank == null) return;                       // an ore we don't handle

        // Tool id looks like "Tool_Pickaxe_Iron". Bare hands is null.
        var tool = event.getItemInHand();
        if (tool == null) return;
        String toolId = tool.getItemId();
        if (toolId == null || !toolId.startsWith("Tool_Pickaxe_")) return;
        int pickRank = RANK.getOrDefault(toolId.substring("Tool_Pickaxe_".length()), 0); // Crude = 0

        // Your table: 1 rank below = 10%, 2 below = 25%, 3+ below = 50%
        int diff = pickRank - oreRank;
        double chance = (diff <= 0) ? 0.0 : (diff == 1) ? 0.10 : (diff == 2) ? 0.25 : 0.50;
        if (TEST_MODE && diff > 0) chance = 1.0;

        System.out.println("BREAK " + blockId + " WITH " + toolId + " -> bonus chance " + chance);

        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < chance) {
            // GUESS: chunk.getReferenceTo(index) = the entity that broke the block (the player)
            Ref<EntityStore> player = chunk.getReferenceTo(index);
            ItemStack bonus = new ItemStack("Ore_" + ore, 1);
            ItemUtils.throwItem(player, bonus, 2.0f, buffer);   // spawns 1 extra ore
            System.out.println("BONUS ORE: Ore_" + ore);
        }
    }
}