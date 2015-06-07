package latmod.ftbu.client;

import static net.minecraft.util.EnumChatFormatting.*;
import latmod.ftbu.FTBU;
import latmod.ftbu.core.*;
import latmod.ftbu.core.event.LMPlayerEvent;
import latmod.ftbu.core.gui.*;
import latmod.ftbu.core.net.*;
import latmod.ftbu.core.util.FastList;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.*;

import org.lwjgl.opengl.GL11;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.*;

@SideOnly(Side.CLIENT)
public class GuiFriends extends GuiLM
{
	public static final ResourceLocation texPlayers = FTBU.mod.getLocation("textures/gui/players.png");
	public static final ResourceLocation texActions = FTBU.mod.getLocation("textures/gui/actions.png");
	public static final ResourceLocation texView = FTBU.mod.getLocation("textures/gui/view.png");
	
	public static final TextureCoords icon_status[] =
	{
		new TextureCoords(texPlayers, 161 + 18 * 0, 0),
		new TextureCoords(texPlayers, 161 + 18 * 1, 0),
		new TextureCoords(texPlayers, 161 + 18 * 2, 0),
	};
	
	public final LMPlayer owner;
	public final FastList<Player> players;
	
	public TextBoxLM searchBox;
	public ButtonLM buttonSave, buttonPrevPage, buttonNextPage;
	public ButtonPlayer pbOwner;
	public ButtonPlayer[] pbPlayers;
	public int page = 0;
	public boolean changed = false;
	
	private static LMPlayer selectedPlayer = null;
	private static AbstractClientPlayer selectedPlayerEntity = null;
	private static boolean hideArmor = false;
	
	public ButtonLM buttonAdd, buttonInfo, buttonClose, buttonHideArmor;
	
	public GuiFriends(EntityPlayer ep)
	{
		super(new ContainerEmpty(ep, null), texPlayers);
		
		owner = LMPlayer.getPlayer(ep);
		players = new FastList<Player>();
		
		xSize = 161;
		ySize = 184;
		
		widgets.add(searchBox = new TextBoxLM(this, 24, 5, 113, 18)
		{
			public void textChanged()
			{
				updateButtons();
			}
		});
		
		searchBox.charLimit = 15;
		
		widgets.add(buttonSave = new ButtonLM(this, 139, 6, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
				changed = true;
				mc.thePlayer.closeScreen();
			}
		});
		
		buttonSave.title = GREEN + FTBU.mod.translate("button.save");
		
		widgets.add(buttonPrevPage = new ButtonLM(this, 7, 158, 35, 16)
		{
			public void onButtonPressed(int b)
			{
				page--;
				playClickSound();
				updateButtons();
			}
		});
		
		buttonPrevPage.title = "Prev Page";
		
		widgets.add(buttonNextPage = new ButtonLM(this, 120, 159, 35, 16)
		{
			public void onButtonPressed(int b)
			{
				page++;
				playClickSound();
				updateButtons();
			}
		});
		
		buttonNextPage.title = "Next Page";
		
		// Action gui buttons //
		
		widgets.add(buttonAdd = new ButtonLM(this, -39, 20, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
				
				if(selectedPlayer.equals(owner))
				{
					mc.displayGuiScreen(new FTBUGuiFactory.ModGuiConfig(GuiFriends.this));
				}
				else
				{
					if(owner.isFriendRaw(selectedPlayer))
						sendUpdate(MessageManageGroups.C_REM_FRIEND, selectedPlayer.playerID);
					else
						sendUpdate(MessageManageGroups.C_ADD_FRIEND, selectedPlayer.playerID);
				}
				
				refreshActionButtons();
			}
			
			public boolean isEnabled()
			{ return selectedPlayer != null; }
		});
		
		widgets.add(buttonInfo = new ButtonLM(this, -20, 39, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
			}
			
			public boolean isEnabled()
			{ return LatCoreMC.isDevEnv; }
		});
		
		buttonInfo.title = "[WIP] " + FTBU.mod.translate("button.info");
		
		widgets.add(buttonClose = new ButtonLM(this, -20, 20, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
				selectedPlayer = null;
			}
			
			public boolean isEnabled()
			{ return selectedPlayer != null; }
		});
		
		buttonClose.title = FTBU.mod.translate("button.close");
		
		widgets.add(buttonHideArmor = new ButtonLM(this, -39, 39, 16, 16)
		{
			public void onButtonPressed(int b)
			{
				playClickSound();
				hideArmor = !hideArmor;
				refreshActionButtons();
			}
			
			public boolean isEnabled()
			{ return selectedPlayer != null; }
		});
		
		buttonHideArmor.title = "Hide armor";
		
		// Player buttons //
		
		widgets.add(pbOwner = new ButtonPlayer(this, -1, 5, 5));
		pbOwner.setPlayer(new Player(owner));
		
		pbPlayers = new ButtonPlayer[7 * 8];
		
		for(int i = 0; i < pbPlayers.length; i++)
			widgets.add(pbPlayers[i] = new ButtonPlayer(this, i, 5 + (i % 8) * 19, 25 + (i / 8) * 19));
		
		updateButtons();
	}
	
	public void refreshActionButtons()
	{
		if(selectedPlayer == null) return;
		
		if(owner.equals(selectedPlayer))
			buttonAdd.title = FTBU.mod.translate("button.settings");
		else
			buttonAdd.title = owner.isFriendRaw(selectedPlayer) ? FTBU.mod.translate("button.remFriend") : FTBU.mod.translate("button.addFriend");
	}
	
	public void sendUpdate(int c, int u)
	{
		changed = true;
		MessageLM.NET.sendToServer(new MessageManageGroups(owner, c, u));
	}
	
	public int maxPages()
	{ return (players.size() / pbPlayers.length) + 1; }
	
	public void drawGuiContainerBackgroundLayer(float f, int mx, int my)
	{
		if(selectedPlayer != null)
		{
			setTexture(texView);
			drawTexturedModalRect(guiLeft - 65, guiTop + 63, 0, 0, 65, 101);
		}
		
		super.drawGuiContainerBackgroundLayer(f, mx, my);
		
		pbOwner.render();
		for(int i = 0; i < pbPlayers.length; i++)
			pbPlayers[i].render();
		
		if(selectedPlayer != null)
		{
			setTexture(texActions);
			drawTexturedModalRect(guiLeft - 46, guiTop + 13, 0, 0, 65, 49);
			
			setTexture(texture);
			
			if(owner.equals(selectedPlayer))
				buttonAdd.render(Icons.settings);
			else
				buttonAdd.render(owner.isFriendRaw(selectedPlayer) ? Icons.Friends.remove : Icons.Friends.add);
			
			buttonInfo.render(Icons.help);
			buttonHideArmor.render(hideArmor ? Icons.Friends.view_gray : Icons.Friends.view);
			buttonClose.render(Icons.cancel);
		}
		
		buttonSave.render(Icons.accept);
		
		if(selectedPlayer != null && selectedPlayerEntity != null)
		{
			int x = guiLeft - 31;
			int y = guiTop + 147;
			
			if(hideArmor)
			{
				for(int i = 0; i < 4; i++)
					selectedPlayerEntity.inventory.armorInventory[i] = null;
			}
			else
			{
				EntityPlayer ep1 = mc.theWorld.func_152378_a(selectedPlayer.uuid);
				if(ep1 != null)
				{
					selectedPlayerEntity.inventory.mainInventory = ep1.inventory.mainInventory.clone();
					selectedPlayerEntity.inventory.armorInventory = ep1.inventory.armorInventory.clone();
					selectedPlayerEntity.inventory.mainInventory[0] = ep1.inventory.getCurrentItem();
				}
				else
				{
					for(int i = 0; i < 4; i++)
						selectedPlayerEntity.inventory.armorInventory[i] = selectedPlayer.lastArmor[i];
					selectedPlayerEntity.inventory.mainInventory[0] = selectedPlayer.lastArmor[4];
				}
			}
			
			GuiInventory.func_147046_a(x, y, 35, x - mx, y - 50 - my, selectedPlayerEntity);
			GL11.glColor4f(1F, 1F, 1F, 1F);
		}
	}
	
	public void drawText(int mx, int my)
	{
		searchBox.render(30, 10, 0xFFA7A7A7);
		drawCenteredString(fontRendererObj, (page + 1) + " / " + maxPages(), guiLeft + 81, guiTop + 163, 0xFF444444);
		
		if(selectedPlayer != null)
		{
			String s = selectedPlayer.username;
			drawString(fontRendererObj, s, guiLeft - fontRendererObj.getStringWidth(s) - 2, guiTop + 3, 0xFFFFFFFF);
		}
		
		super.drawText(mx, my);
	}
	
	public void initGui()
	{
		super.initGui();
		LatCoreMC.EVENT_BUS.register(this);
	}
	
	public void onGuiClosed()
	{
		LatCoreMC.EVENT_BUS.unregister(this);
		if(changed) sendUpdate(0, 0);
		super.onGuiClosed();
	}
	
	@SubscribeEvent
	public void onClientEvent(LMPlayerEvent.DataChanged e)
	{ updateButtons(); }
	
	protected void keyTyped(char c, int k)
	{
		super.keyTyped(c, k);
	}
	
	public void updateButtons()
	{
		players.clear();
		
		for(int i = 0; i < LMPlayer.map.values.size(); i++)
		{
			LMPlayer p = LMPlayer.map.values.get(i);
			if(!p.equals(owner)) players.add(new Player(p));
		}
		
		if(!searchBox.text.isEmpty())
		{
			FastList<Player> l = new FastList<Player>();
			
			String s = searchBox.text.trim().toLowerCase();
			for(int i = 0; i < players.size(); i++)
			{
				String s1 = players.get(i).player.username.toLowerCase();
				if(s1.contains(s)) l.add(players.get(i));
			}
			
			players.clear();
			players.addAll(l);
		}
		
		if(page < 0) page = maxPages() - 1;
		if(page >= maxPages()) page = 0;
		
		players.sort(null);
		
		for(int i = 0; i < pbPlayers.length; i++)
		{
			int j = i + page * pbPlayers.length;
			if(j < 0 || j >= players.size()) j =-1;
			
			pbPlayers[i].setPlayer((j == -1) ? null : players.get(j));
		}
		
		refreshActionButtons();
	}
	
	public class Player implements Comparable<Player>
	{
		public final LMPlayer player;
		public final boolean isOwner;
		
		public Player(LMPlayer p)
		{
			player = p;
			isOwner = player.equals(owner);
		}
		
		public int compareTo(Player o)
		{
			int s0 = getStatus();
			int s1 = o.getStatus();
			
			if(s0 == 0 && s1 != 0) return 1;
			if(s0 != 0 && s1 == 0) return -1;
			
			if(s0 == s1)
			{
				boolean on0 = player.isOnline();
				boolean on1 = o.player.isOnline();
				
				if(on0 && !on1) return -1;
				if(!on0 && on1) return 1;
				
				return player.username.compareToIgnoreCase(o.player.username);
			}
			
			return Integer.compare(s0, s1);
		}
		
		public boolean equals(Object o)
		{
			if(o instanceof Player)
				return equals(((Player)o).player);
			return player.equals(o);
		}
		
		public boolean isOwner()
		{ return owner.equals(player); }
		
		/** 0 - None, 1 - Friend, 2 - Inviting, 3 - Invited */
		public int getStatus()
		{
			boolean b1 = owner.isFriendRaw(player);
			boolean b2 = player.isFriendRaw(owner);
			
			if(b1 && b2) return 1;
			if(b1 && !b2) return 2;
			if(!b1 && b2) return 3;
			return 0;
		}
	}
	
	public class ButtonPlayer extends ButtonLM
	{
		public Player player = null;
		
		public ButtonPlayer(GuiLM g, int i, int x, int y)
		{ super(g, x, y, 18, 18); }
		
		public void setPlayer(Player p)
		{ player = p; }
		
		public void onButtonPressed(int b)
		{
			if(player != null)
			{
				selectedPlayer = player.player;
				
				selectedPlayerEntity = new AbstractClientPlayer(mc.theWorld, new GameProfile(selectedPlayer.uuid, selectedPlayer.username))
				{
					public void addChatMessage(IChatComponent p_145747_1_) { }
					
					public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_)
					{ return false; }
					
					public ChunkCoordinates getPlayerCoordinates()
					{ return new ChunkCoordinates(0, 0, 0); }
					
					public boolean isInvisibleToPlayer(EntityPlayer ep)
					{ return true; }
				};
				
				selectedPlayerEntity.func_152121_a(MinecraftProfileTexture.Type.SKIN, AbstractClientPlayer.getLocationSkin(selectedPlayer.username));
				selectedPlayerEntity.inventory.currentItem = 0;
			}
			
			refreshActionButtons();
		}
		
		public void addMouseOverText(FastList<String> al)
		{
			if(player != null)
			{
				LMPlayer p = LMPlayer.getPlayer(player.player.playerID);
				
				if(p == null) return;
				
				al.add(p.username);
				if(p.isOnline()) al.add(GREEN + "[" + FTBU.mod.translate("label.online") + "]");
				
				if(!player.isOwner())
				{
					boolean raw1 = p.isFriendRaw(owner);
					boolean raw2 = owner.isFriendRaw(p);
					
					if(raw1 && raw2)
						al.add(GREEN + "[" + FTBU.mod.translate("label.friend") + "]");
					else if(raw1 || raw2)
						al.add((raw1 ? GOLD : BLUE) + "[" + FTBU.mod.translate("label.pfriend") + "]");
				}
				
				if(LatCoreMC.isDevEnv && isShiftKeyDown())
					al.add(p.uuidString);
				
				if(p.clientInfo != null && !p.clientInfo.isEmpty())
				{
					//al.add("");
					al.addAll(p.clientInfo);
				}
			}
		}
		
		public void render()
		{
			if(player != null)
			{
				background = null;
				
				drawPlayerHead(player.player.username, GuiFriends.this.guiLeft + posX + 1, GuiFriends.this.guiTop + posY + 1, 16, 16, zLevel);
				
				if(player.player.isOnline()) render(Icons.Friends.online);
				
				if(!player.isOwner())
				{
					int status = player.getStatus();
					if(status > 0) render(icon_status[status - 1]);
				}
			}
		}
	}
}