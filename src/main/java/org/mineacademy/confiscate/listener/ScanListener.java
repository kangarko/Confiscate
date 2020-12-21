package org.mineacademy.confiscate.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.confiscate.model.Scan;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.debug.LagCatcher;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

public class ScanListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChestOpen(InventoryOpenEvent event) {
		final Inventory inventory = event.getInventory();

		if (event.getView() == null || inventory == null) {
			Debugger.debug("scan", "Null inventory, returning");

			return;
		}

		final Player player = (Player) event.getPlayer();

		if (Remain.getLocation(inventory) == null) {
			Debugger.debug("scan", "Virtual " + inventory.getType() + " inventory for " + player.getName() + ", ignoring");

			return;
		}

		if (!Settings.Scan.SCAN_CONTAINERS)
			return;

		final InventoryType inventoryType = event.getView().getType();

		if (Settings.Ignore.CREATIVE && player.getGameMode() == GameMode.CREATIVE) {
			Debugger.debug("scan", "[Configured] Ignoring creative mode for " + player.getName());

			return;
		}

		if (Settings.Ignore.WORLDS.contains(player.getWorld().getName())) {
			Debugger.debug("scan", "[Configured] Ignoring world " + player.getWorld().getName());

			return;
		}

		if (Settings.Scan.CONTAINERS_EXCEPTIONS.contains(inventoryType)) {
			Debugger.debug("scan", "[Configured] Ignoring " + player.getName() + "'s container " + inventoryType);

			return;
		}

		if (PlayerUtil.hasPerm(player, Permissions.Scan.Bypasses.CONTAINER.replace("{inventoryType}", inventoryType.toString()).toLowerCase()))
			return;

		LagCatcher.start("Inventory Open Scan");
		Scan.scanContainer(player, inventory);
		LagCatcher.end("Inventory Open Scan");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		scanInventory(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		scanInventory(event.getEntity());
	}

	private void scanInventory(Player player) {
		if (!scanningAndWorldEnabled(player))
			return;

		if (Settings.Ignore.CREATIVE && player.getGameMode() == GameMode.CREATIVE) {
			Debugger.debug("scan", "[Configured] Ignoring creative mode for " + player.getName());
			return;
		}

		if (PlayerUtil.hasPerm(player, Permissions.Scan.Bypasses.INVENTORY))
			return;

		Scan.scanInventory(player);
	}

	private boolean scanningAndWorldEnabled(Player player) {
		if (!Settings.Scan.SCAN_INVENTORIES)
			return false;

		return !org.mineacademy.confiscate.settings.Settings.Ignore.WORLDS.contains(player.getWorld().getName());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onWorldSwitch(PlayerChangedWorldEvent event) {
		if (!Settings.Scan.SCAN_INVENTORIES)
			return;

		final Player player = event.getPlayer();

		if (Settings.Ignore.WORLDS.contains(player.getWorld().getName()))
			return;

		if (PlayerUtil.hasPerm(player, Permissions.Scan.Bypasses.INVENTORY))
			return;

		new BukkitRunnable() {

			@Override
			public void run() {
				if (Settings.Ignore.CREATIVE && player.getGameMode() == GameMode.CREATIVE)
					return;

				Scan.scanInventory(player);
			}
		}.runTaskLater(SimplePlugin.getInstance(), 6);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onItemSpawn(ItemSpawnEvent event) {
		if (Settings.Confiscate.CRASHABLE_ITEMS) {
			final Material material = event.getEntity().getItemStack().getType();

			for (final CompMaterial technical : Scan.getTechnicalItems())
				if (material == technical.getMaterial()) {
					event.setCancelled(true);

					Common.log("&cRemoving crashable item '" + technical + "' at " + Common.shortLocation(event.getLocation()));
					break;
				}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (!scanningAndWorldEnabled(event.getPlayer()))
			return;

		if (event.getAction() == null || event.getItem() == null)
			return;

		if (event.getAction().toString().contains("RIGHT_CLICK") && event.getItem().getType().toString().contains("POTION")) {
			final boolean tookPotions = Scan.scanPotions(event.getPlayer());

			if (tookPotions) {
				event.getPlayer().getInventory().remove(event.getItem());
				event.setCancelled(true);
			}

			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		if (!Valid.isInListStartsWith(event.getMessage(), Settings.Scan.SCAN_ON_COMMAND_LIST))
			return;

		Common.runLater(4, () -> scanInventory(event.getPlayer()));
	}
}