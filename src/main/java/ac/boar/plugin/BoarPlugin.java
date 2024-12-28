package ac.boar.plugin;

import ac.boar.anticheat.Boar;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class BoarPlugin extends JavaPlugin {
    public static BoarPlugin INSTANCE;
    public static Logger LOGGER;

    @Override
    public void onEnable() {
        INSTANCE = this;
        LOGGER = getLogger();
        Boar.getInstance().init();
    }

    @Override
    public void onDisable() {
        Boar.getInstance().terminate();
    }
}
