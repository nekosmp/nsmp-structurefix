// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package dev.atakku.fsmp.structurefix.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureLocator;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.Structure.StructurePosition;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.atakku.fsmp.structurefix.StructureFix;

@Mixin(StructureLocator.class)
abstract class MixinStructureLocator {
  @Accessor()
  public abstract ChunkGenerator getChunkGenerator();

  @Accessor()
  public abstract NoiseConfig getNoiseConfig();

  @Accessor()
  public abstract HeightLimitView getWorld();

  @Redirect(method = "Lnet/minecraft/world/StructureLocator;isGenerationPossible(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/world/gen/structure/Structure;)Z", at = @At(value = "INVOKE", target = "Ljava/util/Optional;isPresent()Z"))
  private boolean isPresent(Optional<StructurePosition> pos, @Local Structure structure) {
    if (pos.isEmpty()) {
      return false;
    }

    StructurePosition sp = pos.get();
    BlockPos p = sp.position();
    int x = p.getX();
    int y = p.getY();
    int z = p.getZ();

    ChunkGenerator cg = this.getChunkGenerator();
    NoiseConfig nc = this.getNoiseConfig();
    HeightLimitView hlv = this.getWorld();

    IntArrayList heights = new IntArrayList();

    int r = 12;
    // Sample heightmap
    for (int ox = -r; ox <= r; ox++) {
      for (int oz = -r; oz <= r; oz++) {
        heights.add(cg.getHeightOnGround(x + ox, z + oz, Heightmap.Type.OCEAN_FLOOR_WG, hlv, nc));
      }
    }

    int min = Collections.min(heights);
    int max = Collections.max(heights);

    Identifier id = Registries.STRUCTURE_TYPE.getEntry(structure.getType()).getKey().get().getValue();

    StructureFix.LOGGER.info("Structure spawn {} at x:{} y:{} z:{}", id, x, y, z);
    if (y - min > 5) {
      StructureFix.LOGGER.info("Prevented structure {} spawn at x:{} y:{} z:{} due to too much drop", id, x, y, z);
      return false;
    }
    if (max - y > 7) {
      StructureFix.LOGGER.info("Would have prevented structure {} spawn at x:{} y:{} z:{} due to too much elevation", id, x, y, z);
      // TODO: testing for now
      // return false;
    }

    return true;
  }
}
