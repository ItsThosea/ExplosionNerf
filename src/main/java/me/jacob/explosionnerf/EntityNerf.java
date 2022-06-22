package me.jacob.explosionnerf;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityNerf implements Listener {

	private static final ExplosionNerf plugin = ExplosionNerf.getInstance();
	private static final FileConfiguration config = plugin.getConfig();

	private final List<Material> materials;
	private final EntityType type;

	private final Map<UUID, Integer> forceDamage = new IdentityHashMap<>();

	public EntityNerf(Material material, EntityType type) {
		this.materials = Collections.singletonList(material);
		this.type = type;
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();

		if(event.getEntity() instanceof Player && damager.getType() == type) {
			Player player = (Player) event.getEntity();

			if(damager instanceof EnderCrystal) {
				EnderCrystal crystal = (EnderCrystal) damager;

				if(crystal.isShowingBottom() && !config.getBoolean("natural-end-crystal-nerf"))
					return;
			}

			Integer invincibleEntityId = forceDamage.get(player.getUniqueId());

			if(invincibleEntityId != null && invincibleEntityId == damager.getEntityId()) {
				forceDamage.remove(player.getUniqueId());
				return;
			}

			plugin.cancelIfItemNotFound(player, event, materials);
		}
	}

	@EventHandler
	public void onDamage1(EntityDamageByEntityEvent event) {
		if(!(event.getEntityType() == type))
			return;

		Entity entity = event.getEntity();

		if(event.getDamager() instanceof Player) {
			putAndScheduleEntity((Player) event.getDamager(), entity.getEntityId());
		} else if(event.getDamager() instanceof Projectile) {
			Projectile projectile = (Projectile) event.getDamager();

			if(projectile.getShooter() instanceof Player) {
				putAndScheduleEntity(
						(Player) projectile.getShooter(),
						event.getEntity().getEntityId()
				);
			}
		}
	}

	private void putAndScheduleEntity(Player p, int entityId) {
		UUID playerId = p.getUniqueId();
		forceDamage.put(playerId, entityId);

		new BukkitRunnable() {
			public void run() {
				forceDamage.remove(playerId);
			}
		}.runTaskLater(plugin, 2);
	}
}
