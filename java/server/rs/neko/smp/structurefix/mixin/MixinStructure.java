// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package rs.neko.smp.structurefix.mixin;

import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.Context;
import net.minecraft.world.gen.structure.Structure.StructurePosition;
import net.minecraft.world.gen.structure.StructureType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rs.neko.smp.structurefix.Config;
import rs.neko.smp.structurefix.StructureFix;

@Mixin(Structure.class)
abstract class MixinStructure {
  @Inject(method = "getValidStructurePosition(Lnet/minecraft/world/gen/structure/Structure$Context;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
  public void getValidStructurePosition(Context ctx, CallbackInfoReturnable<Optional<StructurePosition>> cir) {
    Optional<StructurePosition> old = cir.getReturnValue();

    if (old.isEmpty()) {
      return;
    }

    Identifier id = getIdentifier();
    int rad = Config.getRadius(id);
    if (rad <= 0) {
      // temporary
      StructureFix.LOGGER.info("Skipping structure '{}'", id);
      return;
    }

    cir.setReturnValue(old.filter(sp -> {
      BlockPos pos = sp.position();
      int x = BiomeCoords.fromBlock(pos.getX());
      int y = BiomeCoords.fromBlock(pos.getY());
      int z = BiomeCoords.fromBlock(pos.getZ());

      // Ducked the context for access
      AccessorStructureContext c = ((AccessorStructureContext) (Object) ctx);
      Predicate<RegistryEntry<Biome>> p = c.getBiomePredicate();
      BiomeSource b = c.getChunkGenerator().getBiomeSource();
      MultiNoiseSampler s = c.getNoiseConfig().getMultiNoiseSampler();

      int r = BiomeCoords.fromBlock(rad);

      for (int ox= -r; ox<=r; ox++) {
        for (int oz= -r; oz<=r; oz++) {
          if (ox == 0 && oz == 0) {
            continue;
          }

          if (!p.test(b.getBiome(x + oz, y, z + oz, s))) {
            StructureFix.LOGGER.info("Prevented structure '{}' spawn at x:{} y:{} z:{}", id, pos.getX(), pos.getY(), pos.getZ());
            return false;
          }
        }
      }
      return true;
    }));
  }

  private Identifier getIdentifier() {
    if ((Object) this instanceof JigsawStructure) {
      return ((AccessorJigsawStructure)(Object)this).getStartPool().getKey().get().getValue();
    }
    StructureType<?> type = ((Structure) (Object) this).getType();
    return Registries.STRUCTURE_TYPE.getKey(type).get().getValue();
  }
}
