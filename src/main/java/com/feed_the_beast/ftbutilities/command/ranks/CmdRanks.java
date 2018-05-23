package com.feed_the_beast.ftbutilities.command.ranks;


import com.feed_the_beast.ftblib.lib.cmd.CmdTreeBase;

/**
 * @author LatvianModder
 */
public class CmdRanks extends CmdTreeBase
{
	public CmdRanks()
	{
		super("ranks");
		addSubcommand(new CmdAdd());
		addSubcommand(new CmdGet());
		addSubcommand(new CmdSet());
		addSubcommand(new CmdGetPermission());
		addSubcommand(new CmdSetPermission());
	}
}