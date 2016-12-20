package net.floodlightcontroller.multicastmachine;

import java.util.Collections;

import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

public class QueryEntry {

	private static final byte QUERY_TTL = 1;	
	private IPv4Address group;
	private MacAddress macGroup;
	private IOFSwitch sw;
	private OFPort port;
	
	public QueryEntry(IPv4Address group, IOFSwitch sw, OFPort port) {
		this.group = group;
		this.sw = sw;
		this.port = port;
		this.macGroup = convertMulticastIpToMulticastMac(group);
	}
	
	public void generateIgmpQuery(int maxResponseTime, MacAddress routerMac, IPv4Address routerIp)
			throws ExceptionBuffer {
		
		Igmpv2Builder igmpBuilder = new Igmpv2Builder(
				Igmpv2Type.QUERY, 
				maxResponseTime, 
				group.toInetAddress());
		Data igmpStructure = new Data();
		igmpStructure.setData(igmpBuilder.build().createByteArrayImage());
		
		IPv4 l3 = new IPv4();
		l3.setSourceAddress(routerIp);
		l3.setDestinationAddress(group);
		l3.setTtl(QUERY_TTL);
		l3.setProtocol(IpProtocol.IGMP);
		l3.setPayload(igmpStructure);
		
		Ethernet l2 = new Ethernet();
		l2.setSourceMACAddress(routerMac);
		l2.setDestinationMACAddress(macGroup);
		l2.setEtherType(EthType.IPv4);
		l2.setPayload(l3);
		
		byte[] serializedData = l2.serialize();
		OFPacketOut po = sw.getOFFactory().buildPacketOut()
			    .setData(serializedData)
			    .setActions(Collections.singletonList((OFAction)sw.getOFFactory()
			    		.actions()
			    		.output(port,0xffFFffFF)))
			    .setInPort(OFPort.CONTROLLER)
			    .build();
		sw.write(po);		
	}	
	
	public boolean compareEntry(IPv4Address group, IOFSwitch sw, OFPort port) {
		return this.group.equals(group) &&
			   this.sw.getId().equals(sw.getId()) &&
			   this.port.equals(port);
	}
	
	public OFPort getPortForGroupAddress(IOFSwitch sw, IPv4Address group) {
		if(this.group.equals(group) && this.sw.getId().equals(sw.getId())) {
			return port;
		} else {
			return null;
		}
	}
	
	public IPv4Address getGroupAddress() {
		return group;
	}
	
	public IOFSwitch getSwitch() {
		return sw;
	}
	
	public OFPort getSourcePort() {
		return port;
	}
	
	public String printGroupAddress() {
		return group.toString();
	}
	
	public String printSwitchId() {
		return sw.getId().toString();
	}
	
	public int printPortId() {
		return port.getPortNumber();
	}
	
	private MacAddress convertMulticastIpToMulticastMac(IPv4Address address) {
		byte[] finalAddress = new byte[6];
		byte[] ipBytes = address.getBytes();
		finalAddress[0] = 1;
		finalAddress[1] = 0;
		finalAddress[2] = 94;
		finalAddress[3] = (byte)(ipBytes[1]&127);
		finalAddress[4] = ipBytes[2];
		finalAddress[5] = ipBytes[3];
		return MacAddress.of(finalAddress);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((port == null) ? 0 : port.hashCode());
		result = prime * result + ((sw == null) ? 0 : sw.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryEntry other = (QueryEntry) obj;
		if(this.hashCode()==other.hashCode()) {
			return true;
		}
		return false;
	}
	
}
