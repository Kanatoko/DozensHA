package net.jumperz.app.MDozensHA.agent;

import net.jumperz.app.MDozensHA.*;
import net.jumperz.util.*;
import net.jumperz.net.*;
import java.util.*;
import java.io.*;
import java.net.*;

/*
 * args : { "port": 1234 } required
 */
public class TCP
extends MAbstractLogAgent
implements MAgent
{
private int port;
private Socket socket;
//--------------------------------------------------------------------------------
public void execute( String ip, Object args )
throws Exception
{
checkArgs( args );

try
	{
	socket = new Socket( ip, port );
	}
finally
	{
	MSystemUtil.closeSocket( socket );
	}
}
//--------------------------------------------------------------------------------
private void checkArgs( Object args )
throws Exception
{
if( args == null
 || !( args instanceof Map )
  )
	{
	throw new Exception( "invalid args" );
	}

Map argsMap = ( Map )args;
if( !argsMap.containsKey( "port" ) )
	{
	throw new Exception( "'port' must be configured" );	
	}
port = MStringUtil.parseInt( argsMap.get( "port" ) );
}
//--------------------------------------------------------------------------------
public void breakAgent()
{
MSystemUtil.closeSocket( socket );
}
//--------------------------------------------------------------------------------
}