package dev.su5ed.koremods.modlaunch;

import dev.su5ed.koremods.api.SplashScreen;
import dev.su5ed.koremods.prelaunch.KoremodsPrelaunch;
import dev.su5ed.koremods.prelaunch.SplashScreenFactory;
import dev.su5ed.koremods.splash.KoremodsSplashScreen;

@SuppressWarnings("unused")
public class SplashScreenFactoryImpl implements SplashScreenFactory {
    
    @Override
    public SplashScreen createSplashScreen(KoremodsPrelaunch prelaunch) {
        SplashScreen splash = new KoremodsSplashScreen();
        
        splash.startThread();
        splash.awaitInit();
        
        return splash;
    }
}
