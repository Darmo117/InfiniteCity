/**
 * This mixin disables the experimental settings warnings.
 * <p>
 * Code from @rdvdev2 on GitHub. See license in package.
 *
 * @see https://github.com/rdvdev2/DisableCustomWorldsAdvice/tree/1.20.4
 */
package net.darmo_creations.infinitecity.mixins;

import net.minecraft.server.integrated.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(IntegratedServerLoader.class)
public abstract class MixinIntegratedServerLoader {
  // Set canShowBackupPrompt = false
  @ModifyVariable(
      method = "start(Lnet/minecraft/world/level/storage/LevelStorage$Session;Lcom/mojang/serialization/Dynamic;ZZLjava/lang/Runnable;)V",
      at = @At("HEAD"),
      argsOnly = true,
      index = 4
  )
  private boolean removeAdviceOnLoad(boolean original) {
    return false;
  }

  // Set bypassWarnings = true
  @ModifyVariable(
      method = "tryLoad",
      at = @At("HEAD"),
      argsOnly = true,
      index = 4
  )
  private static boolean removeAdviceOnCreation(boolean original) {
    return true;
  }
}