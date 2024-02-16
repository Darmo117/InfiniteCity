package net.darmo_creations.infinitecity.chunk_gen;

public class ChunkCirclesManager {
  private final int circleWidth;
  private final int circleSpacing;
  private final int offsetX;
  private final int offsetZ;

  /**
   * Create a chunk circles manager.
   *
   * @param circleWidth   The width of each circle in chunks.
   * @param circleSpacing The space between each circle in chunks.
   * @param offsetX       The offset of the center from the 0-coordinate chunk along the X axis.
   * @param offsetZ       The offset of the center from the 0-coordinate chunk along the Z axis.
   */
  public ChunkCirclesManager(int circleWidth, int circleSpacing, int offsetX, int offsetZ) {
    this.circleWidth = circleWidth;
    this.circleSpacing = circleSpacing;
    this.offsetX = offsetX;
    this.offsetZ = offsetZ;
  }

  /**
   * Indicate whether the chunk at the given coordinates should be filled, based on this manager’s settings.
   *
   * @param chunkX Chunk’s X coord.
   * @param chunkZ Chunk’s Z coord.
   * @return True if the chunk should be filled, false if it should be empty.
   */
  public boolean shouldBeFilled(int chunkX, int chunkZ) {
    return !this.isInTorus(chunkX, chunkZ);
  }

  private boolean isInTorus(int x, int z) {
    final int extRadius = this.circleSpacing + this.circleWidth;
    final double dist = Math.hypot(x + this.offsetX, z + this.offsetZ);
    final double nextCircleCoef = Math.ceil(dist / extRadius);
    final double nextCircleExtRadius = extRadius * nextCircleCoef;
    return nextCircleCoef > 0 && dist <= nextCircleExtRadius && dist >= nextCircleExtRadius - this.circleWidth;
  }
}
