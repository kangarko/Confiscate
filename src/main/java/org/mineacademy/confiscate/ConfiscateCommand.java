package org.mineacademy.confiscate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.Permissible;
import org.mineacademy.confiscate.ConfiscateLog.LogMessage;
import org.mineacademy.confiscate.hook.PowerNBTHook;
import org.mineacademy.confiscate.model.room.RoomManager;
import org.mineacademy.confiscate.model.room.RoomTool;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Localization.Commands;
import org.mineacademy.confiscate.settings.Localization.Commands.Inv;
import org.mineacademy.confiscate.settings.Localization.Commands.Log;
import org.mineacademy.confiscate.settings.Localization.Commands.Reload;
import org.mineacademy.confiscate.settings.Localization.Parts;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.confiscate.util.ArmorSlot;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.TabUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.exception.CommandException;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleSettings;

public class ConfiscateCommand extends SimpleCommand {

	protected ConfiscateCommand() {
		super(SimpleSettings.MAIN_COMMAND_ALIASES);

		setPermission(null);
		setAutoHandleHelp(false);
	}

	private enum Param {
		ROOM("room", "r"),
		INV("inv", "i"),
		LOG("log", "stats", "l"),
		PLAYTIME("playtime", "pt"),
		RELOAD("reload", "rl"),
		LIST("list", "?", "help");

		private final String label;
		private final String[] aliases;

		Param(String label, String... aliases) {
			this.label = label;
			this.aliases = aliases;
		}

		private static Param find(String argument) {
			argument = argument.toLowerCase();

			for (final Param p : values()) {
				if (p.label.equals(argument))
					return p;

				if (p.aliases != null && Arrays.asList(p.aliases).contains(argument))
					return p;
			}

			return null;
		}

		private void checkPerm(SimpleCommand cmd) throws CommandException {
			final String found = formPerm();

			cmd.checkPerm(found);
		}

		private boolean hasPerm(Permissible p) {
			return PlayerUtil.hasPerm(p, formPerm());
		}

		private String formPerm() {
			return "confiscate.command." + label;
		}

		@Override
		public final String toString() {
			return super.toString().toLowerCase().replace("_", "");
		}
	}

	@Override
	protected final void onCommand() {
		if (args.length == 0)
			returnTell(
					"&8   " + Common.chatLine(),
					"&f&l  Confiscate&r\u2122 &7" + SimplePlugin.getVersion(),
					" ",
					"&7 Running &f" + SimplePlugin.getVersion(),
					"&7 Made by &fkangarko &7\u00A9 2017 - 2020");

		final Param p = Param.find(args[0]);

		checkNotNull(p, Localization.Commands.INVALID_ARGUMENT);
		p.checkPerm(this);

		final String argument = args[0].toLowerCase();
		final String param = (args.length > 1 ? args[1] : "").toLowerCase();

		final Player pl = sender instanceof Player ? (Player) sender : null;

		if (p == Param.ROOM) {
			checkConsole();

			if (param.isEmpty())
				returnTell(
						"&r",
						Localization.Commands.Room.LABEL,
						"&r",
						"  &f/c room tool &e- " + Localization.Commands.Room.LABEL_TOOL,
						"  &f/c room tp &2[player] &e- " + Localization.Commands.Room.LABEL_TP);
			else if ("tool".equals(param)) {
				checkPerm(Permissions.Commands.ROOM_TOOL);

				pl.getInventory().addItem(RoomTool.getTool());
				CompSound.BLOCK_DISPENSER_LAUNCH.play(pl, 1, 1);
				returnTell(Localization.Commands.Room.CORNERSTONE_TOOL_GET);
			} else if ("tp".equals(param)) {
				checkPerm(Permissions.Commands.ROOM_TP);
				checkBoolean(RoomManager.isCornerstoneSet(), Localization.Commands.Room.NOT_CREATED);

				Location loc;

				if (args.length == 3) {
					final String playerName = args[2];
					loc = RoomManager.getChest(playerName);
					checkBoolean(loc != null, Localization.Commands.Room.PLAYER_LACKS_CHEST.replace("{player}", playerName));

					loc.add(1, 0, 1.5);

				} else
					loc = RoomManager.getCornerstone().add(1, 0, 1.5);

				loc.setYaw(180);

				pl.teleport(loc);
				CompSound.ENDERDRAGON_WINGS.play(pl, 1, 1);

				if (args.length == 3)
					returnTell(Localization.Commands.Room.TELEPORTED_PLAYER.replace("{player}", args[2]));
				else
					returnTell(Localization.Commands.Room.TELEPORTED_GENERIC);
			} else
				returnTell(Localization.Commands.INVALID_ARGUMENT);

		} else if (p == Param.INV) {
			checkConsole();

			if (param.isEmpty())
				returnTell(
						"&r",
						Commands.Inv.LABEL,
						"&r",
						"  &f/c inv view &6<player> &e- " + Commands.Inv.LABEL_VIEW,
						"  &f/c inv enderview &6<player> &e- " + Commands.Inv.LABEL_ENDERVIEW,
						"  &f/c inv armor &6<player> &e- " + Commands.Inv.LABEL_ARMOR);
			else if ("view".equals(param) || "see".equals(param)) {
				checkPerm(Permissions.Commands.INV_VIEW);
				checkArgs(3, "&c" + Parts.USAGE + ": /{label} " + argument + " " + param + " <player>");

				tellInventoryContent(pl, false);
			} else if ("enderview".equals(param) || "enderchest".equals(param)) {
				checkPerm(Permissions.Commands.INV_ENDERVIEW);
				checkArgs(3, "&c" + Parts.USAGE + ": /{label} " + argument + " " + param + " <player>");

				tellInventoryContent(pl, true);
			} else if ("armor".equals(param) || "a".equals(param)) {
				checkPerm(Permissions.Commands.INV_ARMOR);
				checkArgs(3, "&c" + Parts.USAGE + ": /{label} " + argument + " " + param + " <player>");

				tellArmorContent(pl);
			} else
				returnTell(Commands.INVALID_ARGUMENT);

		} else if (p == Param.LOG) {
			if (param.isEmpty())
				returnTell(
						"&r",
						Commands.Log.LABEL,
						"&r",
						"  &f/c log view &6<player> &2[type]&e- " + Commands.Log.LABEL_VIEW,
						"  &f/c log stats &2[type] &e- " + Commands.Log.LABEL_STATS);

			else if ("view".equals(param)) {
				checkPerm(Permissions.Commands.LOG_BROWSE);
				checkBoolean(args.length == 3 || args.length == 4, "&c" + Parts.USAGE + ": /{label} " + argument + " " + param + " &6<player> &2[type]");

				LogMessage type = null;
				final String targetPlayer = args[2];

				if (args.length == 4)
					type = loadType(args[3]);

				ConfiscateLog.printLogsFor(sender, targetPlayer, type);
			} else if ("stats".equals(param)) {
				checkPerm(Permissions.Commands.LOG_STATS);

				LogMessage type = null;

				if (args.length == 3)
					type = loadType(args[2]);

				final List<String> logs = ConfiscateLog.getLogs(type);
				checkBoolean(!logs.isEmpty(), Log.NO_LOGS);

				tell("&7" + Common.chatLine(),
						Log.LOGS_HEADER,
						type != null ? Log.LOGS_LINE_TYPE.replace("{size}", logs.size() + "").replace("{type}", type.toString()) : Log.LOGS_LINE_GENERIC.replace("{size}", logs.size() + ""),
						"&7" + Common.chatLine());

				Common.tell(sender, new TreeSet<>(logs));
			} else
				returnTell(Commands.INVALID_ARGUMENT);

		} else if (p == Param.RELOAD)
			try {
				SimplePlugin.getInstance().reload();

				tell(Reload.SUCCESS);
			} catch (final Throwable t) {
				tell(Reload.FAIL.replace("{error}", t.getMessage() != null ? t.getMessage() : "unknown"));
				t.printStackTrace();
			}

		else if (p == Param.PLAYTIME) {
			checkBoolean(!param.isEmpty(), "Specify the player name whose playtime to get.");

			final Player player = findPlayer(param);
			final int minutes = Remain.getPlaytimeMinutes(player);

			tell("&6Statistic " + Remain.getPlayTimeStatisticName() + " for " + player.getName() + " is " + minutes + " minutes. That is " + (TimeUtil.formatTimeDays(minutes * 60))
					+ ". Newcomer threshold is: " + Settings.Confiscate.ItemsNewcomers.THRESHOLD + " minutes.");

		} else if (p == Param.LIST)
			returnTell(
					"&8   " + Common.chatLine(),
					"&f&l  Confiscate&r\u2122 &7" + SimplePlugin.getVersion(),
					" ",
					Commands.List.LABEL_OPTIONAL,
					Commands.List.LABEL_REQUIRED,
					" ",
					"  &f/c room &e- " + Commands.List.LABEL_ROOM,
					"  &f/c inv &e- " + Commands.List.LABEL_INV,
					"  &f/c log &e- " + Commands.List.LABEL_LOG,
					"  &f/c reload &e- " + Commands.List.LABEL_RELOAD);
		else
			throw new FoException(Commands.INVALID_PARAM.replace("{param}", p.label));
	}

	private void tellInventoryContent(Player pl, boolean ender) throws CommandException {
		final String plName = args[2];
		checkBoolean(!plName.equals(sender.getName()), Inv.CANNOT_BROWSE_OWN);

		final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(plName);

		if (offlinePlayer == null || !offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore())
			returnTell(Localization.Player.NEVER_PLAYED.replace("{player}", plName));

		if (offlinePlayer.isOnline()) {
			CompSound.FIREWORK_BLAST.play(pl, 1, 1);

			if (PlayerUtil.hasPerm(pl, Permissions.Commands.INV_WRITE)) {
				pl.openInventory(ender ? offlinePlayer.getPlayer().getEnderChest() : offlinePlayer.getPlayer().getInventory());

				returnTell(Replacer.of(Inv.WRITE_ONLINE).find("player", "type").replace(offlinePlayer.getName(), ender ? "ender " : "").getReplacedMessage());
			} else {
				final Inventory i = Bukkit.createInventory(null, 36, Common.colorize("&0Reading " + offlinePlayer.getName() + "'s " + (ender ? "enderchest" : "inventory")));

				i.setContents(Arrays.copyOf((ender ? offlinePlayer.getPlayer().getEnderChest() : offlinePlayer.getPlayer().getInventory()).getContents(), 36));

				pl.openInventory(i);
				returnTell(Replacer.of(Inv.READ_ONLINE).find("player", "type").replace(offlinePlayer.getName(), ender ? "ender " : "").getReplacedMessage());
			}
		} else if (PowerNBTHook.isHooked()) {
			checkBoolean(MinecraftVersion.newerThan(V.v1_7), Inv.UNSUPPORTED_TOO_OLD.replace("{player}", plName).replace("{version}", "1.8.8"));
			checkBoolean(MinecraftVersion.olderThan(V.v1_13), Inv.UNSUPPORTED_TOO_NEW.replace("{player}", plName).replace("{version}", "1.13+"));

			final Inventory inv = PowerNBTHook.loadInventory(offlinePlayer, ender);

			if (inv != null) {
				CompSound.FIREWORK_BLAST.play(pl, 1, 1);
				pl.openInventory(inv);

				returnTell(Replacer.of(Inv.READ_OFFLINE).find("player", "type").replace(offlinePlayer.getName(), ender ? "ender " : "").getReplacedMessage());

			} else
				returnTell(Replacer.of(Inv.READ_OFFLINE_FAIL).find("player", "type").replace(offlinePlayer.getName(), ender ? "ender " : "").getReplacedMessage());

		} else
			returnTell(Inv.UNSUPPORTED_LIBRARY_MISSING.replace("{player}", plName));
	}

	private void tellArmorContent(Player pl) throws CommandException {
		final String plName = args[2];
		checkBoolean(!plName.equals(sender.getName()), Inv.CANNOT_BROWSE_OWN);

		final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(plName);

		if (offlinePlayer == null || !offlinePlayer.isOnline() && !offlinePlayer.hasPlayedBefore())
			returnTell(Localization.Player.NEVER_PLAYED.replace("{player}", plName));

		if (offlinePlayer.isOnline()) {
			CompSound.FIREWORK_BLAST.play(pl, 1, 1);

			final EntityEquipment eq = offlinePlayer.getPlayer().getEquipment();
			final Inventory inv = Bukkit.createInventory(null, 9, Common.colorize("&0Viewing " + offlinePlayer.getName() + "'s armor"));

			for (final ArmorSlot as : ArmorSlot.values())
				if (as == ArmorSlot.OFF_HAND)
					try {
						inv.setItem(as.getInvSlot(), offlinePlayer.getPlayer().getInventory().getItemInOffHand());
					} catch (final Throwable t) {
					}
				else
					inv.setItem(as.getInvSlot(), eq.getArmorContents()[as.getInvSlot()]);

			pl.openInventory(inv);
			returnTell(Inv.ARMOR_WRITE.replace("{player}", offlinePlayer.getName()));
		} else if (PowerNBTHook.isHooked()) {
			checkBoolean(MinecraftVersion.newerThan(V.v1_7), Inv.UNSUPPORTED_TOO_OLD.replace("{player}", plName).replace("{version}", "1.8.8"));

			final Inventory inv = PowerNBTHook.loadArmor(offlinePlayer);

			if (inv != null) {
				CompSound.FIREWORK_BLAST.play(pl, 1, 1);
				pl.openInventory(inv);

				returnTell(Inv.ARMOR_OFFLINE_READ.replace("{player}", offlinePlayer.getName()));
			} else
				returnTell(Inv.ARMOR_OFFLINE_READ_FAIL.replace("{player}", offlinePlayer.getName()));

		} else
			returnTell(Inv.UNSUPPORTED_LIBRARY_MISSING.replace("{player}", plName));
	}

	private LogMessage loadType(String raw) throws CommandException {
		raw = raw.toUpperCase();

		final LogMessage type = ReflectionUtil.lookupEnumSilent(LogMessage.class, raw);

		checkBoolean(type != null, Log.UNKNOWN_TYPE.replace("{type}", raw).replace("{available}", Common.join(Arrays.asList(LogMessage.values()), ", ", new Common.Stringer<LogMessage>() {

			private ChatColor color = ChatColor.GRAY;

			@Override
			public String toString(LogMessage object) {
				color = color == ChatColor.GRAY ? ChatColor.WHITE : ChatColor.GRAY;

				return color + object.name().toLowerCase();
			}
		})));

		return type;
	}

	@Override
	public final List<String> tabComplete() {
		final Param p = args.length > 0 ? Param.find(args[0]) : null;

		if (args.length == 0)
			return new ArrayList<>();

		else if (args.length == 1) {
			final List<String> tab = new ArrayList<>();

			for (final Param param : Param.values())
				if (param.toString().startsWith(args[0].toLowerCase()) && param.hasPerm(sender))
					tab.add(param.toString().toLowerCase());

			return tab;

		} else if (args.length == 2 || args.length == 3) {
			final List<String> players = args.length == 3 ? Common.convert(Remain.getOnlinePlayers(), Player::getName) : null;

			if (p == null)
				return new ArrayList<>();

			if (p == Param.ROOM) {
				if (args.length == 3)
					return completeLastWord(Remain.getOnlinePlayers());

				return TabUtil.complete(args[1], Arrays.asList("tool", "tp"));
			}

			if (p == Param.PLAYTIME)
				return completeLastWordPlayerNames();

			if (p == Param.INV)
				return TabUtil.complete(args[players != null ? 2 : 1], players != null ? players : Arrays.asList("view", "enderview", "armor"));

			if (p == Param.LOG)
				return args[1].equals("stats") ? Common.convert(LogMessage.values(), LogMessage::toString)
						: TabUtil.complete(args[args[1].equals("view") && args.length > 2 ? 2 : 1], players != null ? players : Arrays.asList("view", "stats"));

		} else if (args.length == 4)
			if (p == Param.LOG && args[1].equalsIgnoreCase("view"))
				return PlayerUtil.hasPerm(sender, Permissions.Commands.LOG) ? completeLastWord(Common.convert(LogMessage.values(), LogMessage::toString)) : new ArrayList<>();

		return new ArrayList<>();
	}
}
