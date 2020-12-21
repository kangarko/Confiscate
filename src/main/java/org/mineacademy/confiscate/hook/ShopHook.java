package org.mineacademy.confiscate.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.confiscate.ConfiscateLog;

import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerExchangeShopEvent;

public class ShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopTransaction(PlayerExchangeShopEvent e) {
		final ShopObject shop = e.getShop();
		final ShopType type = e.getType();

		ConfiscateLog.logTranscation("Shop", type, e.getPlayer(), shop.getOwnerName(), shop.getPrice(), shop.getItemStack());
	}
}
