package ac.boar.plugin;

import ac.boar.anticheat.Boar;
import org.bukkit.plugin.java.JavaPlugin;

public final class BoarPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        Boar.getInstance().init();
    }

    @Override
    public void onDisable() {
        Boar.getInstance().terminate();
    }
}
