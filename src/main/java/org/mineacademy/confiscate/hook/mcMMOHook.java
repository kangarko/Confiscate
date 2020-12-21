package org.mineacademy.confiscate.hook;

import org.bukkit.entity.Player;

import com.gmail.nossr50.api.AbilityAPI;

public class mcMMOHook {

	private static McMMOHook0 hook;

	public static final void hook() {
		hook = new McMMOHook0();
	}

	public static boolean hasAbility(Player player) {
		return hook != null && hook.hasAbility(player);
	}
}

class McMMOHook0 {

	final boolean hasAbility(Player player) {
		return AbilityAPI.isAnyAbilityEnabled(player);
	}
}