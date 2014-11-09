package edu.wisc.cs.sdn.apps.l3routing;

import net.floodlightcontroller.core.IOFSwitch;
import edu.wisc.cs.sdn.apps.util.Host;

public class PathInfo{
	int distance;
	long sourceSwid;
	long destinationSwid;
	int sendPort;

	public PathInfo(long source, long destination){

		//if dstHost directly connects to srcSwitch
		if(source == destination) 
			distance = 0;
		else
			distance = Integer.MAX_VALUE;
		
		sourceSwid = source;
		destinationSwid  = destination;
		sendPort = 0;
	}
	
	public int getSendPort() {
		return sendPort;
	}

	public void setSendPort(int sendPort) {
		this.sendPort = sendPort;
	}
	
	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public long getSource() {
		return sourceSwid;
	}

	public void setSource(long source) {
		this.sourceSwid = source;
	}

	public long getDestination() {
		return destinationSwid;
	}

	public void setDestination(long destination) {
		this.destinationSwid = destination;
	}

}
