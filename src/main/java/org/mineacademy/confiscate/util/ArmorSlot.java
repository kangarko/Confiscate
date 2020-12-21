package org.mineacademy.confiscate.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArmorSlot {
	BOOTS(100, 0),
	LEGGINGS(101, 1),
	CHESTPLATE(102, 2),
	HELMET(103, 3),

	OFF_HAND(-106, 8);

	private final int nmsSlot;
	private final int invSlot;

	public static final ArmorSlot fromNmsSlot(int slot) {
		for (final ArmorSlot am : values())
			if (am.nmsSlot == slot)
				return am;

		return null;
	}
}