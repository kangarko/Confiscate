package org.mineacademy.confiscate.model.tag;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.confiscate.hook.PowerNBTHook;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

public class TagProvider {

	static final String DATE = "confiscate_date";

	private final static ITagProvider tagProvider;

	static {
		tagProvider = PowerNBTHook.isHooked() ? new TagProviderPowerNBT() : new TagProviderReflection();
	}

	public static boolean isCompatible() {
		return tagProvider != null;
	}

	public static ItemStack addSpecialTag(ItemStack is) {
		try {
			return getTagProvider().addSpecialTag(is);

		} catch (final ReflectiveOperationException e) {
			throw new FoException(e);
		}
	}

	public static boolean hasSpecialTag(ItemStack is) {
		try {
			return getTagProvider().hasSpecialTag(is);

		} catch (final ReflectiveOperationException e) {
			throw new FoException(e);
		}
	}

	private static ITagProvider getTagProvider() {
		Valid.checkBoolean(isCompatible() && tagProvider != null, "Could not inject NMS, your server version is incompatible! (please also report this error)");

		return tagProvider;
	}
}
