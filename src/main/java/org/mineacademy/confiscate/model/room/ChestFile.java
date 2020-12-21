package org.mineacademy.confiscate.model.room;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;

class ChestFile {

	/**
	 * The chest data file.
	 */
	private File file = null;

	/**
	 * The chest data configuration.
	 */
	private FileConfiguration cfg = null;

	/**
	 * The foundation location;
	 */
	private Location cornerstone;

	ChestFile() {
		loadCfg();

		if (this.isCornerstoneSet())
			loadData();
	}

	private void loadCfg() {
		file = FileUtil.extract("room.db");
		cfg = FileUtil.loadConfigurationStrict(file);

	}

	void saveData() {
		cfg.options().header(
				"\n" +
						"This file stores information related to chest room.\n" +
						"\n" +
						"THIS IS MANAGED BY PLUGIN. PLEASE DO NOT EDIT!\n" +
						"\n");
		cfg.options().copyHeader(true);

		try {
			cfg.save(file);

		} catch (final IOException e) {
			Common.error(e, "Failed to save chest room file");
		}
	}

	boolean isCornerstoneSet() {
		try {
			return cfg.get("cornerstone") instanceof org.bukkit.Location || cfg.isSet("cornerstone");
		} catch (final Throwable ex) { // world got changed
			cfg.set("cornerstone", null);
			saveData();

			return false;
		}
	}

	Location getCornerstone() {
		return cornerstone;
	}

	private void loadData() {
		Valid.checkBoolean(isCornerstoneSet(), "Cornerstone must be set first");

		if (cfg.get("cornerstone") instanceof Location) {
			cornerstone = (Location) cfg.get("cornerstone");
			return;
		}

		final String world = cfg.getString("cornerstone.world");
		Valid.checkNotNull(world, "Internal malfunction: World name is null (?)");

		final double x = cfg.getDouble("cornerstone.x");
		final double y = cfg.getDouble("cornerstone.y");
		final double z = cfg.getDouble("cornerstone.z");

		cornerstone = new Location(Bukkit.getWorld(world), x, y, z); //(Location) config.get("cornerstone");
	}

	void resetCornerstone(Location loc) {
		Valid.checkBoolean(file.delete(), "Could not delete old chest file");
		loadCfg();

		loc = loc.add(0, 1, 0);

		if (MinecraftVersion.newerThan(V.v1_7))
			cfg.set("cornerstone", loc);
		else {
			cfg.set("cornerstone.world", loc.getWorld().getName());
			cfg.set("cornerstone.x", loc.getX());
			cfg.set("cornerstone.y", loc.getY());
			cfg.set("cornerstone.z", loc.getZ());
		}

		saveData();
		loadData();
	}

	World getWorld() {
		Valid.checkNotNull(cornerstone, "Internal malfunction: Cornerstone is null!");

		return cornerstone.getWorld();
	}

	void increaseConfiscated() {
		cfg.set("confiscated", getPlayersConfiscated() + 1);
	}

	void saveNewChest(String player, int x, int y, int z, int num) {
		cfg.set(player + ".chest.x", x);
		cfg.set(player + ".chest.y", y);
		cfg.set(player + ".chest.z", z);
		cfg.set(player + ".chest.num", num);
	}

	void saveZ(String player, int z) {
		cfg.set(player + ".chest.z", z);
	}

	void saveNum(String player, int num) {
		cfg.set(player + ".chest.num", num);
	}

	void saveChestRange(int range) {
		cfg.set("range", range);
	}

	Location getChest(String player) {
		if (!playerExists(player))
			return null;

		return new Location(getWorld(), loadXPosition(player), loadYPosition(player), loadZPosition(player));
	}

	//

	boolean playerExists(String player) {
		return cfg.isSet(player + ".chest.x");
	}

	int loadXPosition(String player) {
		return cfg.getInt(player + ".chest.x");
	}

	int loadYPosition(String player) {
		return cfg.getInt(player + ".chest.y");
	}

	int loadZPosition(String player) {
		return cfg.getInt(player + ".chest.z");
	}

	int loadNum(String player) {
		return cfg.getInt(player + ".chest.num");
	}

	int getPlayersConfiscated() {
		return cfg.getInt("confiscated", 0);
	}

	int getChestRange() {
		return cfg.getInt("range", 0);
	}
}