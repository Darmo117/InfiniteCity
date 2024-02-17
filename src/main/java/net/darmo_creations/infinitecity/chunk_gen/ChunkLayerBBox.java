package net.darmo_creations.infinitecity.chunk_gen;

/**
 * This class represents the area of a chunk layer where any blocks may be encountered.
 */
class ChunkLayerBBox {
  private int minX = 16, maxX = -1;
  private int minZ = 16, maxZ = -1;

  public int minX() {
    return this.minX;
  }

  public int maxX() {
    return this.maxX;
  }

  public int minZ() {
    return this.minZ;
  }

  public int maxZ() {
    return this.maxZ;
  }

  /**
   * Update the bounds with the given (x, z) coordinates.
   * Equivalent to {@code update(x, x, z, z)}.
   *
   * @param x The X coordinate to take into account.
   * @param z The Z coordinate to take into account.
   * @see #update(int, int, int, int)
   */
  public void update(int x, int z) {
    this.update(x, x, z, z);
  }

  /**
   * Update the bounds with the given ((x0, z0), (x1, z1)) coordinates.
   *
   * @param fromX The start X coordinate to take into account.
   * @param toX   The end X coordinate to take into account (inclusive).
   * @param fromZ The start Z coordinate to take into account.
   * @param toZ   The end Z coordinate to take into account (inclusive).
   */
  public void update(int fromX, int toX, int fromZ, int toZ) {
    this.minX = Math.min(this.minX, fromX);
    this.maxX = Math.max(this.maxX, toX);
    this.minZ = Math.min(this.minZ, fromZ);
    this.maxZ = Math.max(this.maxZ, toZ);
  }
}
