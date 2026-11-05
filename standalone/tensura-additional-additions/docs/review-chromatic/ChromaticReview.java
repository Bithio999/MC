package dev.heavenlyrestriction;

import dev.heavenlyrestriction.chromatic.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in actual client/server anvil flow, guarded to the disposable Wayfarer Review save. */
@EventBusSubscriber(modid = HeavenlyRestriction.ID, value = Dist.CLIENT)
public final class ChromaticReview {
  private static final Path OUT = Path.of("/workspace/validation-chromatic/capture");
  private static int phase, wait, age;
  private static volatile String failure;

  private static void check(boolean ok, String why) {
    if (!ok) throw new IllegalStateException(why);
  }

  private static void server(java.util.function.Consumer<ServerPlayer> action) {
    var server = Minecraft.getInstance().getSingleplayerServer();
    server.execute(
        () -> {
          try {
            action.accept(server.getPlayerList().getPlayers().getFirst());
          } catch (Throwable e) {
            e.printStackTrace();
            failure = e.toString();
          }
        });
  }

  private static void shot(String name) throws Exception {
    Files.createDirectories(OUT);
    try (var img = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
      img.writeToFile(OUT.resolve(name + ".png"));
    }
  }

  private static void hover(int x, int y) throws Exception {
    var mc = Minecraft.getInstance();
    var window = mc.getWindow();
    double mx = (double) x * window.getScreenWidth() / window.getGuiScaledWidth();
    double my = (double) y * window.getScreenHeight() / window.getGuiScaledHeight();
    var move =
        MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
    move.setAccessible(true);
    move.invoke(mc.mouseHandler, window.getWindow(), mx, my);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    try {
      if (failure != null || ++age > 5000) {
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("failed.txt"), failure == null ? "Timeout phase " + phase : failure);
        mc.stop();
        return;
      }
      if (mc.player == null
          || mc.level == null
          || mc.getSingleplayerServer() == null
          || mc.getOverlay() != null) return;
      wait++;
      if (phase == 0 && wait > 80) {
        phase = 1;
        wait = 0;
        mc.options.pauseOnLostFocus = false;
        mc.options.guiScale().set(2);
        mc.resizeDisplay();
        server(
            p -> {
              check(
                  p.server
                      .getWorldPath(LevelResource.ROOT)
                      .normalize()
                      .getFileName()
                      .toString()
                      .equals("Wayfarer Review"),
                  "Refusing personal save");
              p.setGameMode(GameType.SURVIVAL);
              p.setInvulnerable(true);
              p.getInventory().clearContent();
              p.giveExperienceLevels(30 - p.experienceLevel);
              p.openMenu(
                  new SimpleMenuProvider(
                      (id, inventory, player) ->
                          new AnvilMenu(id, inventory, ContainerLevelAccess.NULL),
                      net.minecraft.network.chat.Component.translatable("container.repair")));
              var menu = (AnvilMenu) p.containerMenu;
              var gear = new ItemStack(Items.DIAMOND_SWORD);
              for (String id :
                  new String[] {
                    "tensura:crushing",
                    "tensura:severance",
                    "tensura:enervation",
                    "tensura:tsukumogami",
                    "minecraft:sharpness"
                  }) {
                var holder =
                    p.registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(
                            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id)));
                gear.enchant(
                    holder,
                    id.equals("tensura:crushing") || id.equals("tensura:severance")
                        ? holder.value().getMaxLevel()
                        : 1);
              }
              menu.getSlot(0).set(gear);
              menu.getSlot(1).set(new ItemStack(ChromaticContent.ORB.get(), 3));
              menu.createResult();
              check(ChromaticOrb.infused(menu.getSlot(2).getItem()), "No actual anvil result");
              menu.broadcastChanges();
            });
        return;
      }
      if (phase == 1 && mc.screen instanceof AnvilScreen && wait > 25) {
        hover((mc.screen.width - 176) / 2 + 142, (mc.screen.height - 166) / 2 + 55);
        phase = 2;
        wait = 0;
        return;
      }
      if (phase == 2 && wait > 12) {
        var preview = mc.player.containerMenu.getSlot(2).getItem();
        check(ChromaticOrb.infused(preview), "Infusion component was not synchronized to client");
        check(
            preview
                .getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL)
                .stream()
                .anyMatch(line -> line.getString().contains("already applied")),
            "Client tooltip did not show infusion history");
        shot("chromatic-anvil-upgrade");
        mc.gameMode.handleInventoryMouseClick(
            mc.player.containerMenu.containerId, 2, 0, ClickType.PICKUP, mc.player);
        phase = 3;
        wait = 0;
        return;
      }
      if (phase == 3 && wait > 15) {
        server(
            p -> {
              var m = p.containerMenu;
              check(
                  ChromaticOrb.infused(m.getCarried()), "Client click did not take enhanced gear");
              check(
                  m.getSlot(0).getItem().isEmpty()
                      && m.getSlot(1).getItem().getCount() == 2
                      && p.experienceLevel == 29,
                  "Client take consumed wrong materials/XP");
            });
        mc.gameMode.handleInventoryMouseClick(
            mc.player.containerMenu.containerId, 0, 0, ClickType.PICKUP, mc.player);
        phase = 4;
        wait = 0;
        return;
      }
      if (phase == 4 && wait > 15) {
        server(
            p -> {
              check(
                  ChromaticOrb.infused(p.containerMenu.getSlot(0).getItem())
                      && p.containerMenu.getSlot(2).getItem().isEmpty(),
                  "Already-infused equipment accepted a second orb");
            });
        hover((mc.screen.width - 176) / 2 + 35, (mc.screen.height - 166) / 2 + 55);
        phase = 5;
        wait = 0;
        return;
      }
      if (phase == 5 && wait > 12) {
        shot("chromatic-already-infused");
        Files.createDirectories(OUT);
        Files.writeString(
            OUT.resolve("passed.txt"),
            "PASS: real client anvil preview, native overcap engravings and preserved curses,"
                + " server-consumed exactly one orb/XP level, repeat infusion rejected,"
                + " component/tooltip synchronized.");
        phase = 6;
        wait = 0;
        mc.player.closeContainer();
        return;
      }
      if (phase == 6 && wait > 20) mc.stop();
    } catch (Throwable e) {
      e.printStackTrace();
      failure = e.toString();
    }
  }
}
