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
		
		if(mySwitch == null){
			System.out.println("mySwitch is null!!!");
//			return switchesPathInfo;
		}
		
		//step 1 : init graph
		for(IOFSwitch iofSwitch : switches){	
			//actual work
			if(mySwitch != null){
				PathInfo pathInfo = new PathInfo(iofSwitch.getId(), mySwitch.getId());
				switchesPathInfo.put(iofSwitch.getId(), pathInfo);
			}		
		}
		
		if(mySwitch != null)
			switchesPathInfo.get(mySwitch.getId()).setSendPort(myHost.getPort());
		
		//step 2 : relax edges repeatedly
		for(int i = 0 ; i < switches.size() - 1; i++){
			for(Link link : links){
				long u = link.getDst();
				long v = link.getSrc();
				
				if((switchesPathInfo.get(u) !=  null) && (switchesPathInfo.get(v) != null)){
					if(switchesPathInfo.get(u).getDistance() == Integer.MAX_VALUE){
						continue;
					}else if(switchesPathInfo.get(u).getDistance() + DISTANCE < 0){
						continue;
					}else if(switchesPathInfo.get(u).getDistance() + DISTANCE < switchesPathInfo.get(v).getDistance()){
						switchesPathInfo.get(v).setDistance(switchesPathInfo.get(u).getDistance() + DISTANCE);
						switchesPathInfo.get(v).setSendPort(link.getSrcPort());
					}
				}
			}
		}	
		
//		step 3 : check for negative-weight cycles
		for(Link link : links){
			long u = link.getDst();
			long v = link.getSrc();
			if((switchesPathInfo.get(u) !=  null) && (switchesPathInfo.get(v) != null)){
				if(switchesPathInfo.get(u).getDistance() + DISTANCE < switchesPathInfo.get(v).getDistance()){
	//				throw new NegativeWeightCycleException();
					System.out.println("Error: Graph contains a negative-weight cycle!!");
				}
			}
		}
		
		return switchesPathInfo;
	}
}
