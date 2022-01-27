package wtf.gofancy.koremods.service

import cpw.mods.modlauncher.api.IEnvironment
import cpw.mods.modlauncher.api.IModuleLayerManager
import cpw.mods.modlauncher.api.ITransformationService
import cpw.mods.modlauncher.api.ITransformer
import net.minecraftforge.fml.loading.targets.CommonLaunchHandler
import net.minecraftforge.fml.loading.targets.CommonUserdevLaunchHandler
import wtf.gofancy.koremods.launch.KoremodsLaunch
import wtf.gofancy.koremods.prelaunch.KoremodsPrelaunch
import java.net.URL

class KoremodsTransformationService(private val prelaunch: KoremodsPrelaunch) : ITransformationService {
    companion object {
        private val LIBRARIES = arrayOf("asm", "asm-analysis", "asm-commons", "asm-tree", "asm-util")
    }

    override fun name(): String = throw UnsupportedOperationException()

    override fun initialize(environment: IEnvironment) {}

    override fun beginScanning(environment: IEnvironment): List<ITransformationService.Resource> {
        val discoveryURLs = getModClasses(environment)
        
        KoremodsLaunch.launch(prelaunch, discoveryURLs, LIBRARIES, KoremodsPlugin)
        
        return emptyList()
    }

    override fun completeScan(layerManager: IModuleLayerManager): List<ITransformationService.Resource> {
        KoremodsPlugin.verifyScriptPacks()
        
        return emptyList()
    }

    override fun onLoad(env: IEnvironment, otherServices: Set<String>) {}

    override fun transformers(): List<ITransformer<*>> = listOf(KoremodsTransformer)

    private fun getModClasses(environment: IEnvironment): Array<URL> {
        return environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get())
            .flatMap(environment::findLaunchHandler)
            .filter { it is CommonUserdevLaunchHandler }
            .map<Array<URL>?> { handler -> 
                (handler as CommonLaunchHandler).minecraftPaths.otherModPaths.firstOrNull()
                    ?.map { path -> path.toUri().toURL() }
                    ?.toTypedArray()
            }
            .orElseGet(::emptyArray)
    }
}
