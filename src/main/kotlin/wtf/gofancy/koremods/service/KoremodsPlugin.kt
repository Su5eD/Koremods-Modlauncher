/*
 * This file is part of Koremods, licensed under the MIT License
 *
 * Copyright (c) 2021-2022 Garden of Fancy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package wtf.gofancy.koremods.service

import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.loading.progress.StartupMessageManager
import net.minecraftforge.forgespi.language.IModInfo
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import wtf.gofancy.koremods.KoremodsDiscoverer
import wtf.gofancy.koremods.api.KoremodsLaunchPlugin
import wtf.gofancy.koremods.api.SplashScreen
import wtf.gofancy.koremods.prelaunch.KoremodsBlackboard
import wtf.gofancy.koremods.prelaunch.KoremodsPrelaunch
import wtf.gofancy.koremods.splash.KoremodsSplashScreen

object KoremodsPlugin : KoremodsLaunchPlugin {
    private val LOGGER = LogManager.getLogger()
    
    override fun shouldEnableSplashScreen(): Boolean = FMLLoader.getDist().isClient

    override fun createSplashScreen(prelaunch: KoremodsPrelaunch): SplashScreen {
        val logger = KoremodsBlackboard.createLogger("Splash")
        val splash: SplashScreen = KoremodsSplashScreen(logger)
        
        splash.startOnThread()
        
        return splash
    }

    override fun appendLogMessage(level: Level, message: String) {
        StartupMessageManager.addModMessage("[${KoremodsBlackboard.NAME}] $message")
    }

    internal fun verifyScriptPacks() {
        val modList = FMLLoader.getLoadingModList()
        val mods = modList.mods
            .map(IModInfo::getModId)
            .associateWith { modid -> modList.getModFileById(modid).file.filePath }
        
        LOGGER.info("Verifying script packs")

        KoremodsDiscoverer.INSTANCE?.scriptPacks?.forEach { pack ->
            mods.forEach { (modid, source) ->
                if (pack.namespace == modid && pack.path != source) {
                    throw RuntimeException("Source location of namespace '${pack.namespace}' doesn't match the location of its mod")
                } else if (pack.path == source && pack.namespace != modid) {
                    throw RuntimeException("Namespace '${pack.namespace}' doesn't match the modid '$modid' found at the same location")
                }
            }
        }
    }
}
