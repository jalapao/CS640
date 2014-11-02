package edu.wisc.cs.sdn.sr;

import java.util.List;
import java.util.ListIterator;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.RIPv2;
import net.floodlightcontroller.packet.RIPv2Entry;
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

		System.out.println("RIP: Build initial routing table.");
		for(Iface iface : this.router.getInterfaces().values())
		{
			this.router.getRouteTable().addEntry(
					(iface.getIpAddress() & iface.getSubnetMask()),
					0, // No gateway for subnets this router is connected to
					iface.getSubnetMask(), iface.getName(), 0);
		}
		System.out.println("Route Table:\n" + this.router.getRouteTable());

		this.tasksThread.start();

		/*********************************************************************/
		/* TODO: Add other initialization code as necessary                  */
		for(Iface iface : this.router.getInterfaces().values()) {
			RIPv2 ripv2 = new RIPv2();
			ripv2.setCommand(RIPv2.COMMAND_REQUEST);
			List<RIPv2Entry> toBeSent = router.getRouteTable().getRIPv2Entries();
			for (RIPv2Entry r : toBeSent) {
				r.setNextHopAddress(iface.getIpAddress());
			}
			ripv2.setEntries(toBeSent);
			sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
		}
		/*********************************************************************/
	}

	public boolean sendRIPPacket(RIPv2 ripPacket, Iface iface, int destIPAddress, byte[] destMacAddress) {
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
		ipPacket.setTtl((byte) 64);

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
		for (RIPv2Entry ripv2Entry : ripPacket.getEntries()) {
			if (ripv2Entry.getMetric() >= 15) {
				continue;
			}
			RouteTableEntry routeTableEntry = router.getRouteTable().findEntry(ripv2Entry.getAddress(), ripv2Entry.getSubnetMask());
			if (routeTableEntry == null) {
				router.getRouteTable().addEntry(ripv2Entry.getAddress(), ripv2Entry.getNextHopAddress(), ripv2Entry.getSubnetMask(), 
						inIface.getName(), ripv2Entry.getMetric() + 1);
			} else if (ripv2Entry.getMetric() + 1 < routeTableEntry.getCost()) {
				router.getRouteTable().updateEntry(ripv2Entry.getAddress(), ripv2Entry.getSubnetMask(), 
						ripv2Entry.getNextHopAddress(), inIface.getName(), System.currentTimeMillis());
				routeTableEntry.setCost(ripv2Entry.getMetric() + 1);
			}
		}

//		System.out.println("Sending...");
//		System.out.flush();
		// Should not reply to the incoming iface
		if (ripPacket.getCommand() == RIPv2.COMMAND_REQUEST) {
			RIPv2 ripv2 = new RIPv2();
			ripv2.setCommand(RIPv2.COMMAND_RESPONSE);
			List<RIPv2Entry> toBeSent = router.getRouteTable().getRIPv2Entries();
			ListIterator<RIPv2Entry> it = toBeSent.listIterator();
			while (it.hasNext()) {
				RIPv2Entry e = it.next();
				if (e.getInterfaceName().equals(inIface.getName())) {
					it.remove();
				} else {
					e.setNextHopAddress(inIface.getIpAddress());
				}
			}
			ripv2.setEntries(toBeSent);
			// Rest the gateway IP
			sendRIPPacket(ripv2, inIface, ipPacket.getSourceAddress(), etherPacket.getSourceMACAddress());
		}
//		System.out.println("Done...");
//		System.out.println("\nRoute Table:\n" + router.getRouteTable().toString());
//		System.out.flush();
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
//		long lastTimeUpdated = System.currentTimeMillis();
//		long lastTimeRemoved = System.currentTimeMillis();
		while (true) {
//			if (System.currentTimeMillis() - lastTimeUpdated >= UPDATE_INTERVAL * 1000) {
//				lastTimeUpdated = System.currentTimeMillis();
//				for (Iface iface : router.getInterfaces().values()) {
//					RIPv2 ripv2 = new RIPv2();
//					ripv2.setCommand(RIPv2.COMMAND_RESPONSE);
//					List<RIPv2Entry> toBeSent = router.getRouteTable().getRIPv2Entries();
//					ListIterator<RIPv2Entry> it = toBeSent.listIterator();
//					while (it.hasNext()) {
//						RIPv2Entry e = it.next();
//						if (e.getInterfaceName().equals(iface.getName())) {
//							it.remove();
//						}
//					}	
//					ripv2.setEntries(toBeSent);
//
//					sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
//				}
//				
//			}
//			
//			if (System.currentTimeMillis() - lastTimeRemoved >= RIP.TIMEOUT * 1000) {
//				lastTimeRemoved = System.currentTimeMillis();
//				timeoutRouteTableEntries();
//			}
			try {
				Thread.sleep(UPDATE_INTERVAL * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			timeoutRouteTableEntries();

//			System.out.println("Sending...");
//			System.out.flush();
			for (Iface iface : router.getInterfaces().values()) {
				RIPv2 ripv2 = new RIPv2();
				ripv2.setCommand(RIPv2.COMMAND_RESPONSE);
				List<RIPv2Entry> toBeSent = router.getRouteTable().getRIPv2Entries();
				ListIterator<RIPv2Entry> it = toBeSent.listIterator();
				while (it.hasNext()) {
					RIPv2Entry e = it.next();
					if (e.getInterfaceName().equals(iface.getName())) {
						it.remove();
					} else {
						e.setNextHopAddress(iface.getIpAddress());
					}
				}	
				ripv2.setEntries(toBeSent);

				sendRIPPacket(ripv2, iface, RIP_MULTICAST_IP, BROADCAST_MAC);
			}
//			System.out.println("Done...");
//			System.out.println("My routetable is:\n" + router.getRouteTable().toString());
//			System.out.flush();
		}
		/*********************************************************************/
	}

	public void timeoutRouteTableEntries() {
		ListIterator<RouteTableEntry> it = router.getRouteTable().getEntries().listIterator();

		while (it.hasNext()) {
			RouteTableEntry rtEntry = it.next();

			if (rtEntry.getGatewayAddress() == 0)
				continue;

			if (System.currentTimeMillis() - rtEntry.getTime() >= RIP.TIMEOUT * 1000)
				synchronized(router.getRouteTable().getEntries()) {
					it.remove();
//					System.out.println("My routetable is:\n" + router.getRouteTable().toString());
				}
		}
//		System.out.println("\nRoute Table:\n" + router.getRouteTable().toString());
	}
}
