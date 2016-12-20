package net.floodlightcontroller.multicastmachine;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import net.floodlightcontroller.restserver.RestletRoutable;

public class MulticastMachineWebRoutable implements RestletRoutable {

	@Override
	public Restlet getRestlet(Context context) {
		Router router = new Router(context);
		router.attach("/json/transmitters", TransmittersResource.class);
		router.attach("/json/listeners", ListenersResource.class);
		return router;
	}

	@Override
	public String basePath() {
		return "/wm/multicastmachine";
	}

}
