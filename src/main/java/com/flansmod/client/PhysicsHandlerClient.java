package com.flansmod.client;

import java.util.ArrayList;
import java.util.Iterator;

import javax.vecmath.Vector3f;

import jinngine.geometry.Box;
import jinngine.geometry.Geometry;
import jinngine.math.Vector3;
import jinngine.physics.Body;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;

import com.flansmod.client.debug.EntityDebugAABB;
import com.flansmod.common.FlansMod;
import com.flansmod.common.physics.PhysicsHandler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class PhysicsHandlerClient extends PhysicsHandler 
{	
	public PhysicsHandlerClient(World w)
	{
		super(w);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		//DEBUG
		if(FlansMod.ticker % 200 == 0)
		{
			World world = Minecraft.getMinecraft().theWorld;
			EntityPlayer player = Minecraft.getMinecraft().thePlayer;
			if(player == null || !FlansMod.DEBUG)
				return;
			for(Body body : worldBodies.values())
			{
				if(body == null)
					return;
				Iterator<Geometry> geometries = body.getGeometries();
				while(geometries.hasNext())
				{
					Box box = (Box)geometries.next();
					Vector3 origin = new Vector3();
	
	
					box.getLocalTranslation(origin);
					Vector3 lengths = box.getDimentions();
					if(player.getDistance(origin.x, origin.y, origin.z) > 20D)
						continue;
					world.spawnEntityInWorld(new EntityDebugAABB(world, origin, lengths, 400));
				}
			}
		}
	}
}
