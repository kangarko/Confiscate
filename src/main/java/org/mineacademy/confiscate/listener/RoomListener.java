package org.mineacademy.confiscate.listener;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.mineacademy.confiscate.ConfiscateLog;
import org.mineacademy.confiscate.model.room.RoomManager;
import org.mineacademy.confiscate.model.room.RoomTool;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Localization.Room;
import org.mineacademy.confiscate.settings.Localization.Room.NoPermission;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;

public class RoomListener implements Listener {

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerClick(PlayerInteractEvent e) {
		if (!Remain.isInteractEventPrimaryHand(e))
			return;

		if (e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		final Player pl = e.getPlayer();
		final Block block = e.getClickedBlock();

		// In the chest room
		if (RoomManager.isLocationProtected(block.getLocation())) {
			final BlockState state = block.getState();

			// Open inventory
			if (state instanceof InventoryHolder) {
				final Inventory inv = ((InventoryHolder) block.getState()).getInventory();
				e.setCancelled(true);

				if (!PlayerUtil.hasPerm(pl, Permissions.Room.OPEN)) {
					CompSound.FIZZ.play(pl, 1, 1);

					Common.tellTimed(1, pl, NoPermission.CHEST);
				} else {
					final Inventory clone = Bukkit.createInventory(null, inv.getSize(), Common.colorize("&0Confiscated Chest View"));
					clone.setContents(inv.getContents());

					pl.openInventory(clone);
					CompSound.BLOCK_DISPENSER_LAUNCH.play(pl, 1, 1);

					Common.tellTimed(8, pl, Room.BROWSING);
				}

				// Click sign
			} else if (state instanceof Sign) {
				final Sign sign = (Sign) state;
				final String[] lines = Common.revertColorizing(sign.getLines());

				if (lines[0].equals(Localization.Room.SIGN[0]) && lines[1].equals(Localization.Room.SIGN[1]) && lines[3].equals(Localization.Room.SIGN[3]))
					if (!PlayerUtil.hasPerm(pl, Permissions.Room.SIGNS)) {
						CompSound.FIZZ.play(pl, 1, 1);

						Common.tellTimed(1, pl, NoPermission.LOGS);

					} else {
						final String signPlayer = lines[2];

						ConfiscateLog.printLogsFor(pl, signPlayer, null);
					}

			}

			// Outside the room
		} else if (pl.getItemInHand().equals(RoomTool.getTool())) {
			if (!PlayerUtil.hasPerm(pl, Permissions.Room.TOOL)) {
				CompSound.FIZZ.play(pl, 1, 1);

				Common.tellTimed(1, pl, NoPermission.CORNERSTONE);
				return;
			}

			e.setCancelled(true);
			block.setType(Material.EMERALD_BLOCK);
			CompSound.LEVEL_UP.play(pl, 1, 1);

			RoomManager.resetCornerstone(block.getLocation());

			Common.tell(pl, Room.CORNERSTONE_SET);
		}
	}

	// ------------------------- Prevent destroying chest room -------------------------

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent e) {
		preventChestRoomModification(e, e.getPlayer(), e.getBlock().getLocation());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent e) {
		preventChestRoomModification(e, e.getPlayer(), e.getBlock().getLocation());
	}

	// --------------------- Prevent clicking in offline inventories --------------------

	/*@EventHandler(ignoreCancelled=false, priority=EventPriority.MONITOR)
	public final void onInventoryClose(InventoryCloseEvent e) {
		final String t = e.getView().getTitle();

		if (t.startsWith(Common.colorize("&0Viewing ")) && t.endsWith("'s armor")) {
			final String playersName = t.replace(Common.colorize("&0Viewing "), "").replace("'s armor", "");
			final Inventory inv = e.getInventory();

			final Player online = Bukkit.getPlayer(playersName);
			final List<ItemStack> items = new ArrayList<>();

			for (final ArmorSlot as : ArmorSlot.values()) {
				final ItemStack it = inv.getItem(as.getInvSlot());

				if (as == ArmorSlot.OFF_HAND) {
					if (online != null && online.isOnline())
						try {
							online.getEquipment().setItemInOffHand(it);
						} catch (final Throwable ttt) {
						}

					continue;
				}

				items.add(it);
			}

			final ItemStack[] armor = items.toArray( new ItemStack[ items.size() ] );

			if (online != null && online.isOnline())
				online.getEquipment().setArmorContents(armor);
		}
	}*/

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onInventoryInteract(InventoryDragEvent e) {
		preventOfflineInventoriesClicks(e.getWhoClicked(), e, e.getInventory(), e.getView());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onInventoryInteract(InventoryMoveItemEvent e) {
		final List<HumanEntity> viewers = e.getSource().getViewers();

		if (!viewers.isEmpty())
			preventOfflineInventoriesClicks(viewers.get(0), e, e.getDestination(), null);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onInventoryInteract(InventoryClickEvent e) {
		preventOfflineInventoriesClicks(e.getWhoClicked(), e, e.getView().getTopInventory(), e.getView());
	}

	private void preventChestRoomModification(Cancellable e, CommandSender pl, Location loc) {
		if (loc != null && RoomManager.isLocationProtected(loc)) {
			e.setCancelled(true);

			Common.tellTimed(1, pl, NoPermission.DESTROY);
		}
	}

	private boolean preventOfflineInventoriesClicks(HumanEntity viewer, Cancellable e, Inventory inventory, InventoryView view) {
		final String title = view != null ? view.getTitle() : viewer.getOpenInventory().getTitle();

		if (title.endsWith("armor")) {

			e.setCancelled(true);
			return false;
		}

		if (Remain.getLocation(inventory) == null)
			if (title.startsWith(Common.colorize("&0Reading ")) || title.startsWith(Common.colorize("&0Viewing "))) {
				if (PlayerUtil.hasPerm(viewer, Permissions.Commands.INV_WRITE) && !title.endsWith("armor"))
					return false;

				e.setCancelled(true);
				return true;
			}

		return false;
	}
}
