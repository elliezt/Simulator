package simulator.utils;

public class Parameters {
	public static final double INFINITY = Double.MAX_VALUE;
	public static final double SIZE_GRANULARITY = 1;  // 1 bit
	public static final double RATE_GRANULARITY = 0.001;  // 10^-3 bits
	
	public static final boolean COMPUTE_TCT = true;  // true for task CT, false for flow CT
	public static final boolean OUTPUT_TO_FILE = true;
	public static final boolean REC_COMP = false;
	public static final boolean REC_TERM = false;
	
	public static final boolean REC_QUEUE_LEN = true;
	public static final double REC_QUEUE_INTERVAL = 0.01;
	
	public static final boolean REC_FLOW_RATE = false;
	public static final double REC_RATE_INTERVAL = 0.01;
}