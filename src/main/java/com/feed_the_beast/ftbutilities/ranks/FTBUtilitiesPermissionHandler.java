package com.feed_the_beast.ftbutilities.ranks;

import com.feed_the_beast.ftblib.lib.config.ConfigNull;
import com.feed_the_beast.ftblib.lib.config.ConfigValue;
import com.feed_the_beast.ftblib.lib.config.DefaultRankConfigHandler;
import com.feed_the_beast.ftblib.lib.config.IRankConfigHandler;
import com.feed_the_beast.ftblib.lib.config.RankConfigAPI;
import com.feed_the_beast.ftblib.lib.config.RankConfigValueInfo;
import com.feed_the_beast.ftblib.lib.util.misc.Node;
import com.feed_the_beast.ftbutilities.FTBUtilitiesConfig;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.server.permission.DefaultPermissionHandler;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author LatvianModder
 */
public enum FTBUtilitiesPermissionHandler implements IPermissionHandler, IRankConfigHandler
{
	INSTANCE;

	@Override
	public void registerNode(String node, DefaultPermissionLevel level, String desc)
	{
		DefaultPermissionHandler.INSTANCE.registerNode(node, level, desc);
	}

	@Override
	public Collection<String> getRegisteredNodes()
	{
		return DefaultPermissionHandler.INSTANCE.getRegisteredNodes();
	}

	public Event.Result getPermissionResult(GameProfile profile, Node node, @Nullable IContext context)
	{
		if (Ranks.INSTANCE == null)
		{
			return Event.Result.DEFAULT;
		}
		else if (context != null && context.getWorld() != null && context.getWorld().isRemote)
		{
			if (FTBUtilitiesConfig.ranks.crash_client_side_permissions)
			{
				throw new RuntimeException("Do not check permissions on client side! Node: " + node);
			}

			return Event.Result.DEFAULT;
		}

		MinecraftServer server = context != null && context.getWorld() != null ? context.getWorld().getMinecraftServer() : null;
		Rank rank = Ranks.INSTANCE.getRank(server, profile, context);

		if (rank == null)
		{
			return Event.Result.DEFAULT;
		}

		Event.Result result = rank.cachedPermissions.get(node);

		if (result == null)
		{
			result = rank.getPermissionRaw(node);
			rank.cachedPermissions.put(node, result);
		}

		return result;
	}

	@Override
	public boolean hasPermission(GameProfile profile, String nodeS, @Nullable IContext context)
	{
		switch (getPermissionResult(profile, Node.get(nodeS), context))
		{
			case ALLOW:
				return true;
			case DENY:
				return false;
			default:
				return DefaultPermissionHandler.INSTANCE.hasPermission(profile, nodeS, context);
		}
	}

	@Override
	public String getNodeDescription(String node)
	{
		return DefaultPermissionHandler.INSTANCE.getNodeDescription(node);
	}

	@Override
	public void registerRankConfig(RankConfigValueInfo info)
	{
		DefaultRankConfigHandler.INSTANCE.registerRankConfig(info);
	}

	@Override
	public Collection<RankConfigValueInfo> getRegisteredConfigs()
	{
		return DefaultRankConfigHandler.INSTANCE.getRegisteredConfigs();
	}

	@Override
	public ConfigValue getConfigValue(MinecraftServer server, GameProfile profile, Node node, @Nullable IContext context)
	{
		ConfigValue value = ConfigNull.INSTANCE;

		if (Ranks.INSTANCE != null)
		{
			Rank rank = Ranks.INSTANCE.getRank(server, profile, context);

			if (rank != null)
			{
				value = rank.cachedConfig.get(node);

				if (value == null)
				{
					value = ConfigNull.INSTANCE;
					JsonElement json = rank.getConfigRaw(node);

					if (!json.isJsonNull())
					{
						RankConfigValueInfo info = RankConfigAPI.getHandler().getInfo(node);

						if (info != null)
						{
							value = info.defaultValue.copy();
							value.fromJson(json);
						}
					}

					rank.cachedConfig.put(node, value);
				}
			}
		}

		return value.isNull() ? DefaultRankConfigHandler.INSTANCE.getConfigValue(server, profile, node, context) : value;
	}

	@Nullable
	@Override
	public RankConfigValueInfo getInfo(Node node)
	{
		return DefaultRankConfigHandler.INSTANCE.getInfo(node);
	}
}