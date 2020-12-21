package org.mineacademy.confiscate.model.tag;

import java.lang.reflect.Method;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TimeUtil;

class TagProviderReflection implements ITagProvider {

	@Override
	public ItemStack addSpecialTag(ItemStack is) throws ReflectiveOperationException {
		if (is == null)
			return null;

		final Object nms = asNMSCopy(is);
		final Object tag = loadTag(is);

		tag.getClass().getMethod("setLong", String.class, long.class).invoke(tag, TagProvider.DATE, TimeUtil.currentTimeSeconds());
		nms.getClass().getMethod("setTag", tag.getClass()).invoke(nms, tag);

		return asBukkitCopy(nms);
	}

	private final ItemStack asBukkitCopy(Object nms) throws ReflectiveOperationException {
		return (ItemStack) ReflectionUtil.getOBCClass("inventory.CraftItemStack").getMethod("asBukkitCopy", nms.getClass()).invoke(null, nms);
	}

	private final Object asNMSCopy(ItemStack is) throws ReflectiveOperationException {
		final Class<?> craftItemStack = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
		final Method m = craftItemStack.getMethod("asNMSCopy", ItemStack.class);

		return m.invoke(null, is);
	}

	@Override
	public boolean hasSpecialTag(ItemStack is) throws ReflectiveOperationException {
		if (is == null)
			return false;

		final Object compound = loadTag(is);

		return (boolean) compound.getClass().getMethod("hasKey", String.class).invoke(compound, TagProvider.DATE);
	}

	private Object loadTag(ItemStack is) throws ReflectiveOperationException {
		final Object nms = asNMSCopy(is);
		final Class<?> cl = nms.getClass();

		return (boolean) cl.getMethod("hasTag").invoke(nms) ? cl.getMethod("getTag").invoke(nms) : ReflectionUtil.instantiate(ReflectionUtil.getNMSClass("NBTTagCompound"));
	}
}