package wtf.gofancy.koremods.modlaunch

import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.loading.progress.StartupMessageManager
import org.apache.logging.log4j.LogManager
import wtf.gofancy.koremods.KoremodsDiscoverer
import wtf.gofancy.koremods.api.KoremodsLaunchPlugin
import wtf.gofancy.koremods.api.SplashScreen
import wtf.gofancy.koremods.prelaunch.KoremodsBlackboard
import wtf.gofancy.koremods.prelaunch.KoremodsPrelaunch
import wtf.gofancy.koremods.splash.KoremodsSplashScreen
import java.nio.file.Path

class KoremodsPlugin : KoremodsLaunchPlugin {
    companion object {
        private val LOGGER = LogManager.getLogger()
    }
    
    override fun shouldEnableSplashScreen(): Boolean = FMLLoader.getDist().isClient

    override fun createSplashScreen(prelaunch: KoremodsPrelaunch): SplashScreen {
        val logger = KoremodsBlackboard.createLogger("Splash")
        val splash: SplashScreen = KoremodsSplashScreen(logger)
        
        splash.startOnThread()
        
        return splash
    }

    override fun appendLogMessage(message: String) {
        StartupMessageManager.addModMessage("[${KoremodsBlackboard.NAME}] $message")
    }

    override fun verifyScriptPacks(mods: Map<String, Path>) {
        LOGGER.info("Verifying script packs")

        KoremodsDiscoverer.transformers.forEach { pack ->
            mods.forEach { (modid, source) ->
                if (pack.namespace == modid && pack.path != source) {
                    LOGGER.error("Source location of namespace ${pack.namespace} doesn't match the location of its mod")
                } else if (pack.path == source && pack.namespace != modid) {
                    LOGGER.error("Namespace ${pack.namespace} doesn't match the modid $modid found within at the same location")
                }
            }
        }
    }
}
