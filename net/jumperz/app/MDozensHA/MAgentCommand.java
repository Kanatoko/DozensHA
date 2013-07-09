package net.jumperz.app.MDozensHA;

import java.util.*;
import java.io.*;
import net.jumperz.util.*;
import net.arnx.jsonic.*;
import net.jumperz.app.MDozensHA.agent.*;

public class MAgentCommand
extends MAbstractLogAgent
implements MCommand2
{
private Class agentClass;
private MAgent agent;
private MDozensHA dozensHA = MDozensHA.getInstance();
private MSubject2 subject2 = new MSubject2Impl();
private String ip;
private Object args;
private Object result;
//--------------------------------------------------------------------------------
public MAgentCommand( Class agentClass, String ip )
{
this.agentClass = agentClass;
this.ip= ip;
}
//--------------------------------------------------------------------------------
public void setArgs( Object args )
{
this.args = args;
}
//--------------------------------------------------------------------------------
public void execute()
{
try
	{
	debug( "execute:" + this );
	agent = ( MAgent )agentClass.newInstance();
	agent.execute( ip, args );
	result = agent;
	}
catch( Exception e )
	{
	debug( e );
	result = e;
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
agent.breakAgent();
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