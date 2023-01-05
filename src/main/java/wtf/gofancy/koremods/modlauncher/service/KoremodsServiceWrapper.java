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

package wtf.gofancy.koremods.modlauncher.service;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class KoremodsServiceWrapper implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SERVICE_NAME = "koremods.asm.service";
    
    private static final String JIJ_ATTRIBUTE_PREFIX = "Additional-Dependencies-";
    private static final String KOTLIN_JIJ_NAME = "Kotlin";
    private static final String MOD_JIJ_NAME = "Mod";

    static Path modJijPath;
    
    private ITransformationService actualTransformationService;

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    public void initialize(IEnvironment environment) {
        LOGGER.info("Setting up Koremods environment");

        try {
            URL jarLocation = getClass().getProtectionDomain().getCodeSource().getLocation();
            Path path = Path.of(jarLocation.toURI());
            Manifest manifest = new Manifest();
            Path manifestPath = path.resolve("META-INF/MANIFEST.MF");
            manifest.read(Files.newInputStream(manifestPath));
            Attributes attributes = manifest.getMainAttributes();
            SecureJar kotlinJij = SecureJar.from(getJarInJar(path, attributes, KOTLIN_JIJ_NAME));
            modJijPath = getJarInJar(path, attributes, MOD_JIJ_NAME);

            ClassLoader parentCL = getClass().getClassLoader();
            ClassLoader classLoader = new DependencyClassLoader(new URL[]{jarLocation}, parentCL, kotlinJij);

            Object actualITS = classLoader.loadClass("wtf.gofancy.koremods.modlauncher.service.KoremodsTransformationService").getConstructor().newInstance();
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
    public void onLoad(IEnvironment env, Set<String> otherServices) {
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<ITransformer> transformers() {
        return this.actualTransformationService.transformers();
    }
    
    private Path getJarInJar(Path path, Attributes attributes, String name) {
        String depName = attributes.getValue(JIJ_ATTRIBUTE_PREFIX + name);
        if (depName == null) throw new IllegalArgumentException("Required " + name + " embedded jar not found");
        
        return path.resolve(depName);
    }
}
