package edu.wisc.cs.sdn.apps.l3routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.cs.sdn.apps.util.Host;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.routing.Link;

public class BellmanFord {
	
	public static final int DISTANCE = 1;
	
	public static HashMap<Long, PathInfo> getShortestPath(Collection<IOFSwitch> switches, Collection<Link> links, Host myHost){
		HashMap<Long, PathInfo> switchesPathInfo = new HashMap<Long, PathInfo>();
		IOFSwitch mySwitch = myHost.getSwitch();
		
		for(IOFSwitch iofSwitch : switches){
			switchesPathInfo.put(iofSwitch.getId(), new PathInfo(iofSwitch.getId(), mySwitch.getId()));
		}
		
		switchesPathInfo.get(mySwitch.getId()).setSendPort(myHost.getPort());
		
		for(int i = 0 ; i < switches.size() - 1; i++){
			for(Link link : links){
				long u = link.getSrc();
				long v = link.getDst();
				if(switchesPathInfo.get(u).getDistance() == Integer.MAX_VALUE){
					continue;
				}else if(switchesPathInfo.get(u).getDistance() + DISTANCE < 0){
					continue;
				}else if(switchesPathInfo.get(u).getDistance() + DISTANCE < switchesPathInfo.get(v).getDistance()){
					switchesPathInfo.get(v).setDistance(switchesPathInfo.get(u).getDistance() + DISTANCE);
					switchesPathInfo.get(v).setSendPort(link.getDstPort());
				}
			}
		}	
		
		return switchesPathInfo;
	}

}