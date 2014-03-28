package com.flansmod.common.physics;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import jinngine.collision.BroadphaseCollisionDetection;
import jinngine.collision.SAP2;
import jinngine.collision.SweepAndPrune;
import jinngine.geometry.Box;
import jinngine.geometry.Geometry;
import jinngine.physics.Body;
import jinngine.physics.DefaultScene;
import jinngine.physics.Scene;
import jinngine.physics.constraint.contact.ContactConstraintManager;
import jinngine.physics.constraint.contact.DefaultContactConstraintManager;
import jinngine.physics.solver.NonsmoothNonlinearConjugateGradient;
import jinngine.physics.solver.Solver;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.flansmod.client.debug.EntityDebugAABB;
import com.flansmod.common.FlansMod;
import com.flansmod.common.driveables.DriveableData;
import com.flansmod.common.driveables.DriveablePart;
import com.flansmod.common.driveables.EntityDriveable;
import com.flansmod.common.driveables.EnumDriveablePart;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class PhysicsHandler 
{		
	/** Each time a driveable moves into a new block, the blocks around it are added to the physics world. 
	 * To avoid the entire Minecraft world of blocks filling up the physics world, the physics world is cleared
	 * and recalculated every so often. This defines the tick delay between refreshes */
	private static final int geometryRefreshDelay = 200;
	
	protected World world;
	protected ArrayList<EntityDriveable> loadedDriveables = new ArrayList<EntityDriveable>();
	protected HashMap<EntityDriveable, Body> driveableBodies = new HashMap<EntityDriveable, Body>();
	protected HashMap<ChunkCoordinates, Body> worldBodies = new HashMap<ChunkCoordinates, Body>();
	protected Scene physicsScene;
	
	public PhysicsHandler(World w)
	{		
		world = w;
		physicsScene = new DefaultScene();
		physicsScene.setTimestep(1D / 20D);
	}
	
	public Scene getPhysicsScene() 
	{
		return physicsScene;
	}
	
	public void tick()
	{
		//Simulate the physics world
		if (physicsScene != null) 
			physicsScene.tick();
		
		if(FlansMod.ticker % geometryRefreshDelay == 0)
		{
			//FlansMod.log("Tick tock bro " + FMLCommonHandler.instance().getEffectiveSide().toString() + " " + world.getWorldInfo().getWorldName() + " " + world.getWorldInfo().getVanillaDimension());
			for(Body body : worldBodies.values())
				physicsScene.removeBody(body);
			
			worldBodies.clear();
		
			for(EntityDriveable driveable : loadedDriveables)
				driveableMoved(driveable);
			
		}
	}
	
	/** When a driveable moves to a different block, add blocks that were not previously in the physics world */
	protected void driveableMoved(EntityDriveable driveable)
	{
		float radius = driveable.getDriveableType().bulletDetectionRadius;
		int r = MathHelper.ceiling_float_int(radius);
		int posX = MathHelper.floor_double(driveable.posX);
		int posY = MathHelper.floor_double(driveable.posY);
		int posZ = MathHelper.floor_double(driveable.posZ);
		World world = driveable.worldObj;
		AxisAlignedBB mask = AxisAlignedBB.getBoundingBox(posX - r, posY - r, posZ - r, posX + r, posY + r, posZ + r);
		//Iterate over the cube of half-length r
		for(int i = -r; i < r; i++)
		{
			for(int j = -r; j < r; j++)
			{
				for(int k = -r; k < r; k++)
				{
					//Make sure we are only working in the ball of radius r
					if(i * i + j * j + k * k > r * r)
						continue;
					
					//Add the collision body for the block
					Block block = world.getBlock(posX + i, posY + j, posZ + k);
					//First, get the AABB list from the standard method
					ArrayList<AxisAlignedBB> boxList = new ArrayList<AxisAlignedBB>();
					block.addCollisionBoxesToList(world, posX + i, posY + j, posZ + k, mask, boxList, driveable);
					
					//Then convert this into a list of geometries
					ArrayList<Geometry> geometries = new ArrayList<Geometry>();
					for(AxisAlignedBB aabb : boxList)
					{
						Box box = new Box((aabb.maxX - aabb.minX) / 2, (aabb.maxY - aabb.minY) / 2, (aabb.maxZ - aabb.minZ) / 2, (aabb.minX + aabb.maxX) / 2, (aabb.minY + aabb.maxY) / 2, (aabb.minZ + aabb.maxZ) / 2);
						box.setMass(0);
						geometries.add(box);
					}
					
					//No point adding bodies with no actual collision
					if(geometries.size() == 0)
						continue;
					
					//And create the body
					Body body = new Body("x" + (posX + i) + "|y" + (posY + j) + "|z" + (posZ + k), geometries.iterator());
					body.setFixed(true);
					physicsScene.addBody(body);
					worldBodies.put(new ChunkCoordinates(posX + i, posY + j, posZ + k), body);
				}
			}
		}
	}

	/** Called when a driveable is loaded */
	public void driveableLoaded(EntityDriveable driveable) 
	{
		loadedDriveables.add(driveable);
		//Update mesh
		//driveableDamaged(driveable);
	}
	
	/** Called when a driveable loses a part. Refreshes the collision mesh */
	public void driveableDamaged(EntityDriveable driveable)
	{
		ArrayList<Geometry> geometries = new ArrayList<Geometry>();
		DriveableData data = driveable.driveableData;
		for(EnumDriveablePart part : EnumDriveablePart.values())
		{
			DriveablePart dPart = data.parts.get(part);
			if(dPart.maxHealth > 0 && dPart.health > 0 && dPart.geometry != null)
			{
				geometries.add(dPart.geometry);
			}
		}
		
		Body body = new Body("drive" + driveable.getEntityId(), geometries.iterator());
		driveableBodies.put(driveable, body);
		driveable.body = body;
	}
}
