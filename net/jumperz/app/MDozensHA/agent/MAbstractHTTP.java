package net.jumperz.app.MDozensHA.agent;

import net.jumperz.app.MDozensHA.*;
import net.jumperz.util.*;
import net.jumperz.net.*;
import java.util.*;
import java.io.*;
import java.net.*;

public abstract class MAbstractHTTP
extends MAbstractLogAgent
implements MAgent
{
private int port = getDefaultPort();
private Socket socket;

protected abstract int getDefaultPort();
protected abstract Socket connect( String ip, int port ) throws IOException;
protected List headerList = new ArrayList();
//--------------------------------------------------------------------------------
public final void execute( String ip, Object args )
throws Exception
{
prefix = ip;
checkArgs( args );

try
	{
	socket = connect( ip, port );
	OutputStream out = socket.getOutputStream();
	MHttpRequest request = new MHttpRequest();
	for( int i = 0; i < headerList.size(); ++i )
		{
		request.addHeader( ( String )headerList.get( i ) );
		}
	debug( request );
	out.write( request.toByteArray() );
	MHttpResponse response = new MHttpResponse( new BufferedInputStream( socket.getInputStream() ) );
	if( response.getStatusCode() >= 500 )
		{
		throw new Exception( response.getStatusLine() );
		}
	}
finally
	{
	MSystemUtil.closeSocket( socket );
	}
}
//--------------------------------------------------------------------------------
private void checkArgs( Object args )
{
try
	{
	if( args != null
	 && args instanceof Map
	  )
		{
		Map argsMap = ( Map )args;
		if( argsMap.containsKey( "port" ) )
			{
			port = MStringUtil.parseInt( argsMap.get( "port" ), getDefaultPort() );
			debug( "port:" + port );
			}
		if( argsMap.containsKey( "httpHeader" ) )
			{
			headerList = ( List )argsMap.get( "httpHeader" );
			}
		}
	}
catch( Exception e )
	{
	debug( e );
	}
}
//--------------------------------------------------------------------------------
public final void breakAgent()
{
MSystemUtil.closeSocket( socket );
}
//--------------------------------------------------------------------------------
}