package net.jumperz.app.MDozensHA;

import java.util.*;
import java.io.*;

import net.jumperz.net.dozens.MSession;
import net.jumperz.util.*;
import net.arnx.jsonic.*;
import net.jumperz.app.MDozensHA.agent.*;

public class MDozensHA
extends MAbstractLogAgent
implements MObserver1
{
private static MDozensHA instance;

private MThreadPool threadPool;
private int timeCount;
private volatile boolean stopped;
private Map configMap;
private List hosts;

	//dozens
private String user;
private String apiKey;
private String zone;
private Map defaultConf;
private int dozensErrorCount;
private String authToken;
private long lastAuth;

private Map cachedRecordMap;
private long dozensLastLoadTime;
//--------------------------------------------------------------------------------
public MDozensHA()
{
instance = this;
}
//--------------------------------------------------------------------------------
public static MDozensHA getInstance()
{
return instance;
}
//--------------------------------------------------------------------------------
public static void setDebug()
{
debug = true;
}
//--------------------------------------------------------------------------------
public static void main( String[] args )
throws Exception
{
if( args.length == 0 )
	{
	System.err.println( "Usage: java net.jumperz.app.MDozensHA.MDozensHA confFileName [ --debug ]" );
	return;
	}
else if( args.length == 2 )
	{
	if( args[ 1 ].equals( "--debug" ) )
		{
		debug = true;
		}
	}

Runtime.getRuntime().addShutdownHook( new MShutdownHook() );

( new MDozensHA() ).start( args[ 0 ] );
}
//--------------------------------------------------------------------------------
public synchronized Map getRecordMap()
throws IOException
{
long now = System.currentTimeMillis();
if( ( now - dozensLastLoadTime ) > ( 15 * 1000 ) )
	{
	refreshDozens();
	}
return cachedRecordMap;
}
//--------------------------------------------------------------------------------
private void refreshDozens()
throws IOException
{
cachedRecordMap = getSession().getRecord( zone );
debug( "recordMap refreshed." );
debug( cachedRecordMap );
dozensLastLoadTime = System.currentTimeMillis();
}
//--------------------------------------------------------------------------------
private MSession getSession()
throws IOException
{
MSession session = new MSession( user, apiKey );

	//check token expiration
long now = System.currentTimeMillis();
if( ( now - lastAuth ) > 1000 * 60 * 60 * 23 )
	{
	authToken = null;
	lastAuth = System.currentTimeMillis();
	}

session.init( authToken );

authToken = session.getAuthToken();
debug( "authToken:" + authToken );

return session;
}
//--------------------------------------------------------------------------------
public void start( String confFileName )
{
try
	{
	loadConfig( confFileName );
	
	threadPool = new MThreadPool( hosts.size() );
	for( int i = 0; i < hosts.size(); ++i )
		{
		threadPool.addCommand( new MHost( ( Map ) hosts.get( i ) ) );
		MSystemUtil.sleep( ( new Random() ).nextInt( 5000 ) );
		}
	}
catch( Exception e )
	{
	warn( e );
	}
}
//--------------------------------------------------------------------------------
public void loadConfig( String confFileName )
throws Exception
{
configMap = ( Map )JSON.decode( MStringUtil.loadStrFromFile( confFileName ) );
debug( configMap );

if( !configMap.containsKey( "zone" ) )
	{
	throw new Exception( "\"zone\" not found on the configuration file." );
	}
else
	{
	zone = ( String )configMap.get( "zone" );
	}

if( !configMap.containsKey( "user" ) )
	{
	throw new Exception( "\"user\" not found on the configuration file." );
	}
else
	{
	user = ( String )configMap.get( "user" );
	}

if( !configMap.containsKey( "apiKey" ) )
	{
	throw new Exception( "\"apiKey\" not found on the configuration file." );
	}
else
	{
	apiKey = ( String )configMap.get( "apiKey" );
	}

hosts = ( List )configMap.get( "hosts" );

if( configMap.containsKey( "default" ) )
	{
	defaultConf = ( Map )configMap.get( "default" );
	debug( defaultConf );
	for( int i = 0; i < hosts.size(); ++i )
		{
		Map base = new HashMap();
		base.putAll( defaultConf );
		
		Map hostConf = ( Map )hosts.get( i );
		base.putAll( hostConf );
		
		hostConf.putAll( base );
		
		debug( hostConf );
		}
	}

}
//--------------------------------------------------------------------------------
public Map deleteRecord( String recordId )
throws IOException
{
cachedRecordMap = getSession().deleteRecord( recordId );
dozensLastLoadTime = System.currentTimeMillis();
return cachedRecordMap;
}
//--------------------------------------------------------------------------------
public Map addRecord( String host, String ip, int ttl )
throws IOException
{
Map data = new HashMap();
data.put( "domain", zone );
data.put( "type", "A" );
data.put( "name", host.substring( 0, host.length() - zone.length() - 1 ) );
data.put( "content", ip );
data.put( "ttl", ttl + "" );

cachedRecordMap = getSession().addRecord( data );
dozensLastLoadTime = System.currentTimeMillis();
return cachedRecordMap;
}
//--------------------------------------------------------------------------------
public void update()
{
++timeCount;
}
// --------------------------------------------------------------------------------
public synchronized void shutdown()
{
if( stopped )
	{
	return;
	}
stopped = true;
if( threadPool != null )
	{
	threadPool.stop();	
	}
}

}
