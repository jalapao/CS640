package edu.wisc.cs.sdn.sr;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.util.MACAddress;
import edu.wisc.cs.sdn.sr.vns.VNSComm;

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

		/********************************************************************/
		/* TODO: Handle packets                                             */
		short etherType = etherPacket.getEtherType();
		switch(etherType){
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

	private void handleIPv4Packet(Ethernet etherPacket, Iface inIface){
		if (etherPacket.getEtherType() != Ethernet.TYPE_IPv4)
		{ return; }
//		System.out.println("handleIPv4Packet");
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();
		int destinationIP = ipPacket.getDestinationAddress();
		if (destinationIP == inIface.getIpAddress()) { //?
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
//				System.out.println("Ping a!");
				if (checkIPChecksum(ipPacket)){
					ICMP icmpPacket = (ICMP)ipPacket.getPayload();
					if (checkICMPChecksum(icmpPacket) && icmpPacket.getIcmpType() == (byte) 8) {
						// send icmp echo reply here
						sendICMPReply(etherPacket, inIface);
					} else {
						return; // drop the packet
					}
//					System.out.println("Ping after checksum!");
				} else
					return; // drop the packet
			}
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_UDP) {
				//check 520
				UDP udpPacket = (UDP)ipPacket.getPayload();
				//System.out.println("The UDP port is " + (short) udpPacket.getDestinationPort());
				if (udpPacket.getDestinationPort() == 520) {
					//call control plane
				} else
					sendICMPError(etherPacket, inIface, (byte) 3, (byte) 3);
			}
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_TCP) {
				sendICMPError(etherPacket, inIface, (byte) 3, (byte) 3);
			} else
				return;
		} else { // not destined for one of the interfaces
			if (!checkIPChecksum(ipPacket)) {
				// simply drop this ip packet
				return;
			}
			if (ipPacket.getTtl() == 0) {
				// type 11 code 0
				sendICMPError(etherPacket, inIface, (byte) 11, (byte) 0);
				 return;
			}
			ipPacket.setTtl((byte)((int)ipPacket.getTtl() - 1));
			RouteTableEntry routeEntry = findLongestPrefixMatch(destinationIP);
			System.out.println(routeEntry);
			if (routeEntry == null){
				sendICMPError(etherPacket, inIface, (byte) 3, (byte) 0); // unreachable net
				return;
			}
			System.out.println("Gateway is " + routeEntry.getGatewayAddress());
			if (routeEntry.getGatewayAddress() == 0) {
				System.out.println("I set source address to etherPachet : " + interfaces.get(routeEntry.getInterface()).getMacAddress().toBytes());
				etherPacket.setSourceMACAddress(interfaces.get(routeEntry.getInterface()).getMacAddress().toBytes());
				
				if (arpCache.lookup(ipPacket.getDestinationAddress()) == null) {
					System.out.println("I don't have the mac address for " + ipPacket.getDestinationAddress());
					System.out.println(interfaces.get(routeEntry.getInterface()));
					arpCache.waitForArp(etherPacket, interfaces.get(routeEntry.getInterface()), ipPacket.getDestinationAddress());
				} else {
					System.out.println("Have mac address for " + ipPacket.getDestinationAddress());
					System.out.println("Mac address = " + arpCache.lookup(ipPacket.getDestinationAddress()).getMac().toBytes());
					etherPacket.setDestinationMACAddress(arpCache.lookup(ipPacket.getDestinationAddress()).getMac().toBytes());
					System.out.println("Send ether packet!");
					sendPacket(etherPacket, interfaces.get(routeEntry.getInterface()));
				}
			} else {
				ArpEntry arpEntry = arpCache.lookup(routeEntry.getDestinationAddress());
				if (arpEntry == null){
					System.out.println("I don't have the mac address for " + ipPacket.getDestinationAddress());
					arpCache.waitForArp(etherPacket, interfaces.get(routeEntry.getInterface()), routeEntry.getGatewayAddress());
				} else {
					System.out.println("Have mac address for " + ipPacket.getDestinationAddress());
					System.out.println("Mac address = " + arpCache.lookup(ipPacket.getDestinationAddress()).getMac().toBytes());
					etherPacket.setDestinationMACAddress(arpEntry.getMac().toString());
					System.out.println("Send ether packet!");
					sendPacket(etherPacket, interfaces.get(routeEntry.getInterface()));
				}
			}
		}
	}

	// TODO
	private RouteTableEntry findLongestPrefixMatch(int destIp){
		RouteTableEntry bestfit = null;
		for (RouteTableEntry rtEntry : this.getRouteTable().getEntries()){
			int myAddress = destIp & rtEntry.getMaskAddress();
			System.out.println("My address is " + myAddress);
			System.out.println(rtEntry.getDestinationAddress());
			if (myAddress == rtEntry.getDestinationAddress()){
				if (bestfit == null)
					bestfit = rtEntry;
				else if(bestfit.getMaskAddress() < rtEntry.getMaskAddress() )
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
		ip.setChecksum((short) 0);
		int destinationAddress = ip.getSourceAddress();
		int sourceAddress = ip.getDestinationAddress();
		ip.setDestinationAddress(destinationAddress);
		ip.setSourceAddress(sourceAddress);
		
		// TODO: if we should decrease TTL here?
		eth.setPayload(ip);
		byte[] destinationMACAddress = eth.getSourceMACAddress();
		byte[] sourceMACAddress = eth.getDestinationMACAddress();
		eth.setDestinationMACAddress(destinationMACAddress);
		eth.setSourceMACAddress(sourceMACAddress);
		sendPacket(eth, inIface);
	}
	
	// TODO
	public void sendICMPError(Ethernet etherPacket, Iface inIface, byte type, byte code){
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();
		
		ICMP icmpPacket = new ICMP();
//		ICMP icmpPacket = (ICMP) ipPacket.getPayload();
		byte[] originData = ipPacket.getPayload().getPayload().serialize();
		byte[] data = new byte[ipPacket.getHeaderLength() + 8];
		byte[] dataByte = Arrays.copyOfRange(ipPacket.serialize(), 0, ipPacket.getHeaderLength());
		System.arraycopy(dataByte, 0, data, 0, dataByte.length);
		System.arraycopy(originData, 0, data, dataByte.length, 8);
		icmpPacket.setPayload(new Data(data));
		
		icmpPacket.setIcmpCode((byte) code);
		icmpPacket.setIcmpType((byte) type);
		icmpPacket.setChecksum((short) 0);
		
		ipPacket.setPayload(icmpPacket);
		ipPacket.setChecksum((short) 0);
		int destinationAddress = ipPacket.getDestinationAddress();
		int sourceAddress = ipPacket.getDestinationAddress();
		ipPacket.setDestinationAddress(destinationAddress);
		ipPacket.setSourceAddress(sourceAddress);
		
		etherPacket.setPayload(ipPacket);
		byte[] destinationMACAddress = etherPacket.getSourceMACAddress();
		byte[] sourceMACAddress = etherPacket.getDestinationMACAddress();
		etherPacket.setDestinationMACAddress(destinationMACAddress);
		etherPacket.setSourceMACAddress(sourceMACAddress);
//		System.out.println(sendPacket(etherPacket, inIface));
//		System.out.println("Unreachable message sent");
	}

	// done and tested
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
//		System.out.println(checksum);
//		System.out.println(packet.getChecksum());
		if (checksum == packet.getChecksum())
			return true;
		else
			return false;
	}

	// done and tested
	private boolean checkICMPChecksum(ICMP packet) {
		ByteBuffer bb = ByteBuffer.wrap(packet.serialize());
		bb.putShort(2, (short) 0);
		
		int accumulation = 0;
		int length = packet.serialize().length;
        for (int i = 0; i < length / 2; ++i) {
            accumulation += 0xffff & bb.getShort();
        }
        // pad to an even number of shorts
        if (length % 2 > 0) {
            accumulation += (bb.get() & 0xff) << 8;
        }

        accumulation = ((accumulation >> 16) & 0xffff)
                + (accumulation & 0xffff);
        short tmpChecksum = (short) (~accumulation & 0xffff);
//        System.out.println("The tmpChecksum is " + tmpChecksum);
//        System.out.println("Should be " + packet.getChecksum());
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
//			System.out.println("mac address of targert ip = " + arpCache.lookup(targetIp)); //arp
			System.out.println("targetIp = " + targetIp);
			System.out.println("sourceIp = " + Integer.valueOf(arpPacket.getSenderProtocolAddress().toString()));
			System.out.println("target hardware address = " + new MACAddress(arpPacket.getTargetHardwareAddress()));
			System.out.println("source hardware address = " + new MACAddress(arpPacket.getSenderHardwareAddress()));
//			ArpRequest request = this.arpCache.insert(
//					new MACAddress(arpPacket.getTargetHardwareAddress()),
//					targetIp);
			ArpRequest request = this.arpCache.insert(
					new MACAddress(arpPacket.getSenderHardwareAddress()),
					Integer.valueOf(arpPacket.getSenderProtocolAddress().toString()));
			// Process pending ARP request entry, if there is one
			
			if (request != null)
			{				
				System.out.println("I'm here...");
				for (Ethernet packet : request.getWaitingPackets())
				{
					/*********************************************************/
					/* TODO: send packet waiting on this request             */
					System.out.println("The target MAC address is " + arpPacket.getTargetHardwareAddress());
					packet.setSourceMACAddress(inIface.getMacAddress().toBytes());
					packet.setDestinationMACAddress(arpPacket.getTargetHardwareAddress());
					System.out.println("Process pending Arp Request:" + Boolean.toString(sendPacket(packet, inIface)));
					/*********************************************************/
				}
			}
			break;
		}
	}
}
