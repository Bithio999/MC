package dev.heavenlyrestriction.forge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class ArsenalSequenceTest {
  @Test
  void everyRosterSizeKeepsTheSameFullCycleWithoutSimultaneousLaunches() {
    for (int size = 1; size <= 6; size++) {
      var clock = new ArsenalSequence();
      int[] roster = java.util.stream.IntStream.range(0, size).toArray();
      var times = new HashMap<Integer, List<Integer>>();
      for (int tick = 0; tick < 160; tick++) {
        int launches = 0;
        // Reverse entity ticking order must not influence spacing or order.
        for (int blade = size - 1; blade >= 0; blade--)
          if (clock.launch(blade, tick, roster)) {
            launches++;
            times.computeIfAbsent(blade, k -> new ArrayList<>()).add(tick);
          }
        assertTrue(launches <= 1);
      }
      for (var sequence : times.values())
        for (int n = 1; n < sequence.size(); n++)
          assertEquals(24, sequence.get(n) - sequence.get(n - 1));
      assertEquals(size, times.size());
    }
  }

  @Test
  void longIdleDoesNotCreateACatchUpVolleyAndSparseRosterCanChange() {
    var clock = new ArsenalSequence();
    int[] roster = {0, 2, 5};
    assertFalse(clock.launch(0, 0, roster));
    assertTrue(clock.launch(0, 4, roster));
    assertTrue(clock.launch(2, 500, roster));
    assertFalse(clock.launch(5, 500, roster));
    assertTrue(clock.launch(5, 508, roster));
    int[] changed = {2, 5};
    assertFalse(clock.launch(2, 509, changed));
    assertTrue(clock.launch(2, 513, changed));
    assertTrue(clock.launch(5, 525, changed));
  }
}
