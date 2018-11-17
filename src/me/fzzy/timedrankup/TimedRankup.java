package me.fzzy.timedrankup;

import me.fzzy.timedrankup.util.Yaml;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.security.auth.login.Configuration;
import java.io.File;
import java.util.*;

public class TimedRankup extends JavaPlugin implements Listener {

    public static Yaml playersYaml;
    public static Yaml configYaml;

    public static TimedRankup instance;

    public HashMap<UUID, Long> times = new HashMap<>();
    public HashMap<Integer, List<String>> timedCommands = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        instance.getDataFolder().mkdirs();
        playersYaml = new Yaml(instance.getDataFolder().getAbsolutePath() + File.separator + "players.yml");
        configYaml = new Yaml(instance.getDataFolder().getAbsolutePath() + File.separator + "config.yml");

        Bukkit.getPluginManager().registerEvents(this, this);

        addConfigDefaults(configYaml);
        loadConfig(configYaml);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    process(player);
                    playersYaml.save();
                }
            }
        }.runTaskTimer(this, 20L, 20L * 60L);
    }

    public void process(Player player) {
        long timePassed = System.currentTimeMillis() - times.get(player.getUniqueId());

        long total = 0;
        try {
            total = (long) playersYaml.get(player.getUniqueId().toString());
        } catch(Exception ignored) {
        }
        playersYaml.set(player.getUniqueId().toString(), total + timePassed);

        Bukkit.broadcastMessage((total / 1000) + " total");
        Bukkit.broadcastMessage((timePassed / 1000) + " timepassed");

        for (Map.Entry<Integer, List<String>> e : timedCommands.entrySet()) {
            if (total < e.getKey() * 60 * 1000 && total + timePassed >= e.getKey() * 60 * 1000) {
                for (String cmd : e.getValue()) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }
        }

        times.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        times.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        times.remove(event.getPlayer().getUniqueId());
    }

    public void addConfigDefaults(Yaml config) {
        List<String> time = new ArrayList<>();
        time.add("heal %player%");
        config.add("times.10.commands", time);
        config.save();
    }

    public void loadConfig(Yaml config) {
        ConfigurationSection section = config.getConfigurationSection("times");
        Map<String, Object> times = section.getValues(false);
        timedCommands.clear();
        for (Map.Entry<String, Object> t : times.entrySet()) {
            try {
                int minutes = Integer.parseInt(t.getKey());
                List<String> list = (List<String>) config.getList("times." + t.getKey() + ".commands");
                timedCommands.put(minutes, list);
            } catch(NumberFormatException e) {
                System.out.println("Value in config is not a number!");
            }
        }
    }
}