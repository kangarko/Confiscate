package org.mineacademy.confiscate.hook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.confiscate.Confiscate;
import org.mineacademy.confiscate.ConfiscateLog;
import org.mineacademy.confiscate.model.Scan;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.eventbus.EventHandler.Priority;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class WorldEditHook {

	// Block ID, Amount already placed
	private static int totalBlocksPlaced = 0;
	private static Map<CompMaterial, Integer> limitedBlocksPlaced = new HashMap<>();

	private static HashSet<String> playersBlocked = new HashSet<>();

	private static int LIMIT_SECONDS;

	private WorldEditHook() {
	}

	public static void hook() {
		LIMIT_SECONDS = (int) (Settings.WorldEdit.THRESHOLD / 20);

		Debugger.debug("worldedit", "Hooking WorldEdit...");

		// This is the whole operation start.
		final Object early = new Object() {

			@Subscribe(priority = Priority.VERY_EARLY)
			public void filterBlocks(EditSessionEvent e) {
				final Player player = lookupValidPlayer(e);
				if (player == null)
					return;

				final long lastTime = Confiscate.getDataFor(player).lastWorldEditViolation;

				if (lastTime != 0) {
					final long lastSpent = TimeUtil.currentTimeSeconds() - lastTime;
					final String name = e.getActor().getName();

					if (lastSpent + 1 > LIMIT_SECONDS && playersBlocked.contains(name)) {
						playersBlocked.remove(name);

						Debugger.debug("worldedit", "Allowing player " + name + " to edit");
					}
				}
			}
		};

		final Object normal = new Object() {

			@Subscribe(priority = Priority.NORMAL)
			public void filterBlocks(EditSessionEvent event) {
				final Player player = lookupValidPlayer(event);
				if (player == null)
					return;

				// Reset data.
				totalBlocksPlaced = 0;
				limitedBlocksPlaced.clear();

				// Check every changed block within this operation.
				event.setExtent(new AbstractDelegateExtent(event.getExtent()) {

					@Override
					public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {

						String name;

						if (MinecraftVersion.atLeast(V.v1_13))
							name = block.getBlockType().getId().split("\\:")[1];
						else
							name = block.getBlockType().getName();

						// TODO This is broken on MC 1.13+ --> how to get bukkit Material from WorldEdit?
						final CompMaterial id = Common.getOrDefault(CompMaterial.fromString(name), CompMaterial.AIR);

						Debugger.debug("worldedit", "Evaluating if ID " + id + " should be set");

						if (playersBlocked.contains(name)) {
							Debugger.debug("worldedit", "NO: Player already blocked");

							return false;
						}

						if (Settings.Confiscate.CRASHABLE_ITEMS && Scan.isNonWorldEdit(id)) {
							Debugger.debug("worldedit", "NO: Is crashable");

							return false;
						}

						// Return false if the item is limited
						if (Settings.WorldEdit.LIMITS.contains(id)) {
							final int placedAmount = limitedBlocksPlaced.getOrDefault(id, 0) + 1;

							limitedBlocksPlaced.put(id, placedAmount);

							if (placedAmount > Settings.WorldEdit.LIMITS.getLimit(id)) {
								Debugger.debug("worldedit", "NO: Over limit");

								return false;
							}
						}

						if (++totalBlocksPlaced > getBlockLimitFor(player)) {
							Debugger.debug("worldedit", "NO: Over limit (2)");

							return false;
						}

						Debugger.debug("worldedit", "YES: Passed.");
						return super.setBlock(location, block);
					}
				});
			}
		};

		final Object late = new Object() {

			@Subscribe(priority = Priority.VERY_LATE)
			public void filterBlocks(EditSessionEvent e) {
				final Player player = lookupValidPlayer(e);
				if (player == null)
					return;

				Debugger.debug("worldedit", "Filtering blocks (very late) ... ");

				final String name = e.getActor().getName();

				new BukkitRunnable() {

					@Override
					public void run() {
						if (player.isValid() && player.isOnline()) {
							final long lastTime = Confiscate.getDataFor(player).lastWorldEditViolation;
							final long ubehlo = TimeUtil.currentTimeSeconds() - lastTime;

							if (playersBlocked.contains(name) || !limitedBlocksPlaced.isEmpty())
								if (lastTime != 0 && ubehlo < LIMIT_SECONDS) {
									Debugger.debug("worldedit", "Blocking WE for " + name + " due to a recent violation.");

									Common.tellTimed(1, player, Localization.WorldEdit.BLOCK.replace("{time}", Localization.Cases.SECOND.formatWithCount(LIMIT_SECONDS - ubehlo)));
									return;
								}

							boolean blocked = false;

							for (final Map.Entry<CompMaterial, Integer> entry : limitedBlocksPlaced.entrySet()) {
								final CompMaterial id = entry.getKey();
								final int amount = entry.getValue();

								final int lim = Settings.WorldEdit.LIMITS.getLimit(id);
								final int cut = MathUtil.range(amount - lim, 0, Integer.MAX_VALUE);

								if (cut > 0) {
									final String material = ItemUtil.bountifyCapitalized(id).toLowerCase();

									Common.tellReplaced(player, Localization.WorldEdit.CUT, "amount", cut, "material", material);
									ConfiscateLog.saveLog(player, null, ConfiscateLog.LogMessage.WORLDEDIT.formatAndlog(name, cut, material, lim, Common.shortLocation(player.getLocation())));

									blocked = true;
								}
							}

							final int limit = getBlockLimitFor(player);
							if (totalBlocksPlaced > limit) {
								Debugger.debug("worldedit", "Showing " + name + " the limit message.");

								Common.tellReplaced(player, Localization.WorldEdit.LIMIT, "limit", limit, "cut", totalBlocksPlaced - limit);
								blocked = true;
							}

							if (blocked) {
								playersBlocked.add(name);

								Confiscate.getDataFor(player).lastWorldEditViolation = TimeUtil.currentTimeSeconds();
							}
						}
					}
				}.runTaskLaterAsynchronously(SimplePlugin.getInstance(), 2);
			}
		};

		register(early, normal, late);
		Debugger.debug("worldedit", "WorldEdit integration hooked.");
	}

	private static void register(Object... objects) {
		final EventBus eventBus = WorldEdit.getInstance().getEventBus();

		for (final Object object : objects)
			eventBus.register(object);
	}

	private static Player lookupValidPlayer(EditSessionEvent e) {
		if (e.getActor() == null || !e.getActor().isPlayer() || e.getStage() != Stage.BEFORE_HISTORY)
			return null;

		final Player player = Bukkit.getPlayer(e.getActor().getName());

		if (player == null || !player.isOnline())
			return null;

		if (PlayerUtil.hasPerm(player, Permissions.Scan.Bypasses.LIMIT_WORLDEDIT))
			return null;

		if (Settings.Ignore.WORLDS.contains(player.getWorld().getName()))
			return null;

		return player;
	}

	private static int getBlockLimitFor(Player pl) {
		for (final Entry<String, Integer> e : Settings.WorldEdit.TOTAL_GROUP_LIMITS.entrySet())
			if (PlayerUtil.hasPerm(pl, "confiscate.group." + e.getKey()))
				return e.getValue();

		return Integer.MAX_VALUE;
	}
}
