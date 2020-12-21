package org.mineacademy.confiscate.model;

import java.util.Set;

import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Setter;

@Setter
public class WorldEditItemLimiter {

	private Set<CompMaterial> items;

	// Material, Max limit
	// NB: Not every item is listed, only those who have limited amount. If the limit is not set, item is not in list.
	private StrictMap<CompMaterial, Integer> itemLimits;

	public void set(Set<CompMaterial> byId, StrictMap<CompMaterial, Integer> itemLimits) {
		Valid.checkBoolean(this.items == null && this.itemLimits == null, "Already set");

		this.items = byId;
		this.itemLimits = itemLimits;
	}

	public boolean contains(CompMaterial mat) {
		return items.contains(mat);
	}

	public int getLimit(CompMaterial mat) {
		return contains(mat) ? itemLimits.get(mat) : 0;
	}
}