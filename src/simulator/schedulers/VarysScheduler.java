package simulator.schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.RLFairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Task.TaskComparator;
import simulator.traffic.TaskType;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class VarysScheduler extends Scheduler {
	private static final boolean TOPOLOGY_RESTRICT_APPROX = false;
	private static final boolean WORK_CONSERVATION_EN = true;
	private static final boolean STARVATION_FREE_EN = false;
	private static final double STARVATION_FREE_INTERVAL = 2;  // standard value
	private static final double STARVATION_FREE_DURATION = 0.2;
	private boolean TOPOLOGY_RESTRICT;
	private boolean SEBF;
	private boolean ENHANCEMENT_EN;
	
	private RLFairSharingTopology topology;
	private PerTaskSepratedTraffic traffic;
	private LinkedList<Task> newTasks;
	private double[] RemainUpBand;
	private double[] RemainDownBand;
	private double[][] RemainUpBandArray;
	private double[][] RemainDownBandArray;
	
	private boolean isStarvationFreeState;
	private double nextStarvationFreeEvent;
	
	public VarysScheduler(String type) {
		super();
		name = "varys-" + type;
		if (type.equalsIgnoreCase("sebf")) {
			SEBF = true;
			ENHANCEMENT_EN = false;
		}
		else if (type.equalsIgnoreCase("sebf-enhance")) {
			SEBF = true;
			ENHANCEMENT_EN = true;
		}
		if (type.equalsIgnoreCase("size")) {
			SEBF = false;
			ENHANCEMENT_EN = false;
		}
		else if (type.equalsIgnoreCase("size-enhance")) {
			SEBF = false;
			ENHANCEMENT_EN = true;
		}
		traffic = new PerTaskSepratedTraffic();
		topology = new RLFairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
		RemainUpBand = new double[Simulator.config.numOfHosts];
		RemainDownBand = new double[Simulator.config.numOfHosts];
		RemainUpBandArray = new double[1][Simulator.config.numOfHosts];
		RemainDownBandArray = new double[1][Simulator.config.numOfHosts];
		newTasks = new LinkedList<Task>();
		TOPOLOGY_RESTRICT = Simulator.config.layers > 1;
		isStarvationFreeState = false;
		nextStarvationFreeEvent = STARVATION_FREE_INTERVAL;
	}
	
	public Traffic getTraffic() {
		return traffic;
	}
	
	public Topology getTopology() {
		return topology;
	}
	
	public void addTask(Task task) {
		newTasks.add(task);
	}
	
	protected void setNextSchedule() {
		//TODO update available bandwidth
		Arrays.fill(RemainUpBand, Simulator.config.bandPerLayer[0]);
		Arrays.fill(RemainDownBand, Simulator.config.bandPerLayer[0]);
		RemainUpBandArray[0] = RemainUpBand;
		RemainDownBandArray[0] = RemainDownBand;

		
		// deadline schedule
		for (Task task : traffic.deadlineTasks) {
			for (Flow flow : task.getFlows()) {
				RemainUpBand[flow.src] = updateRemainBand(RemainUpBand[flow.src], flow.getAllocRate());
				RemainDownBand[flow.dst] = updateRemainBand(RemainDownBand[flow.dst], flow.getAllocRate());
			}
		}
		for (Iterator<Task> iter = newTasks.iterator(); iter.hasNext(); ) {
			Task task = iter.next();
			if (task.hasDeadline) {
				if (task.getEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray) <= task.deadline - task.availableTime) {
					traffic.addTask(task); //admission control
					for (Flow flow : task.getFlows()) {
						double rate = flow.getReaminingSize() / (task.deadline - task.availableTime);
						flow.setAllocRate(rate);
						RemainUpBand[flow.src] = updateRemainBand(RemainUpBand[flow.src], rate);
						RemainDownBand[flow.dst] = updateRemainBand(RemainDownBand[flow.dst], rate);
					}
				}
				else {
					task.terminate();
				}
				iter.remove();
			}
		}
		
		// avoid perpetual starvation
		if (STARVATION_FREE_EN) {
			if (isStarvationFreeState) {
				Task aggregatedTask = new Task(TaskType.types[0], 0, 0, 0);
				for (Task task : traffic.normalTasks) {
					if (task.isStarving()) {
						aggregatedTask.addFlows(task.getFlows());
					}
				}
				if (!aggregatedTask.getFlows().isEmpty()) {
//					double aggregatedBottleneck = aggregatedTask.getEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray);
					double aggregatedBottleneck = aggregatedTask.getEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray);
					for (Task task : traffic.normalTasks) {
						if (task.isStarving()) {
							for (Flow flow : task.getFlows()) {
								double rate = flow.getReaminingSize() / aggregatedBottleneck;
								flow.setAllocRate(rate);
								RemainUpBand[flow.src] = updateRemainBand(RemainUpBand[flow.src], rate);
								RemainDownBand[flow.dst] = updateRemainBand(RemainDownBand[flow.dst], rate);
							}
						}
					}
				}
			}
		}
		
		// Smallest Bottleneck First schedule
		for (Iterator<Task> iter = newTasks.iterator(); iter.hasNext(); ) {
			Task task = iter.next();
			traffic.addTask(task);
			iter.remove();
		}
		if (!traffic.normalTasks.isEmpty()) {
			for (Task task : traffic.normalTasks) {
//				task.calculateEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray);
				task.calculateEffectiveBottleneckArray(RemainUpBandArray, RemainDownBandArray);
			}
			if (SEBF) {
				Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.EFFECTIVE_BOTTLENECK));
			}
			else {
				Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.REMAIN_SIZE));
			}
			for (Task task : traffic.normalTasks) {
//				double bottleneck = task.getEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray);
				double bottleneck = task.getEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray);
				if (bottleneck != Parameters.INFINITY) {
					task.setStarving(false);
					for (Flow flow : task.getFlows()) {
						double rate = flow.getReaminingSize() / bottleneck;
						if (rate < 0) {
							Simulator.output.error("varys flow rate error");
						}
						flow.setAllocRate(rate);
						RemainUpBand[flow.src] = updateRemainBand(RemainUpBand[flow.src], rate);
						RemainDownBand[flow.dst] = updateRemainBand(RemainDownBand[flow.dst], rate);
						RemainUpBandArray[0][flow.src] = RemainUpBand[flow.src];
						RemainDownBandArray[0][flow.dst] = RemainDownBand[flow.dst];
					}
				}
				else {
					for (Flow flow : task.getFlows()) {
						flow.setAllocRate(0);
					}
				}
			}
		}
		
		// work-conservation
		if (WORK_CONSERVATION_EN) {
			ArrayList<LinkedList<Flow>> upFlows = new ArrayList<LinkedList<Flow>>();
			for (int i = 0; i < Simulator.config.numOfHosts; i++) {
				LinkedList<Flow> flows = new LinkedList<Flow>();
				upFlows.add(flows);
			}
			for (Task task : traffic.deadlineTasks) {
				for (Flow flow : task.getFlows()) {
					upFlows.get(flow.src).add(flow);
				}
			}
			for (Task task : traffic.normalTasks) {
				for (Flow flow : task.getFlows()) {
					upFlows.get(flow.src).add(flow);
				}
			}
			for (int i = 0; i < Simulator.config.numOfHosts; i++) {
				if (Simulator.config.bandPerLayer[0] != RemainUpBand[i]) {
					if (RemainUpBand[i] > Parameters.RATE_GRANULARITY) {
						double ratio = Simulator.config.bandPerLayer[0] / (Simulator.config.bandPerLayer[0] - RemainUpBand[i]) - 1;
						for (Flow flow : upFlows.get(i)) {
							double extraRate = flow.getAllocRate() * ratio;
							if (extraRate > RemainDownBand[flow.dst]) {
								extraRate = RemainDownBand[flow.dst];
							}
							flow.setAllocRate(flow.getAllocRate() + extraRate);
							RemainDownBand[flow.dst] = updateRemainBand(RemainDownBand[flow.dst], extraRate);
						}
					}
				}
				else if (ENHANCEMENT_EN) {  // special case when no bandwidth is allocated to Remain tasks, improvements by shuran on 15/06/10
					double extraRate = RemainUpBand[i] / upFlows.get(i).size();
					for (Flow flow : upFlows.get(i)) {
						if (extraRate < RemainDownBand[flow.dst]) {
							flow.setAllocRate(extraRate);
							RemainDownBand[flow.dst] = updateRemainBand(RemainDownBand[flow.dst], extraRate);
						}
					}
				}
			}
		}
		
		// compute actual rates in topology
		LinkedList<Flow> flows = new LinkedList<Flow>();
		for (Task task : traffic.deadlineTasks) {
			flows.addAll(task.getFlows());
		}
		for (Task task : traffic.normalTasks) {
			flows.addAll(task.getFlows());
		}
		if (TOPOLOGY_RESTRICT) {
			if (TOPOLOGY_RESTRICT_APPROX) {
				topology.setRatesApprox(flows);
			}
			else {
				topology.setRates(flows);
			}
		}
		else {
			topology.setRatesFake(flows);
		}
		/*for (int i = 0; i < RemainUpBand.length; i++) {
			System.out.println("up " + i + " " + RemainUpBand[i]);
		}
		for (int i = 0; i < RemainDownBand.length; i++) {
			System.out.println("down " + i + " " + RemainDownBand[i]);
		}*/
	}
	
	protected void setNextEvent() {
		double minCompletionTime = Parameters.INFINITY;
		for (Task task: traffic.deadlineTasks) {
			for (Flow flow : task.getFlows()) {
				if (flow.getExpectedCompletionTime() < minCompletionTime) {
					minCompletionTime = flow.getExpectedCompletionTime();
				}
			}
		}
		for (Task task: traffic.normalTasks) {
			for (Flow flow : task.getFlows()) {
				if (flow.getExpectedCompletionTime() < minCompletionTime) {
					minCompletionTime = flow.getExpectedCompletionTime();
				}
			}
		}
		nextEvent = Simulator.getTime() + minCompletionTime;
		
		//System.out.println("min flow ct " + minCompletionTime + " nextEvent " + nextEvent);
		if (minCompletionTime != Parameters.INFINITY && nextStarvationFreeEvent <= nextEvent) {
			nextEvent = nextStarvationFreeEvent;
			if (isStarvationFreeState) {
				nextStarvationFreeEvent += STARVATION_FREE_INTERVAL;
				for (Task task : traffic.normalTasks) {
					task.setStarving(true);
				}
			}
			else {
				nextStarvationFreeEvent += STARVATION_FREE_DURATION;
			}
			isStarvationFreeState = !isStarvationFreeState;
		}
	}
	
	private double updateRemainBand(double band, double allocRate) {
		band -= allocRate;
		band = Utils.checkRate(band);
		if (band < 0) {
			Simulator.output.error("varys remaining band error");
		}
		return band;
	}
}