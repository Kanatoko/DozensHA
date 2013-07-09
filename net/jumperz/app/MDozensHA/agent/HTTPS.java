package net.jumperz.app.MDozensHA.agent;

import net.jumperz.app.MDozensHA.*;
import net.jumperz.security.MSecurityUtil;
import net.jumperz.util.*;
import net.jumperz.net.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class HTTPS
extends MAbstractHTTP
implements MAgent
{
//--------------------------------------------------------------------------------
protected int getDefaultPort()
{
return 443;
}
//--------------------------------------------------------------------------------
protected Socket connect( String ip, int port )
throws IOException
{
return MSecurityUtil.getBogusSslSocket( ip, port );
}
//--------------------------------------------------------------------------------
}