package simulator.schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.PriorityTopology;
import simulator.topology.RLFairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.PerTaskSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Task.TaskComparator;
import simulator.traffic.TaskType;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class IICSScheduler extends Scheduler {
	private static final boolean TOPOLOGY_RESTRICT_APPROX = false;
	private static final boolean WORK_CONSERVATION_EN = false;
	private static final boolean STARVATION_FREE_EN = false;
//	private static final double UPDATE_INTERVAL = 2;  // standard value
	private static final double STARVATION_FREE_DURATION = 0.2;
	private boolean TOPOLOGY_RESTRICT;
	private boolean SEBF;
	private boolean ENHANCEMENT_EN;
	
	private PriorityTopology topology;
	private PerTaskSepratedTraffic traffic;
	private LinkedList<Task> newTasks;
	private double[] RemainUpBand;
	private double[] RemainDownBand;
	private double[][] RemainUpBandArray;
	private double[][] RemainDownBandArray;
	
	private boolean isStarvationFreeState;
	private boolean isUpdateFlowState;
	private double nextUpdateFlowEvent;
	private int indexOfUpdatedTask;
	private boolean isUpdateTaskState;
	private double[] generalDistribution;
	private double[] tempDistribution;
	private int tempCount;
	private double ewmaWeight;
	
	public IICSScheduler(String type) {
		super();
		name = "iics-" + type;
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
		topology = new PriorityTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer);
		RemainUpBand = new double[Simulator.config.numOfHosts];
		RemainDownBand = new double[Simulator.config.numOfHosts];
		RemainUpBandArray = new double[1][Simulator.config.numOfHosts];
		RemainDownBandArray = new double[1][Simulator.config.numOfHosts];
		newTasks = new LinkedList<Task>();
		TOPOLOGY_RESTRICT = Simulator.config.layers > 1;
		isStarvationFreeState = false;
		isUpdateFlowState = false;
		nextUpdateFlowEvent = Parameters.INFINITY;
		isUpdateTaskState = false;
		generalDistribution = new double[1999];
		tempDistribution = new double[1999];
		Arrays.fill(generalDistribution, 1/19.99);
		Arrays.fill(tempDistribution, 0);
		tempCount = 0;
		ewmaWeight = 0.2;
	}
	
	public Traffic getTraffic() {
		return traffic;
	}
	
	public Topology getTopology() {
		return topology;
	}
	
	public void addTask(Task task) {
		Flow flow = task.getFlows().get(task.availIndex);
		task.availIndex ++;
		task.addAvailableFlow(flow);
		newTasks.add(task);
		isUpdateTaskState = true;
		Arrays.fill(RemainUpBandArray[0], Simulator.config.bandPerLayer[0]);
		Arrays.fill(RemainDownBandArray[0], Simulator.config.bandPerLayer[0]);
		double effectiveBottleneck = task.getEffectiveBottleneck(RemainUpBandArray, RemainDownBandArray);
		tempDistribution[(int) (effectiveBottleneck/0.001)]++;
		tempCount++;
		if (tempCount >= 100) {
			for (int i = 0; i < 1999; i++) {
				generalDistribution[i] = (1 - ewmaWeight) * generalDistribution[i] + ewmaWeight * tempDistribution[i];
			}
			Arrays.fill(tempDistribution, 0);
			tempCount = 0;
		}
		task.historicUpArrival[flow.src] += flow.getReaminingSize();
		task.historicDownArrival[flow.dst] += flow.getReaminingSize();
		task.cumulativeEffectiveBottleneck = flow.getReaminingSize() / RemainUpBandArray[0][flow.src];
		for (int i = 1998; i > (int)(task.cumulativeEffectiveBottleneck / 0.001); i--) {
			task.probabilitySum += generalDistribution[i];
			task.expectation += (0.001 * i + 0.0005) * generalDistribution[i];
		}
		double residual = (0.001 - task.cumulativeEffectiveBottleneck % 0.001) / 0.001 * generalDistribution[(int)(task.cumulativeEffectiveBottleneck / 0.001)];
		task.probabilitySum += residual;
		task.expectation += residual * (0.001 * (int)(task.cumulativeEffectiveBottleneck / 0.001) + 0.0005);
		task.gittinsIndex = 1 / (task.expectation / task.probabilitySum - (task.cumulativeEffectiveBottleneck - task.getCurrentEffectiveBottleneck(RemainUpBandArray, RemainUpBandArray)));
	}
	
	protected void setNextSchedule() {
		//TODO update available bandwidth
		
		Arrays.fill(RemainUpBand, Simulator.config.bandPerLayer[0]);
		Arrays.fill(RemainDownBand, Simulator.config.bandPerLayer[0]);
		RemainUpBandArray[0] = RemainUpBand;
		RemainDownBandArray[0] = RemainDownBand;
		//update a certain task
		if (!isUpdateTaskState && isUpdateFlowState) {
			Task task = traffic.normalTasks.get(indexOfUpdatedTask);
			if (task.availIndex < task.getFlows().size()) {
				Flow flow = task.getFlows().get(task.availIndex);
				task.availIndex ++;
				task.addAvailableFlow(flow);
				
			
				task.probabilitySum = 0;
				task.expectation = 0;
				task.historicUpArrival[flow.src] += flow.getReaminingSize();
				task.historicDownArrival[flow.dst] += flow.getReaminingSize();
				if (task.cumulativeEffectiveBottleneck < task.historicUpArrival[flow.src] / RemainUpBandArray[0][flow.src]) {
					task.cumulativeEffectiveBottleneck = task.historicUpArrival[flow.src] / RemainUpBandArray[0][flow.src];
				}
				if (task.cumulativeEffectiveBottleneck < task.historicDownArrival[flow.dst] / RemainDownBandArray[0][flow.dst]) {
					task.cumulativeEffectiveBottleneck = task.historicDownArrival[flow.dst] / RemainDownBandArray[0][flow.dst];
				}
				for (int i = 1998; i > (int)(task.cumulativeEffectiveBottleneck / 0.001); i--) {
					task.probabilitySum += generalDistribution[i];
					task.expectation += (0.001 * i + 0.0005) * generalDistribution[i];
				} 
				double residual = (0.001 - task.cumulativeEffectiveBottleneck % 0.001) / 0.001 * generalDistribution[(int)(task.cumulativeEffectiveBottleneck / 0.001)];
				task.probabilitySum += residual;
				task.expectation += residual * (0.001 * (int)(task.cumulativeEffectiveBottleneck / 0.001) + 0.0005);
				task.gittinsIndex = 1 / (task.expectation / task.probabilitySum - (task.cumulativeEffectiveBottleneck - task.getCurrentEffectiveBottleneck(RemainUpBandArray, RemainUpBandArray)));
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
				task.calculateEffectiveBottleneckArray(RemainUpBandArray, RemainDownBandArray);
				task.gittinsIndex = 1 / (task.expectation / task.probabilitySum - (task.cumulativeEffectiveBottleneck - task.getCurrentEffectiveBottleneck(RemainUpBandArray, RemainUpBandArray)));
			}
			if (SEBF) {
				System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
				Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.GITTINS_INDEX));
			}
			else {
				Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.REMAIN_SIZE));
			}
			for (Task task : traffic.normalTasks) {
				task.bottleneckFlows = new LinkedList<Flow>();
				task.nonBottleneckFlows = new LinkedList<Flow>();
				for (Flow flow : task.getAvailableFlows()) {
					if ((task.bottleneckUpDown && flow.src == task.bottleneckLocation) || (!task.bottleneckUpDown && flow.dst == task.bottleneckLocation)) {
						task.bottleneckFlows.add(flow);
					}
					else {
						task.nonBottleneckFlows.add(flow);
					}
				}
				Collections.sort(task.bottleneckFlows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.REMAINING_SIZE));
				Collections.sort(task.nonBottleneckFlows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.REMAINING_SIZE));
				topology.setRates(task.bottleneckFlows);
				topology.setRates(task.nonBottleneckFlows);
//				System.out.println('\n');
//				Collections.sort(task.getAvailableFlows(), new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.REMAINING_SIZE));
//				topology.setRates(task.getAvailableFlows());
//				for (int index = 0; index < task.getAvailableFlows().size(); index++) {
//					System.out.println(task.getAvailableFlows().get(index).getRate());
//				}
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
		
	}
	
	protected void setNextEvent() {
		//System.out.println("Now in SetNextEvent");
		//flow completion and flow arrival are all included here
		double minCompletionTime = Parameters.INFINITY;
		double nextUpdateFlowEvent = Parameters.INFINITY;
		for (Task task: traffic.deadlineTasks) {
			for (Flow flow : task.getAvailableFlows()) {
				if (flow.getExpectedCompletionTime() < minCompletionTime) {
					minCompletionTime = flow.getExpectedCompletionTime();
				}
			}
		}
//		System.out.println(traffic.normalTasks.get(0).availIndex);
		for (Task task: traffic.normalTasks) {
			for (Flow flow : task.getAvailableFlows()) {
				if (flow.getExpectedCompletionTime() < minCompletionTime) {
					minCompletionTime = flow.getExpectedCompletionTime();
				}
			}
			if (task.availIndex < task.getFlows().size()) {
				double tempAwareTime = task.getFlows().get(task.availIndex).awareTime;
				if (nextUpdateFlowEvent > tempAwareTime) {
					nextUpdateFlowEvent = tempAwareTime;
					indexOfUpdatedTask = traffic.normalTasks.indexOf(task);
				}
			}
		}
		nextEvent = Simulator.getTime() + minCompletionTime;
		//System.out.println("min flow ct " + minCompletionTime + " nextEvent " + nextEvent);
		if (nextUpdateFlowEvent <= nextEvent) {
			nextEvent = nextUpdateFlowEvent;
			nextUpdateFlowEvent = Parameters.INFINITY;
			
			isUpdateFlowState = true;
		}
		else {
			nextUpdateFlowEvent = Parameters.INFINITY;
			//System.out.println("Complete");
			isUpdateFlowState = false;
		}
		isUpdateTaskState = false;
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