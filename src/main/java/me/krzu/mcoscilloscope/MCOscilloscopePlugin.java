package me.krzu.mcoscilloscope;

import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;

public class MCOscilloscopePlugin extends JavaPlugin {

  private TaskChainFactory taskChainFactory;
  private MCOscilloscope mcOscilloscope;
  private final Logger LOGGER
      = Logger.getLogger(getName());

  @Override
  public void onEnable() {
    taskChainFactory = BukkitTaskChainFactory.create(this);
    mcOscilloscope = new MCOscilloscope(this);
    getCommand("mco").setExecutor(this);

    getConfig().options().copyDefaults(true);
    saveDefaultConfig();
    
    LOGGER.info("Plugin enabled!");
  }
  @Override
  public void onDisable() {
    LOGGER.info("Plugin disabled!");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("Only players can use this command!");
      return true;
    }

    if (cmd.getName().equalsIgnoreCase("mco")) {
      if (args.length == 0) {
        sender.sendMessage("Usage: /mco <start|stop>");
        return true;
      }

      switch (args[0].toLowerCase(Locale.ROOT)) {
        case "start":
          mcOscilloscope.start(player);
          break;
        case "stop":
          mcOscilloscope.stop(player);
          break;
        case "reload":
          mcOscilloscope.stop(player);
          reloadConfig();
          break;
        default:
          sender.sendMessage("Usage: /mco <start|stop>");
          break;
      }
    }
    return true;
  }

  public <T> TaskChain<T> newChain() {
    return taskChainFactory.newChain();
  }

}