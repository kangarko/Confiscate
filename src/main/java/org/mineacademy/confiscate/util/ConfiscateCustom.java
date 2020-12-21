package org.mineacademy.confiscate.util;

import java.util.Arrays;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.Remain;

public class ConfiscateCustom {

	private final String name, lore;
	private final boolean regexName, regexLore;

	public ConfiscateCustom(String name, String lore) {
		regexName = name.startsWith("* ");
		regexLore = lore.startsWith("* ");

		this.name = regexName ? name.substring(2) : name;
		this.lore = regexLore ? lore.substring(2) : lore;
	}

	public final boolean matches(ItemStack item) {
		if (item == null || item.getType().toString().contains("AIR"))
			return false;

		final ItemMeta meta = item.getItemMeta();

		if (meta == null)
			return false;

		final String itemName = Common.stripColors(Common.getOrDefault(meta.getDisplayName(), Remain.getI18NDisplayName(item)));
		final String itemLore = Common.stripColors(Common.join(Common.getOrDefault(meta.getLore(), Arrays.asList("")), " "));

		final boolean nameMatches = regexName ? Common.regExMatch(name, itemName) : name.equalsIgnoreCase(itemName);
		final boolean loreMatches = regexLore ? Common.regExMatch(lore, itemLore) : lore.equalsIgnoreCase(itemLore);

		final boolean m = nameMatches && loreMatches;

		return m;
	}

	@Override
	public String toString() {
		return "custom '" + name + "', lore '" + lore + "'";
	}
}
