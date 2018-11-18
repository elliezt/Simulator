package simulator.traffic;

import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import simulator.schedulers.BALASScheduler;
import simulator.schedulers.BASRPTScheduler;
import simulator.simulators.Simulator;
import simulator.utils.Config;
import simulator.utils.Parameters;

public class Flow {
	public final Task task;
	private static AtomicInteger nextId = new AtomicInteger();
	public final int id;
	public final int src;
	public final int dst;
	public final double size;
	public final boolean hasDeadline;
	public final int hops;
	public final int[] upLinkId;
	public final int[] downLinkId;
	
	public final double awareTime;  // time when flow informations are known
	public final double availableTime;  // time when flow can be transmitted
	private double startTime;  // time when flow start transmission
	private double endTime;  // time when flow end transmission
	private double completionTime;
	public final double deadline;  // time when flow must be finished
	
	private double allocRate;  // managed by Scheduler
	private double rate;  // managed by Topology
	private double remainSize;  // managed by Traffic
	private double sentSize; // managed by Traffic
	private double bottleneck;
	private HashSet<Flow> relatedFlows;  // managed by Simulator for rapid related flow finding
	
	private double influence;  // for SIF
	private double taskPriority;  // for Bridge
	private double flowPriority;  // for Bridge
	private double barpt;  // for BASRPT
	private double balas; // for BALAS
	private double linkPriceSum; // for DBA
	private double errorPerLink; // for DBA
	
	public static class FlowComparator implements Comparator<Flow> {
		public static enum FLOW_COMPARATOR_TYPE {
			COMPLETION_TIME,  // for result analyzer
			RATE,  // for max-min fairness
			FIFO,
			SIZE,
			REMAINING_SIZE,
			DEADLINE,
			INFLUENCE,
			BRIDGE_PRIORITY,
			BASRPT,
			LAS,
			BALAS
		}
		
		private FLOW_COMPARATOR_TYPE type;
		
		public FlowComparator(FLOW_COMPARATOR_TYPE type) {
			this.type = type;
		}
		
		public int compare(Flow flow1, Flow flow2) {
			int result = 0;
			if (type == FLOW_COMPARATOR_TYPE.COMPLETION_TIME) {
				result = flow1.completionTime > flow2.completionTime ? 1 : (flow1.completionTime == flow2.completionTime ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.RATE) {
				result = flow1.rate > flow2.rate ? 1 : (flow1.rate == flow2.rate ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.FIFO) {
				result = flow1.availableTime > flow2.availableTime ? 1 : (flow1.availableTime == flow2.availableTime ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.SIZE) {
				result = flow1.size > flow2.size ? 1 : (flow1.size == flow2.size ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.REMAINING_SIZE) {
				result = flow1.remainSize > flow2.remainSize ? 1 : (flow1.remainSize == flow2.remainSize ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.DEADLINE) {
				result = flow1.deadline > flow2.deadline ? 1 : (flow1.deadline == flow2.deadline ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.INFLUENCE) {
				result = flow1.influence > flow2.influence ? 1 : (flow1.influence == flow2.influence ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.BRIDGE_PRIORITY) {
				result = flow1.taskPriority > flow2.taskPriority ? 1 : (flow1.taskPriority == flow2.taskPriority ? 0 : -1);
				if (result == 0) {
					result = flow1.flowPriority > flow2.flowPriority ? 1 : (flow1.flowPriority == flow2.flowPriority ? 0 : -1);
				}
			}
			else if (type == FLOW_COMPARATOR_TYPE.BASRPT) {
				result = flow1.barpt > flow2.barpt ? 1 : (flow1.barpt == flow2.barpt ? 0 : -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.LAS) {
				result = flow1.sentSize > flow2.sentSize ? 1 : (flow1.sentSize == flow2.sentSize ? 0: -1);
			}
			else if (type == FLOW_COMPARATOR_TYPE.BALAS) {
				result = flow1.balas > flow2.balas ? 1 : (flow1.balas == flow2.balas ? 0 : -1);
			}
			if (result == 0) {  // default: id
				result = flow1.id > flow2.id ? 1 : (flow1.id == flow2.id ? 0 : -1);
			}
			if (result == 0) {
				Simulator.output.error("flow id equal error!!!");
			}
			return result;
		}
	}
	
	public Flow(Task task, double awareTime, double availableTime, double deadline, int src, int dst, double size, Config config) {
		this.task = task;
		this.id = nextId.incrementAndGet();
		this.src = src;
		this.dst = dst;
		this.size = size * 8;
//		this.size = ((1500) * Math.ceil(size / 1460) + 60) * 8; //normal packet
//		this.size = ((1508) * Math.ceil(size / 1448) + 88) * 8; // DBA packet
		this.awareTime = awareTime;
		this.availableTime = availableTime;
		this.sentSize = 0;
		this.rate = 0;
		this.errorPerLink = 0;
		this.linkPriceSum = 0;
		if (deadline == Parameters.INFINITY) {
			hasDeadline = false;
			this.deadline = 0;
		}
		else {
			hasDeadline = true;
			this.deadline = availableTime + deadline;
		}
		remainSize = this.size;
		hops = config.getFlowHops(this);
		upLinkId = new int[hops];
		downLinkId = new int[hops];
//		for (int i = 0; i < hops; i++) {
//		upLinkId[i] = config.getLinkId(src, i);
//		downLinkId[i] = config.getLinkId(dst, i);
//	}
		upLinkId[0] = src;
		downLinkId[0] = dst;
		for (int i = 1; i < hops; i++) {
			upLinkId[i] = config.getUpLinkId(src, i, upLinkId[i-1]);
//			System.out.println(upLinkId[i]);
		}
		for (int i = 1; i < hops-1; i++){
			downLinkId[i] = config.getDownLinkId(dst, i, downLinkId[i-1]);
		}
		if (hops > 1) {
			downLinkId[hops-1] = config.getHighestDownLinkId(dst, hops-1, downLinkId[hops-2], upLinkId[hops-1]);
		}
	}
	
	public int hashCode() {
		return id;
	}
	
	public static void resetId() {
		nextId = new AtomicInteger();
	}
	
	public void setAllocRate(double rate) {
		allocRate = rate;
	}
	
	public double getAllocRate() {
		return allocRate;
	}
	
	public void setRate(double rate) {
		this.rate = rate;
		if (rate != 0 && remainSize == size) {
			startTime = Simulator.getTime();
			task.flowStart();
		}
		if (rate < 0) {
			Simulator.output.error("set negative rate error in class Flow");
		}
	}
	
	public double getRate() {
		return rate;
	}
	
	public double getReaminingSize() {
		return remainSize;
	}
	
	public double getExpectedCompletionTime() {
		return remainSize/rate;
	}
	
	public double getSentSize(){
		return sentSize;
	}
	public void setBottleneck(int bottleneck) {
		this.bottleneck = bottleneck;
	}
	
	public double getBottleneck() {
		return bottleneck;
	}
	
	public HashSet<Flow> getRelatedFlows() {
		return relatedFlows;
	}
	
	public void initRelatedFlow() {
		relatedFlows = Simulator.scheduler.getTopology().getRelatedFlows(this);
		relatedFlows.remove(this);
		for (Flow flow : relatedFlows) {
			flow.addRelatedFlow(this);
		}
	}
	
	public void addRelatedFlow(Flow flow) {
		if (relatedFlows != null) {
			relatedFlows.add(flow);
		}
	}
	
	public void delRelatedFlow(Flow flow) {
		relatedFlows.remove(flow);
	}
	
	public void setInfluence(double influence) {  // for SIF
		this.influence = influence;
	}
	
	public void setBridgePriority(double taskPriority, double flowPriority) {
		this.taskPriority = taskPriority;
		this.flowPriority = flowPriority;
	}
	
	public void setBarpt(double queueLen) {
		barpt = (double)BASRPTScheduler.V / Simulator.config.numOfHosts * remainSize - queueLen;
	}
	
	public void setBalas(double queueLen) {
		balas = (double)BALASScheduler.V / Simulator.config.numOfHosts * sentSize - queueLen;
	}
	
	public void setLinkPriceSum(double priceSum) {
		linkPriceSum = priceSum;
	}
	
	public void setErrorPerLink(double power, double bottleneck) {
		if (rate > 1) {
			errorPerLink = ((1 - power) * Math.pow(rate, -power) / bottleneck - linkPriceSum) / hops;
		}
		else {
			errorPerLink = 0;
		}
//		System.out.println(linkPriceSum);
//		System.out.println('\n');
	}
	
	private void elseif(boolean b) {
		// TODO Auto-generated method stub
		
	}

	public double getLinkPriceSum () {
		return linkPriceSum;
	}
	
	public double getErrorPerLink () {
		return errorPerLink;
	}
	public double getBalas() {
		return balas;
	}
	
	public boolean updateReaminingSize(double time) {
		remainSize -= rate * time;
		sentSize = size - remainSize;
		if (remainSize <= Parameters.SIZE_GRANULARITY) {  // for approximation, avoiding endless loop
			if (relatedFlows != null) {
				for (Flow flow : relatedFlows) {
					flow.delRelatedFlow(this);
				}
			}
			if (!Parameters.COMPUTE_TCT) {
				endTime = Simulator.getTime();
				completionTime = endTime - availableTime;
				Simulator.resultAnalyzer.flowCompletion(this);
				Simulator.output.completion(task.type.id, "flow "+id+" available time: "+availableTime+" start time: "+startTime
						+" end time: "+endTime+" completion time: "+(endTime - availableTime));
			}
			task.flowCompletion(this);
			return true;
		}
		return false;
	}
	
	public void terminate(boolean terminateTask) {  // for deadline flows
		if (!Parameters.COMPUTE_TCT) {
			Simulator.output.termination(task.type.id, "flow "+id+" terminated at "+Simulator.getTime());
		}
		if (terminateTask) {
			task.terminate();
		}
	}
	
	public double getCompletionTime() {
		return completionTime;
	}
}