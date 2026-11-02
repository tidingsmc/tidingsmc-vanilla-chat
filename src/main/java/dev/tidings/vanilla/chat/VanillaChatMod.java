package dev.tidings.vanilla.chat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("tidings_vanilla_chat")
public class VanillaChatMod {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaChatMod.class);

    public VanillaChatMod(FMLJavaModLoadingContext context) {
        if (ModList.get().isLoaded("tidings")) {
            ChatAddon.register(context);
        } else {
            LOGGER.warn("Tidings isn't installed: Tidings Vanilla Chat does nothing without it");
        }
    }
}
