package simulator.schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.lang.Math;

import simulator.simulators.Simulator;
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

public class DBAScheduler extends Scheduler {
	private static final boolean TOPOLOGY_RESTRICT_APPROX = false;
//	private static final double STARVATION_FREE_INTERVAL = 2;  // standard value
	private static final double UPDATE_INTERVAL = 0.001;
	private static final double POWER = 0.9;
	private boolean TOPOLOGY_RESTRICT;

	
	private RLFairSharingTopology topology;
	private PerTaskSepratedTraffic traffic;
	private LinkedList<Task> newTasks;
	private double[][] RemainUpBand;
	private double[][] RemainDownBand;
	private int clock = 0;
	private int memory = 0;
//	private boolean isStarvationFreeState;
//	private double nextStarvationFreeEvent;
	
	public DBAScheduler() {
		super();
		name = "dba";
	
		traffic = new PerTaskSepratedTraffic();
		topology = new RLFairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
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
	}
	
	protected void setNextSchedule() {
		clock = 0;
		memory = 0;
		//TODO update available bandwidth
		
		for(int i = 0; i < Simulator.config.layers; i++) {
			Arrays.fill(RemainUpBand[i], Simulator.config.bandPerLayer[i]);
			Arrays.fill(RemainDownBand[i], Simulator.config.bandPerLayer[i]);
		}
		
		
		// Smallest Bottleneck First schedule
		for (Iterator<Task> iter = newTasks.iterator(); iter.hasNext(); ) {
			Task task = iter.next();
			traffic.addTask(task);
			iter.remove();
		}
		if (!traffic.normalTasks.isEmpty()) {
			for (Task task : traffic.normalTasks) {
//				task.calculateEffectiveBottleneck(RemainUpBand, RemainDownBand);
				task.calculateEffectiveBottleneckArray(RemainUpBand, RemainDownBand);
			}
			
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.EFFECTIVE_BOTTLENECK));

			for (Task task : traffic.normalTasks) {
				double bottleneck = task.getEffectiveBottleneck(RemainUpBand, RemainDownBand);
//				System.out.println(bottleneck);
				if (bottleneck != Parameters.INFINITY) {
					task.setStarving(false);
					for (Flow flow : task.getFlows()) {
						double linkPriceSum = 0;
						for (int i = 0; i < flow.hops; i++) {
							linkPriceSum += topology.upLinks.get(i).get(flow.upLinkId[i]).price + topology.downLinks.get(i).get(flow.downLinkId[i]).price;
						}
						flow.setLinkPriceSum(linkPriceSum);
						double rate = Math.pow(bottleneck * linkPriceSum / (1 - POWER), -1/POWER);
						if (rate < 0) {
							Simulator.output.error("varys flow rate error");
						}
						if (rate == Double.POSITIVE_INFINITY) {
							rate = 1 * Math.pow(10, 26);
						}

						flow.setAllocRate(rate);
						if (flow.src == 1) {
							clock += 291;
							memory += 48 + 32;
						}
						if (flow.dst == 1) {
							memory += 16 + 32;
						}
					}
				}
				else {
					for (Flow flow : task.getFlows()) {
						flow.setAllocRate(0);
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
				topology.resetRemainBand();
				topology.setRatesWFQ(flows);
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
		for (Task task : traffic.normalTasks) {
			for (Flow flow: task.getFlows()) {
				flow.setErrorPerLink(POWER, task.getEffectiveBottleneck(RemainUpBand, RemainDownBand));
			}
		}
//		System.out.println(traffic.normalTasks.get(0).getFlows().get(2).getErrorPerLink());
		// updata link prices
		topology.updateLinkPrice(flows);
		
//		System.out.println(Simulator.getTime());
//		System.out.println(clock);
//		System.out.println(memory);
//		System.out.println(topology.upLinks.get(0).get(1).remainBand);
//		System.out.println('\n');
		
//		System.out.println(Simulator.getTime());
//		System.out.println(topology.clock);
		System.out.println(topology.memory + 3 * traffic.normalTasks.size() * 12);
//		System.out.println(topology.upLinks.get(1).get(0).remainBand + topology.upLinks.get(1).get(1).remainBand + topology.upLinks.get(1).get(2).remainBand);
//		System.out.println('\n');
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
		double updateInterval = minCompletionTime;
		if (minCompletionTime != Parameters.INFINITY) {
			updateInterval = minCompletionTime < UPDATE_INTERVAL? minCompletionTime : UPDATE_INTERVAL;
		}
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