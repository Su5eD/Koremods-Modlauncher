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

package wtf.gofancy.koremods.modlauncher.bootstrap;

import com.mojang.datafixers.util.Pair;
import cpw.mods.cl.ProtectionDomainHelper;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DependencyClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final List<SecureJar> jars;

    public DependencyClassLoader(URL[] urls, ClassLoader parent, List<SecureJar> jars) {
        super(urls, parent);

        this.jars = jars;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignored) {

                }
                if (c == null) {
                    c = super.loadClass(name, resolve);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/') + ".class";
        Pair<byte[], SecureJar> pair = this.jars.stream()
            .map(jar -> {
                SecureJar.ModuleDataProvider provider = jar.moduleDataProvider();
                return provider.findFile(path)
                    .map(Path::of)
                    .flatMap(p -> {
                        try {
                            return Optional.of(Files.readAllBytes(p));
                        } catch (IOException e) {
                            return Optional.empty();
                        }
                    })
                    .map(bytes -> Pair.of(bytes, jar))
                    .orElse(null);
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        if (pair != null) {
            try {
                byte[] bytes = pair.getFirst();
                SecureJar jar = pair.getSecond();
                String pname = name.substring(0, name.lastIndexOf('.'));
                if (getDefinedPackage(pname) == null) definePackage(pname, jar.moduleDataProvider().getManifest(), null);

                CodeSigner[] codeSigners = jar.getManifestSigners();
                CodeSource cs = ProtectionDomainHelper.createCodeSource(jar.getPrimaryPath().toUri().toURL(), codeSigners);
                return defineClass(name, bytes, 0, bytes.length, ProtectionDomainHelper.createProtectionDomain(cs, this));
            } catch (Exception ignored) {}
        }

        return super.findClass(name);
    }

    @Override
    public URL findResource(String name) {
        for (SecureJar jar : this.jars) {
            URL url = jar.moduleDataProvider().findFile(name)
                .map(LamdbaExceptionUtils.rethrowFunction(URI::toURL))
                .orElse(null);
            if (url != null) {
                return url;
            }
        }
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> ret = super.findResources(name);
        List<URL> urls = Collections.list(ret);
        for (SecureJar jar : this.jars) {
            jar.moduleDataProvider().findFile(name)
                .map(LamdbaExceptionUtils.rethrowFunction(URI::toURL))
                .ifPresent(urls::add);
        }
        return Collections.enumeration(urls);
    }
}
