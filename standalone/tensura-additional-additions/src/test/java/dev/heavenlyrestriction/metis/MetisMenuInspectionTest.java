package dev.heavenlyrestriction.metis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MetisMenuInspectionTest {
  static class Mixed {
    public void onPressed(Object skill, Object owner, int key, int mode) {
      switch (mode) {
        case 0 -> attack();
        case 1 -> openMenu();
      }
    }
  }

  static class Helper {
    public void onPressed(Object skill, Object owner, int key, int mode) {
      indirect(mode);
    }

    private static void indirect(int selected) {
      int alias = selected;
      if (alias == 2) openMenu();
      else attack();
    }
  }

  static class Lambda {
    public void onPressed(Object skill, Object owner, int key, int mode) {
      Runnable action =
          () -> {
            if (mode == 1) openMenu();
            else attack();
          };
      action.run();
    }
  }

  static class Owner {
    boolean isShiftKeyDown() {
      return true;
    }
  }

  static class Shift {
    public void onPressed(Object skill, Owner owner, int key, int mode) {
      if (owner.isShiftKeyDown()) openMenu();
      else attack();
    }
  }

  static class Data {
    double getMastery() {
      return -1;
    }
  }

  static class Learned {
    public void onPressed(Data skill, Object owner, int key, int mode) {
      if (skill.getMastery() < 0) openMenu();
      else attack();
    }
  }

  static class Unknown {
    public void onPressed(Object skill, Object owner, int key, int mode) {
      if (System.nanoTime() > 0) openMenu();
    }
  }

  static class Caught {
    public void onPressed(Object skill, Object owner, int key, int mode) {
      try {
        if (System.nanoTime() > 0) throw new IllegalStateException();
      } catch (RuntimeException error) {
        recovery();
      }
    }

    private void recovery() {
      openMenu();
    }
  }

  @Test
  void followsMenuHelpersInExceptionHandlers() {
    assertTrue(MetisMenuInspection.blocks(Caught.class, 0, false));
  }

  static class GuiNames {
    public void onPressed(Object skill, Object owner, int key, int mode) {
      if (mode == 0) openCustomGui();
      else openHorseInventory();
    }

    static void openCustomGui() {}

    static void openHorseInventory() {}
  }

  @Test
  void excludesGuiAndInventoryAliases() {
    assertTrue(MetisMenuInspection.blocks(GuiNames.class, 0, false));
    assertTrue(MetisMenuInspection.blocks(GuiNames.class, 1, false));
  }

  static void openMenu() {}

  static void attack() {}

  @Test
  void separatesMixedSwitchModes() {
    assertFalse(MetisMenuInspection.blocks(Mixed.class, 0, false));
    assertTrue(MetisMenuInspection.blocks(Mixed.class, 1, false));
  }

  @Test
  void followsHelpersAndAliasesWithoutExecutingThem() {
    assertFalse(MetisMenuInspection.blocks(Helper.class, 0, false));
    assertTrue(MetisMenuInspection.blocks(Helper.class, 2, false));
  }

  @Test
  void followsCapturedLambdaModes() {
    assertFalse(MetisMenuInspection.blocks(Lambda.class, 0, false));
    assertTrue(MetisMenuInspection.blocks(Lambda.class, 1, false));
  }

  @Test
  void onlyAllowsMainActivationAndLearnedBranch() {
    assertFalse(MetisMenuInspection.blocks(Shift.class, 0, false));
    assertFalse(MetisMenuInspection.blocks(Learned.class, 0, false));
  }

  @Test
  void uncertainMenuPathsAreExcluded() {
    assertTrue(MetisMenuInspection.blocks(Unknown.class, 0, false));
  }
}
