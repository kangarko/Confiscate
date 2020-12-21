package org.mineacademy.confiscate.model.tag;

import org.bukkit.inventory.ItemStack;

interface ITagProvider {

	/**
	 * Marks the item as handled by this plugin.
	 */
	ItemStack addSpecialTag(ItemStack is) throws ReflectiveOperationException;

	/**
	 * Has the item been handled by this plugin?
	 */
	boolean hasSpecialTag(ItemStack is) throws ReflectiveOperationException;
}