package simulator.schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.lang.Math;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.RLFairSharingTopology;
import simulator.topology.Topology;
//import simulator.topology.LeafSpineTopology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Task.TaskComparator;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class DBAIdealScheduler extends Scheduler {

	private static final double UPDATE_INTERVAL = 0.0001;
	private boolean TOPOLOGY_RESTRICT;
	private int roundCount = 1;
	private int taskCount = 0;

	
	private FairSharingTopology topology;
	private PerTaskSepratedTraffic traffic;
	private LinkedList<Task> newTasks;
	private double[][] RemainUpBand;
	private double[][] RemainDownBand;
	
	public DBAIdealScheduler() {
		super();
		name = "dba-ideal";
	
		traffic = new PerTaskSepratedTraffic();
		topology = new FairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
		int max = 0;
		for (int i = 0; i < Simulator.config.layers; i++) {
			if (max < topology.linksPerLayer[i]) {
				max = topology.linksPerLayer[i];
			}
		}	
		RemainUpBand = new double[Simulator.config.layers][max];
		RemainDownBand = new double[Simulator.config.layers][max];
		newTasks = new LinkedList<Task>();
		TOPOLOGY_RESTRICT = Simulator.config.layers > 1;
	}
	
	public Traffic getTraffic() {
		return traffic;
	}
	
	public Topology getTopology() {
		return topology;
	}
	
	public void addTask(Task task) {
		newTasks.add(task);
		roundCount = 1;
	}
	
	protected void setNextSchedule() {
		//TODO update available bandwidth
		
		for(int i = 0; i < Simulator.config.layers; i++) {
			Arrays.fill(RemainUpBand[i], Simulator.config.bandPerLayer[i]);
			Arrays.fill(RemainDownBand[i], Simulator.config.bandPerLayer[i]);
		}
		
		if (newTasks.isEmpty()) {
			roundCount --;
			if (roundCount < 1) {
				roundCount = 1;
			}
		}
		// Smallest Bottleneck First schedule
		for (Iterator<Task> iter = newTasks.iterator(); iter.hasNext(); ) {
			Task task = iter.next();
			
			//routing strategy
			for (Flow flow : task.getFlows()) {
//				//Hedera
//				if (flow.size > 100000000 && flow.hops > 1) {
//					double srcDemand = Simulator.config.bandPerLayer[0];
//					double dstDemand = Simulator.config.bandPerLayer[0];
//					if (!(topology.upLinks.get(0).get(flow.src).unallocFlows == null)) {
//						srcDemand = Simulator.config.bandPerLayer[0] / (topology.upLinks.get(0).get(flow.src).unallocFlows.size() + 1);
//					}
//					if (!(topology.upLinks.get(0).get(flow.src).unallocFlows == null)) {
//						dstDemand = Simulator.config.bandPerLayer[0] / (topology.downLinks.get(0).get(flow.dst).unallocFlows.size() + 1);
//					}
//					double demand = srcDemand < dstDemand ? srcDemand : dstDemand;
//					int min = flow.upLinkId[1] / Simulator.config.nodesPerLayer[2] * Simulator.config.nodesPerLayer[2];
//					int max = min + Simulator.config.nodesPerLayer[2];
//					for (int i = min; i < max; i++) {
//						int j = Simulator.config.getHighestDownLinkId(flow.dst, 1, flow.dst, i);
////						System.out.println(topology.downLinks.get(1).get(j).remainBand);
//						if (topology.upLinks.get(1).get(i).remainBand >= demand && topology.downLinks.get(1).get(j).remainBand >= demand) {
//							flow.upLinkId[1] = i;
//							flow.downLinkId[1] = j;
//							break;
//						}
//					}
//				}
//				//Hedera ends
				//CONGA
				if (flow.hops > 1) {
					int min = flow.upLinkId[1] / Simulator.config.nodesPerLayer[2] * Simulator.config.nodesPerLayer[2];
					int max = min + Simulator.config.nodesPerLayer[2];
					double largestRemainBand = 0;
					for (int i = min; i < max; i++) {
						int j = Simulator.config.getHighestDownLinkId(flow.dst, 1, flow.dst, i);
						double upRemainBand = topology.upLinks.get(1).get(i).remainBand;
						double downRemainBand = topology.downLinks.get(1).get(j).remainBand;
						double realRemainBand = upRemainBand < downRemainBand ? upRemainBand : downRemainBand;
						if (realRemainBand > largestRemainBand) {
							largestRemainBand = realRemainBand;
							flow.upLinkId[1] = i;
							flow.downLinkId[1] = j;
						}
					}
				}
				//CONGA ends
			}
			//routing ends
			traffic.addTask(task);
			iter.remove();
		}
		topology.resetRemainBand();
		
		if (!traffic.normalTasks.isEmpty()) {
			for (Task task : traffic.normalTasks) {
//				task.calculateEffectiveBottleneck(RemainUpBand, RemainDownBand);
				task.calculateEffectiveBottleneckArray(RemainUpBand, RemainDownBand);
//				System.out.println(task.effective_bottleneck);
			}
			
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.EFFECTIVE_BOTTLENECK));
//			System.out.println(traffic.normalTasks.size());
		}
			
		// compute actual rates in topology
		for (Task task : traffic.deadlineTasks) {
			LinkedList<Flow> flows = new LinkedList<Flow>();
			flows.addAll(task.getFlows());
		}
//		for (Task task : traffic.normalTasks) {
//			LinkedList<Flow> flows = new LinkedList<Flow>();
//			flows.addAll(task.getFlows());
//			if (TOPOLOGY_RESTRICT) {
//					topology.setRates(flows);
//			}
//		}
		LinkedList<Flow> flows = new LinkedList<Flow>();
		for (Task task : traffic.normalTasks) {
			flows.addAll(task.getFlows());
			taskCount ++;
			if (taskCount == roundCount || taskCount >= 40) {
					topology.setRates(flows);
					flows.clear();
					taskCount = 0;
			}
		}
		topology.setRates(flows);
		flows.clear();
		taskCount = 0;
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
//				System.out.println(flow.getRate());
				if (flow.getExpectedCompletionTime() < minCompletionTime) {
					minCompletionTime = flow.getExpectedCompletionTime();
				}
			}
		}
		double updateInterval = minCompletionTime < UPDATE_INTERVAL? minCompletionTime : UPDATE_INTERVAL;
		nextEvent = Simulator.getTime() + minCompletionTime;
		//System.out.println("min flow ct " + minCompletionTime + " nextEvent " + nextEvent);
	}
	
	private double updateRemainBand(double band, double allocRate) {
		band -= allocRate;
		band = Utils.checkRate(band);
		if (band < 0) {
			band = 0;
		}
		return band;
	}
}