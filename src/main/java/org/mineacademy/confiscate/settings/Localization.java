package org.mineacademy.confiscate.settings;

import java.util.HashMap;

import org.apache.commons.lang.WordUtils;
import org.mineacademy.confiscate.ConfiscateLog.LogMessage;
import org.mineacademy.fo.settings.SimpleLocalization;
import org.mineacademy.fo.settings.YamlConfig.CasusHelper;

@SuppressWarnings("unused")
public class Localization extends SimpleLocalization {

	@Override
	protected int getConfigVersion() {
		return 1;
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#saveComments()
	 */
	@Override
	protected boolean saveComments() {
		return false;
	}

	public static class Log {

		private static HashMap<String /*path*/, String /*message*/> lazyMap = new HashMap<>();

		public static String getMessage(String path) {
			return lazyMap.getOrDefault(path, "{MissingLocaleKey:" + path + "}");
		}

		private static void init() {
			pathPrefix("Log");

			lazyMap.clear();

			for (final LogMessage log : LogMessage.values()) {
				final String p = WordUtils.capitalizeFully(log.name().toLowerCase().replace("_", " ")).replace(" ", "_") + ".";

				final String type = p + "Type";
				final String consoleMsg = p + "Console_Msg";
				final String logMsg = p + "Log_Msg";

				lazyMap.put(type, getString(type));
				lazyMap.put(consoleMsg, getString(consoleMsg));
				lazyMap.put(logMsg, getString(logMsg));
			}
		}
	}

	public static class Room {

		public static String CHEST_MENU_TITLE, BROWSING, CORNERSTONE_SET;
		public static String[] SIGN;

		private static void init() {
			pathPrefix("Room");

			CHEST_MENU_TITLE = getString("Chest_Menu_Title");
			BROWSING = getString("Browsing");
			CORNERSTONE_SET = getString("Cornerstone_Set");
			SIGN = getStringArray("Sign");
		}

		public static class NoPermission {
			public static String CHEST, LOGS, CORNERSTONE, DESTROY;

			private static void init() {
				pathPrefix("Room.No_Permission");

				CHEST = getString("Chest");
				LOGS = getString("Logs");
				CORNERSTONE = getString("Cornerstone");
				DESTROY = getString("Destroy");
			}
		}

	}

	public static class Verbs {

		public static String BUY, SELL, GAMBLE, BARTER;

		private static void init() {
			pathPrefix("Verbs");

			BUY = getString("Buy");
			SELL = getString("Sell");
			GAMBLE = getString("Gamble");
			BARTER = getString("Barter");
		}
	}

	public static class Commands {

		public static String NO_CONSOLE, INVALID_ARGUMENT, INVALID_PARAM;

		private static void init() {
			pathPrefix("Commands");

			NO_CONSOLE = getString("No_Console");
			INVALID_ARGUMENT = getString("Invalid_Argument");
			INVALID_PARAM = getString("Invalid_Param");
		}

		public static class Room {

			public static String LABEL, LABEL_TOOL, LABEL_TP, CORNERSTONE_TOOL_GET, NOT_CREATED, PLAYER_LACKS_CHEST, TELEPORTED_GENERIC, TELEPORTED_PLAYER;

			private static void init() {
				pathPrefix("Commands.Room");

				LABEL = getString("Label");
				LABEL_TOOL = getString("Label_Tool");
				LABEL_TP = getString("Label_Tp");
				CORNERSTONE_TOOL_GET = getString("Cornerstone_Tool_Get");
				NOT_CREATED = getString("Not_Created");
				PLAYER_LACKS_CHEST = getString("Player_Lacks_Chest");
				TELEPORTED_GENERIC = getString("Teleported_Generic");
				TELEPORTED_PLAYER = getString("Teleported_Player");
			}
		}

		public static class Inv {

			public static String LABEL, LABEL_VIEW, LABEL_ENDERVIEW, LABEL_ARMOR, CANNOT_BROWSE_OWN, WRITE_ONLINE, READ_ONLINE, READ_OFFLINE, READ_OFFLINE_FAIL, UNSUPPORTED_TOO_NEW, UNSUPPORTED_TOO_OLD, UNSUPPORTED_LIBRARY_MISSING, MENU_ARMOR_VIEW, MENU_READ, ARMOR_WRITE, ARMOR_OFFLINE_READ, ARMOR_OFFLINE_READ_FAIL;

			private static void init() {
				pathPrefix("Commands.Inv");

				LABEL = getString("Label");
				LABEL_VIEW = getString("Label_View");
				LABEL_ENDERVIEW = getString("Label_Enderview");
				LABEL_ARMOR = getString("Label_Armor");

				CANNOT_BROWSE_OWN = getString("Cannot_Browse_Own");
				WRITE_ONLINE = getString("Write_Online");
				READ_ONLINE = getString("Read_Online");
				READ_OFFLINE = getString("Read_Offline");
				READ_OFFLINE_FAIL = getString("Read_Offline_Fail");

				UNSUPPORTED_TOO_NEW = getString("Unsupported_Too_New");
				UNSUPPORTED_TOO_OLD = getString("Unsupported_Too_Old");
				UNSUPPORTED_LIBRARY_MISSING = getString("Unsupported_Library_Missing");

				MENU_ARMOR_VIEW = getString("Menu_Armor_View");
				MENU_READ = getString("Menu_Read");
				ARMOR_WRITE = getString("Armor_Write");
				ARMOR_OFFLINE_READ = getString("Armor_Offline_Read");
				ARMOR_OFFLINE_READ_FAIL = getString("Armor_Offline_Read_Fail");
			}
		}

		public static class Log {

			public static String LABEL, LABEL_VIEW, LABEL_STATS, LABEL_VIEW_COMMAND, NO_LOGS, NO_LOGS_PLAYER, FINISHED, LOGS_HEADER, LOGS_LINE_TYPE, LOGS_LINE_GENERIC, UNKNOWN_TYPE;

			private static void init() {
				pathPrefix("Commands.Log");

				LABEL = getString("Label");
				LABEL_VIEW = getString("Label_View");
				LABEL_STATS = getString("Label_Stats");
				LABEL_VIEW_COMMAND = getString("Label_View_Command");

				NO_LOGS = getString("No_Logs");
				NO_LOGS_PLAYER = getString("No_Logs_Player");
				FINISHED = getString("Finished");
				LOGS_HEADER = getString("Logs_Header");
				LOGS_LINE_TYPE = getString("Logs_Line_Type");
				LOGS_LINE_GENERIC = getString("Logs_Line_Generic");
				UNKNOWN_TYPE = getString("Unknown_Type");
			}
		}

		public static class Reload {

			public static String SUCCESS, FAIL;

			private static void init() {
				pathPrefix("Commands.Reload");

				SUCCESS = getString("Success");
				FAIL = getString("Fail");
			}
		}

		public static class List {

			public static String LABEL_OPTIONAL, LABEL_REQUIRED, LABEL_ROOM, LABEL_INV, LABEL_LOG, LABEL_RELOAD;

			private static void init() {
				pathPrefix("Commands.List");

				LABEL_OPTIONAL = getString("Label_Optional");
				LABEL_REQUIRED = getString("Label_Required");
				LABEL_ROOM = getString("Label_Room");
				LABEL_INV = getString("Label_Inv");
				LABEL_LOG = getString("Label_Log");
				LABEL_RELOAD = getString("Label_Reload");
			}
		}
	}

	public static class Player {

		public static String NEVER_PLAYED;

		private static void init() {
			pathPrefix("Player");

			NEVER_PLAYED = getString("Never_Played");
		}
	}

	public static class WorldEdit {

		public static String BLOCK, CUT, LIMIT;

		private static void init() {
			pathPrefix("WorldEdit");

			BLOCK = getString("Block");
			CUT = getString("Cut");
			LIMIT = getString("Limit");
		}
	}

	public static class Cases {

		public static CasusHelper SECOND;

		private static void init() {
			pathPrefix("Cases");

			SECOND = getCasus("Second");
		}
	}

	public static class Parts {

		public static String UNKNOWN, CONFISCATED, DENY, SPY, USAGE, TYPE_LOWERCASE, PLAYER_LOWERCASE;

		private static void init() {
			pathPrefix("Parts");

			UNKNOWN = getString("Unknown");
			CONFISCATED = getString("Confiscated");
			DENY = getString("Deny");
			SPY = getString("Spy");
			USAGE = getString("Usage");
			TYPE_LOWERCASE = getString("Type_Lowercase");
			PLAYER_LOWERCASE = getString("Player_Lowercase");
		}
	}
}
