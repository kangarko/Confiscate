package org.mineacademy.confiscate.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.confiscate.ConfiscateLog;
import org.mineacademy.confiscate.ConfiscateLog.LogMessage;
import org.mineacademy.confiscate.model.room.RoomManager;
import org.mineacademy.confiscate.model.tag.TagProvider;
import org.mineacademy.confiscate.settings.Settings;
import org.mineacademy.confiscate.util.ConfiscateCustom;
import org.mineacademy.confiscate.util.PermissionCache;
import org.mineacademy.confiscate.util.Permissions;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.debug.Debugger;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

/**
 * Scan is responsible for scanning and taking items.
 *
 * @author kangarko
 */
public class Scan {

	/**
	 * The chest title
	 */
	public static final String CHEST_TITLE = Common.colorize("&0&lConfiscated Chest");

	/**
	 * Player that triggered the scan.
	 */
	private final Player player;

	/**
	 * The scanned inventory.
	 */
	private final Inventory inv;

	/**
	 * What kind of scan is that?
	 */
	private final ScanType scanType;

	/**
	 * A workaround for cloning items
	 */
	private final HashMap<Integer, ItemStack> cloneQueue = new HashMap<>();

	private enum ScanType {
		CONTAINER,
		PLAYERS_INVENTORY,
	}

	/**
	 * Containers that lack location in the world may be from third party plugins or
	 * virtual.
	 */
	private final boolean hasLocation;

	public Scan(Player pl, Inventory inv, ScanType scanType) {
		this.player = pl;
		this.inv = inv;
		this.scanType = scanType;

		hasLocation = scanType == org.mineacademy.confiscate.model.Scan.ScanType.CONTAINER && org.mineacademy.fo.remain.Remain.getLocation(inv) != null;
	}

	// ****************************** Static ******************************

	public static boolean isNonWorldEdit(CompMaterial type) {
		return TechnicalItems.getWorldEditCompatible().contains(type);
	}

	public static boolean isTechnical(CompMaterial mat) {
		return TechnicalItems.getTechnical().contains(mat);
	}

	public static boolean scanPotions(Player pl) {
		final Scan s = new Scan(pl, pl.getInventory(), ScanType.PLAYERS_INVENTORY);

		return s.isScanAllowed() && s.confiscatePotions(false);
	}

	public static void scanInventory(Player pl) {
		final Scan s = new Scan(pl, pl.getInventory(), ScanType.PLAYERS_INVENTORY);

		s.scan();
	}

	public static void scanContainer(Player triggered, Inventory container) {
		final Scan s = new Scan(triggered, container, ScanType.CONTAINER);

		s.scan();
	}

	public static StrictSet<CompMaterial> getTechnicalItems() {
		return TechnicalItems.getTechnical();
	}

	// ****************************** Public ******************************

	/**
	 * The main method that launches the entire scanning process.
	 */
	public void scan() {
		if (!isScanAllowed())
			return;

		Debugger.debug("scan", "Scanning " + player.getName() + "'s " + inv.getType() + " (" + getInvTitle() + ")");

		scanExploits();
		scanBannedItems();
		scanEnchants();
		scanCustom();

		cloneFromQueue();
	}

	private boolean isScanAllowed() {
		final InventoryType type = inv.getType();

		// Ignore inventories in chest room
		if (hasLocation && scanType == ScanType.CONTAINER && RoomManager.isLocationProtected(getContainerLocation())) {
			if (Debugger.isDebugged("scan"))
				log(null, LogMessage.IGNORE_IN_ROOM.formatAndlog(player, type));

			return false;
		}

		final String title = getInvTitle();
		final String defTitle = type.getDefaultTitle();

		if (type == InventoryType.CHEST || type == InventoryType.ENDER_CHEST)
			if (!isSpecialCase(title, defTitle) && !getInvTitle().equals(type.getDefaultTitle())) {
				Debugger.debug("scan", "Ignoring " + type + " inventory with custom title (" + getInvTitle() + " &r!= " + type.getDefaultTitle() + ")");

				return false;
			}

		if (inv.getSize() != type.getDefaultSize() && inv.getSize() != type.getDefaultSize() * 2) {
			Debugger.debug("scan", "Ignoring " + type + " inventory with custom size (" + inv.getSize() + " != " + type.getDefaultSize() + ")");

			return false;
		}

		return true;
	}

	// Special cases for newer Minecraft versions
	private boolean isSpecialCase(String title, String defTitle) {
		if (getInvTitle().contains("container."))
			return true;

		if ((title.equals("Crafting") || title.equals("Large Chest") || title.equals("Minecart with Chest"))
				&& (defTitle.equals("Chest") || defTitle.equals("Ender Chest")))
			return true;

		return title.equals("Minecart with Hopper") && defTitle.equals("Item Hopper");
	}

	// ---------------------------- Private methods ----------------------------

	private void scanExploits() {
		// Take items that may crash the server
		if (Settings.Confiscate.CRASHABLE_ITEMS)
			removeCrashableItems();

		// Take items that naturally do not stack
		if (Settings.Confiscate.ItemsStacked.ENABLED) {
			if (PlayerUtil.hasPerm(player, Permissions.Scan.Bypasses.ILLEGAL_STACKS))
				return;

			confiscateUnnaturalStacks();
		}

		// Take illegal potions
		if (Settings.Confiscate.Potions.HACKED || Settings.Confiscate.Potions.INFINITE)
			confiscatePotions(true);
	}

	private void removeCrashableItems() {
		for (final CompMaterial technical : TechnicalItems.getTechnical())
			if (inv.contains(technical.getMaterial())) {
				inv.remove(technical.getMaterial());

				logAndRunCommands(technical, LogMessage.CRASHABLE.formatAndlog(player, technical, technical.getData()));
			}
	}

	private void confiscateUnnaturalStacks() {
		final ItemStack[] con = inv.getContents();
		int slot = -1;
		boolean changedInventory = false;

		for (final ItemStack is : con) {
			slot++;

			if (!isItemScannable(is))
				continue;

			if (Settings.Confiscate.ItemsStacked.EXCEPTIONS.contains(CompMaterial.fromItem(is)))
				continue;

			if (is.getAmount() > is.getType().getMaxStackSize()) {
				logAndRunCommands(is, LogMessage.STACK_ILLEGAL.formatAndlog(player, is, is.getAmount()));

				paintAndMoveItem(is);
				cloneOrRemove(slot, is, true);

				changedInventory = true;
			}
		}

		if (changedInventory)
			try {
				inv.setContents(con);

			} catch (final Throwable t) {
				Debugger.saveError(t, "Method: unnatural", "Player {" + player.getName() + ", in " + player.getGameMode() + "}, Inventory: {" + inv.getType() + ", size: " + inv.getSize() + ", name: " + getInvTitle() + "}");
			}
	}

	private boolean confiscatePotions(boolean take) {
		final ItemStack[] content = inv.getContents();
		int slot = -1;
		boolean changedInventory = false;

		try {
			for (final ItemStack item : content) {
				slot++;

				if (!isItemScannable(item))
					continue;

				if (hasIgnoredMetadata(item))
					continue;

				if (!(item.getItemMeta() instanceof PotionMeta))
					continue;

				final PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

				// The actual scan
				if (potionMeta.hasCustomEffects()) {
					String name = "Unknown potion";

					if (potionMeta.getCustomEffects().size() == PotionEffectType.values().length) {
						takeOnePotion(slot, take, item, name, false);

						changedInventory = true;
						continue;
					}

					for (final PotionEffect effect : potionMeta.getCustomEffects()) {
						name = effect.getType().getName().toLowerCase().replace("_", " ");

						if (PermissionCache.hasPermission(player, Permissions.Scan.Bypasses.POTIONS, effect))
							continue;

						if (Settings.Confiscate.Potions.INFINITE && (effect.getDuration() < 0 || effect.getDuration() == Integer.MAX_VALUE)) {
							takeOnePotion(slot, take, item, name, true);

							changedInventory = true;
						} else {
							final int lvl = effect.getAmplifier();

							if (lvl > Settings.Confiscate.Potions.MAX_EFFECT || effect.getType().getName().contains("HEAL") && lvl > 100) {
								takeOnePotion(slot, take, item, name, false);

								changedInventory = true;
							}
						}
					}
				}
			}
		} catch (final Throwable t) {
			t.printStackTrace();
		}

		if (changedInventory)
			try {
				inv.setContents(content);

			} catch (final Throwable t) {
				Debugger.saveError(t, "Method: potions", "Player {" + player.getName() + ", in " + player.getGameMode() + "}, Inventory: {" + inv.getType() + ", size: " + inv.getSize() + ", name: " + getInvTitle() + "}");
			}

		return changedInventory;
	}

	private void takeOnePotion(int slot, boolean take, ItemStack is, String type, boolean infinite) {
		final LogMessage msg = infinite ? LogMessage.INFINITE_POTION : LogMessage.HACKED_POTION;

		logAndRunCommands(is, msg.formatAndlog(player, type));

		paintAndMoveItem(is);

		if (take)
			cloneOrRemove(slot, is, true);
		//Common.kick(pl, "&cIllegal potion type");
	}

	private void scanBannedItems() {

		// Confiscate all.
		confiscateBannedMaterials(Settings.Confiscate.Items.ITEM_LIST, false);

		// Confiscate creative-only items.
		scanCreativeOnlyMaterials(Settings.Confiscate.ItemsSurvival.ITEM_LIST);

		// Confiscate for newcomers.
		scanNewcomersAboveLimitItems();

		// Confiscate above limit.
		scanAboveLimit(Settings.Confiscate.ItemsAbove.ITEM_LIST);
	}

	private void scanEnchants() {
		// Merged into one for performance.
		if (Settings.Confiscate.Enchants.TOO_HIGH || Settings.Confiscate.Enchants.NON_APPLICABLE)
			scanUnnaturalAndTooHighEnchants();
	}

	private void scanUnnaturalAndTooHighEnchants() {
		final ItemStack[] con = inv.getContents();
		int slot = -1;

		boolean changedInventory = false;

		for (final ItemStack is : con) {
			slot++;

			if (!isItemScannable(is))
				continue;

			if (hasIgnoredMetadata(is))
				continue;

			scan:
			for (final Map.Entry<Enchantment, Integer> entry : is.getEnchantments().entrySet()) {
				final Enchantment ench = entry.getKey();
				final int level = entry.getValue();
				boolean shouldConfiscate = false;

				if (Settings.Confiscate.Enchants.NON_APPLICABLE) {
					if (Settings.Confiscate.Enchants.NON_APPLICABLE_EXCEPTIONS.contains(ench))
						continue;

					if (!ench.canEnchantItem(is)) {
						logAndRunCommands(is, LogMessage.ENCHANT_UNNATURAL.formatAndlog(player, ItemUtil.bountify(ench), is));

						shouldConfiscate = true;
					}
				}

				if (!shouldConfiscate && Settings.Confiscate.Enchants.TOO_HIGH) {

					final Integer limit = Settings.Confiscate.Enchants.TOO_HIGH_EXCEPTIONS.get(ench);

					if (limit != null && level <= limit)
						continue;

					if (level > ench.getMaxLevel()) {
						logAndRunCommands(is, LogMessage.ENCHANT_TOO_HIGH.formatAndlog(player, ItemUtil.bountify(ench), level, is));

						shouldConfiscate = true;
					}
				}

				if (shouldConfiscate) {

					if (Settings.Confiscate.Enchants.SUPPLY_WITH_UNENCHANTED) {
						paintAndMoveItem(is);

						for (final Enchantment localEnch : is.getEnchantments().keySet())
							is.removeEnchantment(localEnch);

					} else {
						paintAndMoveItem(is);
						cloneOrRemove(slot, is, true);
					}

					changedInventory = true;
					break scan;
				}
			}
		}

		if (changedInventory)
			try {
				inv.setContents(con);
			} catch (final Throwable t) {
				Debugger.saveError(t, "Method: unnatural + too high", "Player {" + player.getName() + ", in " + player.getGameMode() + "}, Inventory: {" + inv.getType() + ", size: " + inv.getSize() + ", name: " + getInvTitle() + "}");
			}
	}

	private boolean hasIgnoredMetadata(ItemStack is) {
		if (!is.hasItemMeta())
			return false;

		final ItemMeta m = is.getItemMeta();

		// Check mcMMO
		if (m.hasLore() && m.getLore().contains("mcMMO Ability Tool"))
			return false;

		// Check ignored
		ignoreCheck:
		if (Settings.Confiscate.Enchants.ALLOW_STRICTNESS > 0) {
			final boolean paranoid = Settings.Confiscate.Enchants.ALLOW_STRICTNESS > 1;
			final boolean hasColoredMeta = m.hasDisplayName() && m.getDisplayName().contains(getColorChar()) || m.hasLore() && StringUtils.join(m.getLore(), "").contains(getColorChar());

			if (hasColoredMeta && is.getType().toString().contains("POTION"))
				break ignoreCheck;

			if (paranoid ? m.hasDisplayName() || m.hasLore() : hasColoredMeta) {
				if (Debugger.isDebugged("metadata"))
					log(is, LogMessage.IGNORE_METADATA.formatAndlog(player, is, paranoid && hasColoredMeta ? "colored " : "custom"));

				return true;
			}
		}

		if (m.hasLore())
			for (final String whitelist : Settings.Confiscate.Enchants.LORE_WHITELIST) {
				final String[] split = whitelist.split("\\|");
				final List<String> lore = m.getLore();

				for (int i = 0; i < split.length; i++)
					if (lore.size() > i && lore.get(i).contains(split[i]))
						return true;
			}

		return false;
	}

	private void confiscateBannedMaterials(IsInList<CompMaterial> bannedMaterials, boolean creativeOnly) {
		for (final CompMaterial banned : bannedMaterials.getList()) {
			boolean takenSomething = false;

			for (final Entry<Integer, ? extends ItemStack> e : inv.all(banned.getMaterial()).entrySet()) {
				final ItemStack is = e.getValue();

				if (banned != CompMaterial.fromItem(is))
					continue;

				final int slot = e.getKey();

				if (!isItemScannable(is))
					continue;

				final String perm = creativeOnly ? Permissions.Scan.Bypasses.BANNED_SURVIVAL : Permissions.Scan.Bypasses.BANNED;

				if (PermissionCache.hasPermission(player, perm, banned))
					continue;

				paintAndMoveItem(is);
				cloneOrRemove(slot, is, true);

				takenSomething = true;
			}

			if (takenSomething)
				logAndRunCommands(banned, LogMessage.ILLEGAL.formatAndlog(player, banned)); // Exempt multiple items?
		}
	}

	private void scanCreativeOnlyMaterials(IsInList<CompMaterial> bannedMaterials) {
		if (player.getGameMode() != GameMode.CREATIVE)
			confiscateBannedMaterials(bannedMaterials, true);
	}

	private void scanNewcomersAboveLimitItems() {
		if (!Settings.Confiscate.ItemsNewcomers.ENABLED)
			return;

		if (Remain.getPlaytimeMinutes(player) > Settings.Confiscate.ItemsNewcomers.THRESHOLD)
			return;

		for (final Map.Entry<CompMaterial, Integer> item : Settings.Confiscate.ItemsNewcomers.ITEM_LIST.entrySet())
			if (!PermissionCache.hasPermission(player, Permissions.Scan.Bypasses.LIMIT_NEWCOMERS, item.getKey()))
				scanAboveLimit0(item.getKey(), item.getValue());
	}

	private void scanAboveLimit(StrictMap<CompMaterial, Integer> itemsWithLimits) {
		for (final Map.Entry<CompMaterial, Integer> item : itemsWithLimits.entrySet()) {
			if (item.getKey() == null || item.getValue() == null) {
				Common.log("Warning: Invalid item with limit. Correct your settings.yml looking for item: " + item.getKey() + " with limit: " + item.getValue() + "");

				continue;
			}

			final CompMaterial key = item.getKey();

			if (!PermissionCache.hasPermission(player, Permissions.Scan.Bypasses.LIMIT, key))
				scanAboveLimit0(item.getKey(), item.getValue());
		}
	}

	private void scanCustom() {
		final ItemStack[] content = inv.getContents();
		boolean tookSomething = false;

		for (final ConfiscateCustom banned : Settings.Confiscate.CustomItems.CUSTOM_ITEMS)
			for (int i = 0; i < content.length; i++) {
				final ItemStack it = content[i];

				if (it == null)
					continue;

				if (PermissionCache.hasPermission(player, Permissions.Scan.Bypasses.BANNED, it.getType()))
					continue;

				if (banned.matches(it)) {
					logAndRunCommands(it, LogMessage.ILLEGAL.formatAndlog(player, banned));

					paintAndMoveItem(it);
					cloneOrRemove(i, it, true);

					tookSomething = true;
				}
			}

		if (tookSomething)
			try {
				inv.setContents(content);
			} catch (final Throwable t) {
				Debugger.saveError(t, "Method: one item", "Player {" + player.getName() + ", in " + player.getGameMode() + "}, Inventory: {" + inv.getType() + ", size: " + inv.getSize() + ", name: " + getInvTitle() + "}");
			}
	}

	/**
	 * Scans the content and removes the item above its limit.
	 *
	 * Returns how many items were removed.
	 */
	private int scanAboveLimit0(CompMaterial item, int maxAllowedPieces) {
		final ItemStack[] content = inv.getContents();
		int found = 0;

		// How much of the item does the inventory contain?
		for (final ItemStack is : content) {
			if (!isItemScannable(is))
				continue;

			if (item == CompMaterial.fromItem(is))
				found += is.getAmount();
		}

		if (found > 0)
			Debugger.debug("scan", "Scanning " + item + " above limit. Found: " + found + " pieces vs. " + maxAllowedPieces + " max allowed.");

		// Remove any above limit if found
		if (found > maxAllowedPieces) {
			final int removeCount = Math.abs(maxAllowedPieces - found);

			logAndRunCommands(item, LogMessage.ABOVE_LIMIT.formatAndlog(removeCount, player, item, maxAllowedPieces));

			for (int i = 0; i < removeCount; i++)
				confiscateOnePiece(content, item);

			try {
				inv.setContents(content);
			} catch (final Throwable t) {
				Debugger.saveError(t, "Method: one item", "Player {" + player.getName() + ", in " + player.getGameMode() + "}, Inventory: {" + inv.getType() + ", size: " + inv.getSize() + ", name: " + getInvTitle() + "}");
			}

			return removeCount;
		}

		return 0;
	}

	/**
	 * Removes one piece of item from the content.
	 */
	private void confiscateOnePiece(ItemStack[] content, CompMaterial toRemove) {
		// Scan from last to first
		for (int i = content.length - 1; i >= 0; i--) {
			final ItemStack is = content[i];

			if (!isItemScannable(is))
				continue;

			if (toRemove == CompMaterial.fromItem(is)) {
				{
					// Workaround start
					final ItemStack copy = is.clone();
					// Workaround end

					if (!Settings.Confiscate.CLONE)
						copy.setAmount(1);

					paintAndMoveItem(copy);
				}

				content[i] = cloneOrRemove(i, is, false);

				break;
			}
		}
	}

	private boolean isItemScannable(ItemStack is) {
		return is != null && is.getType() != Material.AIR && !TagProvider.hasSpecialTag(is);
	}

	/**
	 * Creates a virtual clone of the itemstack that is moved into the chest room.
	 *
	 * Removes the real itemstack if configured to do so.
	 */
	private void paintAndMoveItem(ItemStack is) {
		final ItemStack clone = is.clone();

		if (Settings.Confiscate.LABEL_ITEM)
			giveItemLore(clone);

		RoomManager.moveToChest(player.getName(), clone);
	}

	private ItemStack cloneOrRemove(int slot, ItemStack item, boolean remove) {
		if (Settings.Confiscate.CLONE) {
			final ItemStack clone = TagProvider.addSpecialTag(item.clone());
			cloneQueue.put(slot, clone);

			return clone;

		}

		item.setAmount(remove ? 0 : item.getAmount() - 1 < 0 ? 0 : item.getAmount() - 1);

		if (item.getAmount() == 0)
			inv.setItem(slot, new ItemStack(Material.AIR));

		return item;
	}

	private void cloneFromQueue() {
		for (final Entry<Integer, ItemStack> e : cloneQueue.entrySet())
			inv.setItem(e.getKey(), e.getValue());

		for (final HumanEntity pl : inv.getViewers())
			((Player) pl).updateInventory();

		cloneQueue.clear();
	}

	private void giveItemLore(ItemStack is) {
		final StrictList<String> lore = new StrictList<>();

		if (!is.getEnchantments().isEmpty()) // Space if has enchantments
			lore.add("&r");

		lore.add("&6Origin: &7" + inv.getType().toString().toLowerCase().replace("_", " ") + " " + (hasLocation ? "at" : "nearby") + " " + Common.shortLocation(hasLocation ? getContainerLocation() : player.getLocation()));
		lore.add("&6Triggered: &7" + player.getName());
		lore.add("&6Date: &7" + TimeUtil.getFormattedDateShort());

		// Add extra information about item's original holder
		if (hasLocation) {
			final Location loc = getContainerLocation();

			if (loc != null && player != null) {
				class PartyHelper {
					void addThirdParty(String... parties) {
						for (final String party : parties)
							if (party != null)
								lore.add(party);
					}
				}

				new PartyHelper().addThirdParty(
						ProtectionOwner.getBlockProtectionOwner(loc.getBlock(), player),
						ProtectionOwner.getAreaProtectionOwner(loc),
						ProtectionOwner.getWorldGuardRegions(loc));
			}
		}

		final ItemMeta meta = is.hasItemMeta() ? is.getItemMeta() : Bukkit.getItemFactory().getItemMeta(is.getType());
		Valid.checkNotNull(meta, "Unable to create item meta for " + is);

		meta.setDisplayName("Confiscated " + (meta.hasDisplayName() ? meta.getDisplayName() : WordUtils.capitalize(is.getType().toString().toLowerCase().replace("_", " "))));
		meta.setLore(Common.colorize(lore.getSource()));

		is.setItemMeta(meta);
	}

	private Location getContainerLocation() {
		return hasLocation ? Remain.getLocation(inv) : null;
	}

	private void logAndRunCommands(ItemStack is, LogMessage message) {
		Valid.checkNotNull(is, "Use material method");

		log(is, message);
		runCommands(CompMaterial.fromItem(is), message);
	}

	private void logAndRunCommands(CompMaterial mat, LogMessage message) {
		log(null, message);

		runCommands(mat, message);
	}

	private void log(ItemStack is, LogMessage message) {
		ConfiscateLog.saveLog(player, is, message);
	}

	private void runCommands(CompMaterial mat, LogMessage message) {
		Valid.checkBoolean(!message.toString().contains("IGNORE"), "[REPORT ME] Bacha, aby sa prikazy nespustili ked nieco prehliadneme.");

		for (final String cmd : Settings.AfterConfiscate.RUN_COMMANDS)
			Common.dispatchCommand(player, cmd
					.replace("{date}", TimeUtil.getFormattedDate())
					.replace("{material}", mat == null ? "" : ItemUtil.bountifyCapitalized(mat))
					.replace("{location}", Common.shortLocation(player.getLocation()))
					.replace("{log_type}", ItemUtil.bountifyCapitalized(message).toLowerCase()));
	}

	@Deprecated // Utility
	private String getColorChar() {
		return new String(new char[] { ChatColor.COLOR_CHAR });
	}

	private String getInvTitle() {
		return player.getOpenInventory().getTitle();
	}
}

/**
 * Hook into third party to resolve who actually owns the container or get the
 * residence/territory owner in the container's position.
 */
class ProtectionOwner {

	static String getBlockProtectionOwner(Block block, Player player) {
		String owner = null;

		owner = HookManager.getLWCOwner(block);
		if (owner != null && !owner.isEmpty())
			return "&2LWC: &7" + owner;

		if (HookManager.isLocketteOwner(block, player))
			return "&2Lockette: &7" + player.getName();

		return null;
	}

	static String getAreaProtectionOwner(Location loc) {
		String owner = null;

		owner = HookManager.getResidenceOwner(loc);
		if (owner != null && !owner.isEmpty())
			return "&2Residence " + HookManager.getResidence(loc) + ": &7" + owner;

		owner = HookManager.getFactionOwner(loc);
		if (owner != null && !owner.isEmpty())
			return "&2Faction " + HookManager.getFaction(loc) + ": &7" + owner;

		owner = HookManager.getTownOwner(loc);
		if (owner != null && !owner.isEmpty())
			return "&2Town " + HookManager.getTown(loc) + ": &7" + owner;

		return null;
	}

	static String getWorldGuardRegions(Location loc) {
		String owner = null;

		owner = String.join(", ", Common.getOrDefault(HookManager.getRegions(loc), new ArrayList<>()));

		if (owner != null && !owner.isEmpty())
			return "&2WorldGuard regions: &7" + owner;

		return null;
	}
}

/**
 * Technical items that may crash the server.
 */
enum TechnicalItems {
	STATIONARY_WATER(true),
	WATER(true),
	STATIONARY_LAVA(true),
	LAVA(true),
	BED_BLOCK,
	PISTON_EXTENSION,
	PISTON_MOVING_PIECE,
	FIRE(true),
	REDSTONE_WIRE(true),
	//CROPS(true),
	SIGN_POST,
	WALL_SIGN(true),
	WOODEN_DOOR,
	IRON_DOOR_BLOCK,
	GLOWING_REDSTONE_ORE,
	REDSTONE_TORCH_OFF,
	//SUGAR_CANE_BLOCK,
	PORTAL,
	CAKE_BLOCK,
	DIODE_BLOCK_OFF,
	DIODE_BLOCK_ON,
	PUMPKIN_STEM,
	MELON_STEM,
	NETHER_WARTS(true),
	BREWING_STAND(true),
	CAULDRON(true),
	ENDER_PORTAL,
	REDSTONE_LAMP_ON,
	COCOA(true),
	TRIPWIRE,
	FLOWER_POT(true),
	CARROT(true),
	POTATO(true),
	SKULL(true),
	REDSTONE_COMPARATOR_ON,
	REDSTONE_COMPARATOR_OFF,
	STANDING_BANNER,
	WALL_BANNER,
	DAYLIGHT_DETECTOR_INVERTED,
	SPRUCE_DOOR,
	BIRCH_DOOR,
	JUNGLE_DOOR,
	ACACIA_DOOR,
	DARK_OAK_DOOR,
	END_GATEWAY,
	FROSTED_ICE(true);

	private boolean worldEditCompatible = false;

	TechnicalItems() {
	}

	TechnicalItems(boolean worldEditCompatible) {
		this.worldEditCompatible = worldEditCompatible;
	}

	private static final StrictSet<CompMaterial> worldEditCompatibleMaterials = new StrictSet<>();
	private static final StrictSet<CompMaterial> materials = new StrictSet<>();

	static StrictSet<CompMaterial> getTechnical() {
		return materials;
	}

	static StrictSet<CompMaterial> getWorldEditCompatible() {
		return worldEditCompatibleMaterials;
	}

	static {
		for (final TechnicalItems i : values())
			try {
				final CompMaterial mat = CompMaterial.fromString(i.toString());

				if (mat != null && !materials.contains(mat)) {
					materials.add(mat);

					if (!i.worldEditCompatible)
						worldEditCompatibleMaterials.add(mat);
				}
			} catch (final Throwable t) {
				// MC incompatible
			}
	}
}
