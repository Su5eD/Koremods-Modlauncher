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
