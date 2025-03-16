package com.khan.imperialaddon;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.khan.imperialaddon.modules.CrystalESP;
import com.khan.imperialaddon.modules.BloodKillEffect;
import com.khan.imperialaddon.modules.ImpGreet;
import com.khan.imperialaddon.modules.ImpY;

public class ImperialAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(ImperialAddon.class);
    public static final Category Main = new Category("ImperialAddon");

    @Override
    public void onInitialize() {
        LOG.info("Initializing ImperialAddon!");
        Modules.get().add(new CrystalESP());
        Modules.get().add(new BloodKillEffect());
        Modules.get().add(new ImpGreet());
        Modules.get().add(new ImpY());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Main);
    }

    @Override
    public String getPackage() {
        return "com.khan.imperialaddon";
    }
}
