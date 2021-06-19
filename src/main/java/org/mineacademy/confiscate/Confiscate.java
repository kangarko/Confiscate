package org.mineacademy.confiscate;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mineacademy.confiscate.hook.BlueShopHook;
import org.mineacademy.confiscate.hook.ChestShopHook;
import org.mineacademy.confiscate.hook.PowerNBTHook;
import org.mineacademy.confiscate.hook.ShopGUIHook;
import org.mineacademy.confiscate.hook.ShopHook;
import org.mineacademy.confiscate.hook.SignShopHook;
import org.mineacademy.confiscate.hook.WorldEditHook;
import org.mineacademy.confiscate.hook.mcMMOHook;
import org.mineacademy.confiscate.listener.CommandSpyListener;
import org.mineacademy.confiscate.listener.RoomListener;
import org.mineacademy.confiscate.listener.ScanListener;
import org.mineacademy.confiscate.model.room.RoomManager;
import org.mineacademy.confiscate.model.tag.TagProvider;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlStaticConfig;

import lombok.NonNull;

/**
 * The main class of Confiscate.
 *
 * @author kangarko
 */
public class Confiscate extends SimplePlugin {

	// Player Name, Player Cache
	private static final StrictMap<UUID, PlayerCache> playerCache = new StrictMap<>();

	@Override
	protected final String[] getStartupLogo() {
		return new String[] {
				"&f____ ____ _  _ ____ _ ____ ____ ____ ___ ____ ",
				"&f|    |  | |\\ | |___ | [__  |    |__|  |  |___ ",
				"&7|___ |__| | \\| |    | ___] |___ |  |  |  |___ ",
				" "
		};
	}

	@Override
	public V getMinimumVersion() {
		return V.v1_7;
	}

	@Override
	public void onPluginStart() {
		final long now = System.currentTimeMillis();

		// Hook
		if (Common.doesPluginExist("PowerNBT"))
			try {
				Class.forName("me.dpohvar.powernbt.utils.EntityUtils");
				PowerNBTHook.hook();

			} catch (final Throwable t) {
				Common.log("&cCould not hook into PowerNBT! That plugin appears broken or incompatible on your server version. (This is not an issue with " + getName() + "!)");
			}

		TagProvider.isCompatible();

		if (MinecraftVersion.olderThan(V.v1_11) && getServer().getPluginManager().getPlugin("PowerNBT") == null) {
			Common.logFramed(true,
					"Native NMS version is incompatible!",
					"For Minecraft older than 1.11, please",
					"install PowerNBT to enable this plugin.");
			return;
		}

		ConfiscateLog.init();

		// Hook events
		registerEvents(new ScanListener());
		registerEvents(new RoomListener());
		registerEvents(new CommandSpyListener());

		if (Common.doesPluginExist("ChestShop"))
			registerEvents(new ChestShopHook());

		if (Common.doesPluginExist("SignShop"))
			registerEvents(new SignShopHook());

		if (Common.doesPluginExist("ShopGUIPlus"))
			registerEvents(new ShopGUIHook());

		final Plugin shop = getServer().getPluginManager().getPlugin("Shop");

		if (shop != null && shop.getDescription().getMain().startsWith("com.snowgears"))
			if (Common.doesPluginExist("Shop")) // just for the message to show up
				registerEvents(new ShopHook());

		if (Common.doesPluginExist("BlueShop"))
			registerEvents(new BlueShopHook());

		if (Common.doesPluginExist("mcMMO"))
			mcMMOHook.hook();

		if (Settings.WorldEdit.ENABLED && HookManager.isWorldEditLoaded())
			WorldEditHook.hook();

		startReloadable();

		// Only messages below
		if (Settings.Confiscate.ItemsNewcomers.ENABLED && Remain.isStatSavingDisabled()) {
			Common.logFramed(false,
					"&cWarning! In order to scan inventories of",
					"&cnewcoming players, you need to enable",
					"&cstatistics in spigot.yml (stat.disable-saving)");

			Settings.Confiscate.ItemsNewcomers.ENABLED = false;
		}

		if (Settings.Room.WAIT_WITH_LOADING)
			Common.runLaterAsync(0, this::loadRoom);
		else
			loadRoom();

		Common.log(
				"&7__________________________________________________________",
				"&7",
				"&7Confiscate " + getVersion() + " premium",
				"&7",
				"&7For documentation and reporting bugs, please visit",
				"&7https://github.com/kangarko/Confiscate");

		Common.log(" ",
				"&fInitialization done. Took " + (System.currentTimeMillis() - now) + " ms.",
				"&7__________________________________________________________",
				" ");
	}

	private void loadRoom() {
		if (Settings.Room.ENABLED)
			if (!RoomManager.isCornerstoneSet())
				Common.logFramed(false,
						"&eYou have not yet configured your chest room!",
						"&fPlease run &7/c room tool &fand select where",
						"&fthe cornerstone of the room shall be!");
			else
				Common.log("Chest room loaded successfully.");
	}

	@Override
	protected void onPluginReload() {
		startReloadable();
	}

	private void startReloadable() {
		new ConfiscateCommand().register();
	}

	@Override
	public final List<Class<? extends YamlStaticConfig>> getSettings() {
		return Arrays.asList(Settings.class, Localization.class);
	}

	/*@Override
	public final SpigotUpdater getUpdateCheck() {
		return new SpigotUpdater(37893);
	}*/

	@Override
	public final int getFoundedYear() {
		return 2017; // Around February-March
	}

	public static PlayerCache getDataFor(@NonNull final Player player) {
		PlayerCache cache = playerCache.get(player.getUniqueId());

		if (cache == null) {
			cache = new PlayerCache();

			playerCache.put(player.getUniqueId(), cache);
		}

		return cache;
	}
}
