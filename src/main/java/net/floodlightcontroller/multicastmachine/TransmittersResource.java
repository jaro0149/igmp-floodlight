package net.floodlightcontroller.multicastmachine;

import java.util.Collection;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransmittersResource extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(TransmittersResource.class);
	
	@Get("json")
	public Collection <HostEntry> retrieve() {
		IMulticastService service =
                (IMulticastService)getContext().getAttributes().
                    get(IMulticastService.class.getCanonicalName());
		return service.listTransmitters();
	}
	
}
