package dev.heavenlyrestriction.cartography;

/** Cursor-anchored zoom and block/screen conversion, independent of rendering and networking. */
public final class MapView {
  public static final double MIN_SCALE = .125, MAX_SCALE = 8, LIMIT = 29999984;
  public double x, z, scale = 1;

  public void center(double x, double z) {
    if (Double.isFinite(x) && Double.isFinite(z)) {
      this.x = Math.clamp(x, -LIMIT, LIMIT);
      this.z = Math.clamp(z, -LIMIT, LIMIT);
    }
  }

  public double worldX(double screenX, double left, double width) {
    return x + (screenX - left - width / 2) / scale;
  }

  public double worldZ(double screenY, double top, double height) {
    return z + (screenY - top - height / 2) / scale;
  }

  public double screenX(double blockX, double left, double width) {
    return left + width / 2 + (blockX - x) * scale;
  }

  public double screenZ(double blockZ, double top, double height) {
    return top + height / 2 + (blockZ - z) * scale;
  }

  public void drag(double dx, double dy) {
    center(x - dx / scale, z - dy / scale);
  }

  public void zoom(
      double steps, double sx, double sy, double left, double top, double width, double height) {
    if (!Double.isFinite(steps)) return;
    double wx = worldX(sx, left, width), wz = worldZ(sy, top, height);
    scale = Math.clamp(scale * Math.pow(1.35, Math.clamp(steps, -8, 8)), MIN_SCALE, MAX_SCALE);
    center(wx - (sx - left - width / 2) / scale, wz - (sy - top - height / 2) / scale);
  }
}
