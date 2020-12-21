package org.mineacademy.confiscate;

public final class PlayerCache {

	public long lastWorldEditViolation = 0;

	public void reset() {
		lastWorldEditViolation = 0;
	}
}