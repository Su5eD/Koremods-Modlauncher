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

package wtf.gofancy.koremods.modlauncher.service

import cpw.mods.jarhandling.SecureJar
import net.minecraftforge.fml.loading.FMLLoader
import net.minecraftforge.fml.loading.progress.StartupMessageManager
import net.minecraftforge.forgespi.language.IModInfo
import org.apache.commons.lang3.SystemUtils
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import wtf.gofancy.koremods.dsl.ClassTransformerParams
import wtf.gofancy.koremods.dsl.FieldTransformerParams
import wtf.gofancy.koremods.dsl.MethodTransformerParams
import wtf.gofancy.koremods.dsl.TransformerPropertiesStore
import wtf.gofancy.koremods.launch.KoremodsLaunch
import wtf.gofancy.koremods.launch.KoremodsLaunchPlugin
import wtf.gofancy.koremods.modlauncher.dsl.*
import java.nio.file.FileSystems
import java.nio.file.Path

class ModlauncherKoremodsLaunchPlugin : KoremodsLaunchPlugin {
    companion object {
        private val LOGGER = LogManager.getLogger()
        private val DEFAULT_FS = FileSystems.getDefault()

        internal fun verifyScriptPacks() {
            val modList = FMLLoader.getLoadingModList()
            val mods = modList.mods
                .map(IModInfo::getModId)
                .associateWith { modid -> modList.getModFileById(modid).file.filePath.toAbsolutePath().normalize() }

            LOGGER.info("Verifying script packs")

            KoremodsLaunch.LOADER!!.scriptPacks.forEach { pack ->
                mods.forEach { (modid, source) ->
                    val packPath = pack.path.toAbsolutePath().normalize()
                    if (pack.namespace == modid && packPath != source) {
                        LOGGER.error("Expected path $source, got $packPath")
                        throw RuntimeException("Source location of namespace '${pack.namespace}' doesn't match the location of its mod")
                    } else if (packPath == source && pack.namespace != modid) {
                        throw RuntimeException("Namespace '${pack.namespace}' doesn't match the modid '$modid' found at the same location")
                    }
                }
            }
        }

        private fun getPathToScript(base: Path): Path {
            if (base.fileSystem == DEFAULT_FS) {
                return SecureJar.from(base).rootPath
            } else if (base.fileSystem.provider().scheme == "jar") {
                val uri = base.toUri().schemeSpecificPart.removePrefix("file://").let { str ->
                    if (SystemUtils.IS_OS_WINDOWS) str.removePrefix("/")
                    else str
                }
                val parts = uri.split("!/")
                val jar = SecureJar.from(Path.of(parts[0]))
                val scriptJar = SecureJar.from(jar.getPath(parts[1]))
                return scriptJar.rootPath
            }
            throw RuntimeException("Unknown base path file system")
        }
    }

    override val allowedClasses: List<String> = listOf(
        "wtf.gofancy.koremods.modlauncher.dsl."
    )

    override val defaultImports: List<String> = listOf(
        "wtf.gofancy.koremods.modlauncher.dsl.*"
    )

    override fun appendLogMessage(level: Level, message: String) {
        StartupMessageManager.addModMessage("[Koremods] $message")
    }

    override fun createCompiledScriptClassLoader(path: Path, parent: ClassLoader?): ClassLoader {
        val scriptPath = getPathToScript(path)
        return CompiledScriptClassLoader(scriptPath, parent)
    }

    override fun mapClassTransformer(params: ClassTransformerParams, props: TransformerPropertiesStore): ClassTransformerParams {
        return if (props[TransformerPropertiesStore.autoRemap] == true) params.copy(name = mapClassName(params.name))
        else params
    }

    override fun mapMethodTransformer(params: MethodTransformerParams, props: TransformerPropertiesStore): MethodTransformerParams {
        return if (props[TransformerPropertiesStore.autoRemap] == true) params.copy(owner = mapClassName(params.owner), name = mapMethodName(params.name), desc = mapMethodDesc(params.desc))
        else params
    }

    override fun mapFieldTransformer(params: FieldTransformerParams, props: TransformerPropertiesStore): FieldTransformerParams {
        return if (props[TransformerPropertiesStore.autoRemap] == true) params.copy(owner = mapClassName(params.owner), name = mapFieldName(params.name))
        else params
    }
}
