package dev.su5ed.koremods.modlaunch

import cpw.mods.modlauncher.api.IEnvironment
import cpw.mods.modlauncher.api.ITransformationService
import cpw.mods.modlauncher.api.ITransformer
import dev.su5ed.koremods.KoremodDiscoverer
import dev.su5ed.koremods.preloadScriptEngine
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.file.Paths

class KoremodsTransformationService : ITransformationService {
    private val logger: Logger = LogManager.getLogger()
    
    override fun name(): String = "koremods.asm.service"

    override fun initialize(environment: IEnvironment) {
        logger.info("Setting up Koremods environment")
        
        logger.debug("Locating game directory")
        val gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
            .orElseThrow { IllegalStateException("Could not find game directory") }
        
        val modsDir = gameDir.resolve("mods")
        val classpath = System.getenv("MOD_CLASSES")
            ?.split(File.pathSeparator)
            ?.map { it.split("%%").last() }
            ?.map(Paths::get)
            ?.map { it.toUri().toURL() }
            ?.toTypedArray()
            ?: emptyArray()
        
        KoremodDiscoverer.discoverKoremods(modsDir, classpath)
    }

    override fun beginScanning(environment: IEnvironment) {}

    override fun onLoad(env: IEnvironment, otherServices: Set<String>) {
        preloadScriptEngine(logger)
    }

    override fun transformers(): List<ITransformer<ClassNode>> {
        logger.info("Registering Koremods Transformer")
        
        return listOf(KoremodsTransformer())
    }
}
