// Copyright 2024 Atakku <https://atakku.dev>
//
// This project is dual licensed under MIT and Apache.

package rs.neko.smp.structurefix;

import static net.minecraft.server.command.CommandManager.literal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructureFix implements ModInitializer {
  public static final String MOD_ID = "nsmp-structurefix";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing NSMP StructureFix");
    CommandRegistrationCallback.EVENT.register((d, r, e) -> {
      d.register(literal("nsmp-structurefix").requires(s -> s.hasPermissionLevel(2)).then(literal("reload").executes(ctx -> {
        Config.loadFile();
        return 1;
      })));
    });
  }
}
