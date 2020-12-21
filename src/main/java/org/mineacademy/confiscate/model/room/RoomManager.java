package org.mineacademy.confiscate.model.room;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.confiscate.model.tag.TagProvider;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

public class RoomManager {

	private static final Integer MAX_X_DISTANCE = 120;

	private static ChestFile roomFile = new ChestFile();

	private RoomManager() {
	}

	public static boolean isCornerstoneSet() {
		return roomFile.isCornerstoneSet();
	}

	public static void resetCornerstone(Location loc) {
		roomFile.resetCornerstone(loc);
	}

	public static Location getCornerstone() {
		return roomFile.getCornerstone().clone();
	}

	public static Location getChest(String player) {
		return roomFile.getChest(player);
	}

	public static boolean isLocationProtected(Location loc) {
		Valid.checkNotNull(loc, "Location cannot be null!");

		if (!Settings.Room.ENABLED)
			return false;

		if (!isCornerstoneSet())
			return false;

		Valid.checkNotNull(roomFile.getWorld(), "Cornerstone misconfigured, please generate new!");

		if (!loc.getWorld().getName().equals(roomFile.getWorld().getName()))
			return false;

		// No room yet created
		if (roomFile.getPlayersConfiscated() == 0 && roomFile.getChestRange() == 0)
			return Valid.locationEquals(loc, roomFile.getCornerstone().clone().subtract(0, 1, 0));

		final int minX = (int) roomFile.getCornerstone().getX() - 1;
		final int minY = (int) roomFile.getCornerstone().getY() - 1;
		final int minZ = (int) roomFile.getCornerstone().getZ() - 1;

		final int maxX = roomFile.getPlayersConfiscated() * 3 + minX;
		final int maxY = minY + yLayerNum() * 4 + 3;
		final int maxZ = roomFile.getChestRange() * 2 + minZ;

		final Location minLoc = new Location(roomFile.getWorld(), minX, minY, minZ);
		final Location maxLoc = new Location(roomFile.getWorld(), maxX, maxY, maxZ);

		return loc.toVector().isInAABB(minLoc.toVector(), maxLoc.toVector());
	}

	public static void moveToChest(String player, ItemStack is) {
		if (!Settings.Room.ENABLED)
			return;

		if (!isCornerstoneSet()) {
			Common.logTimed(1, "&cChest room not found, only removing item. Create chest room via '/c room tool' first.");
			return;
		}

		// Add our NBT to item
		is = TagProvider.addSpecialTag(is);

		// Make chest
		final Chest chest = prepareChest(player);
		final Inventory chestInv = prepareInventory(player, chest);

		chestInv.addItem(is);
	}

	private static Chest prepareChest(String player) {
		int x, y, z;

		if (roomFile.playerExists(player)) {
			x = roomFile.loadXPosition(player);
			y = roomFile.loadYPosition(player);
			z = roomFile.loadZPosition(player);

		} else {
			x = (int) roomFile.getCornerstone().getX() + (roomFile.getPlayersConfiscated() - yLayerNum() * (MAX_X_DISTANCE / 3)) * 3;
			y = (int) roomFile.getCornerstone().getY() + yLayerNum() * 4;
			z = (int) roomFile.getCornerstone().getZ();

			roomFile.increaseConfiscated();

			generateChest(x, y, z, true);

			final Location signLoc = new Location(roomFile.getWorld(), x, y + 1, z);

			setBlock(x, y + 1, z, CompMaterial.WALL_SIGN);
			paintSign(signLoc, player);

			roomFile.saveNewChest(player, x, y, z, 1);

			if (roomFile.getChestRange() < 1)
				roomFile.saveChestRange(1);

			roomFile.saveData();

			Debugger.debug("scan", "New chest made [" + z + ", " + y + ", " + z + "] for " + player);
		}

		final Location chestLoc = new Location(roomFile.getWorld(), x, y, z);

		if (!(chestLoc.getBlock().getState() instanceof Chest)) {
			Debugger.debug("scan", "Could not find " + player + "'s chest [" + x + ", " + y + ", " + z + "], regenerating..");

			setChests(x, y, z);
		}

		return (Chest) chestLoc.getBlock().getState();
	}

	private static void paintSign(Location signLocation, String player) {
		final Sign sign = (Sign) signLocation.getBlock().getState();
		sign.setRawData((byte) 3); // Rotate

		for (int i = 0; i < 4; i++)
			sign.setLine(i, Common.colorize(Localization.Room.SIGN[i]).replace("{player}", player));

		sign.update();

		if (MinecraftVersion.atLeast(V.v1_13))
			RoomManager1_13.setSignRotation(signLocation);
	}

	private static Inventory prepareInventory(String playerName, Chest chest) {
		Inventory chestInv = chest.getInventory();

		if (chestInv.firstEmpty() == -1) {
			Debugger.debug("scan", playerName + "'s chest is full, creating new..");

			final int origz = roomFile.loadZPosition(playerName);
			int chestNum = roomFile.loadNum(playerName);

			chestNum++;

			roomFile.saveZ(playerName, origz + 2);
			roomFile.saveNum(playerName, chestNum);

			if (roomFile.getChestRange() < chestNum)
				roomFile.saveChestRange(chestNum);

			final int y = chest.getY();
			final int x = roomFile.loadXPosition(playerName);
			final int z = roomFile.loadZPosition(playerName);

			roomFile.saveData();

			generateChest(x, y, z, false);

			final Location newChestLoc = new Location(roomFile.getWorld(), x, y, z);

			chest = (Chest) newChestLoc.getBlock().getState();
			chestInv = chest.getInventory();
		}

		return chestInv;
	}

	private static void generateChest(int x, int y, int z, boolean starter) {
		if (starter) {
			for (int x1 = x - 1; x1 <= x + 2; x1++)
				for (int y1 = y; y1 <= y + 2; y1++)
					for (int z1 = z; z1 <= z + 1; z1++)
						setAir(x1, y1, z1);

			for (int y1 = y; y1 <= y + 2; y1++)
				for (int x1 = x - 1; x1 <= x + 2; x1++)
					setBlock(x1, y1, z - 1, Settings.Room.MATERIAL);

			setAir(x, y + 1, z);
			setBlock(x + 1, y + 1, z, Material.TORCH, 3);
			setAir(x, y + 2, z);
			setAir(x + 1, y + 2, z);

		} else
			for (int x1 = x - 1; x1 <= x + 2; x1++)
				for (int y1 = y; y1 <= y + 2; y1++)
					for (int z1 = z - 1; z1 <= z + 1; z1++)
						setAir(x1, y1, z1);

		setChests(x, y, z);

		for (int z1 = z - 1; z1 <= z + 1; z1++)
			for (int x1 = x - 1; x1 <= x + 2; x1++)
				setBlock(x1, y - 1, z1, Settings.Room.MATERIAL);

	}

	private static void setChests(int x, int y, int z) {
		if (MinecraftVersion.olderThan(V.v1_13)) {
			setBlock(x, y, z, CompMaterial.CHEST);
			setBlock(x + 1, y, z, CompMaterial.CHEST);

		} else {
			final Block leftBlock = roomFile.getWorld().getBlockAt(x, y, z), rightBlock = roomFile.getWorld().getBlockAt(x + 1, y, z); // input

			RoomManager1_13.setChests(leftBlock, rightBlock);
		}
	}

	private static void setAir(int x, int y, int z) {
		setBlock(x, y, z, CompMaterial.AIR);
	}

	private static void setBlock(int x, int y, int z, CompMaterial mat) {
		roomFile.getWorld().getBlockAt(x, y, z).setType(mat.getMaterial());
	}

	private static void setBlock(int x, int y, int z, Material mat, int data) {
		Remain.setTypeAndData(roomFile.getWorld().getBlockAt(x, y, z), mat, (byte) data, true);
	}

	private static int yLayerNum() {
		return MAX_X_DISTANCE == 0 ? 0 : MAX_X_DISTANCE >= 3 ? roomFile.getPlayersConfiscated() / (MAX_X_DISTANCE / 3) : roomFile.getPlayersConfiscated() - 1;
	}
}
