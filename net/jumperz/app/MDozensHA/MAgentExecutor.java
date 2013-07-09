package net.jumperz.app.MDozensHA;

import java.util.*;
import java.io.*;
import net.jumperz.util.*;
import net.arnx.jsonic.*;
import net.jumperz.app.MDozensHA.agent.*;

public class MAgentExecutor
extends MAbstractLogAgent
implements MCommand2
{
private Class agentClass;
private String ip;
private Object args;
private int retry;
private int interval;
private int timeout;
private boolean inFailbackState;

private MSubject2 subject2 = new MSubject2Impl();
private Object result;
//--------------------------------------------------------------------------------
public MAgentExecutor( Class agentClass, String ip, int retry, int interval, int timeout, boolean inFailbackState )
{
prefix = ip;

this.agentClass = agentClass;
this.ip = ip;
this.retry = retry;
this.interval = interval;
this.timeout = timeout;
this.inFailbackState = inFailbackState;
}
//--------------------------------------------------------------------------------
public void setArgs( Object args )
{
this.args = args;
}
//--------------------------------------------------------------------------------
public String getIp()
{
return ip;
}
//--------------------------------------------------------------------------------
public void execute()
{
debug( "execute:" + this );
boolean success = false;
debug( "retry:" + retry );
for( int i = 0; i < retry + 1; ++i )
	{
	if( i > 0 )
		{
		MSystemUtil.sleep( 1000 * interval );
		}
	
	MAgentCommand ac = new MAgentCommand( agentClass, ip );
	if( args != null )
		{
		ac.setArgs( args );
		}
	MAsyncCommandManager acm = new MAsyncCommandManager( ac );
	try
		{
		Object result = acm.getFastestAndStopThreadPool( timeout * 1000 );
		debug( result );
		if( result == null )
			{
			info( "Error:" + i + ":" + ip );
			if( inFailbackState )
				{
				break;
				}
			}
		else if( result instanceof Exception )
			{
			info( "Error:" + i + ":" + ip );
			if( inFailbackState )
				{
				break;
				}
			}
		else
			{
			if( inFailbackState )
				{
				debug( ip + ":success:" + i );
				if( i == retry )
					{
					info( ip + " is alive." );
					success = true;
					}
				}
			else
				{
				info( ip + " is alive." );
				success = true;
				break;
				}
			}
		}
	catch( InterruptedException e )
		{
		warn( e );
		}
	}

if( success )
	{
	result = Boolean.TRUE;
	}
else
	{
	result = Boolean.FALSE;
	warn( "* * * * " + ip + " seems down * * * *" );
	}
notify2( result, this );
}
//--------------------------------------------------------------------------------
public Object getResult()
{
return result;
}
//--------------------------------------------------------------------------------
public void breakCommand()
{
}
//-----------------------------------------------------------------------------------
public final void notify2( Object event, Object source )
{
subject2.notify2( event, source );
}
//----------------------------------------------------------------
public final void register2( MObserver2 observer )
{
subject2.register2( observer );
}
//----------------------------------------------------------------
public final void removeObservers2()
{
subject2.removeObservers2();
}
//----------------------------------------------------------------
public final void removeObserver2( MObserver2 observer )
{
subject2.removeObserver2( observer );
}
//--------------------------------------------------------------------------------
}