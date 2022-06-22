package me.jacob.explosionnerf;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ExplosionNerf extends JavaPlugin {

	private static ExplosionNerf instance;

	private String notConsoleMessage;

	public void onEnable() {
		instance = this;

		this.saveDefaultConfig();
		notConsoleMessage = getConfig().getString("not-console-message");

		if(notConsoleMessage == null) {
			notConsoleMessage = "&cYou must be console to run this command!";
		}

		notConsoleMessage = ChatColor.translateAlternateColorCodes('&', notConsoleMessage);

		PluginCommand cmd = getCommand("reloadexplosionnerf");
		cmd.setTabCompleter((sender, command, label, args) -> Collections.emptyList());
		cmd.setExecutor((sender, command, label, args) -> {
			if(!(sender instanceof ConsoleCommandSender)) {
				sender.sendMessage(notConsoleMessage);
				return true;
			}

			reloadConfig();

			PluginManager pm = getServer().getPluginManager();
			pm.disablePlugin(this);
			pm.enablePlugin(this);

			sender.sendMessage("Reloaded!");
			return true;
		});

		// Events
		FileConfiguration config = getConfig();

		if(config.getBoolean("end-crystal-nerf"))
			registerEvent(new EntityNerf(Material.END_CRYSTAL, EntityType.ENDER_CRYSTAL));
		if(config.getBoolean("bed-nerf"))
			registerEvent(new BlockNerf(
					Stream.of(Material.values())
					.filter(m -> m.name().contains("BED") && !m.isLegacy())
					.collect(Collectors.toList()),
					(m) -> m.name().equals("BED")));
		if(config.getBoolean("anchor-nerf"))
			registerEvent(new BlockNerf(
					Collections.singletonList(Material.GLOWSTONE),
					(m) -> m == Material.RESPAWN_ANCHOR));
		if(config.getBoolean("tnt-minecart-nerf"))
			registerEvent(new EntityNerf(Material.BOW, EntityType.MINECART_TNT));
	}

	public void cancelIfItemNotFound(Player player, Cancellable event,
	                                 List<Material> noCancelMaterials) {
		PlayerInventory inv = player.getInventory();
		List<ItemStack> items = new ArrayList<>();

		for(ItemStack item : inv.getContents()) {
			if(item != null)
				items.add(item);
		}
		items.add(inv.getItemInOffHand());
		items.add(player.getItemOnCursor());

		if(items.stream()
				.map(ItemStack::getType)
				.noneMatch(noCancelMaterials::contains))
			event.setCancelled(true);
	}

	private void registerEvent(Listener listener) {
		getServer().getPluginManager().registerEvents(listener, this);
	}

	public static ExplosionNerf getInstance() {
		return instance;
	}
}
