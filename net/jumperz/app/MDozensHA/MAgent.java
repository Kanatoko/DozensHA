package net.jumperz.app.MDozensHA;

public interface MAgent
{
public abstract void execute( String ip, Object args ) throws Exception;
public abstract void breakAgent();
}