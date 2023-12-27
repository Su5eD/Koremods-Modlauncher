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

package wtf.gofancy.koremods.mod;

import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wtf.gofancy.koremods.modlauncher.bootstrap.KoremodsServiceWrapper;

import java.lang.reflect.Field;
import java.util.Map;

@Mod(KoremodsMod.MODID)
public class KoremodsMod {
    public static final String MODID = "koremods";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KoremodsMod() {
        LOGGER.info("Constructed Koremods mod instance");

        KoremodsServiceWrapper wrapper = getServiceWrapper();
        if (wrapper != null) {
            Throwable serviceException = wrapper.getServiceException();
            if (serviceException != null) {
                throw new RuntimeException("Koremods loading error", serviceException);
            }
        }
    }

    @Nullable
    private KoremodsServiceWrapper getServiceWrapper() {
        try {
            Field handlerField = Launcher.class.getDeclaredField("transformationServicesHandler");
            handlerField.setAccessible(true);
            Field serviceLookupField = handlerField.getType().getDeclaredField("serviceLookup");
            serviceLookupField.setAccessible(true);
            Field serviceField = TransformationServiceDecorator.class.getDeclaredField("service");
            serviceField.setAccessible(true);

            Object handler = handlerField.get(Launcher.INSTANCE);
            //noinspection unchecked
            Map<String, TransformationServiceDecorator> serviceLookup = (Map<String, TransformationServiceDecorator>) serviceLookupField.get(handler);
            TransformationServiceDecorator decorator = serviceLookup.get(KoremodsServiceWrapper.SERVICE_NAME);
            if (decorator != null) {
                return (KoremodsServiceWrapper) serviceField.get(decorator);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
