package me.mss1r.axiomata.blueprint;

import dev.architectury.networking.NetworkManager;
import me.mss1r.axiomata.ResourceIds;
import me.mss1r.axiomata.blueprint.api.BlueprintTags;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinition;
import me.mss1r.axiomata.blueprint.api.definition.BlueprintDefinitions;
import me.mss1r.axiomata.blueprint.internal.definition.BlueprintDefinitionRepository;
import me.mss1r.axiomata.blueprint.item.BlueprintItem;
import me.mss1r.axiomata.blueprint.menu.BlueprintUseMenu;
import me.mss1r.axiomata.blueprint.network.C2SUseBlueprintPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BlueprintItemUseTest {
    private static final String ID = "test:item_tool";
    private ServerPlayer player;
    private Inventory inventory;
    private NetworkManager.PacketContext context;
    private BlueprintDefinition recipe;
    private String previousCatalog;

    @BeforeEach void setup() {
        previousCatalog = BlueprintDefinitionRepository.toNetworkJson();
        BlueprintDefinitionRepository.applySyncedCatalog("""
                {"test:item_tool":{"formatVersion":3,"result":{"item":"minecraft:stick","count":4},
                "stages":[{"section":"assembly","materials":{"minecraft:iron_ingot":2}}]}}
                """);
        recipe = BlueprintDefinitions.get(ID);
        assertNotNull(recipe);
        player = mock(ServerPlayer.class);
        inventory = new Inventory(player);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getAbilities()).thenReturn(new Abilities());
        when(player.getMainHandItem()).thenAnswer(call -> inventory.items.get(inventory.selected));
        when(player.getOffhandItem()).thenAnswer(call -> inventory.offhand.get(0));
        when(player.getItemInHand(any())).thenAnswer(call -> call.getArgument(0) == InteractionHand.MAIN_HAND
                ? player.getMainHandItem() : player.getOffhandItem());
        doAnswer(call -> { player.containerMenu = null; return null; }).when(player).closeContainer();
        context = mock(NetworkManager.PacketContext.class);
        when(context.getPlayer()).thenReturn(player);
        doAnswer(call -> { call.getArgument(0, Runnable.class).run(); return null; }).when(context).queue(any());
    }

    @AfterEach void restoreCatalog() { BlueprintDefinitionRepository.applySyncedCatalog(previousCatalog); }

    private ItemStack open(InteractionHand hand) {
        ItemStack blueprint = BlueprintItem.create(recipe, ID);
        if (hand == InteractionHand.MAIN_HAND) inventory.items.set(0, blueprint);
        else inventory.offhand.set(0, blueprint);
        player.containerMenu = new BlueprintUseMenu(1, inventory, recipe, blueprint, ID, hand);
        return blueprint;
    }

    private void use() { C2SUseBlueprintPacket.handle(new C2SUseBlueprintPacket(ID), context); }

    @Test void itemBlueprintWorksInEitherHandWithoutHammerAndIsConsumedOnce() {
        for (InteractionHand hand : InteractionHand.values()) {
            inventory.clearContent();
            ItemStack blueprint = open(hand);
            inventory.items.set(1, new ItemStack(Items.IRON_INGOT, 3));
            assertTrue(player.containerMenu.stillValid(player));
            use();
            assertTrue(blueprint.isEmpty());
            assertEquals(1, inventory.countItem(Items.IRON_INGOT));
            assertEquals(4, inventory.countItem(Items.STICK));
            use();
            assertEquals(4, inventory.countItem(Items.STICK), "A repeated packet crafted twice");
        }
    }

    @Test void missingMaterialsNeverConsumeBlueprintOrPartialPayment() {
        ItemStack blueprint = open(InteractionHand.MAIN_HAND);
        inventory.items.set(1, new ItemStack(Items.IRON_INGOT));
        use();
        assertEquals(1, blueprint.getCount());
        assertEquals(1, inventory.countItem(Items.IRON_INGOT));
        assertEquals(0, inventory.countItem(Items.STICK));
    }

    @Test void aPacketWithoutTheMatchingOpenMenuDoesNotCraft() {
        ItemStack blueprint = open(InteractionHand.MAIN_HAND);
        inventory.items.set(1, new ItemStack(Items.IRON_INGOT, 2));
        player.containerMenu = null;
        use();
        assertEquals(1, blueprint.getCount());
        assertEquals(2, inventory.countItem(Items.IRON_INGOT));
    }

    @Test void movingBlueprintToTheOtherHandInvalidatesTheMenu() {
        ItemStack blueprint = open(InteractionHand.MAIN_HAND);
        inventory.items.set(0, ItemStack.EMPTY);
        inventory.offhand.set(0, blueprint);
        inventory.items.set(1, new ItemStack(Items.IRON_INGOT, 2));
        assertFalse(player.containerMenu.stillValid(player));
        use();
        assertEquals(1, blueprint.getCount());
        assertEquals(2, inventory.countItem(Items.IRON_INGOT));
    }

    @Test void constructionStillRequiresOffhandBlueprintAndHammer() {
        BlueprintDefinition worldRecipe = new BlueprintDefinition(new BlueprintDefinition.Result(
                ResourceIds.id("minecraft", "stick"), 1, Map.of(), BlueprintDefinition.Placement.GROUND,
                ResourceIds.id("minecraft", "boat")), List.of(), null);
        open(InteractionHand.MAIN_HAND);
        assertFalse(BlueprintItem.canUse(player, worldRecipe, ID, InteractionHand.MAIN_HAND));
        open(InteractionHand.OFF_HAND);
        assertFalse(BlueprintItem.canUse(player, worldRecipe, ID, InteractionHand.OFF_HAND));
        ItemStack hammer = mock(ItemStack.class);
        when(hammer.is(BlueprintTags.CONSTRUCTION_HAMMERS)).thenReturn(true);
        inventory.items.set(0, hammer);
        assertTrue(BlueprintItem.canUse(player, worldRecipe, ID, InteractionHand.OFF_HAND));
    }

    @Test void selectedBlueprintCannotAlsoPayForItsOwnRecipe() {
        ItemStack blueprint = open(InteractionHand.MAIN_HAND);
        assertFalse(((BlueprintUseMenu) player.containerMenu).materialStacks(player).contains(blueprint));
    }

    @Test void partialInventoryInsertionDropsOnlyTheRemainder() {
        ItemStack blueprint = open(InteractionHand.OFF_HAND);
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            inventory.items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        inventory.items.set(1, new ItemStack(Items.IRON_INGOT, 64));
        inventory.items.set(2, new ItemStack(Items.STICK, 62));
        use();
        assertTrue(blueprint.isEmpty());
        assertEquals(64, inventory.countItem(Items.STICK));
        verify(player).drop(argThat(stack -> stack.is(Items.STICK) && stack.getCount() == 2), eq(false));
    }
}
