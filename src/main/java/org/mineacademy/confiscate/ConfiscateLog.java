package org.mineacademy.confiscate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.confiscate.ConfiscateLog.LogMessage;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.confiscate.util.ConfiscateCustom;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.model.SimpleComponent;
import org.mineacademy.fo.plugin.SimplePlugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class ConfiscateLog {

	private static final StrictMap<String, PlayerLogFile> logCache = new StrictMap<>();

	public static void init() {
	}

	public static void saveLog(Player pl, ItemStack is, LogMessage message) {
		Valid.checkNotNull(pl);
		Valid.checkNotNull(message);

		if (!canLog(pl, message))
			return;

		Common.runAsync(() -> {
			synchronized (logCache) {
				final PlayerLogFile file = getConfig(pl.getName());

				file.getConfig().set(message + "." + System.currentTimeMillis() + ".time", TimeUtil.getFormattedDate());
				file.getConfig().set(message + "." + System.currentTimeMillis() + ".item", is);

				if (message == LogMessage.TRANSACTION)
					file.getConfig().set(message + "." + System.currentTimeMillis() + ".location", Common.shortLocation(pl.getLocation()));

				file.getConfig().set(message + "." + System.currentTimeMillis() + ".log", message.lastFileLoggedMessage);

				file.save();
			}
		});
	}

	public static final void logTranscation(String plugin, Enum<?> transaction, Player buyer, String shopHolder, Object price, ItemStack... items) {
		logTranscationRaw(plugin, parseTransactionType(transaction), buyer, shopHolder, price, items);
	}

	public static final void logTranscationRaw(String plugin, String transaction, Player buyer, String shopHolder, Object price, ItemStack... itemstacks) {
		if (!canLog(buyer, LogMessage.TRANSACTION))
			return;

		String items = "";

		if (itemstacks.length == 1)
			items = parseItem(itemstacks[0]);
		else
			for (final ItemStack is : itemstacks)
				items += "{" + parseItem(is) + "},";

		final LogMessage message = LogMessage.TRANSACTION.formatAndlog(plugin, transaction, buyer != null ? buyer.getName() : Localization.Parts.UNKNOWN, shopHolder, price instanceof Double ? MathUtil.formatThreeDigits((double) price) : price, items);
		ConfiscateLog.saveLog(buyer, null, message);
	}

	private static boolean canLog(Player player, LogMessage message) {
		if (!Settings.Log.ENABLED)
			return false;

		if (player != null)
			if (PlayerUtil.hasPerm(player, Permissions.Exempt.LOG.replace("{log_type}", message.toString().toLowerCase())))
				return false;

		return !org.mineacademy.fo.Valid.isInListStartsWith(message.toString(), org.mineacademy.confiscate.settings.Settings.Log.EXCEPTIONS);
	}

	public static List<String> getLogs(@Nullable LogMessage type) {
		final List<String> logs = new ArrayList<>();

		final File logDir = new File(SimplePlugin.getInstance().getDataFolder(), "logs");
		final File[] files = logDir.listFiles(file -> {
			if (!file.getName().endsWith(".yml"))
				return false;

			if (type == null) // All files allowed
				return true;

			return FileUtil.loadConfigurationStrict(file).isSet(type.toString());
		});

		if (files != null)
			for (final File f : files)
				logs.add(" &7- &f" + f.getName().replace(".yml", ""));

		return logs;
	}

	public static void printLogsFor(CommandSender whosAsking, String player, @Nullable LogMessage typeToLookFor) {
		final List<SimpleComponent> logs = new ArrayList<>();

		if (new File(SimplePlugin.getInstance().getDataFolder(), "logs/" + player + ".yml").exists()) {
			final PlayerLogFile file = getConfig(player);

			// Fill logs
			for (final Map.Entry<LogMessage, StrictList<LoggedMessage>> entry : file.getLog().entrySet()) {
				final LogMessage logType = entry.getKey();

				if (typeToLookFor != null) {
					if (logType != typeToLookFor)
						continue;
				} else
					logs.add(SimpleComponent.of("&f&l> &6" + logType.getLogType()));

				for (final LoggedMessage log : entry.getValue()) {
					final String logMessage = log.getMessage();

					if (logMessage != null && !logMessage.isEmpty()) {
						final SimpleComponent line = SimpleComponent.of("  &8- ").append("&7" + logMessage).onHover(Localization.Parts.CONFISCATED + TimeUtil.getFormattedDateShort(Long.parseLong(log.getTime())));

						logs.add(line);
					}
				}

				logs.add(SimpleComponent.of(" "));
			}

		}

		// Send to whomever is asking for
		{
			if (logs.isEmpty())
				Common.tellNoPrefix(whosAsking, "&cPlayer '" + player + "' has no stored logs.");
			else {
				Common.tellNoPrefix(whosAsking,

						"&7" + Common.chatLine(),
						"&fConfiscate Exempt View for " + player,
						"&7" + Common.chatLine());

				for (final SimpleComponent log : logs)
					log.send(whosAsking);

				Common.tellNoPrefix(whosAsking, "&7Log finished. Run &6/c room tp " + player + " &7to review their items.");
			}
		}
	}

	private static PlayerLogFile getConfig(String player) {
		if (!logCache.contains(player))
			logCache.put(player, new PlayerLogFile(player));

		return logCache.get(player);
	}

	private static String parseItem(ItemStack item) {
		return item.getAmount() + " " + WordUtils.capitalize(item.getType().toString().toLowerCase().replace("_", " "));
	}

	private static String parseTransactionType(Enum<?> e) {
		final String raw = e.toString().toLowerCase().replace("_", " ");

		return raw.replace("buy", Localization.Verbs.BUY).replace("sell", Localization.Verbs.SELL).replace("gamble", Localization.Verbs.GAMBLE).replace("barter", Localization.Verbs.BARTER);
	}

	@RequiredArgsConstructor
	public enum LogMessage {
		IGNORE_IN_ROOM("Ignore_In_Room"),
		IGNORE_METADATA("Ignore_Metadata"),

		CRASHABLE("Crashable"),

		ILLEGAL("Illegal"),
		STACK_ILLEGAL("Stack_Illegal"),

		HACKED_POTION("Hacked_Potion"),
		INFINITE_POTION("Infinite_Potion"),

		ENCHANT_UNNATURAL("Enchant_Unnatural"),
		ENCHANT_TOO_HIGH("Enchant_Too_High"),

		WORLDEDIT("Worldedit"),
		ABOVE_LIMIT("Above_Limit"),

		TRANSACTION("Transaction");

		private final String key;

		@Override
		public String toString() {
			return key;
		}

		private String lastFileLoggedMessage;

		public LogMessage formatAndlog(Object... variables) {
			notify(variables);

			return this;
		}

		public String getLogType() {
			return Localization.Log.getMessage(key + ".Type");
		}

		private void notify(Object... variables) {
			if (!canLog(null, this))
				return;

			final String message = replaceVariables(Localization.Log.getMessage(key + ".Console_Msg"), variables);

			lastFileLoggedMessage = replaceVariables(Localization.Log.getMessage(key + ".Log_Msg"), variables);

			Common.broadcastWithPerm(Permissions.Notify.CONFISCATE, message, true);
		}

		private String replaceVariables(String message, Object... variables) {
			for (int i = 0; i < variables.length; i++) {
				Object var = variables[i];

				if (var != null)
					if (var instanceof Material)
						var = ItemUtil.bountifyCapitalized((Material) var);

					else if (var instanceof ItemStack)
						var = ItemUtil.bountifyCapitalized(((ItemStack) var).getType());

					else if (var instanceof Player)
						var = ((Player) var).getName();

					else if (var instanceof Enum)
						var = ItemUtil.bountifyCapitalized((Enum<?>) var);

					else if (var instanceof ConfiscateCustom)
						var = var.toString();

				message = message.replace("{" + i + "}", var != null ? var.toString() : "");
			}

			return message;
		}
	}
}

class PlayerLogFile {

	private final File file;
	private final FileConfiguration cfg;

	PlayerLogFile(String player) {
		file = FileUtil.getOrMakeFile("logs/" + player + ".yml");
		cfg = FileUtil.loadConfigurationStrict(file);
	}

	void save() {
		cfg.options().header(
				"\n" +
						"This is player's log file\n" +
						"\n" +
						"Every time an item is confiscated, message with\n" +
						"detailed information is stored in this file.\n" +
						"\n" +
						"You can either search within this file directly,\n" +
						"or use inbuilt command '/c log view <player>'.\n" +
						"\n" +
						"It is not recommended to edit it, as it is managed by the plugin.\n");
		cfg.options().copyHeader(true);

		try {
			cfg.save(file);

		} catch (final IOException e) {
			Common.error(e, "Failed to save log file");
		}
	}

	FileConfiguration getConfig() {
		return cfg;
	}

	Map<LogMessage, StrictList<LoggedMessage>> getLog() {
		//        violation, log
		final TreeMap<LogMessage, StrictList<LoggedMessage>> logMap = new TreeMap<>((first, sec) -> first.toString().compareTo(sec.toString()));

		for (final String violation : cfg.getKeys(false)) {
			LogMessage type = null;

			try {
				type = LogMessage.valueOf(violation.toUpperCase());

			} catch (final IllegalArgumentException ex) {
				Common.log("&cError displaying log of type '" + violation.toUpperCase() + "', perhaps the internal name has been changed or have you edited the files? Available: " + Arrays.toString(LogMessage.values()));
				continue;
			}

			final StrictList<LoggedMessage> messages = new StrictList<>();

			for (final String time : cfg.getConfigurationSection(violation).getKeys(false)) {
				final Object item = cfg.get(violation + "." + time + ".item");
				final ItemStack is = item instanceof ItemStack ? (ItemStack) item : null;

				messages.add(new LoggedMessage(type, time, cfg.getString(violation + "." + time + ".log"), is));
			}

			logMap.put(type, messages);
		}

		return logMap;
	}
}

@RequiredArgsConstructor
@Getter
class LoggedMessage {

	private final LogMessage type;
	private final String time;
	private final String message;
	private final ItemStack item;
}