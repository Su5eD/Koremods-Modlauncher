/*
 * This file is part of Koremods, licensed under the MIT License
 *
 * Copyright (c) 2021 Garden of Fancy
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

package dev.su5ed.koremods.modlaunch

import cpw.mods.modlauncher.api.IEnvironment
import cpw.mods.modlauncher.api.ITransformationService
import cpw.mods.modlauncher.api.ITransformer
import dev.su5ed.koremods.KoremodDiscoverer
import dev.su5ed.koremods.KoremodsBlackboard
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
        val cacheDir = modsDir.resolve("koremods").resolve("cache").toFile()
        val classpath = System.getenv("MOD_CLASSES")
            ?.split(File.pathSeparator)
            ?.map { it.split("%%").last() }
            ?.map(Paths::get)
            ?.map { it.toUri().toURL() }
            ?.toTypedArray()
            ?: emptyArray()
        
        cacheDir.mkdir()
        KoremodsBlackboard.cacheDir = cacheDir
        KoremodDiscoverer.discoverKoremods(modsDir, classpath)
    }

    override fun beginScanning(environment: IEnvironment) {}

    override fun onLoad(env: IEnvironment, otherServices: Set<String>) {}

    override fun transformers(): List<ITransformer<ClassNode>> {
        logger.info("Registering Koremods Transformer")
        
        return listOf(KoremodsTransformer())
    }
}
