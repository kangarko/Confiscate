package org.mineacademy.confiscate.listener;

import java.util.Locale;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.confiscate.settings.Localization;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.constants.FoConstants;

public class CommandSpyListener implements Listener {

	private static final Pattern PATTERN_ON_SPACE = Pattern.compile(" ", 16);

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (!Settings.Spy.ENABLED)
			return;

		String msg = e.getMessage();
		if (msg == null || msg.isEmpty())
			return;

		final Player pl = e.getPlayer();

		if (PlayerUtil.hasPerm(pl, Permissions.Exempt.COMMAND_SPY))
			return;

		final Command cmd = identifyCommand(msg);
		String perm = null;

		if (cmd != null) {
			perm = cmd.getPermission();

			if (perm == null && cmd instanceof PluginCommand)
				perm = ((PluginCommand) cmd).getPlugin().getName().toLowerCase() + "." + cmd.getLabel();

			if (perm != null && !PlayerUtil.hasPerm(pl, perm))
				return;
		}

		// Try to match /<label>
		boolean matched = Valid.isInListStartsWith(msg, Settings.Spy.COMMAND_LIST);

		// If not, try to match it as a regular expression
		if (!matched) {
			if (msg.startsWith("/"))
				msg = "\\/" + msg.substring(1);

			for (String regex : Settings.Spy.COMMAND_LIST) {
				if (regex.startsWith("/"))
					regex = "\\/" + regex.substring(1);

				if (Common.regExMatch(regex, msg)) {
					matched = true;

					break;
				}
			}
		}

		if (matched) {
			Common.broadcastWithPerm(Permissions.Notify.COMMAND_SPY, "&8C " + (Settings.Spy.BLOCK ? Localization.Parts.DENY : Localization.Parts.SPY) + " &8// &7" + pl.getName() + "&8: &7" + msg, false);

			if (Settings.Spy.WRITE_TO_FILE)
				FileUtil.writeFormatted(FoConstants.File.ChatControl.COMMAND_SPY, pl.getName() + " at " + Common.shortLocation(pl.getLocation()), msg);

			if (Settings.Spy.BLOCK) {
				Common.tell(pl, identifyPermissionMessage(cmd));

				e.setCancelled(true);
			}
		}
	}

	private Command identifyCommand(String commandLine) {
		final String[] args = PATTERN_ON_SPACE.split(commandLine);
		if (args.length == 0)
			return null;

		for (final Plugin p : Bukkit.getPluginManager().getPlugins()) {
			final JavaPlugin plugin = (JavaPlugin) p;
			final Command cmd = plugin.getCommand(args[0].toLowerCase(Locale.ENGLISH).substring(1));

			if (cmd != null)
				return cmd;

			//Common.Log("Command /" + cmd.getName() + " [" + StringUtils.join(cmd.getAliases(), ", ") + "] - " + perm + " " + (hasPerm ? "&acan" : "&ccannot"));
		}

		return null;
	}

	private String identifyPermissionMessage(Command cmd) {
		if (cmd == null || cmd.getPermissionMessage() == null)
			return "&cI'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.";

		return cmd.getPermissionMessage().replace("<permission>", cmd.getPermission());
	}
}