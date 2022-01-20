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

package wtf.gofancy.koremods.service;

import cpw.mods.modlauncher.api.*;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.targets.CommonLaunchHandler;
import net.minecraftforge.fml.loading.targets.CommonUserdevLaunchHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import wtf.gofancy.koremods.prelaunch.KoremodsPrelaunch;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class KoremodsTransformationService implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SERVICE_NAME = "koremods.asm.service";
    private static final String LAUNCH_PLUGIN_CLASS = "wtf.gofancy.koremods.service.KoremodsPlugin";
    private static final String TRANSFORMER_CLASS = "wtf.gofancy.koremods.service.KoremodsTransformer";
    private static final String[] LIBRARIES = new String[] {
            "asm", "asm-analysis", "asm-commons", "asm-tree", "asm-util"
    };

    private static KoremodsPrelaunch prelaunch;

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    public void initialize(IEnvironment environment) {
        LOGGER.info("Setting up Koremods environment");

        LOGGER.debug("Locating game directory");
        Path gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(() -> new IllegalStateException("Could not find game directory"));
        String mcVersion = FMLLoader.versionInfo().mcVersion();
        try {
            URL jarLocation = getCurrentLocation();
            prelaunch = new KoremodsPrelaunch(gameDir, mcVersion, jarLocation);
            
            URL[] discoveryURLs = getModClasses(environment);
            URL kotlinDep = prelaunch.extractDependency("Kotlin");
            ClassLoader classLoader = new MLDependencyClassLoader(new URL[] { prelaunch.mainJarUrl, kotlinDep }, getClass().getClassLoader(), KoremodsPrelaunch.KOTLIN_DEP_PACKAGES);
            prelaunch.launch(LAUNCH_PLUGIN_CLASS, discoveryURLs, LIBRARIES, classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        prelaunch.getLaunchPlugin().verifyScriptPacks();
        
        return List.of();
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {}

    @SuppressWarnings("rawtypes")
    @Override
    public List<ITransformer> transformers() {
        LOGGER.info("Registering Koremods Transformer");

        try {
            Class<?> cls = prelaunch.getDependencyClassLoader().loadClass(TRANSFORMER_CLASS);
            //noinspection unchecked
            ITransformer<ClassNode> transformer = (ITransformer<ClassNode>) cls.getConstructor().newInstance();
            return List.of(transformer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize KoremodsTransformer", e);
        }
    }

    private URL[] getModClasses(IEnvironment environment) {
        return environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get())
                .flatMap(environment::findLaunchHandler)
                .filter(CommonUserdevLaunchHandler.class::isInstance)
                .flatMap(handler -> {
                    List<List<Path>> otherModPaths = ((CommonLaunchHandler) handler).getMinecraftPaths().otherModPaths();
                    return otherModPaths.size() > 0 ? Optional.of(otherModPaths.get(0).stream()
                            .map(LamdbaExceptionUtils.rethrowFunction(path -> path.toUri().toURL()))
                            .toArray(URL[]::new))
                            : Optional.empty();
                })
                .orElseGet(() -> new URL[0]);
    }

    public URL getCurrentLocation() throws URISyntaxException, MalformedURLException {
        URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        // Thanks, SJH
        return new URI("file", null, URLDecoder.decode(jarLocation.getPath(), StandardCharsets.UTF_8).split("#")[0], null).toURL();
    }
}
