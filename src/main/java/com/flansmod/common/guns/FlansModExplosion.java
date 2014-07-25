package com.flansmod.common.guns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.flansmod.common.types.InfoType;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;

public class FlansModExplosion extends Explosion 
{
    
	public FlansModExplosion(World w, Entity e, EntityPlayer p, InfoType t, double x, double y, double z, float r, boolean breakBlocks) 
	{
		super(w, e, x, y, z, r);
		worldObj = w;
		type = t;
		player = p;
        isFlaming = false;
        isSmoking = breakBlocks;
        w.createExplosion(p, x, y, z, r, isSmoking);
	}
}
