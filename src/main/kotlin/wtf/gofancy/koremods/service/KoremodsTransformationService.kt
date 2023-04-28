/*
 * This file is part of Koremods, licensed under the MIT License
 *
 * Copyright (c) 2021-2023 Garden of Fancy
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

import cpw.mods.modlauncher.api.IEnvironment
import cpw.mods.modlauncher.api.IModuleLayerManager
import cpw.mods.modlauncher.api.ITransformationService
import cpw.mods.modlauncher.api.ITransformer
import net.minecraftforge.fml.loading.targets.CommonLaunchHandler
import net.minecraftforge.fml.loading.targets.CommonUserdevLaunchHandler
import wtf.gofancy.koremods.EvalLoad
import wtf.gofancy.koremods.launch.KoremodsLaunch
import java.nio.file.Path

class KoremodsTransformationService : ITransformationService {

    override fun name(): String = throw UnsupportedOperationException()

    override fun initialize(environment: IEnvironment) {}

    override fun beginScanning(environment: IEnvironment): List<ITransformationService.Resource> {
        val gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
            .orElseThrow { IllegalStateException("Could not find game directory") }
        val configDir = gameDir.resolve("config")
        val modsDir = gameDir.resolve("mods")
        val discoveryURLs = getModClasses(environment)

        KoremodsLaunch.launch(EvalLoad, ModlauncherKoremodsLaunchPlugin, configDir, modsDir, discoveryURLs)

        return emptyList()
    }

    override fun completeScan(layerManager: IModuleLayerManager): List<ITransformationService.Resource> {
        ModlauncherKoremodsLaunchPlugin.verifyScriptPacks()

        return emptyList()
    }

    override fun onLoad(env: IEnvironment, otherServices: Set<String>) {}

    override fun transformers(): List<ITransformer<*>> = listOf(
        KoremodsClassTransformer,
        KoremodsMethodTransformer,
        KoremodsFieldTransformer
    )

    private fun getModClasses(environment: IEnvironment): Iterable<Path> {
        return environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get())
            .flatMap(environment::findLaunchHandler)
            .filter { it is CommonUserdevLaunchHandler }
            .map<List<Path>?> { handler ->
                (handler as CommonLaunchHandler).minecraftPaths.otherModPaths.firstOrNull()
                    ?.toList()
            }
            .orElseGet(::emptyList)
    }
}
