package me.jacob.explosionnerf;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Bed.Part;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class BlockNerf implements Listener {

	private static final ExplosionNerf plugin = ExplosionNerf.getInstance();

	private final List<Material> materials;
	private final Predicate<Material> blockChecker;

	private final Map<UUID, Location> forceDamage = new HashMap<>();

	public BlockNerf(List<Material> materials, Predicate<Material> blockChecker) {
		this.materials = materials;
		this.blockChecker = blockChecker;
	}

	@EventHandler
	public void onDamage(EntityDamageByBlockEvent event) {
		if(!(event.getEntity() instanceof Player))
			return;
		if(event.getCause() != DamageCause.BLOCK_EXPLOSION)
			return;

		Player p = (Player) event.getEntity();
		Block block = event.getDamager();

		if(block == null)
			return;

		Location loc = forceDamage.get(p.getUniqueId());
		if(loc != null && loc.equals(block.getLocation())) {
			forceDamage.remove(p.getUniqueId());
			return;
		}

		Material blockType = block.getType();

		if(blockChecker.test(blockType))
			plugin.cancelIfItemNotFound(p, event, materials);
	}

	@EventHandler
	public void onInteractOnBlock(PlayerInteractEvent event) {
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		Block clickedBlock = event.getClickedBlock();
		if(clickedBlock == null)
			return;
		BlockData blockData = clickedBlock.getBlockData();

		if(blockData instanceof RespawnAnchor) {
			RespawnAnchor anchor = (RespawnAnchor) blockData;

			if(clickedBlock.getWorld().getEnvironment() == Environment.NETHER)
				return;
			if(anchor.getCharges() == 0)
				return;

			putAndScheduleBlock(event.getPlayer(), clickedBlock.getLocation());
		} else if(blockData instanceof Bed) {
			Bed bedData = (Bed) blockData;

			Location blockLocation = clickedBlock.getLocation();
			if(bedData.getPart() == Part.HEAD) {
				BlockFace face = bedData.getFacing();

				blockLocation.add(reverseInt(face.getModX()), 0, reverseInt(face.getModZ()));
			}

			putAndScheduleBlock(event.getPlayer(), blockLocation);
		}
	}

	private int reverseInt(int integer) {
		return -integer;
	}

	private void putAndScheduleBlock(Player p, Location loc) {
		UUID id = p.getUniqueId();
		forceDamage.put(id, loc);

		new BukkitRunnable() {
			public void run() {
				forceDamage.remove(id);
			}
		}.runTaskLater(plugin, 2);
	}

}
