package net.jumperz.app.MDozensHA.agent;

import net.jumperz.app.MDozensHA.*;
import net.jumperz.util.*;
import net.jumperz.net.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class HTTP
extends MAbstractHTTP
implements MAgent
{
//--------------------------------------------------------------------------------
protected int getDefaultPort()
{
return 80;
}
//--------------------------------------------------------------------------------
protected Socket connect( String ip, int port )
throws IOException
{
return new Socket( ip, port );
}
//--------------------------------------------------------------------------------
}