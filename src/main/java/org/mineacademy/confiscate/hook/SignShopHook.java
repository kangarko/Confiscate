package org.mineacademy.confiscate.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.confiscate.ConfiscateLog;
import org.wargamer2010.signshop.events.SSPostTransactionEvent;

public class SignShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopTransaction(SSPostTransactionEvent e) {
		ConfiscateLog.logTranscationRaw("SignShop", e.getOperation(), e.getPlayer().getPlayer(), e.getOwner().getName(), e.getPrice(), e.getItems());
	}
}
