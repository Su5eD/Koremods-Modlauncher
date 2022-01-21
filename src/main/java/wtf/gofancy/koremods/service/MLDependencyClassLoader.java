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

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import wtf.gofancy.koremods.prelaunch.DependencyClassLoader;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MLDependencyClassLoader extends DependencyClassLoader {

    public MLDependencyClassLoader(URL[] urls, ClassLoader parent, List<String> priorityClasses) {
        super(urls, parent, priorityClasses);
    }

    @Override
    protected Class<?> loadClassFallback(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClassFallback(name, resolve);
        } catch (StringIndexOutOfBoundsException ignored) {
            // https://github.com/McModLauncher/securejarhandler/issues/21
            throw new ClassNotFoundException();
        }
    }

    @Override
    public URL[] getURLs() {
        String legacyClassPath = System.getProperty("legacyClassPath", "");
        String[] parts = legacyClassPath.split(File.pathSeparator);
        
        return Stream.concat(
                Arrays.stream(super.getURLs()),
                Arrays.stream(parts)
                        .map(LamdbaExceptionUtils.rethrowFunction(path -> new File(path).toURI().toURL()))
        )
                .distinct()
                .toArray(URL[]::new);
    }
}
