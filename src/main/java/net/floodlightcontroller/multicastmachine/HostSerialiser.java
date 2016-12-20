package net.floodlightcontroller.multicastmachine;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class HostSerialiser extends JsonSerializer<HostEntry> {

	@Override
	public void serialize(HostEntry member, JsonGenerator jGen, SerializerProvider serializer)
			throws IOException, JsonProcessingException {
		
		jGen.writeStartObject();
		jGen.writeStringField("mac",member.printSrcMac());
		jGen.writeStringField("ip", member.printSrcIp());
		jGen.writeStringField("group", member.printGroupAddress());
		jGen.writeStringField("switch_id",member.printSwitchId());
		jGen.writeNumberField("port_id",member.printPortId());
		jGen.writeNumberField("active_time", member.printActiveTime());
		jGen.writeEndObject();
		
	}

}
