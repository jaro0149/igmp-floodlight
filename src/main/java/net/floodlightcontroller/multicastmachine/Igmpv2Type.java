package net.floodlightcontroller.multicastmachine;

public enum Igmpv2Type {
	
	QUERY(17),
	REPORT(22),
	LEAVE(23);
	
	private int type;
	
	private Igmpv2Type(int type) {
		this.type = type;
	}
	
	public int getCode() {
		return type;
	}
	
	public static Igmpv2Type parseType(int type) {
		switch(type) {
		case 17:	return Igmpv2Type.QUERY;
		case 22:	return Igmpv2Type.REPORT;
		case 23:	return Igmpv2Type.LEAVE;
		default:	return null;
		}
	}
	
}
