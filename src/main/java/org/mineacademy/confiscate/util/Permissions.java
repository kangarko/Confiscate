package org.mineacademy.confiscate.util;

/**
 * This class holds all permissions used by the plugin.
 *
 * If you are not familiar with Java and want to use these permissions,
 * remember the only efficient part is the part in the brackets:
 *
 * For example the permission from this line:
 * public static final String ROOM = "confiscate.command.room";
 *
 * would just be: confiscate.command.room
 */
public class Permissions {

	private Permissions() {
	}

	public class Commands {

		// Permission to access the /c list command.
		public static final String LIST = "confiscate.command.list";

		// Permission to access the /c reload command.
		public static final String RELOAD = "confiscate.command.reload";

		// Permission to access the /c room command.
		public static final String ROOM = "confiscate.command.room";

		// Permission to obtain the room tool that sets room's foundation.
		public static final String ROOM_TOOL = "confiscate.command.room.tool";

		// Permission to be teleported to the room.
		public static final String ROOM_TP = "confiscate.command.room.tp";

		// Permission to access the /c inv command.
		public static final String INV = "confiscate.command.inv";

		// Permission to view inventories of other online and offline players.
		public static final String INV_VIEW = "confiscate.command.inv.view";

		// Permission to view ender chest of other online and offline players.
		public static final String INV_ENDERVIEW = "confiscate.command.inv.enderview";

		// Permission to view armor content of other online and offline players.
		public static final String INV_ARMOR = "confiscate.command.inv.armor";

		// Permission to manipulate with the content of inventories or armor. Defaults to false,
		// that means that the viewer only sees the content as it is.
		public static final String INV_WRITE = "confiscate.command.inv.write";

		// Permission to access the /c log command.
		public static final String LOG = "confiscate.command.log";

		// Permission to browse players' log.
		public static final String LOG_BROWSE = "confiscate.command.log.browse";

		// Permission to view stored log statistics.
		public static final String LOG_STATS = "confiscate.command.log.stats";
	}

	public class Scan {

		public class Bypasses {

			// Permission to bypass scanning when opening a specified container.
			public static final String CONTAINER = "confiscate.bypass.container.{inventoryType}";

			// Permission to bypass inventory scan, e.g. when they log in, die or change world.
			public static final String INVENTORY = "confiscate.bypass.inventory";

			// Permission to allow items stacked unnaturally (e.g. 64x minecarts).
			public static final String ILLEGAL_STACKS = "confiscate.bypass.stacks";

			// Permission to allow holding of illegal potions (infinite, killer potions etc) of specified type.
			// Please use names from https://hub.spigotmc.org/javadocs/spigot/org/bukkit/potion/PotionEffectType.html
			public static final String POTIONS = "confiscate.bypass.potion.{potion_type}";

			// Permission to allow holding of banned items of specific material.
			public static final String BANNED = "confiscate.bypass.item.{material}";

			// Permission to allow holding of banned items in survival of specific material.
			public static final String BANNED_SURVIVAL = "confiscate.bypass.item_creative.{material}";

			// Permission to bypass item amount restriction for specific item.
			public static final String LIMIT = "confiscate.bypass.limit.{material}";

			// Permission to bypass item amount restriction for specific item for newcomers.
			public static final String LIMIT_NEWCOMERS = "confiscate.bypass.limit_newcomers.{material}";

			// Permission to bypass item limit for WorldEdit operations. Since some operations work
			// with thousands of items, we do not allow only certain items to be bypassed because of the performance
			// impact could freeze the server.
			public static final String LIMIT_WORLDEDIT = "confiscate.bypass.worldedit";
		}

	}

	public class Notify {
		// Permission to be notified when something is confiscated.
		public static final String CONFISCATE = "confiscate.notify.confiscate";

		// Permission to receive alerts when a command is executed.
		public static final String COMMAND_SPY = "confiscate.notify.commandspy";

		// Permission to receive alerts on join when a new version is available.
		public static final String UPDATE = "confiscate.notify.updates";
	}

	public class Room {
		// Permission to open chests in the chest room.
		public static final String OPEN = "confiscate.room.open";

		// Permission to view player logs by clicking on their sign in the chest room.
		public static final String SIGNS = "confiscate.room.signs";

		// Permission to use the room foundation tool.
		public static final String TOOL = "confiscate.room.tool";
	}

	public class Exempt {
		// Player will be ignored from logging.
		public static final String LOG = "confiscate.exempt.log.{log_type}";

		// Player will be ignored from command spy.
		public static final String COMMAND_SPY = "confiscate.exempt.commandspy";

	}
}