// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package dev.atakku.fsmp.structurefix;

import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;

public class Config {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static Object2IntOpenHashMap<String> config = null;

  public static Object2IntOpenHashMap<String> getConfig() {
    if (config == null) {
      loadFile();
      saveFile();
    }
    return config;
  }

  private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fsmp-structurefix.json");

  public static void loadFile() {
    String json = "{}";
    try {
      json = Files.readString(PATH);
      config = JsonHelper.deserialize(GSON, json, new TypeToken<Object2IntOpenHashMap<String>>() {
      });
    } catch (Exception ex) {
      StructureFix.LOGGER.warn("Failed to load json from {}: {}", PATH, ex);
      config = new Object2IntOpenHashMap<String>();
    }
  }

  public static void saveFile() {
    try {
      Files.writeString(PATH, GSON.toJson(config));
    } catch (Exception ex) {
      StructureFix.LOGGER.warn("Failed to save json to {}: {}", PATH, ex);
    }
  }

  public static Integer getRadius(Identifier id) {
    String id_string = id.toString();
    if (!getConfig().containsKey(id_string))
      return null;
    return Math.max(getConfig().getInt(id_string) / 2, 0);
  }
}
