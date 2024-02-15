package net.darmo_creations.infinitecity.chunk_gen;

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

  public void update(int x, int z) {
    this.update(x, x, z, z);
  }

  public void update(int fromX, int toX, int fromZ, int toZ) {
    this.minX = Math.min(this.minX, fromX);
    this.maxX = Math.max(this.maxX, toX);
    this.minZ = Math.min(this.minZ, fromZ);
    this.maxZ = Math.max(this.maxZ, toZ);
  }
}
