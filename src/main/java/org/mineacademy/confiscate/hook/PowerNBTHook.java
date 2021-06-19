package org.mineacademy.confiscate.hook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.confiscate.util.ArmorSlot;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;

import me.dpohvar.powernbt.PowerNBT;
import me.dpohvar.powernbt.api.NBTCompound;
import me.dpohvar.powernbt.api.NBTList;

public class PowerNBTHook {

	private static PowerNBTHook0 hook;

	public static final void hook() {
		hook = new PowerNBTHook0();
	}

	public static final boolean isHooked() {
		return hook != null;
	}

	public static final Inventory loadInventory(OfflinePlayer player, boolean ender) {
		return isHooked() ? hook.loadInventory(player, ender) : null;
	}

	public static final Inventory loadArmor(OfflinePlayer player) {
		return isHooked() ? hook.loadArmor(player) : null;
	}
}

class PowerNBTHook0 {

	final Inventory loadInventory(OfflinePlayer player, boolean ender) {
		Valid.checkNotNull(player);

		final String name = ender ? "EnderItems" : "Inventory";
		final String type = ender ? "enderchest" : "inventory";

		final NBTList list = PowerNBT.getApi().readOfflinePlayer(player).getList(name);
		final Inventory inventory = Bukkit.createInventory(null, 36, Common.colorize("&0Reading " + player.getName() + "'s " + type));

		for (int inc = 0; inc < list.size(); inc++) {
			final NBTCompound nbt = (NBTCompound) list.get(inc);
			final int slot = nbt.getByte("Slot");

			if (slot >= 0 && slot < 100) {
				ItemStack item = null;
				try {
					item = getItemFromNBT(nbt);
					Valid.checkNotNull(item, "Null item at slot!");

					inventory.setItem(slot, item);
				} catch (final Throwable t) {
					Common.throwError(t,
							"Failed to load " + player.getName() + "'s " + type + "!",
							"Problematic item tag: " + nbt,
							"Icon (may be not loaded): " + item,
							"Slot: " + slot,
							"%error");
				}
			}
		}

		return inventory;
	}

	final Inventory loadArmor(OfflinePlayer player) {
		Valid.checkNotNull(player);

		final NBTCompound compound = PowerNBT.getApi().readOfflinePlayer(player);
		final NBTList list = compound.getList("Inventory");
		final Inventory inventory = Bukkit.createInventory(null, 9, Common.colorize("&0Reading " + player.getName() + "'s armor"));

		for (int inc = 0; inc < list.size(); inc++) {
			final NBTCompound nbt = (NBTCompound) list.get(inc);
			final ArmorSlot parsed = ArmorSlot.fromNmsSlot(nbt.getByte("Slot"));

			if (parsed != null)
				inventory.setItem(parsed.getInvSlot(), getItemFromNBT(nbt));
		}

		return inventory;
	}

	private final ItemStack getItemFromNBT(NBTCompound nbt) {
		try {
			final Class<?> cis = ReflectionUtil.getOBCClass("inventory.CraftItemStack");
			final Class<?> nmsItem = ReflectionUtil.getNMSClass("Item", "net.minecraft.world.item.Item");

			final Method asItemStack = cis.getDeclaredMethod("asNewCraftStack", nmsItem, Integer.TYPE);
			final Object nmsItemObj = nmsItem.getDeclaredMethod(getItemMethodName(), String.class).invoke(null, nbt.getString("id"));

			final ItemStack item = (ItemStack) asItemStack.invoke(null, nmsItemObj,
					nbt.getInt("Count"));

			item.setDurability(nbt.getShort("Damage"));

			if (nbt.containsKey("tag")) {
				final NBTCompound comp = nbt.getCompound("tag");
				if (comp.containsKey("ench"))
					for (final Object o : comp.getList("ench").toArray())
						if (o instanceof NBTCompound) {
							final NBTCompound c = (NBTCompound) o;
							item.addUnsafeEnchantment((Enchantment) Enchantment.class.getMethod("getById", int.class).invoke(null, c.getShort("id")), c.getShort("lvl"));
						}

				if (comp.containsKey("display")) {
					final ItemMeta m = item.getItemMeta();
					final NBTCompound disp = comp.getCompound("display");

					if (disp.containsKey("Lore")) {
						final NBTList l = disp.getList("Lore");
						final List<String> lore = new ArrayList<>();

						for (int i = 0; i < l.size(); i++)
							lore.add((String) l.get(i));

						m.setLore(lore);
					}

					if (disp.containsKey("Name"))
						m.setDisplayName(disp.getString("Name"));

					item.setItemMeta(m);
				}
			}
			return item;

		} catch (final Exception e) {
			Common.error(e, "Failed to read item from NBT", "Tag: " + nbt, "%error");
		}

		return new ItemStack(Material.AIR);
	}

	private final String getItemMethodName() {
		switch (MinecraftVersion.getCurrent()) {
			case v1_12:
			case v1_11:
				return "b";
			case v1_10:
			case v1_9:
			case v1_8:
				return "d";

			default:
				throw new FoException("Viewing offline inventories only works on 1.12-1.8");
		}
	}
}