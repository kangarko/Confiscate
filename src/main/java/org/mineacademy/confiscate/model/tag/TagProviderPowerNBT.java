package org.mineacademy.confiscate.model.tag;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.TimeUtil;

import me.dpohvar.powernbt.PowerNBT;
import me.dpohvar.powernbt.api.NBTCompound;
import me.dpohvar.powernbt.api.NBTManager;

class TagProviderPowerNBT implements ITagProvider {

	@Override
	public ItemStack addSpecialTag(ItemStack is) {
		final NBTManager manager = PowerNBT.getApi();
		final NBTCompound itemData = parseInternalTag(is);

		if (itemData != null) {
			itemData.put(TagProvider.DATE, TimeUtil.currentTimeSeconds());

			manager.write(is, itemData);
		}

		return is;
	}

	@Override
	public boolean hasSpecialTag(ItemStack is) {
		final NBTCompound com = parseInternalTag(is);

		return com != null && com.containsKey(TagProvider.DATE, Long.class);
	}

	private NBTCompound parseInternalTag(ItemStack is) {
		final NBTManager manager = PowerNBT.getApi();
		NBTCompound tag = null;

		try {
			tag = manager.read(is);

		} catch (final Throwable t) {
			Common.error(t, "PowerNBT could not read item: " + is);
		}

		return tag;
	}
}
