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
    private static final int RIP_MULTICAST_IP = 0xE0000009;
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
		for (Iface iface : router.getInterfaces().values()){
			RIPv2 ripv2 = new RIPv2();
			ripv2.setCommand(RIPv2.COMMAND_REQUEST);
			sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
		}
        /*********************************************************************/
	}
	
	public boolean sendRIPPacket(RIPv2 ripv2, Iface iface, int destIPAddress, byte[] destMacAddress ){
		UDP udpPacket = new UDP();
		udpPacket.setPayload(ripv2);
		udpPacket.setSourcePort(UDP.RIP_PORT);
		udpPacket.setDestinationPort(UDP.RIP_PORT);
		udpPacket.setChecksum((short) 0);
		
		IPv4 ipPacket = new IPv4(); //version = 4, isTruncated = false
		ipPacket.setPayload(udpPacket);
		ipPacket.setChecksum((short) 0);
		ipPacket.setSourceAddress(iface.getIpAddress());
		ipPacket.setDestinationAddress(destIPAddress);
		ipPacket.setProtocol(IPv4.PROTOCOL_UDP);
		ipPacket.setTtl((byte) 64);
		
		Ethernet etherPacket = new Ethernet();
		etherPacket.setPayload(ipPacket);
		etherPacket.setSourceMACAddress(iface.getMacAddress().toBytes());
		etherPacket.setDestinationMACAddress(destMacAddress);
		System.out.println("sending rip packet: " + etherPacket.toString());
		System.out.println("------------------------");
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
		System.out.println("received packet:\n" + etherPacket.toString());
		System.out.println("ripPacket print:\n" + ripPacket.toString());
		System.out.println("------------------------");
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
		for (Iface iface : router.getInterfaces().values()){
			RIPv2 ripv2 = new RIPv2();
			ripv2.setCommand(RIPv2.COMMAND_RESPONSE);
			sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
		}
		
		try {
			Thread.sleep(UPDATE_INTERVAL * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        /*********************************************************************/
	}
}
