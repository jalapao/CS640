package edu.wisc.cs.sdn.sr;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.wisc.cs.sdn.sr.vns.VNSComm;

import net.floodlightcontroller.packet.ARP;
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
		System.out.println("handleIPv4Packet");
		IPv4 ipPacket = (IPv4) etherPacket.getPayload();
		int destinationIP = ipPacket.getDestinationAddress();
		if (destinationIP == inIface.getIpAddress()) {
			System.out.println("Destinated.");
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
				System.out.println("Ping a!");
				if(checkChecksum(ipPacket)){
					System.out.println("Ping after checksum!");
				}
				else
					return;
			}
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_UDP) {
				//check 520
				UDP udpPacket = (UDP) ipPacket.getPayload();
				if (udpPacket.getDestinationPort() == 520) {
					//call control plane
				}
				else
					sendICMPUnreachable();
			}
			if (ipPacket.getProtocol() == IPv4.PROTOCOL_TCP) {
				sendICMPUnreachable();
			}
			else
				return;
		}
		else{
			if (!checkChecksum(ipPacket)) {
				sendICMPError();
				return;
			}
			ipPacket.setTtl((byte)((int)ipPacket.getTtl() - 1)); //problem potential
			RouteTableEntry routeEntry = findLongestPrefixMatch(destinationIP);
			if (routeEntry == null){
				sendICMPError();
				return;
			}
			if (routeEntry.getGatewayAddress() == 0){
				sendPacket(etherPacket, interfaces.get(routeEntry.getInterface()));
			}else{
				ArpEntry arpEntry = arpCache.lookup(routeEntry.getDestinationAddress());
				if (arpEntry == null){
					arpCache.waitForArp(etherPacket, interfaces.get(routeEntry.getInterface()), routeEntry.getGatewayAddress());
				}else{
					etherPacket.setDestinationMACAddress(arpEntry.getMac().toString());
					sendPacket(etherPacket, interfaces.get(routeEntry.getInterface()));
				}
			}

		}

	}

	private RouteTableEntry findLongestPrefixMatch(int destIp){

		RouteTableEntry bestfit = null;
		for (RouteTableEntry rtEntry : this.getRouteTable().getEntries()){
			int myAddress =  (byte)destIp & (byte)rtEntry.getMaskAddress();
			if (myAddress == rtEntry.getDestinationAddress()){
				if (bestfit == null)
					bestfit = rtEntry;
				else if( bestfit.getMaskAddress() < rtEntry.getMaskAddress() )
					bestfit = rtEntry;
			}
		}
		return bestfit;
	}

	private boolean sendICMPError(){
		return false;
	}

	private boolean sendICMPUnreachable(){
		return false;
	}

	private boolean checkChecksum(IPv4 packet){
		int accumulation = 0;
		ByteBuffer byteBuffer = ByteBuffer.wrap(packet.serialize(), 0, packet.getHeaderLength());
		//byteBuffer.rewind();
		for (int i = 0; i < packet.getHeaderLength() * 2; ++i) {
			//byteBuffer.putShort(10, (short) 0);
			accumulation += 0xffff & byteBuffer.getShort();
		}
		accumulation = ((accumulation >> 16) & 0xffff)
		+ (accumulation & 0xffff);
		short checksum = (short) (~accumulation & 0xffff);
		System.out.println(checksum);
		System.out.println(packet.getChecksum());
		if (checksum == packet.getChecksum())
			return true;
		else
			return false;
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
			ArpRequest request = this.arpCache.insert(
					new MACAddress(arpPacket.getTargetHardwareAddress()),
					targetIp);

			// Process pending ARP request entry, if there is one
			if (request != null)
			{				
				for (Ethernet packet : request.getWaitingPackets())
				{
					/*********************************************************/
					/* TODO: send packet waiting on this request             */
					packet.setDestinationMACAddress(arpPacket.getTargetHardwareAddress());
					sendPacket(packet, inIface);
					/*********************************************************/
				}
			}
			break;
		}
	}
}
