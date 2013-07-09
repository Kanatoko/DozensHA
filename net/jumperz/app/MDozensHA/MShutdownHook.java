package net.jumperz.app.MDozensHA;

public class MShutdownHook
extends Thread
{
//--------------------------------------------------------------------------------
public void run()
{
System.err.println( "Shutdown..." );
MDozensHA.getInstance().shutdown();
}
//--------------------------------------------------------------------------------
}