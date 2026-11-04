package dev.heavenlyrestriction.cartography;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Constant-memory, center-first square spiral. Units may be chunks or placement regions. */
public final class StructureSearchGrid implements Iterator<StructureSearchGrid.Cell> {
  public record Cell(int x, int z) {}

  private final int x, z, radius;
  private int ring, edge;

  public StructureSearchGrid(int x, int z, int radius) {
    if (radius < 0
        || radius > 1000000
        || (long) x - radius < Integer.MIN_VALUE
        || (long) x + radius > Integer.MAX_VALUE
        || (long) z - radius < Integer.MIN_VALUE
        || (long) z + radius > Integer.MAX_VALUE)
      throw new IllegalArgumentException("Invalid search grid");
    this.x = x;
    this.z = z;
    this.radius = radius;
  }

  @Override
  public boolean hasNext() {
    return ring <= radius;
  }

  @Override
  public Cell next() {
    if (!hasNext()) throw new NoSuchElementException();
    if (ring == 0) {
      ring = 1;
      return new Cell(x, z);
    }
    int side = ring * 2, segment = edge / side, offset = edge % side;
    int dx =
        switch (segment) {
          case 0 -> -ring + offset;
          case 1 -> ring;
          case 2 -> ring - offset;
          default -> -ring;
        };
    int dz =
        switch (segment) {
          case 0 -> -ring;
          case 1 -> -ring + offset;
          case 2 -> ring;
          default -> ring - offset;
        };
    if (++edge == side * 4) {
      edge = 0;
      ring++;
    }
    return new Cell(x + dx, z + dz);
  }
}
