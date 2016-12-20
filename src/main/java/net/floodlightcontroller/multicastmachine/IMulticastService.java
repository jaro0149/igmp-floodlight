package net.floodlightcontroller.multicastmachine;

import java.util.Collection;
import net.floodlightcontroller.core.module.IFloodlightService;

public interface IMulticastService extends IFloodlightService  {

	public Collection<HostEntry> listTransmitters();
	public Collection<HostEntry> listListeners();
	
}
