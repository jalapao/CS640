package edu.wisc.cs.sdn.sr;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.RIPv2;
import net.floodlightcontroller.packet.UDP;

/**
  * Implements RIP. 
  * @author Anubhavnidhi Abhashkumar and Aaron Gember-Jacobson
  */
public class RIP implements Runnable
{
    public static final int RIP_MULTICAST_IP = 0xE0000009;
    private static final byte[] BROADCAST_MAC = {(byte)0xFF, (byte)0xFF, 
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
    
    /** Send RIP updates every 10 seconds */
    private static final int UPDATE_INTERVAL = 10;

    /** Timeout routes that neighbors last advertised more than 30 seconds ago*/
    private static final int TIMEOUT = 30;

    /** Router whose route table is being managed */
	private Router router;

    /** Thread for periodic tasks */
    private Thread tasksThread;

	public RIP(Router router)
	{ 
        this.router = router; 
        this.tasksThread = new Thread(this);
    }

	public void init()
	{
        // If we are using static routing, then don't do anything
        if (this.router.getRouteTable().getEntries().size() > 0)
        { return; }

        System.out.println("RIP: Build initial routing table");
        for(Iface iface : this.router.getInterfaces().values())
        {
            this.router.getRouteTable().addEntry(
                    (iface.getIpAddress() & iface.getSubnetMask()),
                    0, // No gateway for subnets this router is connected to
                    iface.getSubnetMask(), iface.getName());
        }
        System.out.println("Route Table:\n"+this.router.getRouteTable());

		this.tasksThread.start();

        /*********************************************************************/
        /* TODO: Add other initialization code as necessary                  */
//		send request to all
		for(Iface iface : this.router.getInterfaces().values()){
			RIPv2 ripv2 = new RIPv2();
			ripv2.setCommand(RIPv2.COMMAND_REQUEST);
			ripv2.setEntries(router.getRouteTable().getRIPv2Entries());
			sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
		}
        /*********************************************************************/
	}
	
	public boolean sendRIPPacket(RIPv2 ripPacket, Iface iface, int destIPAddress, byte[] destMacAddress){
		UDP udp = new UDP();
		udp.setPayload(ripPacket);
		udp.setDestinationPort(UDP.RIP_PORT);
		udp.setSourcePort(UDP.RIP_PORT);
		udp.setChecksum((short) 0);
		
		IPv4 ipPacket = new IPv4();
		ipPacket.setChecksum((short) 0);
		ipPacket.setPayload(udp);
		ipPacket.setProtocol(IPv4.PROTOCOL_UDP);
		ipPacket.setSourceAddress(iface.getIpAddress());
		ipPacket.setDestinationAddress(destIPAddress);
		
		Ethernet etherPacket = new Ethernet();
		etherPacket.setDestinationMACAddress(destMacAddress);
		etherPacket.setSourceMACAddress(iface.getMacAddress().toBytes());
		etherPacket.setEtherType(Ethernet.TYPE_IPv4);
		etherPacket.setPayload(ipPacket);
		return router.sendPacket(etherPacket, iface);
	}

    /**
      * Handle a RIP packet received by the router.
      * @param etherPacket the Ethernet packet that was received
      * @param inIface the interface on which the packet was received
      */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
        // Make sure it is in fact a RIP packet
        if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
        { return; } 
		IPv4 ipPacket = (IPv4)etherPacket.getPayload();
        if (ipPacket.getProtocol() != IPv4.PROTOCOL_UDP)
        { return; } 
		UDP udpPacket = (UDP)ipPacket.getPayload();
        if (udpPacket.getDestinationPort() != UDP.RIP_PORT)
        { return; }
		RIPv2 ripPacket = (RIPv2)udpPacket.getPayload();

        /*********************************************************************/
        /* TODO: Handle RIP packet                                           */
//		send response to requests
		System.out.println("I got packet! " + ripPacket.toString());
		System.out.println("my routetable is : " + router.getRouteTable().toString());
		
        /*********************************************************************/
	}
    
    /**
      * Perform periodic RIP tasks.
      */
	@Override
	public void run() 
    {
        /*********************************************************************/
        /* TODO: Send period updates and time out route table entries        */
//		send response to all
		while(true){
			try {
				Thread.sleep(UPDATE_INTERVAL * 100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			for(Iface iface : router.getInterfaces().values()){
				RIPv2 ripv2 = new RIPv2();
				ripv2.setCommand(RIPv2.COMMAND_RESPONSE);
				ripv2.setEntries(router.getRouteTable().getRIPv2Entries());
				sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
			}
		}
        /*********************************************************************/
	}
}
