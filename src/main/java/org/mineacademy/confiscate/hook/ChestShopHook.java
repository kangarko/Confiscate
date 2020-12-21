package org.mineacademy.confiscate.hook;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.mineacademy.confiscate.ConfiscateLog;

import com.Acrobot.ChestShop.Events.TransactionEvent;

public class ChestShopHook implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopTransaction(TransactionEvent e) {
		ConfiscateLog.logTranscation("ChestShop", e.getTransactionType(), e.getClient(), e.getOwner() != null ? e.getOwner().getName() : "adminshop", e.getPrice(), e.getStock());
	}
}