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

package dev.su5ed.koremods.modlaunch;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import dev.su5ed.koremods.prelaunch.KoremodsPrelaunch;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.versions.mcp.MCPVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class KoremodsTransformationService implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SERVICE_NAME = "koremods.asm.service";
    private static final String SPLASH_FACTORY_CLASS = "dev.su5ed.koremods.modlaunch.SplashScreenFactoryImpl";
    
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
        try {
            KoremodsPrelaunch prelaunch = new KoremodsPrelaunch(gameDir, MCPVersion.getMCVersion());
            prelaunch.launch(FMLLoader.getDist() == Dist.CLIENT ? SPLASH_FACTORY_CLASS : null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beginScanning(IEnvironment environment) {}

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) {}

    @SuppressWarnings("rawtypes")
    @Override
    public List<ITransformer> transformers() {
        LOGGER.info("Registering Koremods Transformer");
        
        return Collections.singletonList(new KoremodsTransformerWrapper());
    }
}
