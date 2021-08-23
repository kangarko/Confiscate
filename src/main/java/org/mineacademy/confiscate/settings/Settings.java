package org.mineacademy.confiscate.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.mineacademy.confiscate.model.WorldEditItemLimiter;
import org.mineacademy.confiscate.util.ConfiscateCustom;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.SimpleSettings;

@SuppressWarnings("unused")
public final class Settings extends SimpleSettings {

	@Override
	protected int getConfigVersion() {
		return 5;
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#saveComments()
	 */
	@Override
	protected boolean saveComments() {
		return false;
	}

	@Override
	protected void beforeLoad() {
		super.beforeLoad();

		migrateSettings();
	}

	private void migrateSettings() {
		// Check old settings
		final File old = FileUtil.getFile("settings.yml");

		if (old.exists()) {
			final FileConfiguration cfg = FileUtil.loadConfigurationStrict(old);

			// Is incompatible?
			if (cfg.getInt("Version", 0) < 3) {

				Common.logFramed(false,
						"&eCONFIGURATION NOTICE",
						"",
						"We are very sorry, but your settings.yml file",
						"appears incompatible with the current format.",
						" ",
						"The file has been renamed to settings_old.yml",
						"Please migrate your keys, as needed, manually",
						"to the new settings.yml file.");

				final File renamed = FileUtil.getFile("settings_old.yml");

				old.renameTo(renamed);
				old.delete();
			}
		}
	}

	// ------------------------------------------------------------------------------------------
	// Settings sections
	// ------------------------------------------------------------------------------------------

	public static class Room {
		public static Boolean ENABLED, WAIT_WITH_LOADING;
		public static CompMaterial MATERIAL;

		private static void init() {
			pathPrefix("Chest_Room");

			ENABLED = getBoolean("Enabled");
			WAIT_WITH_LOADING = getBoolean("Load_After_Server_Starts");
			MATERIAL = getMaterial("Material");
		}
	}

	public static class Scan {
		public static Boolean SCAN_INVENTORIES;
		public static Boolean SCAN_CONTAINERS;
		public static List<String> SCAN_ON_COMMAND_LIST;
		public static List<InventoryType> CONTAINERS_EXCEPTIONS;

		private static void init() {

			pathPrefix("Scan");
			SCAN_INVENTORIES = getBoolean("Inventories");
			SCAN_ON_COMMAND_LIST = getStringList("On_Command");

			pathPrefix("Scan.Containers");
			SCAN_CONTAINERS = getBoolean("Enabled");
			CONTAINERS_EXCEPTIONS = getList("Ignored", InventoryType.class);

			if (!CONTAINERS_EXCEPTIONS.contains(InventoryType.ANVIL) && VERSION < 5) {
				Common.log("&cYour Scan.Ignored list did not contain ANVIL. We added this to prevent problems in renaming items. If you remove it manually, it won't be readded later.");

				CONTAINERS_EXCEPTIONS.add(InventoryType.ANVIL);
				set("Ignored", Common.convert(CONTAINERS_EXCEPTIONS, InventoryType::toString));
			}

		}
	}

	public static class Ignore {
		public static Boolean CREATIVE;
		public static StrictList<String> WORLDS;

		private static void init() {
			pathPrefix("Ignore");

			CREATIVE = getBoolean("Creative_Mode");
			WORLDS = new StrictList<>(getStringList("Worlds"));
		}
	}

	public static class Confiscate {

		public static Boolean CRASHABLE_ITEMS;
		public static Boolean CLONE;
		public static Boolean LABEL_ITEM;

		private static void init() {
			pathPrefix("Confiscate");

			CRASHABLE_ITEMS = getBoolean("Crashable_Items");

			if (CRASHABLE_ITEMS && MinecraftVersion.atLeast(V.v1_13)) {
				Common.logFramed(false,
						"The option Confiscate.Crashable_Item is no",
						"longer necessary on Minecraft 1.13+ and",
						"was disabled to prevent false catches.");

				CRASHABLE_ITEMS = false;
				set("Crashable_Items", false);
			}

			CLONE = getBoolean("Clone");
			LABEL_ITEM = getBoolean("Label_Item");
		}

		public static class Potions {
			public static Boolean HACKED;
			public static Boolean INFINITE;
			public static Integer MAX_EFFECT;

			private static void init() {
				pathPrefix("Confiscate.Potions");

				HACKED = getBoolean("Hacked");
				INFINITE = getBoolean("Infinite");
				MAX_EFFECT = getInteger("Max_Effect");

				if ((HACKED || INFINITE) && !MinecraftVersion.newerThan(V.v1_8)) {
					Common.log("&cConfiscating potions requires MC 1.9 or greater. Disabling..");

					set("Hacked", false);
					set("Infinite", false);
				}
			}
		}

		public static class Enchants {
			public static Integer ALLOW_STRICTNESS;
			public static StrictList<String> LORE_WHITELIST;

			public static Boolean SUPPLY_WITH_UNENCHANTED;

			public static Boolean NON_APPLICABLE;
			public static StrictList<Enchantment> NON_APPLICABLE_EXCEPTIONS;

			public static Boolean TOO_HIGH;
			public static StrictMap<Enchantment, Integer> TOO_HIGH_EXCEPTIONS;

			private static void init() {
				pathPrefix("Confiscate.Enchants");
				ALLOW_STRICTNESS = getInteger("Allow_Strictness");
				LORE_WHITELIST = new StrictList<>(getStringList("Lore_Whitelist"));
				SUPPLY_WITH_UNENCHANTED = getBoolean("Supply_With_Unenchanted");

				pathPrefix("Confiscate.Enchants.Non_Applicable");
				NON_APPLICABLE = getBoolean("Enabled");
				NON_APPLICABLE_EXCEPTIONS = getEnchantments("Exceptions");

				pathPrefix("Confiscate.Enchants.Too_High");
				TOO_HIGH = getBoolean("Enabled");
				TOO_HIGH_EXCEPTIONS = loadEnchantsLimitList("Exceptions");
			}
		}

		public static class Items {
			public static IsInList<CompMaterial> ITEM_LIST;

			private static void init() {
				pathPrefix("Confiscate");

				ITEM_LIST = new IsInList<>(getMaterialList("Items"));
			}
		}

		public static class ItemsSurvival {
			public static IsInList<CompMaterial> ITEM_LIST;

			private static void init() {
				pathPrefix("Confiscate");

				ITEM_LIST = new IsInList<>(getMaterialList("Items_Survival"));
			}
		}

		public static class ItemsStacked {
			public static Boolean ENABLED;
			public static IsInList<CompMaterial> EXCEPTIONS;

			private static void init() {
				pathPrefix("Confiscate.Items_Stacked_Unnaturally");

				ENABLED = getBoolean("Enabled");
				EXCEPTIONS = new IsInList<>(getMaterialList("Exceptions"));
			}
		}

		public static class ItemsAbove {
			public static StrictMap<CompMaterial, Integer> ITEM_LIST;

			private static void init() {
				pathPrefix("Confiscate");

				ITEM_LIST = loadMaterialLimitList("Items_Above");
			}
		}

		public static class ItemsNewcomers {
			public static Boolean ENABLED;
			public static Long THRESHOLD;
			public static StrictMap<CompMaterial, Integer> ITEM_LIST;

			private static void init() {
				pathPrefix("Confiscate.Items_Newcomers");

				ENABLED = !getString("Threshold").equalsIgnoreCase("none");
				THRESHOLD = getTime("Threshold").getTimeSeconds() / 60L;
				ITEM_LIST = loadMaterialLimitList("List");

				if (ENABLED && MinecraftVersion.olderThan(V.v1_13)) {
					Common.log("&cThe option &fConfiscate.Items_Newcomer&c requires MC 1.13+ and has been disabled.");

					set("Threshold", "none");
					ENABLED = false;
				}
			}
		}

		public static class CustomItems {

			public static List<ConfiscateCustom> CUSTOM_ITEMS = new ArrayList<>();

			private static void init() {
				pathPrefix("Confiscate");

				CUSTOM_ITEMS.clear();

				final List<?> list = (List<?>) getObject("Custom_Items");

				for (final Object obj : list) {
					Valid.checkBoolean(obj instanceof Map, "Custom_Items list must contain a list! Got: " + obj + " (" + obj.getClass().getSimpleName() + ") instead");

					final Map<String, Object> raw = (Map<String, Object>) obj;

					final String name = raw.get("name").toString();
					final String lore = raw.get("lore").toString();
					Valid.checkBoolean(name != null && lore != null, "Custom_List item must contain both 'name' and 'lore'!");

					CUSTOM_ITEMS.add(new ConfiscateCustom(name, lore));
				}
			}
		}
	}

	public static class AfterConfiscate {
		public static StrictList<String> RUN_COMMANDS;

		private static void init() {
			pathPrefix("After_Confiscating");

			RUN_COMMANDS = new StrictList<>(getStringList("Run_Commands"));
		}
	}

	public static class Log {
		public static Boolean ENABLED;
		public static List<String> EXCEPTIONS;

		private static void init() {
			pathPrefix("Log");

			ENABLED = getBoolean("Enabled");
			EXCEPTIONS = getStringList("Exceptions");
		}
	}

	public static class Spy {
		public static Boolean ENABLED;
		public static Boolean WRITE_TO_FILE;
		public static Boolean BLOCK;
		public static StrictList<String> COMMAND_LIST;

		private static void init() {
			pathPrefix("Spy");

			ENABLED = getBoolean("Enabled");
			WRITE_TO_FILE = getBoolean("Write_To_File");
			BLOCK = getBoolean("Block");
			COMMAND_LIST = new StrictList<>(getStringList("Command_List"));
		}
	}

	public static class WorldEdit {
		public static Boolean ENABLED;
		public static StrictMap<String, Integer> TOTAL_GROUP_LIMITS;
		public static WorldEditItemLimiter LIMITS;
		public static Long THRESHOLD;

		private static void init() {
			pathPrefix("WorldEdit");

			ENABLED = getBoolean("Enabled");
			TOTAL_GROUP_LIMITS = new StrictMap<>(getMap("Total_Blocks_Limit", String.class, Integer.class));

			final StrictMap<CompMaterial, Integer> denied = createLimitedList("Blocks_Limits");
			final Set<CompMaterial> deniedById = new HashSet<>();

			for (final CompMaterial mat : denied.keySet())
				deniedById.add(mat);

			LIMITS = new WorldEditItemLimiter();
			LIMITS.set(deniedById, denied);

			THRESHOLD = TimeUtil.toTicks(getString("Wait_Threshold")); // getInteger("Wait_Threshold_Seconds");
		}
	}

	// ------------------------------------------------------------------------------------------
	// Private helper methods
	// ------------------------------------------------------------------------------------------

	private static StrictMap<CompMaterial, Integer> createLimitedList(String path) {
		final StrictMap<CompMaterial, Integer> list = new StrictMap<>();

		for (final String itemRaw : getStringList(path)) {
			String rawMaterial;
			Integer maxAmount = 0;

			if (itemRaw.contains("-")) {
				final String[] parts = itemRaw.split("-");
				Valid.checkBoolean(parts.length == 2, "Invalid material syntax, use the following: <material_name>-<max_amount>. Affected: " + path + "." + itemRaw);

				rawMaterial = parts[0];

				try {
					maxAmount = Integer.parseInt(parts[1]);
				} catch (final NumberFormatException ex) {
					Common.log("&cError: Limit must be a number, check your syntax on: " + path + "." + itemRaw);
				}

			} else
				rawMaterial = itemRaw;

			final CompMaterial mat = CompMaterial.fromStringCompat(rawMaterial);

			if (mat != null && maxAmount != 0 && !list.contains(mat))
				list.put(mat, maxAmount);
		}

		return list;
	}

	private static StrictMap<Enchantment, Integer> loadEnchantsLimitList(String path) {
		final StrictMap<Enchantment, Integer> map = new StrictMap<>();

		for (final Map.Entry<String, Integer> e : getMap(path, String.class, Integer.class).entrySet()) {
			final Enchantment enchant = ItemUtil.findEnchantment(e.getKey());
			final int limit = e.getValue();

			map.put(enchant, limit);
		}

		return map;
	}

	private static StrictMap<CompMaterial, Integer> loadMaterialLimitList(String path) {
		final StrictMap<CompMaterial, Integer> map = new StrictMap<>();

		for (final Map.Entry<String, Integer> e : getMap(path, String.class, Integer.class).entrySet()) {
			final CompMaterial item = CompMaterial.fromString(e.getKey());
			final int limit = e.getValue();

			map.put(item, limit);
		}

		return map;
	}
}