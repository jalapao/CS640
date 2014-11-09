package edu.wisc.cs.sdn.apps.l3routing;

import java.util.ArrayList;
import java.util.HashMap;

import edu.wisc.cs.sdn.apps.util.Host;

import net.floodlightcontroller.core.IOFSwitch;

public class BellmanFord {
	
	public static ArrayList<PathInfo> getShortestPath(ArrayList<Host> hosts, Host myHost){
		HashMap<IOFSwitch, PathInfo> switches = new HashMap<IOFSwitch, PathInfo>();
		IOFSwitch mySwitch = myHost.getSwitch();
		
		for(Host h : hosts){
			switches.put(h.getSwitch(), new PathInfo(h.getPort(), myHost.getPort()));
		}
		
		
		return null;
	}

}
