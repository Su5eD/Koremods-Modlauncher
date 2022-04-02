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

package wtf.gofancy.koremods.service;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.gofancy.koremods.prelaunch.KoremodsPrelaunch;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class KoremodsServiceWrapper implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SERVICE_NAME = "koremods.asm.service";

    private ITransformationService actualTransformationService;

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    public void initialize(IEnvironment environment) {
        LOGGER.info("Setting up Koremods environment");

        Path gameDir = environment.getProperty(IEnvironment.Keys.GAMEDIR.get())
            .orElseThrow(() -> new IllegalStateException("Could not find game directory"));

        try {
            URL jarLocation = getCurrentLocation();
            KoremodsPrelaunch prelaunch = new KoremodsPrelaunch(gameDir, jarLocation);

            URL kotlinDep = prelaunch.extractDependency("Kotlin");
            ClassLoader parentCL = getClass().getClassLoader();
            ClassLoader classLoader = new MLDependencyClassLoader(new URL[] { prelaunch.mainJarUrl, kotlinDep }, parentCL, KoremodsPrelaunch.KOTLIN_DEP_PACKAGES);

            Object actualITS = classLoader.loadClass("wtf.gofancy.koremods.service.KoremodsTransformationService").getConstructor(KoremodsPrelaunch.class).newInstance(prelaunch);
            this.actualTransformationService = (ITransformationService) actualITS;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        return this.actualTransformationService.beginScanning(environment);
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        return this.actualTransformationService.completeScan(layerManager);
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {}

    @SuppressWarnings("rawtypes")
    @Override
    public List<ITransformer> transformers() {
        return this.actualTransformationService.transformers();
    }

    public URL getCurrentLocation() throws URISyntaxException, MalformedURLException {
        URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
        // Thanks, SJH
        return new URI("file", null, URLDecoder.decode(jarLocation.getPath(), StandardCharsets.UTF_8).split("#")[0], null).toURL();
    }
}
