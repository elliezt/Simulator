package simulator.schedulers;

import java.util.ArrayList;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.RLFairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class AaloScheduler extends Scheduler {
	private static final int K = 10;  // number of queues
	private static final int E = 10;  // size ratio between adjacent queues
	private static final int Q_HI_1 = 10 * 8 * 1024 * 1024;  // 10MB, size threshold of first queue
	private static final double DELTA = 10.0 / 1000.0;  // 10 ms, global size infomation update interval
	
	private RLFairSharingTopology topology;
	private PerTaskMixedTraffic traffic;
	
	private double nextSizeEvent;
	
	public AaloScheduler() {
		super();
		name = "aalo";
		traffic = new PerTaskMixedTraffic();
		topology = new RLFairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
		nextSizeEvent = DELTA;
	}
	
	public Traffic getTraffic() {
		return traffic;
	}
	
	public Topology getTopology() {
		return topology;
	}
	
	public void addTask(Task task) {
		traffic.addTask(task);
	}
	
	protected void setNextSchedule() {
		if (Simulator.getTime() >= nextSizeEvent) {
			for (Task task : traffic.tasks) {
				task.setAaloSize();
			}
			nextSizeEvent += DELTA;
		}
		
		ArrayList<ArrayList<LinkedList<Flow>>> flowQueues = new ArrayList<ArrayList<LinkedList<Flow>>>(Simulator.config.numOfHosts);
		for (int i = 0; i < Simulator.config.numOfHosts; i++) {
			flowQueues.add(new ArrayList<LinkedList<Flow>>(K));
			for (int j = 0; j < K; j++) {
				flowQueues.get(i).add(new LinkedList<Flow>());
			}
		}
		for (Task task : traffic.tasks) {
			double sentSize = task.getAaloSize();
			int taskClass = 0;
			double threshold = (double)Q_HI_1;
			while (sentSize > threshold & taskClass < K) {
				threshold *= (double)E;
				taskClass++;
			}
			for (Flow flow : task.getFlows()) {
				flowQueues.get(flow.src).get(taskClass).add(flow);
			}
		}
		
		for (int i = 0; i < Simulator.config.numOfHosts; i++) {
			double weightedSum = 0;
			for (int j = 0; j < K; j++) {
				if (!flowQueues.get(i).get(j).isEmpty()) {
					weightedSum += getWeight(j);  // i starts from 0 instead of 1
				}
			}
			for (int j = 0; j < K; j++) {
				if (!flowQueues.get(i).get(j).isEmpty()) {
					double firstFlow = Parameters.INFINITY;
					for (Flow flow : flowQueues.get(i).get(j)) {
						if (flow.availableTime < firstFlow) {
							firstFlow = flow.availableTime;
						}
					}
					int firstTaskCount = 0;
					for (Flow flow : flowQueues.get(i).get(j)) {
						if (flow.availableTime == firstFlow) {
							firstTaskCount++;
						}
					}
					double rate = Simulator.config.bandPerLayer[0] * getWeight(j) / weightedSum / firstTaskCount;
					for (Flow flow : flowQueues.get(i).get(j)) {
						if (flow.availableTime == firstFlow) {
							flow.setAllocRate(rate);
						}
						else {
							flow.setAllocRate(0);
						}
					}
				}
			}
		}
		
		LinkedList<Flow> flows = new LinkedList<Flow>();
		for (Task task : traffic.tasks) {
			flows.addAll(task.getFlows());
		}
		topology.setRates(flows);
	}
	
	protected void setNextEvent() {
		double minCompletionTime = Parameters.INFINITY;
		for (Task task: traffic.tasks) {
			for (Flow flow : task.getFlows()) {
				if (flow.getExpectedCompletionTime() < minCompletionTime) {
					minCompletionTime = flow.getExpectedCompletionTime();
				}
			}
		}
		nextEvent = Simulator.getTime() + minCompletionTime;
		if (minCompletionTime != Parameters.INFINITY && nextSizeEvent < nextEvent) {
			nextEvent = nextSizeEvent;
		}
	}
	
	private int getWeight(int queueIdx) {
		return K - queueIdx;  // queue index starts from 0 instead of 1
	}
}