// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package rs.neko.smp.structurefix.mixin;

import java.util.function.Predicate;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Structure.Context.class)
public interface AccessorStructureContext {
  @Accessor
  Predicate<RegistryEntry<Biome>> getBiomePredicate();

  @Accessor
  ChunkGenerator getChunkGenerator();

  @Accessor
  NoiseConfig getNoiseConfig();
}
