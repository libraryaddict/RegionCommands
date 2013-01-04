package me.libraryaddict.RegionCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Main extends JavaPlugin implements Listener {
  WorldGuardPlugin wg;
  Map<String, List<String>> enteredRegions = new HashMap<String, List<String>>();
  Map<String, Map<String, List<String>>> commandsToRunForEnter = new HashMap<String, Map<String, List<String>>>();
  Map<String, Map<String, List<String>>> commandsToRunForExit = new HashMap<String, Map<String, List<String>>>();

  public void onEnable() {
    saveDefaultConfig();
    wg = getWorldGuard();
    String[] list = { "Enter", "Exit" };
    for (String e : list)
      if (getConfig().getConfigurationSection(e) != null)
        for (String worldName : getConfig().getConfigurationSection(e).getKeys(
            false)) {
          Map<String, List<String>> worldCommands = new HashMap<String, List<String>>();
          for (String regionName : getConfig().getConfigurationSection(
              e + "." + worldName).getKeys(false)) {
            List<String> commands = new ArrayList<String>();
            for (String command : getConfig().getStringList(
                e + "." + worldName + "." + regionName))
              commands.add(command);
            worldCommands.put(regionName, commands);
          }
          if (e.equals("Enter"))
            commandsToRunForEnter.put(worldName, worldCommands);
          else
            commandsToRunForExit.put(worldName, worldCommands);
        }
    Bukkit.getPluginManager().registerEvents(this, this);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMove(PlayerMoveEvent event) {
    if (event.isCancelled())
      return;
    Player player = event.getPlayer();
    if (!enteredRegions.containsKey(player.getName()))
      enteredRegions.put(player.getName(), new ArrayList<String>());
    List<String> currentRegion = enteredRegions.get(player.getName());
    Iterator<ProtectedRegion> iter = wg.getRegionManager(player.getWorld())
        .getApplicableRegions(player.getLocation()).iterator();
    List<String> regions = new ArrayList<String>();
    while (iter.hasNext()) {
      String name = iter.next().getId();
      regions.add(name);
    }
    enteredRegions.put(player.getName(), regions);
    for (String regionName : regions) {
      if (currentRegion.contains(regionName))
        continue;
      if (!player.hasPermission("RegionCommands.Use." + regionName))
        continue;
      // He is in the region and hasn't been here
      if (commandsToRunForEnter.containsKey(player.getWorld().getName())
          && commandsToRunForEnter.get(player.getWorld().getName())
              .containsKey(regionName))
        for (String command : commandsToRunForEnter.get(
            player.getWorld().getName()).get(regionName)) {
          if (command.startsWith("/")) {
            boolean op = player.isOp();
            if (!op)
              player.setOp(true);
            Bukkit.dispatchCommand(player,
                command.substring(1).replaceAll("%p", player.getName()));
            if (!op)
              player.setOp(false);
          } else
            player.chat(command);
        }
    }
    for (String regionName : currentRegion) {
      if (regions.contains(regionName))
        continue;
      if (!player.hasPermission("RegionCommands.Use." + regionName))
        continue;
      // He is in the region and hasn't been here
      if (commandsToRunForExit.containsKey(player.getWorld().getName())
          && commandsToRunForExit.get(player.getWorld().getName()).containsKey(
              regionName))
        for (String command : commandsToRunForExit.get(
            player.getWorld().getName()).get(regionName)) {
          if (command.startsWith("/")) {
            boolean op = player.isOp();
            if (!op)
              player.setOp(true);
            Bukkit.dispatchCommand(player,
                command.substring(1).replaceAll("%p", player.getName()));
            if (!op)
              player.setOp(false);
          } else
            player.chat(command);
        }
    }
  }

  private WorldGuardPlugin getWorldGuard() {
    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

    // WorldGuard may not be loaded
    if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
      return null; // Maybe you want throw an exception instead
    }
    return (WorldGuardPlugin) plugin;
  }

  void saveCommands() {
    for (String worldName : commandsToRunForEnter.keySet()) {
      for (String regionName : commandsToRunForEnter.get(worldName).keySet()) {
        getConfig().set("Enter." + worldName + "." + regionName,
            commandsToRunForEnter.get(worldName).get(regionName));
      }
    }
    for (String worldName : commandsToRunForExit.keySet()) {
      for (String regionName : commandsToRunForExit.get(worldName).keySet()) {
        getConfig().set("Exit." + worldName + "." + regionName,
            commandsToRunForExit.get(worldName).get(regionName));
      }
    }
    saveConfig();
  }

  public boolean onCommand(CommandSender sender, Command cmd,
      String commandLabel, String[] args) {
    if (sender.getName().equalsIgnoreCase("console")) {
      sender
          .sendMessage(ChatColor.BLUE
              + "Due to the nature of the command you are running, You must be a player in the world you wish to edit regions for commands");
      sender
          .sendMessage(ChatColor.BLUE
              + "I will allow you to 'try' to use this. But its gonna error! And it will NOT work!");
    }
    if (cmd.getName().equalsIgnoreCase("regioncommand")) {
      if (!sender.hasPermission("RegionCommands.Commands")) {
        sender.sendMessage(ChatColor.RED
            + "You do not have permission to use this command");
        return true;
      }
      Player p = Bukkit.getPlayerExact(sender.getName());
      if (args.length > 1) {
        String regionName = null;
        for (String region : wg.getRegionManager(p.getWorld()).getRegions()
            .keySet())
          if (region.equalsIgnoreCase(args[0]))
            regionName = region;
        if (regionName == null) {
          sender.sendMessage(ChatColor.BLUE + "Can not find region: "
              + ChatColor.GOLD + args[0]);
          return true;
        }
        List<String> commands = null;
        if (args.length > 2) {
          if (args[2].equalsIgnoreCase("enter")
              && commandsToRunForEnter.get(p.getWorld().getName()) != null) {
            commands = commandsToRunForEnter.get(p.getWorld().getName()).get(
                regionName);
          } else if (args[2].equalsIgnoreCase("exit")
              && commandsToRunForExit.get(p.getWorld().getName()) != null) {
            commands = commandsToRunForExit.get(p.getWorld().getName()).get(
                regionName);
          }
        }
        if (commands == null)
          commands = new ArrayList<String>();
        String command = null;
        if (args.length > 3)
          command = StringUtils.join(args, " ").substring(
              args[0].length() + args[1].length() + args[2].length() + 3);
        if (args[1].equalsIgnoreCase("remove")) {
          if (args.length <= 3)
            sender.sendMessage(ChatColor.BLUE + "Not enough parameters");
          else {
            if (commands.contains(command)) {
              commands.remove(command);
              sender.sendMessage(ChatColor.BLUE + "Removed command: "
                  + ChatColor.GOLD + command + ChatColor.BLUE + " from region "
                  + regionName + " " + ChatColor.YELLOW + args[1].toLowerCase());
              if (args[2].equalsIgnoreCase("enter"))
                commandsToRunForEnter.get(p.getWorld().getName()).put(
                    regionName, commands);
              else if (args[2].equalsIgnoreCase("exit"))
                commandsToRunForExit.get(p.getWorld().getName()).put(
                    regionName, commands);
              saveCommands();
            } else
              sender.sendMessage(ChatColor.BLUE + "Can't find command: "
                  + ChatColor.GOLD + command + ChatColor.BLUE + " in region "
                  + regionName + " " + ChatColor.YELLOW + args[1].toLowerCase());
            return true;
          }
        } else if (args[1].equalsIgnoreCase("add")) {
          if (args.length <= 3)
            sender.sendMessage(ChatColor.BLUE + "Not enough parameters");
          else {
            commands.add(command);
            if (args[2].equalsIgnoreCase("enter")) {
              if (!commandsToRunForEnter.containsKey(p.getWorld().getName()))
                commandsToRunForEnter.put(p.getWorld().getName(),
                    new HashMap<String, List<String>>());
            } else if (!commandsToRunForExit
                .containsKey(p.getWorld().getName()))
              commandsToRunForExit.put(p.getWorld().getName(),
                  new HashMap<String, List<String>>());
            sender.sendMessage(ChatColor.BLUE + "Added command: "
                + ChatColor.GOLD + command + ChatColor.BLUE + " to region "
                + ChatColor.GOLD + regionName + " " + ChatColor.YELLOW + args[1].toLowerCase());
            if (args[2].equalsIgnoreCase("enter"))
              commandsToRunForEnter.get(p.getWorld().getName()).put(regionName,
                  commands);
            else if (args[2].equalsIgnoreCase("exit"))
              commandsToRunForExit.get(p.getWorld().getName()).put(regionName,
                  commands);
            saveCommands();
            return true;
          }
        } else if (args[1].equalsIgnoreCase("purge")
            || args[1].equalsIgnoreCase("clear")) {
          if (args.length < 3) {
            sender.sendMessage(ChatColor.BLUE
                + "/regioncommand <region> (purge/clear) " + ChatColor.RED
                + "<enter/exit>");
            return true;
          }
          sender.sendMessage(ChatColor.BLUE + "" + commands.size()
              + " commands were removed from region " + ChatColor.GOLD
              + regionName + " " + ChatColor.YELLOW + args[1].toLowerCase());
          commands.clear();
          if (args[2].equalsIgnoreCase("enter"))
            commandsToRunForEnter.get(p.getWorld().getName()).put(regionName,
                commands);
          else if (args[2].equalsIgnoreCase("exit"))
            commandsToRunForExit.get(p.getWorld().getName()).put(regionName,
                commands);
          saveCommands();
          return true;
        } else if (args[1].equalsIgnoreCase("list")) {
          if (args.length < 3) {
            sender.sendMessage(ChatColor.BLUE + "/regioncommand <region> list "
                + ChatColor.RED + "<enter/exit>");
            return true;
          }
          sender.sendMessage(ChatColor.BLUE + "Listing commands for region "
              + ChatColor.GOLD + regionName + " : " + args[2].toLowerCase());
          for (String string : commands) {
            sender.sendMessage(ChatColor.BLUE + "Region: " + ChatColor.GOLD
                + regionName + " " + ChatColor.YELLOW + args[1].toLowerCase() + ChatColor.BLUE
                + ", Command: " + ChatColor.GOLD + string);

          }
          if (commands.size() == 0)
            sender.sendMessage(ChatColor.BLUE + "No commands found in region "
                + ChatColor.GOLD + regionName + " " + ChatColor.YELLOW + args[1].toLowerCase());
          return true;
        }
      }
      sender
          .sendMessage(ChatColor.BLUE
              + "/regioncommand <region> <remove/add/list/(purge/clear)> <enter/exit> <command>");
    }
    return true;
  }
}
