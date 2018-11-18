package simulator.traffic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import simulator.schedulers.BridgeScheduler;
import simulator.simulators.Simulator;
import simulator.utils.Config;
import simulator.utils.Parameters;

public class Task {
	private static AtomicInteger nextId = new AtomicInteger();
	public final int id;
	private int numOfFlows;
	private LinkedList<Flow> flows;
	private LinkedList<Flow> availableFlows; //flows alrealy come
	public LinkedList<Flow> bottleneckFlows; //current flows on the current bottleneck port 
	public LinkedList<Flow> nonBottleneckFlows; //current flows except bottleneckFlows;
//	public Iterator<Flow> availIter;
	public int availIndex;
	public final boolean hasDeadline;
	public final TaskType type;
	
	public final double awareTime;  // time when task informations are known
	public final double availableTime;  // time when task can be transmitted
	private double startTime;  // time when task start transmission
	private double endTime;  // time when task end transmission
	private double completionTime;
	public final double deadline;  // time when task must be finished
	
	private int numOfStartedFlows;
	private int numOfCompletedFlows;
	private double completedFlowSize;  // for Baraat
	public double effective_bottleneck;  // for Varys
	private double aaloSize;  // for Aalo
	public double current_effective_bottleneck; // for IICS
	private boolean starving;  // for Varys
	private double size;  // for performance comparison
	private double remainSize;
	private double sizeTime;
	private double length;  // for performance comparison
	private double remainLength;
	private double lengthTime;
	private boolean hasPriority;  // for Bridge
	private double bottleneck;  // for Bridge
	private double remainBottleneck;  // for Bridge
	private double priorityTime;
	private double influence;  // for SIF
	public double gittinsIndex; // for IICS
	public double[] historicUpArrival; // for IICS
	public double[] historicDownArrival; // for IICS
	public double cumulativeEffectiveBottleneck; // for IICS
	public double probabilitySum; // for IICS
	public double expectation; // for IICS
	public boolean bottleneckUpDown; // for IICS true-up, false-down
	public int bottleneckLocation;
	
	
	public static class TaskComparator implements Comparator<Task> {
		public static enum TASK_COMPARATOR_TYPE {
			COMPLETION_TIME,
			FIFO,
			DEADLINE,
			EFFECTIVE_BOTTLENECK,
			GITTINS_INDEX,
			SIZE,
			REMAIN_SIZE,
			SIZE_ENHANCE,
			WIDTH,
			REMAIN_WIDTH,
			LENGTH,
			REMAIN_LENGTH,
			BOTTLENECK,
			REMAIN_BOTTLENECK,
			BOTTLENECK_ENHANCE,
			INCLUENCE,
		}
		
		private TASK_COMPARATOR_TYPE type;
		
		public TaskComparator(TASK_COMPARATOR_TYPE type) {
			this.type = type;
		}
		
		public int compare(Task task1, Task task2) {
			int result = 0;
			if (type == TASK_COMPARATOR_TYPE.COMPLETION_TIME) {
				result = task1.completionTime > task2.completionTime ? 1 : (task1.completionTime == task2.completionTime ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.FIFO) {
				result = task1.availableTime > task2.availableTime ? 1 : (task1.availableTime == task2.availableTime ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.DEADLINE) {
				result = task1.deadline > task2.deadline ? 1 : (task1.deadline == task2.deadline ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.EFFECTIVE_BOTTLENECK) {  // for Varys
				result = task1.effective_bottleneck > task2.effective_bottleneck ? 1 : (task1.effective_bottleneck == task2.effective_bottleneck ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.GITTINS_INDEX) {  // for IICS
				result = task1.gittinsIndex > task2.gittinsIndex ? 1 : (task1.effective_bottleneck == task2.effective_bottleneck ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.SIZE) {
				result = task1.size > task2.size ? 1 : (task1.size == task2.size ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.REMAIN_SIZE) {
				task1.calculateRemainingSize();
				task2.calculateRemainingSize();
				result = task1.remainSize > task2.remainSize ? 1 : (task1.remainSize == task2.remainSize ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.SIZE_ENHANCE) {
				result = task1.sizeEnhance() > task2.sizeEnhance() ? 1 : (task1.sizeEnhance() == task2.sizeEnhance() ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.WIDTH) {
				result = task1.numOfFlows > task2.numOfFlows ? 1 : (task1.numOfFlows == task2.numOfFlows ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.REMAIN_WIDTH) {
				result = task1.getFlows().size() > task2.getFlows().size() ? 1 : (task1.getFlows().size() == task2.getFlows().size() ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.LENGTH) {
				result = task1.length > task2.length ? 1 : (task1.length == task2.length ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.REMAIN_LENGTH) {
				task1.calculateRemainingLength();
				task2.calculateRemainingLength();
				result = task1.remainLength > task2.remainLength ? 1 : (task1.remainLength == task2.remainLength ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.BOTTLENECK) {
				task1.calculateBottleneck(Simulator.config.bandPerLayer);
				task2.calculateBottleneck(Simulator.config.bandPerLayer);
				result = task1.bottleneck > task2.bottleneck ? 1 : (task1.bottleneck == task2.bottleneck ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.REMAIN_BOTTLENECK) {
				task1.calculateRemainingBottleneck(Simulator.config.bandPerLayer);
				task2.calculateRemainingBottleneck(Simulator.config.bandPerLayer);
				result = task1.remainBottleneck > task2.remainBottleneck ? 1 : (task1.remainBottleneck == task2.remainBottleneck ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.BOTTLENECK_ENHANCE) {
				task1.calculateBottleneck(Simulator.config.bandPerLayer);
				task2.calculateBottleneck(Simulator.config.bandPerLayer);
				result = task1.bottleneckEnhance() > task2.bottleneckEnhance() ? 1 : (task1.bottleneckEnhance() == task2.bottleneckEnhance() ? 0 : -1);
			}
			else if (type == TASK_COMPARATOR_TYPE.INCLUENCE) {
				result = task1.influence > task2.influence ? 1 : (task1.influence == task2.influence ? 0 : -1);
			}
			if (result == 0) {  // default: id
				result = task1.id > task2.id ? 1 : (task1.id == task2.id ? 0 : -1);
			}
			if (result == 0) {
				Simulator.output.error("task id equal error!!!");
			}
			return result;
		}
	}
	
	public Task(String type, double awareTime, double availableTime, double deadline) {
		this.id = nextId.incrementAndGet();
		this.awareTime = awareTime;
		this.availableTime = availableTime;
		if (deadline == Parameters.INFINITY) {
			hasDeadline = false;
			this.deadline = 0;
		}
		else {
			hasDeadline = true;
			this.deadline = availableTime + deadline;
		}
		this.type = new TaskType(type);
		flows = new LinkedList<Flow>();
		availableFlows = new LinkedList<Flow>();
		availIndex = 0;
		bottleneckFlows = new LinkedList<Flow>();
		nonBottleneckFlows = new LinkedList<Flow>();
		historicUpArrival = new double[Simulator.config.numOfHosts];
		historicDownArrival = new double[Simulator.config.numOfHosts];
		numOfStartedFlows = 0;
		numOfCompletedFlows = 0;
		completedFlowSize = 0;
		size = 0;
		length = 0;
		sizeTime = 0;
		lengthTime = 0;
		starving = true;
		hasPriority = false;
	}
	
	public static void resetId() {
		nextId = new AtomicInteger();
	}
	
	public void addFlow(double awareTime, int src, int dst, double size, Config config) {
		Flow flow = new Flow(this, awareTime, availableTime, deadline, src, dst, size, config);
		flows.add(flow);
		numOfFlows++;
		this.size += size;
		if (size > length)
			length = size;
	}
	
	public void addAvailableFlow(Flow flow) {
//		Flow flow = new Flow(this, awareTime, availableTime, deadline, src, dst, size, config);
		availableFlows.add(flow);
	}
	
	public void addFlows(LinkedList<Flow> newFlows) {  // for Varys
		for (Flow flow : newFlows) {
			flows.add(flow);
		}
	}
	
	public LinkedList<Flow> getFlows() {
		return flows;
	}
	
	public LinkedList<Flow> getAvailableFlows() {
		return availableFlows;
	}
	
	public void flowStart() {
		if (numOfStartedFlows == 0) {
			startTime = Simulator.getTime();
		}
		numOfStartedFlows++;
	}
	
	public void flowCompletion(Flow flow) {
		numOfCompletedFlows++;
		completedFlowSize += flow.size;
		if (numOfCompletedFlows == numOfFlows) {
			if (Parameters.COMPUTE_TCT) {
				endTime = Simulator.getTime();
				completionTime = endTime - availableTime;
				Simulator.resultAnalyzer.taskCompletion(this);
				Simulator.output.completion(type.id, "task "+id+" available time: "+availableTime+" start time: "+startTime
						+" end time: "+endTime+" completion time: "+(endTime - availableTime));
			}
		}
	}
	
	public void terminate() {  // for deadline tasks
		if (Parameters.COMPUTE_TCT) {
			Simulator.output.termination(type.id, "task "+id+" terminated at "+Simulator.getTime());
		}
	}
	
	public double getSentSize() {  // for Baraat
		double sentSize = completedFlowSize;
		for (Flow flow : flows) {
			sentSize += flow.size - flow.getReaminingSize();
		}
		return sentSize;
	}
	
	public double getEffectiveBottleneck(double[][] remainingUpBand, double[][] remainingDownBand) {
		//calculateEffectiveBottleneck(remainingUpBand, remainingDownBand);
		calculateEffectiveBottleneckArray(remainingUpBand, remainingDownBand);
		return effective_bottleneck;
	}
	
	public double getCurrentEffectiveBottleneck(double[][] remainingUpBand, double[][] remainingDownBand) {
		//calculateEffectiveBottleneck(remainingUpBand, remainingDownBand);
		calculateCurrentEffectiveBottleneck(remainingUpBand, remainingDownBand);
		return current_effective_bottleneck;
	}
	
	private void calculateRemainingSize() {
		if (sizeTime != Simulator.getTime()) {
			remainSize = 0;
			for (Flow flow : flows) {
				remainSize += flow.getReaminingSize();
			}
			sizeTime = Simulator.getTime();
		}
	}
	
	private double sizeEnhance() {
		return size - (Simulator.getTime() - availableTime) * BridgeScheduler.SIZE_ENHANCE_FACTOR;
	}
	
	private double bottleneckEnhance() {
		return bottleneck - (Simulator.getTime() - availableTime) * BridgeScheduler.BOTTLENECK_ENHANCE_FACTOR;
	}
	
	private void calculateRemainingLength() {
		if (lengthTime != Simulator.getTime()) {
			remainLength = 0;
			for (Flow flow : flows) {
				if (flow.size > length)
					remainLength = flow.size;
			}
			lengthTime = Simulator.getTime();
		}
	}
	
	public void calculateEffectiveBottleneck(double remainingUpBand[][], double remainingDownBand[][]) {  // for Varys
		HashMap<Integer, Double> totalUpSizes[] = new HashMap[remainingUpBand.length]; 
		HashMap<Integer, Double> totalDownSizes[] = new HashMap[remainingDownBand.length]; 
		for (int i = 0; i < remainingUpBand.length; i++) {
			totalUpSizes[i] = new HashMap<Integer, Double>();
		}
		for (Flow flow : flows) {
			for (int i = 0; i < remainingUpBand.length; i++) {
				Double size = totalUpSizes[i].get(flow.upLinkId[i]);
				if (size != null) {
					totalUpSizes[i].put(flow.upLinkId[i], size + flow.getReaminingSize());
				}
				else {
					totalUpSizes[i].put(flow.upLinkId[i], flow.getReaminingSize());
				}
				size = totalDownSizes[i].get(flow.downLinkId[i]);
				if (size != null) {
					totalDownSizes[i].put(flow.downLinkId[i], size + flow.getReaminingSize());
				}
				else {
					totalDownSizes[i].put(flow.downLinkId[i], flow.getReaminingSize());
				}
			}
		}
		effective_bottleneck = 0;
		for (int i = 0; i < remainingUpBand.length; i++) {
			for (Map.Entry<Integer, Double> entry : totalUpSizes[i].entrySet()) {
				// if (remainingUpBand[entry.getKey()] <= 0) {
				if (remainingUpBand[i][entry.getKey()] <= Parameters.RATE_GRANULARITY) {
					effective_bottleneck = Parameters.INFINITY;
				}
				if (effective_bottleneck < entry.getValue() / remainingUpBand[i][entry.getKey()]) {
					effective_bottleneck = entry.getValue() / remainingUpBand[i][entry.getKey()];
				}
			}
			for (Map.Entry<Integer, Double> entry : totalDownSizes[i].entrySet()) {
				// if (remainingDownBand[entry.getKey()] <= 0) {
				if (remainingDownBand[i][entry.getKey()] <= Parameters.RATE_GRANULARITY) {
					effective_bottleneck = Parameters.INFINITY;
				}
				if (effective_bottleneck < entry.getValue() / remainingDownBand[i][entry.getKey()]) {
					effective_bottleneck = entry.getValue() / remainingDownBand[i][entry.getKey()];
				}
			}
		}
	}
	
	public void calculateEffectiveBottleneckArray(double remainingUpBand[][], double remainingDownBand[][]) {  // for Varys
		double[][] totalUpSizes = new double[remainingUpBand.length][remainingUpBand[0].length];
		double[][] totalDownSizes = new double[remainingDownBand.length][remainingUpBand[0].length];
		for (Flow flow : flows) {
			for (int i = 0; i < flow.hops; i++) {
//			for (int i = 0; i < totalUpSizes.length; i++) {
				totalUpSizes[i][flow.upLinkId[i]] += flow.getReaminingSize();
				totalDownSizes[i][flow.downLinkId[i]] += flow.getReaminingSize();
			}
		}
		effective_bottleneck = 0;
		for (int i = 0; i < totalUpSizes.length; i++) {
			for (int j = 0; j < totalUpSizes[i].length; j++) {
				if (totalUpSizes[i][j] != 0 && remainingUpBand[i][j] <= Parameters.RATE_GRANULARITY) {
					effective_bottleneck = Parameters.INFINITY;
				}
				if (effective_bottleneck < totalUpSizes[i][j] / remainingUpBand[i][j]) {
					effective_bottleneck = totalUpSizes[i][j] / remainingUpBand[i][j];
				}
				if (totalDownSizes[i][j] != 0 && remainingDownBand[i][j] <= Parameters.RATE_GRANULARITY) {
					effective_bottleneck = Parameters.INFINITY;
				}
				if (effective_bottleneck < totalDownSizes[i][j] / remainingDownBand[i][j]) {
					effective_bottleneck = totalDownSizes[i][j] / remainingDownBand[i][j];
				}
			}
		}
	}
	
	public void calculateCurrentEffectiveBottleneck(double remainingUpBand[][], double remainingDownBand[][]) {  // for Varys
		double[][] totalUpSizes = new double[remainingUpBand.length][remainingUpBand[0].length];
		double[][] totalDownSizes = new double[remainingDownBand.length][remainingUpBand[0].length];
		for (Flow flow : availableFlows) {
//			for (int i = 0; i < flow.hops; i++) {
			for (int i = 0; i < totalUpSizes.length; i++) {
				totalUpSizes[i][flow.upLinkId[i]] += flow.getReaminingSize();
				totalDownSizes[i][flow.downLinkId[i]] += flow.getReaminingSize();
			}
		}
		current_effective_bottleneck = 0;
		for (int i = 0; i < totalUpSizes.length; i++) {
			for (int j = 0; j < totalUpSizes[i].length; j++) {
				if (totalUpSizes[i][j] != 0 && remainingUpBand[i][j] <= Parameters.RATE_GRANULARITY) {
					current_effective_bottleneck = Parameters.INFINITY;
				}
				if (current_effective_bottleneck < totalUpSizes[i][j] / remainingUpBand[i][j]) {
					current_effective_bottleneck = totalUpSizes[i][j] / remainingUpBand[i][j];
					bottleneckUpDown = true;
					bottleneckLocation = j;
				}
				if (totalDownSizes[i][j] != 0 && remainingDownBand[i][j] <= Parameters.RATE_GRANULARITY) {
					current_effective_bottleneck = Parameters.INFINITY;
				}
				if (current_effective_bottleneck < totalDownSizes[i][j] / remainingDownBand[i][j]) {
					current_effective_bottleneck = totalDownSizes[i][j] / remainingDownBand[i][j];
					bottleneckUpDown = false;
					bottleneckLocation = j;
				}
			}
		}
	}

	public void setStarving(boolean isStarving) {
		starving = isStarving;
	}
	
	public boolean isStarving() {
		return starving;
	}
	
	private void calculateBottleneck(double bandPerLayer[]) {  // for Bridge
		if (!hasPriority) {
			ArrayList<HashMap<Integer, Double>> totalUpSizes = new ArrayList<HashMap<Integer, Double>>(bandPerLayer.length);
			ArrayList<HashMap<Integer, Double>> totalDownSizes = new ArrayList<HashMap<Integer, Double>>(bandPerLayer.length);
			for (int i = 0; i < bandPerLayer.length; i++) {
				totalUpSizes.add(new HashMap<Integer, Double>());
				totalDownSizes.add(new HashMap<Integer, Double>());
			}
			for (Flow flow : flows) {
				for (int i = 0; i < flow.hops; i++) {
					Double size = totalUpSizes.get(i).get(flow.upLinkId[i]);
					if (size != null) {
						totalUpSizes.get(i).put(flow.upLinkId[i], size + flow.size);
					}
					else {
						totalUpSizes.get(i).put(flow.upLinkId[i], flow.size);
					}
					size = totalDownSizes.get(i).get(flow.downLinkId[i]);
					if (size != null) {
						totalDownSizes.get(i).put(flow.downLinkId[i], size + flow.size);
					}
					else {
						totalDownSizes.get(i).put(flow.downLinkId[i], flow.size);
					}
				}
			}
			double bottleneckSize = 0;
			for (int i = 0; i < bandPerLayer.length; i++) {
				for (Map.Entry<Integer, Double> entry : totalUpSizes.get(i).entrySet()) {
					if (bottleneckSize < entry.getValue() / bandPerLayer[i]) {
						bottleneckSize = entry.getValue() / bandPerLayer[i];
					}
				}
				for (Map.Entry<Integer, Double> entry : totalDownSizes.get(i).entrySet()) {
					if (bottleneckSize < entry.getValue() / bandPerLayer[i]) {
						bottleneckSize = entry.getValue() / bandPerLayer[i];
					}
				}
			}
			bottleneck = bottleneckSize;
			for (Flow flow : flows) {
				double flowBottleneckSize = 0;
				for (int i = 0; i < flow.hops; i++) {
					if (flowBottleneckSize < totalUpSizes.get(i).get(flow.upLinkId[i]) / bandPerLayer[i]) {
						flowBottleneckSize = totalUpSizes.get(i).get(flow.upLinkId[i]) / bandPerLayer[i];
					}
					if (flowBottleneckSize < totalDownSizes.get(i).get(flow.downLinkId[i]) / bandPerLayer[i]) {
						flowBottleneckSize = totalDownSizes.get(i).get(flow.downLinkId[i]) / bandPerLayer[i];
					}
				}
				flow.setBridgePriority(bottleneckSize, flowBottleneckSize);
			}
			hasPriority = true;
		}
	}
	
	private void calculateRemainingBottleneck(double bandPerLayer[]) {  // for Bridge
		if (priorityTime != Simulator.getTime()) {
			ArrayList<HashMap<Integer, Double>> totalUpSizes = new ArrayList<HashMap<Integer, Double>>(bandPerLayer.length);
			ArrayList<HashMap<Integer, Double>> totalDownSizes = new ArrayList<HashMap<Integer, Double>>(bandPerLayer.length);
			for (int i = 0; i < bandPerLayer.length; i++) {
				totalUpSizes.add(new HashMap<Integer, Double>());
				totalDownSizes.add(new HashMap<Integer, Double>());
			}
			for (Flow flow : flows) {
				for (int i = 0; i < flow.hops; i++) {
					Double size = totalUpSizes.get(i).get(flow.upLinkId[i]);
					if (size != null) {
						totalUpSizes.get(i).put(flow.upLinkId[i], size + flow.getReaminingSize());
					}
					else {
						totalUpSizes.get(i).put(flow.upLinkId[i], flow.getReaminingSize());
					}
					size = totalDownSizes.get(i).get(flow.downLinkId[i]);
					if (size != null) {
						totalDownSizes.get(i).put(flow.downLinkId[i], size + flow.getReaminingSize());
					}
					else {
						totalDownSizes.get(i).put(flow.downLinkId[i], flow.getReaminingSize());
					}
				}
			}
			double bottleneckSize = 0;
			for (int i = 0; i < bandPerLayer.length; i++) {
				for (Map.Entry<Integer, Double> entry : totalUpSizes.get(i).entrySet()) {
					if (bottleneckSize < entry.getValue() / bandPerLayer[i]) {
						bottleneckSize = entry.getValue() / bandPerLayer[i];
					}
				}
				for (Map.Entry<Integer, Double> entry : totalDownSizes.get(i).entrySet()) {
					if (bottleneckSize < entry.getValue() / bandPerLayer[i]) {
						bottleneckSize = entry.getValue() / bandPerLayer[i];
					}
				}
			}
			remainBottleneck = bottleneckSize;
			for (Flow flow : flows) {
				double flowBottleneckSize = 0;
				for (int i = 0; i < flow.hops; i++) {
					if (flowBottleneckSize < totalUpSizes.get(i).get(flow.upLinkId[i]) / bandPerLayer[i]) {
						flowBottleneckSize = totalUpSizes.get(i).get(flow.upLinkId[i]) / bandPerLayer[i];
					}
					if (flowBottleneckSize < totalDownSizes.get(i).get(flow.downLinkId[i]) / bandPerLayer[i]) {
						flowBottleneckSize = totalDownSizes.get(i).get(flow.downLinkId[i]) / bandPerLayer[i];
					}
				}
				flow.setBridgePriority(bottleneckSize, flowBottleneckSize);
			}
			priorityTime = Simulator.getTime();
		}
	}
	
	public double getCompletionTime() {
		return completionTime;
	}
	
	public void setAaloSize() {
		aaloSize = getSentSize();
	}
	
	public double getAaloSize() {
		return aaloSize;
	}
}