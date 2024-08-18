// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package rs.neko.smp.structurefix.mixin;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.JigsawStructure;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.Context;
import net.minecraft.world.gen.structure.Structure.StructurePosition;
import net.minecraft.world.gen.structure.StructureType;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rs.neko.smp.structurefix.Config;
import rs.neko.smp.structurefix.StructureFix;

@Mixin(Structure.class)
abstract class MixinStructure {
  @Inject(method = "getValidStructurePosition(Lnet/minecraft/world/gen/structure/Structure$Context;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
  public void getValidStructurePosition(Context c, CallbackInfoReturnable<Optional<StructurePosition>> cir) {
    Optional<StructurePosition> old = cir.getReturnValue();

    if (old.isEmpty()) {
      return;
    }

    Pair<Identifier, Integer> rad = getStructureRadius();
    if (rad == null || rad.second() == 0)
      return;

    cir.setReturnValue(old.filter(sp -> {
      BlockPos pos = sp.position();
      int x = BiomeCoords.fromBlock(pos.getX());
      int y = BiomeCoords.fromBlock(pos.getY());
      int z = BiomeCoords.fromBlock(pos.getZ());

      Predicate<RegistryEntry<Biome>> p = c.biomePredicate();
      ChunkGenerator cg = c.chunkGenerator();
      BiomeSource bs = cg.getBiomeSource();
      NoiseConfig nc = c.noiseConfig();
      MultiNoiseSampler mns = nc.getMultiNoiseSampler();
      HeightLimitView hlv = c.world();

      int r = BiomeCoords.fromBlock(rad.right());

      IntArrayList heights = new IntArrayList();

      // Check for bordering with unwanted biomes (e.g. rivers), and sample flatness
      for (int ox= -r; ox<=r; ox++) {
        for (int oz= -r; oz<=r; oz++) {
          if (ox == 0 && oz == 0) {
            continue;
          }

          int cx = x + ox;
          int cz = z + oz;

          //if (!p.test(bs.getBiome(cx, y, cz, mns))) {
          //  StructureFix.LOGGER.info("Prevented structure '{}' spawn at x:{} y:{} z:{} due to bordering a forbidden biome", rad.left(), pos.getX(), pos.getY(), pos.getZ());
          //  return false;
          //}
          heights.add(cg.getHeightInGround(BiomeCoords.toBlock(cx), BiomeCoords.toBlock(cz), Heightmap.Type.OCEAN_FLOOR_WG, hlv, nc));
        }
      }

      int min = Collections.min(heights);
      int mid = pos.getY();
      if (mid - min > 4) {
        StructureFix.LOGGER.info("Prevented structure '{}' spawn at x:{} y:{} z:{} due to too much drop", rad.left(), pos.getX(), pos.getY(), pos.getZ());
        return false;
      }
      int max = Collections.max(heights);
      if (max - mid > 7) {
        StructureFix.LOGGER.info("Prevented structure '{}' spawn at x:{} y:{} z:{} due to too much elevation", rad.left(), pos.getX(), pos.getY(), pos.getZ());
        // TODO: testing for now
        //return false;
      }
      return true;
    }));
  }

  private Pair<Identifier, Integer> getStructureRadius() {
    StructureType<?> type = ((Structure) (Object) this).getType();
    Identifier fallback = Registries.STRUCTURE_TYPE.getKey(type).get().getValue();
    if ((Object) this instanceof JigsawStructure) {
      Identifier id = ((AccessorJigsawStructure)(Object)this).getStartPool().getKey().get().getValue();
      Integer rad = Config.getRadius(id);
      if (rad != null)
        return Pair.of(id, rad);
      StructureFix.LOGGER.info("No radius defined for jigsaw structure '{}', falling back to structure type '{}'", id, fallback);
    }
    Integer rad = Config.getRadius(fallback);
    if (rad != null)
        return Pair.of(fallback, rad);
    StructureFix.LOGGER.info("No radius defined for structure type '{}', skipping", fallback);
    return null;
  }
}
