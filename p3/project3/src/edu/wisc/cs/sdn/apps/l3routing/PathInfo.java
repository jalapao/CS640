package edu.wisc.cs.sdn.apps.l3routing;

public class PathInfo{
	int distance;
	int nexthop;
	int sourceIP;
	int destinationIP;
	
	public PathInfo(int srcIP, int dstIP){
		distance = Integer.MAX_VALUE;
		nexthop = 0;
		sourceIP = srcIP;
		destinationIP  = dstIP;
	}
	
}
