package simulator.simulators;

import simulator.schedulers.BASRPTScheduler;
import simulator.schedulers.AaloIdealScheduler;
import simulator.schedulers.AaloScheduler;
import simulator.schedulers.BALASScheduler;
import simulator.schedulers.BaraatScheduler;
import simulator.schedulers.BridgeScheduler;
import simulator.schedulers.DBAIdealScheduler;
import simulator.schedulers.ElasticScheduler;
import simulator.schedulers.FairSharingScheduler;
import simulator.schedulers.FastpassScheduler;
import simulator.schedulers.FlowFifoScheduler;
import simulator.schedulers.IICSScheduler;
import simulator.schedulers.PDQScheduler;
import simulator.schedulers.LASScheduler;
import simulator.schedulers.Scheduler;
import simulator.schedulers.SifScheduler;
import simulator.schedulers.VarysScheduler;
import simulator.schedulers.DBAScheduler;
import simulator.traffic.Flow;
import simulator.traffic.ResultAnalyzer;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.trafficgen.DctcpTrafficGenerator;
import simulator.trafficgen.SimpleTaskTrafficGenerator;
import simulator.trafficgen.TrafficGenerator;
import simulator.trafficgen.VarysTaskTrafficGenerator;
import simulator.trafficgen.Vl2TrafficGenerator;
import simulator.utils.Config;
import simulator.utils.Output;
import simulator.utils.Parameters;
import simulator.utils.RunTime;

public class Simulator {
	private static enum EVENT_TYPE {
		NEW_TRAFFIC,
		SCHEDULER_UPDATE
	}
	
	private static double time;
	public static Config config;
	public static Output output;
	public static ResultAnalyzer resultAnalyzer;
	private double lastEventTime;
	
	public static TrafficGenerator trafficGenerator;
	public static Traffic traffic;
	public static Scheduler scheduler;
	public static int scheduleEventCount;
	
	private class Event {
		private double time;
		public EVENT_TYPE type;
		
		public Event(double time, EVENT_TYPE type) {
			this.time = time;
			this.type = type;
		}
	}
	
	public Simulator(String schedulerName, String trafficName, Config config, String outputPath) {
		scheduleEventCount = 0;
		time = 0;
		Simulator.config = config;
		lastEventTime = 0;
		if (schedulerName.equalsIgnoreCase("pdq")) {
			Simulator.scheduler = new PDQScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("fair")) {
			Simulator.scheduler = new FairSharingScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("flow-fifo")) {
			Simulator.scheduler = new FlowFifoScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("las")) {
			Simulator.scheduler = new LASScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("sif1")) {
			Simulator.scheduler = new SifScheduler(1);
		}
		else if (schedulerName.equalsIgnoreCase("baraat")) {
			Simulator.scheduler = new BaraatScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("varys-sebf")) {
			Simulator.scheduler = new VarysScheduler("sebf");
		}
		else if (schedulerName.equalsIgnoreCase("varys-sebf-enhance")) {
			Simulator.scheduler = new VarysScheduler("sebf-enhance");
		}
		else if (schedulerName.equalsIgnoreCase("varys-size")) {
			Simulator.scheduler = new VarysScheduler("size");
		}
		else if (schedulerName.equalsIgnoreCase("varys-size-enhance")) {
			Simulator.scheduler = new VarysScheduler("size-enhance");
		}
		else if (schedulerName.equalsIgnoreCase("elastic")) {
			Simulator.scheduler = new ElasticScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("bridge-tsize")) {
			Simulator.scheduler = new BridgeScheduler("tsize");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-remain-tsize")) {
			Simulator.scheduler = new BridgeScheduler("remain-tsize");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-tsize-enhance")) {
			Simulator.scheduler = new BridgeScheduler("tsize-enhance");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-width")) {
			Simulator.scheduler = new BridgeScheduler("width");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-remain-width")) {
			Simulator.scheduler = new BridgeScheduler("remain-width");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-length")) {
			Simulator.scheduler = new BridgeScheduler("length");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-remain-length")) {
			Simulator.scheduler = new BridgeScheduler("remain-length");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-bot")) {
			Simulator.scheduler = new BridgeScheduler("bot");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-remain-bot")) {
			Simulator.scheduler = new BridgeScheduler("remain-bot");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-bot-enhance")) {
			Simulator.scheduler = new BridgeScheduler("bot-enhance");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-fair")) {
			Simulator.scheduler = new BridgeScheduler("fair");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-crowd")) {
			Simulator.scheduler = new BridgeScheduler("crowd");
		}
		else if (schedulerName.equalsIgnoreCase("bridge-pfair")) {
			Simulator.scheduler = new BridgeScheduler("pfair");
		}
		else if (schedulerName.equalsIgnoreCase("basrpt")) {
			Simulator.scheduler = new BASRPTScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("balas")) {
			Simulator.scheduler = new BALASScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("dba")) {
			Simulator.scheduler = new DBAScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("dba-ideal")) {
			Simulator.scheduler = new DBAIdealScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("iics-sebf")) {
			Simulator.scheduler = new IICSScheduler("sebf");
		}
		else if (schedulerName.equalsIgnoreCase("aalo")) {
			Simulator.scheduler = new AaloScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("aalo-ideal")) {
			Simulator.scheduler = new AaloIdealScheduler();
		}
		else if (schedulerName.equalsIgnoreCase("fastpass")) {
			Simulator.scheduler = new FastpassScheduler();
		}

		else {
			System.out.println("scheduler " + schedulerName + " not found!!!");
			System.exit(0);
		}
		if (trafficName.equalsIgnoreCase("simple-task")) {
			trafficGenerator = new SimpleTaskTrafficGenerator();
		}
		else if (trafficName.equalsIgnoreCase("varys-task")) {
			trafficGenerator = new VarysTaskTrafficGenerator();
		}
		else if (trafficName.equalsIgnoreCase("dctcp")) {
			trafficGenerator = new DctcpTrafficGenerator();
		}
		else if (trafficName.equalsIgnoreCase("vl2")) {
			trafficGenerator = new Vl2TrafficGenerator();
		}
		else {
			System.out.println("traffic generator " + trafficName + " not found");
			System.exit(0);
		}
		traffic = scheduler.getTraffic();
		output = new Output(outputPath, trafficGenerator.name, scheduler.name);
		resultAnalyzer = new ResultAnalyzer();
		Task.resetId();
		Flow.resetId();
	}
	
	public static double getTime() {
		return time;
	}
	
	public void simulate() {
		RunTime runtime = new RunTime();
		int lastTime = 0;
		double nextQueueRecTime = 0;
		double nextRateRecTime = 0;
		/*int i = 0;
		for (Event nextEvent = getNextEvent(); nextEvent.time != Parameters.INFINITY && i < 10; nextEvent = getNextEvent()) {
			i++;*/
		for (Event nextEvent = getNextEvent(); nextEvent.time != Parameters.INFINITY; nextEvent = getNextEvent()) {
			time = nextEvent.time;
//			System.out.println(time);
//			System.out.println('\n');
			if ((int)time > lastTime) {
				lastTime++;
				System.out.println("simulator time: " + lastTime + "s, uncompleted flows: " + scheduler.getTraffic().getFlows().size());
			}
			if (nextEvent.time != lastEventTime) {
				traffic.updateTraffic(nextEvent.time - lastEventTime);
//				System.out.println(nextEvent.time - lastEventTime);
//				System.out.println("\n");
//				System.out.println("flows.size = " + traffic.getFlows().size());
				lastEventTime = nextEvent.time;
			}
			if (nextEvent.type == EVENT_TYPE.NEW_TRAFFIC) {
				do {
					scheduler.addTask(trafficGenerator.nextTask());
				} while (nextEvent.type == EVENT_TYPE.NEW_TRAFFIC && trafficGenerator.nextEvent == time);
				scheduler.updateSchedule();
			}
			else if (nextEvent.type == EVENT_TYPE.SCHEDULER_UPDATE) {
				scheduler.updateSchedule();
//				scheduleEventCount ++;
//				System.out.println(scheduleEventCount);
			}
			if (Parameters.REC_QUEUE_LEN) {
				if (time >= nextQueueRecTime) {
					nextQueueRecTime += Parameters.REC_QUEUE_INTERVAL;
					double[] queueLen = traffic.getQueueLen();
					Simulator.output.recQueueLen(time, queueLen);
				}
			}
			if (Parameters.REC_FLOW_RATE) {
				if (time >= nextRateRecTime) {
					nextRateRecTime += Parameters.REC_RATE_INTERVAL;
					double[] flowRates = traffic.getFlowRate();
					Simulator.output.recFlowRate(time, flowRates);
				}
			}
		}
		System.out.println("simulation duration of " + scheduler.name + " is " + runtime.getRunTime("s-us"));
		resultAnalyzer.analyze();
		output.closeAll();
	}
	
	private Event getNextEvent() {
		Event nextEvent;
		if (trafficGenerator.nextEvent <= scheduler.nextEvent) {
			nextEvent = new Event(trafficGenerator.nextEvent, EVENT_TYPE.NEW_TRAFFIC);
		}
		else {
			nextEvent = new Event(scheduler.nextEvent, EVENT_TYPE.SCHEDULER_UPDATE);
		}
		//System.out.println("nextEvent " + nextEvent.time + " gen " + trafficGenerator.nextEvent + " sch " + scheduler.nextEvent);
		return nextEvent;
	}
}