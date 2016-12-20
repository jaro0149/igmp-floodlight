package net.floodlightcontroller.multicastmachine;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;

public class MulticastMachine implements IOFMessageListener, IFloodlightModule, IMulticastService {
	
	private static final int MAX_TRANSMITTER_AGE = 300;
	private static final short FLOW_IDLE_TIMEOUT = 180;
	private static final int QUERY_INTERVAL = 125;
	private static final int MAX_RESPONSE_TIME = 10;
	private static final int QUERY_FREQUENCY = 1;	
	
	private static final int APP_ID = 10;
	private static final int APP_ID_BITS = 12;
	private static final int APP_ID_SHIFT = (64 - APP_ID_BITS);
	private static final long COOKIE = (long) (APP_ID & ((1 << APP_ID_BITS) - 1)) << APP_ID_SHIFT;
	
	private static final short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 0;
	private static final short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0;
	private static final short DEF_IGMP_PRIOTITY = 10;
	private static final short DEF_MULTICAST_PRIORITY = 10;
	
	private final IPv4AddressWithMask MULTICAST_NET = IPv4AddressWithMask.of("224.0.0.0/4");
	private final IPv4Address IGMP_LEAVE_DST = IPv4Address.of("224.0.0.2");
	private final MacAddress ROUTER_MAC = MacAddress.of("00:50:56:AA:AA:AA");
	private final IPv4Address ROUTER_IP = IPv4Address.of("10.0.0.254");
	private final StampedLock confSwitchesLock = new StampedLock();
	
	private IRestApiService restApiService;
	@SuppressWarnings("unused")
	private IMulticastService multicastService;
	private static Logger logger;
	private IFloodlightProviderService floodlightProvider;	
	private HostManager transmitterManager;
	private HostManager listenerManager;
	private FlowManager flowManager;
	private HashSet<DatapathId> configuredSwitches;
	
	@Override
	public String getName() {
		return MulticastMachine.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return (type == OFType.PACKET_IN && 
				(name.equals("forwarding") || name.equals("learningswitch")));
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IMulticastService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IMulticastService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		try {
			floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
			restApiService = context.getServiceImpl(IRestApiService.class);
			multicastService = context.getServiceImpl(IMulticastService.class);
			
		    logger = LoggerFactory.getLogger(MulticastMachine.class);
		    flowManager = new FlowManager(FLOW_IDLE_TIMEOUT);
		    transmitterManager = new TransmitterManager(ROUTER_MAC, ROUTER_IP, 
		    		MAX_TRANSMITTER_AGE, logger, flowManager);
		    listenerManager = new ListenerManager(ROUTER_MAC, ROUTER_IP, QUERY_INTERVAL, MAX_RESPONSE_TIME, 
		    		QUERY_FREQUENCY, logger, flowManager);
		    transmitterManager.registerOtherManager(listenerManager);
		    listenerManager.registerOtherManager(transmitterManager);
		    configuredSwitches = new HashSet<>();
		    transmitterManager.startMachine();
		    listenerManager.startMachine();
		} catch(ExceptionBuffer | IllegalArgumentException exc) {
			logger.error(exc.getMessage());
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiService.addRestletRoutable(new MulticastMachineWebRoutable());
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		try {
			scanSwitch(sw);
			return processPacket(sw, msg, cntx);
		} catch (UnknownHostException | IllegalArgumentException e) {
			logger.error(e.getMessage());
			return Command.CONTINUE;
		}		
	}
	
	private net.floodlightcontroller.core.IListener.Command processPacket(IOFSwitch sw, OFMessage msg, 
			FloodlightContext cntx) throws UnknownHostException, IllegalArgumentException {
		if(msg.getType()==OFType.PACKET_IN) {
			OFPacketIn msgCasted = (OFPacketIn) msg;
			Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, 
					IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			MacAddress srcMac = eth.getSourceMACAddress();
			MacAddress dstMac = eth.getDestinationMACAddress();
			EthType type = eth.getEtherType();
			if(type==EthType.IPv4 && dstMac.isMulticast()) {
				IPv4 ipv4 = (IPv4) eth.getPayload();
				IPv4Address srcIp = ipv4.getSourceAddress();
				IPv4Address dstIp = ipv4.getDestinationAddress();
				IpProtocol protocol = ipv4.getProtocol();
				if(protocol==IpProtocol.IGMP && dstIp.isMulticast()) {
					Data data = (Data) ipv4.getPayload();
					processIgmpMessage(sw, msgCasted, data, srcMac, srcIp, dstIp);
					return Command.STOP;
				} else if(dstIp.isMulticast()) {
					boolean added = transmitterManager.addOrRefreshHost(sw, msgCasted.getMatch().get(MatchField.IN_PORT),
							srcMac, srcIp, dstIp);
					return (added==true) ? Command.CONTINUE : Command.STOP;
				}
			}
		}
		return Command.CONTINUE;
	}
	
	private void processIgmpMessage(IOFSwitch sw, OFPacketIn msg, Data data, MacAddress srcMac, 
			IPv4Address srcIp, IPv4Address dstIp) throws UnknownHostException, IllegalArgumentException {		
		 Igmpv2Parser igmpParser = new Igmpv2Parser(new FlexBytes(data.getData()));
		 if(igmpParser.isValid() && igmpParser.testChecksum()) {
			 if(igmpParser.getType()==Igmpv2Type.REPORT) {			 
				 IPv4Address group = IPv4Address.of(igmpParser.getGroupAddress());
				 listenerManager.addOrRefreshHost(sw, msg.getMatch().get(MatchField.IN_PORT),
						 srcMac, srcIp, group);
			 } else if(igmpParser.getType()==Igmpv2Type.LEAVE 
					 && dstIp.equals(IGMP_LEAVE_DST)) {
				 IPv4Address group = IPv4Address.of(igmpParser.getGroupAddress());
				 listenerManager.removeHost(sw, msg.getMatch().get(MatchField.IN_PORT),
						 srcMac, group);
			 }
		 }
	}
	
	private void scanSwitch(IOFSwitch sw) {
		long stamp = confSwitchesLock.writeLock();
		try {
			DatapathId id = sw.getId();
			if(!configuredSwitches.contains(id)) {
				buildDefaultEntryIgmp(sw);
				buildDefaultEntryMulticast(sw);
				configuredSwitches.add(id);
			}
		} finally {
			confSwitchesLock.unlock(stamp);
		}
	}
	
	private void buildDefaultEntryIgmp(IOFSwitch sw) {
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb	//.setExact(MatchField.IN_PORT,OFPort.ALL)
			.setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setExact(MatchField.IP_PROTO,IpProtocol.IGMP);
		List<OFAction> al = new ArrayList<OFAction>();
		al	.add(sw.getOFFactory()
			.actions()
			.buildOutput()
			.setPort(OFPort.CONTROLLER)
			.setMaxLen(0xffFFffFF)
			.build());		
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		fmb	.setMatch(mb.build())
			.setCookie((U64.of(COOKIE)))
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setPriority(DEF_IGMP_PRIOTITY)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setOutPort(OFPort.CONTROLLER)
			.setFlags(sfmf)
			.setActions(al);
		sw.write(fmb.build());	
	}
	
	private void buildDefaultEntryMulticast(IOFSwitch sw) {
		Set<OFFlowModFlags> sfmf = new HashSet<OFFlowModFlags>();
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb	//.setExact(MatchField.IN_PORT,OFPort.ALL)
			.setExact(MatchField.ETH_TYPE,EthType.IPv4)
			.setMasked(MatchField.IPV4_DST,MULTICAST_NET);
		List<OFAction> al = new ArrayList<OFAction>();
		al	.add(sw.getOFFactory()
			.actions()
			.buildOutput()
			.setPort(OFPort.CONTROLLER)
			.setMaxLen(0xffFFffFF)
			.build());		
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		fmb	.setMatch(mb.build())
			.setCookie((U64.of(COOKIE)))
			.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
			.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
			.setPriority(DEF_MULTICAST_PRIORITY)
			.setBufferId(OFBufferId.NO_BUFFER)
			.setOutPort(OFPort.CONTROLLER)
			.setFlags(sfmf)
			.setActions(al);
		sw.write(fmb.build());
	}

	@Override
	public Collection<HostEntry> listTransmitters() {
		return transmitterManager.getList();
	}

	@Override
	public Collection<HostEntry> listListeners() {
		return listenerManager.getList();
	}
	
}
