package org.mineacademy.confiscate.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.confiscate.ConfiscateLog;

import net.brcdev.shopgui.event.ShopPostTransactionEvent;
import net.brcdev.shopgui.shop.ShopTransactionResult;

public class ShopGUIHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopTransaction(ShopPostTransactionEvent e) {
		final ShopTransactionResult r = e.getResult();
		final ItemStack item = new ItemStack(r.getShopItem().getItem().getType(), r.getAmount());

		ConfiscateLog.logTranscation("ShopGUI", r.getShopAction(), r.getPlayer(), "server", r.getPrice(), item);
	}
}
