package com.flansmod.common;

import java.util.EnumSet;

import com.flansmod.common.physics.PhysicsHandler;
import com.flansmod.common.teams.TeamsManager;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class CommonTickHandler 
{
	public CommonTickHandler()
	{
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void tick(TickEvent.ClientTickEvent event)
	{
		switch(event.phase)
		{
		case START :
		{
			break;
		}
		case END :
		{
			if(Minecraft.getMinecraft().theWorld != null)
			{
				PhysicsHandler handler = FlansMod.proxy.getPhysicsHandler(Minecraft.getMinecraft().theWorld);
				if(handler == null)
				{
					FlansMod.log("Could not get physics handler for client world");
					break;
				}
				handler.tick();
			}
			break;
		}		
		}
	}
	
	@SubscribeEvent
	public void tick(TickEvent.ServerTickEvent event)
	{
		switch(event.phase)
		{
		case START :
		{
			break;
		}
		case END :
		{
			TeamsManager.getInstance().tick();
			FlansMod.playerHandler.tick();
			for(World world : MinecraftServer.getServer().worldServers)
			{
				if(world == null)
					continue;
				PhysicsHandler handler = FlansMod.proxy.getPhysicsHandler(world);
				if(handler == null)
				{	
					FlansMod.log("Could not get physics handler for server world " + world.getWorldInfo().getWorldName());
					continue;
				}
				handler.tick();
			}
			FlansMod.ticker++;
			break;
		}		
		}
	}
}
