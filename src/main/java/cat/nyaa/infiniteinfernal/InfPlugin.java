package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.bossbar.BossbarManager;
import cat.nyaa.infiniteinfernal.configs.MessageConfig;
import cat.nyaa.infiniteinfernal.controler.ISpawnControler;
import cat.nyaa.infiniteinfernal.controler.InfSpawnControler;
import cat.nyaa.infiniteinfernal.group.GroupCommands;
import cat.nyaa.infiniteinfernal.loot.IMessager;
import cat.nyaa.infiniteinfernal.loot.InfMessager;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.support.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class InfPlugin extends JavaPlugin {
    public static InfPlugin plugin;
    public static boolean wgEnabled = false;

    Events events;
    Config config;
    MessageConfig messageConfig;
    I18n i18n;

    ImiCommands imiCommand;
    DebugCommands debugCommands;
    AdminCommands commands;
    GroupCommands groupCommands;

    LootManager lootManager;
    MobManager mobManager;
    BroadcastManager broadcastManager;
    BossbarManager bossbarManager;

    InfMessager infMessager;
    ISpawnControler spawnControler;



    @Override
    public void onEnable() {
        super.onEnable();
        plugin = this;
        config = new Config(this);
        config.load();
        events = new Events(this);
        i18n = new I18n(this, config.language);
        i18n.load();

        groupCommands = new GroupCommands(this, i18n);
        commands = new AdminCommands(this, i18n, groupCommands);
        imiCommand = new ImiCommands(this, i18n);
        debugCommands = new DebugCommands(this, i18n);


        lootManager = LootManager.instance();
        mobManager = MobManager.instance();
        broadcastManager = new BroadcastManager();
        broadcastManager.load();
        messageConfig = new MessageConfig();
        messageConfig.load();
        infMessager = new InfMessager(messageConfig);
        spawnControler = new InfSpawnControler(this);

        Bukkit.getPluginManager().registerEvents(events, this);
        Bukkit.getPluginCommand("infiniteinfernal").setExecutor(commands);
        Bukkit.getPluginCommand("ig").setExecutor(groupCommands);
        Bukkit.getPluginCommand("imi").setExecutor(imiCommand);
        Bukkit.getPluginCommand("infdebug").setExecutor(debugCommands);

        try {
            WorldGuardUtils.init();
            wgEnabled = true;
        } catch (NoClassDefFoundError e) {
            Bukkit.getLogger().log(Level.WARNING, "WorldGuard didn't detected, support will be disabled");
        }
        bossbarManager = new BossbarManager();
        if (config.bossbar.enabled) {
            bossbarManager.start(10);
        }
        MainLoopTask.start();
    }

    public void onReload() {
        config.load();
        i18n = new I18n(this, config.language);
        i18n.load();
        LootManager.instance().load();
        MainLoopTask.start();

        mobManager.load();
        broadcastManager.load();
//        messageConfig = new MessageConfig();
        messageConfig.load();
        infMessager = new InfMessager(messageConfig);
        if (config.bossbar.enabled) {
            bossbarManager.start(10);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        config.save();
        LootManager.disable();
        MobManager.disable();
    }

    public Config config() {
        return config;
    }

    public ISpawnControler getSpawnControler() {
        return spawnControler;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public BroadcastManager getBroadcastManager() {
        return broadcastManager;
    }

    public IMessager getMessager() {
        return infMessager;
    }
}
