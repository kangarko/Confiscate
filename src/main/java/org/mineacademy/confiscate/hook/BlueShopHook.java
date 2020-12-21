package org.mineacademy.confiscate.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.confiscate.ConfiscateLog;
import org.mineacademy.fo.remain.Remain;

import com.bluesoapturtle.blueshop.events.ShopSignEvent;

public class BlueShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopTransaction(ShopSignEvent e) {
		ConfiscateLog.logTranscationRaw("ChestShop", e.isBuying() ? "bought" : "sold", Remain.getPlayerByUUID(e.getUuid()), "server", e.getPrice(), e.getItem());
	}
}