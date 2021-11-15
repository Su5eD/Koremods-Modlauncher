package dev.su5ed.koremods.modlaunch;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("koremods")
public class Koremods { // TODO
    private static final Logger LOGGER = LogManager.getLogger();
    
    public Koremods() {
        LOGGER.info("Mod Constructed");
    }
}
