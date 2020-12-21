package org.mineacademy.confiscate.model.room;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.remain.CompProperty;

public class RoomTool {

	private static final ItemStack roomWand;

	static {
		roomWand = prepareItem(Material.DIAMOND_AXE);
	}

	private static ItemStack prepareItem(Material mat) {
		final ItemStack is = new ItemStack(mat);
		final ItemMeta m = is.getItemMeta();

		m.setDisplayName(Common.colorize("&8> &6&lRoom Wand &8<"));
		m.setLore(Common.colorize(Arrays.asList(
				"&r",
				"&6Left click &7to set up",
				"&7the cornerstone for",
				"&7the chest room.")));

		m.addEnchant(Enchantment.DURABILITY, 1, true);
		CompProperty.UNBREAKABLE.apply(m, true);

		try {
			m.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
		} catch (final Throwable t) {
		} // Older MC

		is.setItemMeta(m);

		return is;
	}

	public static ItemStack getTool() {
		Valid.checkNotNull(roomWand);

		return roomWand;
	}
}
