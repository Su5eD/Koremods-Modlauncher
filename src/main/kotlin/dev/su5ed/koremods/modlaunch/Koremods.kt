package dev.su5ed.koremods.modlaunch

import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod("koremods")
class Koremods {
    private val logger: Logger = LogManager.getLogger()
    
    init {
        logger.info("Mod Constructed")
    }
}
