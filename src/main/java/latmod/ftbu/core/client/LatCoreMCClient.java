package latmod.ftbu.core.client;

import java.io.File;
import java.util.UUID;

import latmod.ftbu.core.*;
import latmod.ftbu.core.gui.GuiLM;
import latmod.ftbu.core.util.FastMap;
import latmod.ftbu.mod.client.FTBURenderHandler;
import latmod.ftbu.mod.player.ClientNotifications;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.*;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.client.*;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.*;

/** Made by LatvianModder */
public final class LatCoreMCClient // LatCoreMC
{
	public static IIcon blockNullIcon, unknownItemIcon;
	private static float lastBrightnessX, lastBrightnessY;
	private static final FastMap<ResourceLocation, Integer> textureMap = new FastMap<ResourceLocation, Integer>();
	private static final ResourceLocation clickSound = new ResourceLocation("gui.button.press");
	public static int displayW, displayH;
	private static final FastMap<String, ResourceLocation> cachedSkins = new FastMap<String, ResourceLocation>();
	
	public static Minecraft getMinecraft()
	{ return FMLClientHandler.instance().getClient(); }
	
	public static UUID getUUID()
	{ return getMinecraft().getSession().func_148256_e().getId(); }
	
	public static void addEntityRenderer(Class<? extends Entity> c, Render r)
	{ RenderingRegistry.registerEntityRenderingHandler(c, r); }
	
	public static void addTileRenderer(Class<? extends TileEntity> c, TileEntitySpecialRenderer r)
	{ ClientRegistry.bindTileEntitySpecialRenderer(c, r); }
	
	public static int getNewArmorID(String s)
	{ return RenderingRegistry.addNewArmourRendererPrefix(s); }
	
	public static int getNewBlockRenderID()
	{ return RenderingRegistry.getNextAvailableRenderId(); }
	
	public static void addBlockRenderer(int i, ISimpleBlockRenderingHandler r)
	{ RenderingRegistry.registerBlockHandler(i, r); }
	
	public static void addItemRenderer(Item item, IItemRenderer i)
	{ MinecraftForgeClient.registerItemRenderer(item, i); }
	
	public static void addItemRenderer(Block block, IItemRenderer i)
	{ MinecraftForgeClient.registerItemRenderer(Item.getItemFromBlock(block), i); }
	
	public static void spawnPart(EntityFX e)
	{ Minecraft.getMinecraft().effectRenderer.addEffect(e); }
	
	public static KeyBinding addKeyBinding(String name, int key, String cat)
	{ KeyBinding k = new KeyBinding(name, key, cat); ClientRegistry.registerKeyBinding(k); return k; }
	
	public static void addCallbackEvent(ServerTickCallback e)
	{ FTBURenderHandler.callbacks.add(e); }
	
	public static void pushMaxBrightness()
	{
		lastBrightnessX = OpenGlHelper.lastBrightnessX;
		lastBrightnessY = OpenGlHelper.lastBrightnessY;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
	}
	
	public static void popMaxBrightness()
	{ OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY); }

	public static EntityPlayerSP getPlayerSP(UUID uuid)
	{
		//getMinecraft().getIntegratedServer().getConfigurationManager().playerEntityList
		
		World w = getMinecraft().theWorld;
		if(w != null)
		{
			EntityPlayer ep = w.func_152378_a(uuid);
			if(ep != null && ep instanceof EntityPlayerSP)
				return (EntityPlayerSP)ep;
		}
		
		return null;
	}
	
	public static ThreadDownloadImageData getDownloadImage(ResourceLocation out, String url, ResourceLocation def, IImageBuffer buffer)
	{
		TextureManager t = getMinecraft().getTextureManager();
		ThreadDownloadImageData img = (ThreadDownloadImageData)t.getTexture(out);
		
		if (img == null)
		{
			img = new ThreadDownloadImageData((File)null, url, def, buffer);
			t.loadTexture(out, img);
		}
		
		return img;
	}
	
	public static ResourceLocation getSkinTexture(String username)
	{
		ResourceLocation r = cachedSkins.get(username);
		
		if(r == null)
		{
			r = AbstractClientPlayer.getLocationSkin(username);
			
			try
			{
				AbstractClientPlayer.getDownloadImageSkin(r, username);
				cachedSkins.put(username, r);
			}
			catch(Exception e)
			{ e.printStackTrace(); }
		}
		
		return r;
	}
	
	public static void setTexture(ResourceLocation tex)
	{
		if(GuiLM.currentGui != null)
		{
			GuiLM.currentGui.setTexture(tex);
			return;
		}
		
		Integer i = textureMap.get(tex);
		
		if(i == null)
		{
			getMinecraft().getTextureManager().bindTexture(tex);
			textureMap.put(tex, i = Integer.valueOf(getMinecraft().getTextureManager().getTexture(tex).getGlTextureId()));
		}
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, i.intValue());
	}
	
	public static void resetTextureMaps()
	{
		textureMap.clear();
		cachedSkins.clear();
	}
	
	public static void playClickSound()
	{ LatCoreMCClient.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(clickSound, 1F)); }

	public static void notifyClient(String ID, Object text, int t)
	{ ClientNotifications.add(new Notification(ID, LatCoreMC.getChatComponent(text), t)); }
}