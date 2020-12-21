package org.mineacademy.confiscate.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.remain.CompMaterial;

public class PermissionCache {

	/**
	 * For maximum performance, we store the permission names for different materials here for each checked section
	 */
	private static volatile StrictMap<String, StrictMap<String, String>> cachedPermissionsMap = new StrictMap<>();

	public static boolean hasPermission(Player player, String permissionRaw, Material material) {
		return hasPermission(player, permissionRaw, "material", material.toString());
	}

	public static boolean hasPermission(Player player, String permissionRaw, CompMaterial material) {
		return hasPermission(player, permissionRaw, "material", material.toString());
	}

	public static boolean hasPermission(Player player, String permissionRaw, PotionEffect potionEffect) {
		return hasPermission(player, permissionRaw, "potion_type", potionEffect.getType().getName());
	}

	private static boolean hasPermission(Player player, String permissionRaw, String variableToReplace, String replacement) {
		final StrictMap<String, String> permissions = cachedPermissionsMap.getOrPut(permissionRaw, new StrictMap<>());
		String permissionWeak = permissions.get(replacement);

		if (permissionWeak == null) {
			permissionWeak = permissionRaw.replace("{" + variableToReplace + "}", replacement).toLowerCase();

			permissions.put(replacement, permissionWeak);
		}

		return player.hasPermission(permissionWeak);
	}
}
