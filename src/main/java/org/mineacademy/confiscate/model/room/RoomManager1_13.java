package org.mineacademy.confiscate.model.room;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class RoomManager1_13 {

	public static final void setChests(Block leftBlock, Block rightBlock) {
		leftBlock.setType(Material.CHEST);
		rightBlock.setType(Material.CHEST);

		final org.bukkit.block.data.type.Chest left = (org.bukkit.block.data.type.Chest) Material.CHEST.createBlockData();
		final org.bukkit.block.data.type.Chest right = (org.bukkit.block.data.type.Chest) Material.CHEST.createBlockData();

		left.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
		right.setType(org.bukkit.block.data.type.Chest.Type.LEFT);

		left.setFacing(BlockFace.SOUTH);
		right.setFacing(BlockFace.SOUTH);

		leftBlock.setBlockData(left, false);
		rightBlock.setBlockData(right, false);
	}

	public static final void setSignRotation(Location signLocation) {
		final org.bukkit.block.data.type.WallSign wallSign = (org.bukkit.block.data.type.WallSign) signLocation.getBlock().getState().getBlockData();

		wallSign.setFacing(BlockFace.SOUTH);
		signLocation.getBlock().setBlockData(wallSign);
	}
}
