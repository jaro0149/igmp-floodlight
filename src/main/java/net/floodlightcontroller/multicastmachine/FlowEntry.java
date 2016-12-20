package net.floodlightcontroller.multicastmachine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.core.IOFSwitch;

public class FlowEntry {

	private static final int APP_ID = 10;
	private static final int APP_ID_BITS = 12;
	private static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
	private static final long COOKIE = (long) (APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
	private static final short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0;
	private static final short DEF_PRIORITY = 20;
	
	private IOFSwitch sw;
	private IPv4Address group;
	private OFPort srcPort;	
	private short idleTimeout;
	private final List<OFPort> dstPorts = new ArrayList<>();
	
	public FlowEntry(IOFSwitch sw, IPv4Address group, OFPort srcPort, OFPort dstPort, short idleTimeout) {
		this.sw = sw;
		this.group = group;
		this.srcPort = srcPort;
		this.idleTimeout = idleTimeout;
		dstPorts.add(dstPort);
	}
	
	public boolean compareSwitchAndGroup(IOFSwitch sw, IPv4Address group) {
		if(this.sw.getId().equals(sw.getId()) && this.group.equals(group)) {
			return true;
		}
		return false;
	}
	
	public boolean findDstPort(OFPort port) {
		if(dstPorts.contains(port)) {
			return true;
		}
		return false;
	}
	
	public boolean addDstPort(OFPort port) {
		if(!dstPorts.contains(port)) {
			dstPorts.add(port);
			return true;
		}
		return false;
	}
	
	public boolean isAlive() {
		return dstPorts.size()!=0 ? true : false;
	}
	
	public boolean removeDstPort(OFPort port) {
		return dstPorts.remove(port);
	}
	
	public void removeActualFlowEntry() {
		try {
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowDelete();
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb	.setExact(MatchField.IN_PORT,srcPort)
			.setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setExact(MatchField.IPV4_DST,group);
		fmb	.setMatch(mb.build())
			.setCookie((U64.of(COOKIE)))
			.setIdleTimeout(idleTimeout)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setPriority(DEF_PRIORITY)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setFlags(sfmf);
		sw.write(fmb.build());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void insertActualFlowEntry() {
		try {
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb	.setExact(MatchField.IN_PORT,srcPort)
			.setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setExact(MatchField.IPV4_DST,group);
		List<OFAction> al = new ArrayList<OFAction>();
		
		dstPorts.forEach(port -> {
			al.add(sw.getOFFactory()
				.actions()
				.buildOutput()
				.setPort(port)
				.setMaxLen(0xffFFffFF)
				.build());
		});
		fmb	.setMatch(mb.build())
			.setCookie((U64.of(COOKIE)))
			.setIdleTimeout(idleTimeout)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setPriority(DEF_PRIORITY)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setFlags(sfmf)
			.setActions(al);
		sw.write(fmb.build());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void modifyToActualFlowEntry() {
		try {
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowModify();
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb	.setExact(MatchField.IN_PORT,srcPort)
			.setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setExact(MatchField.IPV4_DST,group);
		List<OFAction> al = new ArrayList<OFAction>();
		
		dstPorts.forEach(port -> {
			al.add(sw.getOFFactory()
				.actions()
				.buildOutput()
				.setPort(port)
				.setMaxLen(0xffFFffFF)
				.build());
		});
		fmb	.setMatch(mb.build())
			.setCookie((U64.of(COOKIE)))
			.setIdleTimeout(idleTimeout)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setPriority(DEF_PRIORITY)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setFlags(sfmf)
			.setActions(al);
		sw.write(fmb.build());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((srcPort == null) ? 0 : srcPort.hashCode());
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
		FlowEntry other = (FlowEntry) obj;
		if(this.hashCode()==other.hashCode()) {
			return true;
		}
		return false;
	}
	
}
