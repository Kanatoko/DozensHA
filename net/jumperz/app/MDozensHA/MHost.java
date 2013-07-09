package net.jumperz.app.MDozensHA;

import java.util.*;
import java.io.*;

import net.jumperz.net.dozens.MSession;
import net.jumperz.util.*;
import net.arnx.jsonic.*;
import net.jumperz.app.MDozensHA.agent.*;

public class MHost
extends MAbstractLogAgent
implements MCommand
{
private String host;
private List ipList;
private Class commandClass = HTTP.class;
private int ttl = 7200;
private Object args;
private String sorryServer;
private int failover_interval;
private int failover_timeout;
private int failover_threshold;
private int failback_interval;
private int failback_timeout;
private int failback_threshold;

public static final int STATE_OK = 1;
public static final int STATE_NG = -1;

private int state = STATE_OK;
private volatile boolean terminated;
//--------------------------------------------------------------------------------
public int getState()
{
return state;
}
//--------------------------------------------------------------------------------
public MHost( Map configMap )
throws Exception
{
host = ( String )configMap.get( "host" );

String commandClassName = ( String )configMap.get( "class" );
if( commandClassName.indexOf( "." ) == -1 )
	{
	commandClassName = HTTP.class.getName().replaceFirst( "HTTP", "" ) + commandClassName;
	commandClass = Class.forName( commandClassName );
	}

ttl = MStringUtil.parseInt( configMap.get( "ttl" ).toString() );

args = configMap.get( "args" );

ipList = ( List )configMap.get( "ipList" );

sorryServer = ( String )configMap.get( "sorryServer" );
if( !sorryServer.matches( "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$" ) )
	{
	throw new Exception( "\"sorryServer\" must be an IP address." );
	}

Map failover = ( Map )configMap.get( "failover" );
failover_interval	= MStringUtil.parseInt( failover.get( "interval" ) );
failover_timeout	= MStringUtil.parseInt( failover.get( "timeout" ) );
failover_threshold	= MStringUtil.parseInt( failover.get( "threshold" ) );

Map failback = ( Map )configMap.get( "failback" );
failback_interval	= MStringUtil.parseInt( failback.get( "interval" ) );
failback_timeout	= MStringUtil.parseInt( failback.get( "timeout" ) );
failback_threshold	= MStringUtil.parseInt( failback.get( "threshold" ) );

prefix = host;
}
//--------------------------------------------------------------------------------
public void execute()
{
try
	{
	while( !terminated )
		{
		final List commandList = getCommandList();
		final Map resultMap = getCommandResult( commandList );
		List[] lists = getCommandResultAsLists( commandList, resultMap );

		info( lists[ 0 ].size() + "/" + ipList.size() + " servers alive." );
		info( lists[ 1 ].size() + "/" + ipList.size() + " servers dead." );

		try
			{
			checkDNS( MDozensHA.getInstance().getRecordMap(), lists[ 0 ], lists[ 1 ] );
			//dozensErrorCount = 0; //reset error count
			}
		catch( Exception e )
			{
			//++dozensErrorCount;
			warn( e.getClass() );
			warn( e );
			}

		
		if( state == STATE_OK )
			{
			MSystemUtil.sleep( failover_interval * 1000 );			
			}
		else
			{
			MSystemUtil.sleep( failback_interval * 1000 );
			}
		}
	}
catch( Exception e )
	{
	warn( e );
	}
}
//--------------------------------------------------------------------------------
public List getCommandList()
throws Exception
{
List commandList = new ArrayList();
for( int i = 0; i < ipList.size(); ++i )
	{
	String ip = ( String )ipList.get( i );	
	MAgentExecutor ae = null;
	
	if( state == STATE_OK )
		{
		ae = new MAgentExecutor( commandClass, ip, failover_threshold - 1, failover_interval, failover_timeout, false );
		}
	else
		{
		ae = new MAgentExecutor( commandClass, ip, failback_threshold - 1, failback_interval, failback_timeout, true );		
		}
	if( args != null )
		{
		ae.setArgs( args );
		}
	commandList.add( ae );
	}
return commandList;
}
//--------------------------------------------------------------------------------
public Map getCommandResult( final List commandList )
throws InterruptedException
{
MAsyncCommandManager acm = new MAsyncCommandManager( commandList );
return acm.getAllAndStopThreadPool();
}
//--------------------------------------------------------------------------------
public List[] getCommandResultAsLists( final List commandList, final Map resultMap )
{
List okList = new ArrayList();
List ngList = new ArrayList();
for( int i = 0; i < commandList.size(); ++i )
	{
	MAgentExecutor ae = ( MAgentExecutor )commandList.get( i );
	Boolean result = ( Boolean )resultMap.get( ae );
	if( result.booleanValue() == true )
		{
		okList.add( ae.getIp() );
		}
	else
		{
		ngList.add( ae.getIp() );
		}
	}

if( okList.size() > 0 )
	{
	state = STATE_OK;
	}
else
	{
	state = STATE_NG;
	}

return new List[]{ okList, ngList };
}
//--------------------------------------------------------------------------------
private static String dozensContains( Map recordMap, String host, String ip )
{
if( !recordMap.containsKey( "record" ) )
	{
	return null;
	}

List recordList = ( List )recordMap.get( "record" );
for( int i = 0; i < recordList.size(); ++i )
	{
	Map record = ( Map )recordList.get( i );
	if( record.containsKey( "type" )	&& record.get( "type" ).equals( "A" )
	 && record.containsKey( "name" )	&& record.get( "name" ).equals( host )
	 && record.containsKey( "content" )	&& record.get( "content" ).equals( ip )
	  )
		{
		return ( String )record.get( "id" );
		}
	}
return null;
}
//--------------------------------------------------------------------------------
public Map applyOkListToDozens( Map recordMap, List okList )
throws Exception
{
	//check OK
for( int i = 0; i < okList.size(); ++i )
	{
	String okIp = ( String )okList.get( i );
	if( dozensContains( recordMap, host, okIp ) == null )
		{
		info( okIp + " : Record not found on Dozens. Adding new one..." );
		recordMap = MDozensHA.getInstance().addRecord( host, okIp, ttl );
		}
	else
		{
		info( okIp + " : Record found on Dozens. OK." );
		}
	}
return recordMap;
}
//--------------------------------------------------------------------------------
public Map removeNgListFromDozens( Map recordMap, List ngList, List validIpList )
throws Exception
{
	//check NG
for( int i = 0; i < ngList.size(); ++i )
	{
	String ngIp = ( String )ngList.get( i );
	String recordId = dozensContains( recordMap, host, ngIp );
	if( recordId != null )
		{
		info( "Deleting " + ngIp + " from Dozens..." );
		recordMap = MDozensHA.getInstance().deleteRecord( recordId );
		validIpList.remove( ngIp );
		}
	}
return recordMap;
}
//--------------------------------------------------------------------------------
public Map removeInvalidIpFromDozens( Map recordMap, List validIpList )
throws Exception
{
final List recordList = ( List )recordMap.get( "record" );
if( recordList != null )
	{
	for( int i = 0; i < recordList.size(); ++i )
		{
		final Map _record = ( Map )recordList.get( i );
		final String _name = ( String )_record.get( "name" );
		if( _name.equals( host ) )
			{
			String _ip = ( String )_record.get( "content" );
			if( !validIpList.contains( _ip ) )
				{
				info( "Deleting invalid record " + _ip + " from Dozens..." );
				recordMap = MDozensHA.getInstance().deleteRecord( ( String )_record.get( "id" ) );
				}
			}
		}
	}
return recordMap;
}
//--------------------------------------------------------------------------------
public Map checkDNS( Map recordMap, List okList, List ngList )
throws Exception
{
final List validIpList = new ArrayList();
validIpList.addAll( okList );
validIpList.addAll( ngList );

recordMap = applyOkListToDozens( recordMap, okList );

	//Sorry Server
if( sorryServer != null && okList.size() == 0 )
	{
	info( "No servers alive." );
	validIpList.add( sorryServer );
	if( dozensContains( recordMap, host, sorryServer ) == null )
		{
		info( "Activating Sorry Server..." );
		recordMap = MDozensHA.getInstance().addRecord( host, sorryServer, ttl );
		}
	else
		{
		info( "Sorry Server " + sorryServer + " : Record found on Dozens. OK." );
		}
	}

recordMap = removeNgListFromDozens( recordMap, ngList, validIpList );
recordMap = removeInvalidIpFromDozens( recordMap, validIpList );
return recordMap;
}
//--------------------------------------------------------------------------------
public synchronized void breakCommand()
{
terminated = true;
}
//--------------------------------------------------------------------------------
}
