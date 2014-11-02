package edu.wisc.cs.sdn.sr;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import edu.wisc.cs.sdn.sr.vns.VNSComm;

import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.util.MACAddress;

/**
 * @author Aaron Gember-Jacobson
 */
public class Router 
{
	/** User under which the router is running */
	private String user;

	/** Hostname for the router */
	private String host;

	/** Template name for the router; null if no template */
	private String template;

	/** Topology ID for the router */
	private short topo;

	/** List of the router's interfaces; maps interface name's to interfaces */
	private Map<String,Iface> interfaces;

	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	/** PCAP dump file for logging all packets sent/received by the router;
	 *  null if packets should not be logged */
	private DumpFile logfile;

	/** Virtual Network Simulator communication manager for the router */
	private VNSComm vnsComm;

	/** RIP subsystem */
	private RIP rip;

	/**
	 * Creates a router for a specific topology, host, and user.
	 * @param topo topology ID for the router
	 * @param host hostname for the router
	 * @param user user under which the router is running
	 * @param template template name for the router; null if no template
	 */
	public Router(short topo, String host, String user, String template)
	{
		this.topo = topo;
		this.host = host;
		this.setUser(user);
		this.template = template;
		this.logfile = null;
		this.interfaces = new HashMap<String,Iface>();
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache(this);
		this.vnsComm = null;
		this.rip = new RIP(this);
	}

	public void init()
	{ this.rip.init(); }

	/**
	 * @param logfile PCAP dump file for logging all packets sent/received by 
	 * 		  the router; null if packets should not be logged
	 */
	public void setLogFile(DumpFile logfile)
	{ this.logfile = logfile; }

	/**
	 * @return PCAP dump file for logging all packets sent/received by the
	 *         router; null if packets should not be logged
	 */
	public DumpFile getLogFile()
	{ return this.logfile; }

	/**
	 * @param template template name for the router; null if no template
	 */
	public void setTemplate(String template)
	{ this.template = template; }

	/**
	 * @return template template name for the router; null if no template
	 */
	public String getTemplate()
	{ return this.template; }

	/**
	 * @param user user under which the router is running; if null, use current 
	 *        system user
	 */
	public void setUser(String user)
	{
		if (null == user)
		{ this.user = System.getProperty("user.name"); }
		else
		{ this.user = user; }
	}

	/**
	 * @return user under which the router is running
	 */
	public String getUser()
	{ return this.user; }

	/**
	 * @return hostname for the router
	 */
	public String getHost()
	{ return this.host; }

	/**
	 * @return topology ID for the router
	 */
	public short getTopo()
	{ return this.topo; }

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }

	/**
	 * @return list of the router's interfaces; maps interface name's to
	 * 	       interfaces
	 */
	public Map<String,Iface> getInterfaces()
	{ return this.interfaces; }

	/**
	 * @param vnsComm Virtual Network System communication manager for the router
	 */
	public void setVNSComm(VNSComm vnsComm)
	{ this.vnsComm = vnsComm; }

	/**
	 * Close the PCAP dump file for the router, if logging is enabled.
	 */
	public void destroy()
	{
		if (logfile != null)
		{ this.logfile.close(); }
	}

	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}

		System.out.println("Loading routing table");
		System.out.println("---------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("---------------------------------------------");
	}

	/**
	 * Add an interface to the router.
	 * @param ifaceName the name of the interface
	 */
	public Iface addInterface(String ifaceName)
	{
		Iface iface = new Iface(ifaceName);
		this.interfaces.put(ifaceName, iface);
		return iface;
	}

	/**
	 * Gets an interface on the router by the interface's name.
	 * @param ifaceName name of the desired interface
	 * @return requested interface; null if no interface with the given name 
	 * 		   exists
	 */
	public Iface getInterface(String ifaceName)
	{ return this.interfaces.get(ifaceName); }

	/**
	 * Send an Ethernet packet out a specific interface.
	 * @param etherPacket an Ethernet packet with all fields, encapsulated
	 * 		  headers, and payloads completed
	 * @param iface interface on which to send the packet
	 * @return true if the packet was sent successfully, otherwise false
	 */
	public boolean sendPacket(Ethernet etherPacket, Iface iface)
	{ return this.vnsComm.sendPacket(etherPacket, iface.getName()); }

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));
		System.out.flush();
		/********************************************************************/
		/* TODO: Handle packets                                             */
		short etherType = etherPacket.getEtherType();
		switch (etherType) {
		case Ethernet.TYPE_IPv4:
			handleIPv4Packet(etherPacket, inIface);
			break;
		case Ethernet.TYPE_ARP:
			handleArpPacket(etherPacket, inIface);
			break;
		default:
			return;
		}
		/********************************************************************/
	}

	private void handleIPv4Packet(Ethernet etherPacket, Iface inIface) {
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
		{ return; }
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();
		int destinationIP = ipPacket.getDestinationAddress();

		// TODO
		if (!checkIPChecksum(ipPacket))
			return;

		boolean thisIsMyIP = false;
		if (destinationIP == RIP.RIP_MULTICAST_IP)
			thisIsMyIP = true;
		for (Iface i : interfaces.values()) {
			if (i.getIpAddress() == destinationIP) {
				thisIsMyIP = true;
				break;
			}
		}

		if (thisIsMyIP) {
			// Check if timeout
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
				if (ipPacket.getTtl() <= 1) {
					// Type 11 code 0
					sendICMPError(etherPacket, inIface, (byte) 11, (byte) 0);
					return;
				}
				ICMP icmpPacket = (ICMP)ipPacket.getPayload();
				if (checkICMPChecksum(icmpPacket) && icmpPacket.getIcmpType() == (byte) 8) {
					// Send ICMP echo reply here
					sendICMPReply(etherPacket, inIface);
				} else {
					return; // Drop the packet if ICMP checksum is wrong
				}
			}
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_UDP) {
				// Check 520
				UDP udpPacket = (UDP)ipPacket.getPayload();
				if (udpPacket.getDestinationPort() == UDP.RIP_PORT) {
					if (ipPacket.getTtl() <= 1) {
						// Type 11 code 0
						sendICMPError(etherPacket, inIface, (byte) 11, (byte) 0);
						return;
					}
					rip.handlePacket(etherPacket, inIface);
				} else {
					sendICMPError(etherPacket, inIface, (byte) 3, (byte) 3);
//					if (ipPacket.getTtl() <= 1) {
//						// Type 11 code 0
//						sendICMPError(etherPacket, inIface, (byte) 11, (byte) 0);
//						return;
//					}
				}
			}
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_TCP) {
				sendICMPError(etherPacket, inIface, (byte) 3, (byte) 3);
//				if (ipPacket.getTtl() <= 1) {
//					// Type 11 code 0
//					sendICMPError(etherPacket, inIface, (byte) 11, (byte) 0);
//					return;
//				}
			} else
				return;
		} else { // Not destined for one of the interfaces
			if (ipPacket.getTtl() <= 1) {
				// Type 11 code 0
				sendICMPError(etherPacket, inIface, (byte) 11, (byte) 0);
				return;
			}
			ipPacket.setTtl((byte)((int)ipPacket.getTtl() - 1));
			ipPacket.setChecksum((short) 0);
			etherPacket.setPayload(ipPacket);

			RouteTableEntry routeEntry = findLongestPrefixMatch(destinationIP);
			if (routeEntry == null) {
				sendICMPError(etherPacket, inIface, (byte) 3, (byte) 0); // Unreachable net
				return;
			}

			// Forward message procedures
			if (routeEntry.getGatewayAddress() == 0) {
//				etherPacket.setSourceMACAddress(interfaces.get(routeEntry.getInterface()).getMacAddress().toBytes());
				if (arpCache.lookup(ipPacket.getDestinationAddress()) == null) {
					arpCache.waitForArp(etherPacket, interfaces.get(routeEntry.getInterface()), ipPacket.getDestinationAddress());
				} else {
					etherPacket.setSourceMACAddress(interfaces.get(routeEntry.getInterface()).getMacAddress().toBytes());
					etherPacket.setDestinationMACAddress(arpCache.lookup(ipPacket.getDestinationAddress()).getMac().toBytes());
					sendPacket(etherPacket, interfaces.get(routeEntry.getInterface()));
				}
			} else {
				ArpEntry arpEntry = arpCache.lookup(routeEntry.getGatewayAddress());
//				etherPacket.setSourceMACAddress(interfaces.get(routeEntry.getInterface()).getMacAddress().toBytes());
				if (arpEntry == null) {
					arpCache.waitForArp(etherPacket, interfaces.get(routeEntry.getInterface()), routeEntry.getGatewayAddress());
				} else {
					etherPacket.setSourceMACAddress(interfaces.get(routeEntry.getInterface()).getMacAddress().toBytes());
					etherPacket.setDestinationMACAddress(arpEntry.getMac().toBytes());
					sendPacket(etherPacket, interfaces.get(routeEntry.getInterface()));
				}
			}
		}
	}

	// TODO
	private RouteTableEntry findLongestPrefixMatch(int destIp) {
		RouteTableEntry bestfit = null;
		for (RouteTableEntry rtEntry : this.getRouteTable().getEntries()) {
			int myAddress = destIp & rtEntry.getMaskAddress();
			if (myAddress == (rtEntry.getDestinationAddress() & rtEntry.getMaskAddress())) {
				if (bestfit == null)
					bestfit = rtEntry;
				else if (bestfit.getMaskAddress() < rtEntry.getMaskAddress())
					bestfit = rtEntry;
			}
		}
		return bestfit;
	}

	// Done
	private void sendICMPReply(Ethernet etherPacket, Iface inIface) {
		Ethernet eth = etherPacket;
		IPv4 ip = (IPv4) etherPacket.getPayload();
		ICMP icmp = (ICMP) ip.getPayload();
		icmp.setIcmpCode((byte) 0);
		icmp.setIcmpType((byte) 0);
		icmp.setChecksum((short) 0);

		ip.setPayload(icmp);
		ip.setProtocol(IPv4.PROTOCOL_ICMP);
		ip.setChecksum((short) 0);
		ip.setTtl((byte) 64);
		int destinationAddress = ip.getSourceAddress();
		int sourceAddress = ip.getDestinationAddress();
		ip.setDestinationAddress(destinationAddress);
		ip.setSourceAddress(sourceAddress);

		eth.setPayload(ip);
		byte[] destinationMACAddress = eth.getSourceMACAddress();
		byte[] sourceMACAddress = eth.getDestinationMACAddress();
		eth.setDestinationMACAddress(destinationMACAddress);
		eth.setSourceMACAddress(sourceMACAddress);
		sendPacket(eth, inIface);
	}

	// Done 
	public void sendICMPError(Ethernet etherPacket, Iface inIface, byte type, byte code) {
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();		
		ICMP icmpPacket = new ICMP();
		int ipHeaderLengthInBytes = ipPacket.getHeaderLength() * 4;
		byte[] originData = ipPacket.getPayload().serialize();
		byte[] ipheader = Arrays.copyOfRange(ipPacket.serialize(), 0, ipHeaderLengthInBytes);
		byte[] unused = {(byte)0, (byte)0, (byte)0, (byte)0};
		byte[] data = new byte[4 + ipHeaderLengthInBytes + 8];

		System.arraycopy(unused, 0, data, 0, 4);
		System.arraycopy(ipheader, 0, data, 4, ipheader.length);
		System.arraycopy(originData, 0, data, 4 + ipheader.length, 8);

		icmpPacket.setPayload(new Data(data));
		icmpPacket.setIcmpCode((byte) code);
		icmpPacket.setIcmpType((byte) type);
		icmpPacket.setChecksum((short) 0);

		ipPacket.setPayload(icmpPacket);
		ipPacket.setChecksum((short) 0);
		ipPacket.setTtl((byte) 64);
		ipPacket.setProtocol(IPv4.PROTOCOL_ICMP);

		int destinationAddress = ipPacket.getSourceAddress();
		int sourceAddress = inIface.getIpAddress();
		ipPacket.setDestinationAddress(destinationAddress);
		ipPacket.setSourceAddress(sourceAddress);

		etherPacket.setPayload(ipPacket);

		byte[] destinationMACAddress = etherPacket.getSourceMACAddress();
		byte[] sourceMACAddress = etherPacket.getDestinationMACAddress();
		etherPacket.setDestinationMACAddress(destinationMACAddress);
		etherPacket.setSourceMACAddress(sourceMACAddress);
		
//		System.out.println("Error: " + type + " " + code);
		if (type == (byte) 3 && code == (byte) 1) {
			for (Iface i : interfaces.values()) {
				if (i.getMacAddress().equals(new MACAddress(sourceMACAddress))) {
					ipPacket.setSourceAddress(i.getIpAddress());
					etherPacket.setPayload(ipPacket);
//					System.out.println(etherPacket.toString());
					sendPacket(etherPacket, i);
				}
			}
		} else {
//			System.out.println(etherPacket.toString());
			sendPacket(etherPacket, inIface);
		}
	}

	// Done and tested
	private boolean checkIPChecksum(IPv4 packet) {
		int accumulation = 0;
		ByteBuffer byteBuffer = ByteBuffer.wrap(packet.serialize());
		byteBuffer.putShort(10, (short) 0); // Set the checksum in the buffer to 0 
		for (int i = 0; i < packet.getHeaderLength() * 2; ++i) {
			accumulation += 0xffff & byteBuffer.getShort();
		}
		accumulation = ((accumulation >> 16) & 0xffff)
		+ (accumulation & 0xffff);
		short checksum = (short) (~accumulation & 0xffff);
		if (checksum == packet.getChecksum())
			return true;
		else
			return false;
	}

	// Done and tested
	private boolean checkICMPChecksum(ICMP packet) {
		ByteBuffer bb = ByteBuffer.wrap(packet.serialize());
		bb.putShort(2, (short) 0);

		int accumulation = 0;
		int length = packet.serialize().length;
		for (int i = 0; i < length / 2; ++i) {
			accumulation += 0xffff & bb.getShort();
		}
		// Pad to an even number of shorts
		if (length % 2 > 0) {
			accumulation += (bb.get() & 0xff) << 8;
		}

		accumulation = ((accumulation >> 16) & 0xffff)
		+ (accumulation & 0xffff);
		short tmpChecksum = (short) (~accumulation & 0xffff);
		if (tmpChecksum == packet.getChecksum()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Handle an ARP packet received on a specific interface.
	 * @param etherPacket the complete ARP packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	private void handleArpPacket(Ethernet etherPacket, Iface inIface)
	{
		// Make sure it's an ARP packet
		if (etherPacket.getEtherType() != Ethernet.TYPE_ARP)
		{ return; }

		// Get ARP header
		ARP arpPacket = (ARP)etherPacket.getPayload();
		int targetIp = ByteBuffer.wrap(
				arpPacket.getTargetProtocolAddress()).getInt();

		switch(arpPacket.getOpCode())
		{
		case ARP.OP_REQUEST:
			// Check if request is for one of my interfaces
			if (targetIp == inIface.getIpAddress())
			{ this.arpCache.sendArpReply(etherPacket, inIface); }
			break;
		case ARP.OP_REPLY:
			// Check if reply is for one of my interfaces
			if (targetIp != inIface.getIpAddress())
			{ break; }

			// Update ARP cache with contents of ARP reply
			int senderIp = ByteBuffer.wrap(arpPacket.getSenderProtocolAddress()).getInt();
			ArpRequest request = this.arpCache.insert(
					new MACAddress(arpPacket.getSenderHardwareAddress()),
					senderIp);
			// Process pending ARP request entry, if there is one
			if (request != null)
			{				
				for (Ethernet packet : request.getWaitingPackets())
				{
					/*********************************************************/
					/* TODO: send packet waiting on this request             */
					packet.setSourceMACAddress(inIface.getMacAddress().toBytes());
					packet.setDestinationMACAddress(arpPacket.getSenderHardwareAddress());
					sendPacket(packet, inIface);
					/*********************************************************/
				}
			}
			break;
		}
	}
}
