package simulator.schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.RLFairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.traffic.Task.TaskComparator;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class AaloIdealScheduler extends Scheduler {
	private static final int K = 10;  // number of queues
	private static final int E = 10;  // size ratio between adjacent queues
	private static final int Q_HI_1 = 10 * 8 * 1024 * 1024;  // 10MB, size threshold of first queue
	private static final double DELTA = 10.0 / 1000.0;  // 10 ms, global size infomation update interval
	
	private RLFairSharingTopology topology;
	private PerTaskMixedTraffic traffic;
	
	private double nextSizeEvent;
	
	public AaloIdealScheduler() {
		super();
		name = "aalo-ideal-fair";
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
		
		ArrayList<LinkedList<Task>> taskQueues = new ArrayList<LinkedList<Task>>(K);
		for (int i = 0; i < K; i++) {
			taskQueues.add(new LinkedList<Task>());
		}
		for (Task task : traffic.tasks) {
			//double sentSize = task.getSentSize();
			double sentSize = task.getAaloSize();
			int taskClass = 0;
			double threshold = (double)Q_HI_1;
			while (sentSize > threshold & taskClass < K) {
				threshold *= (double)E;
				taskClass++;
			}
			taskQueues.get(taskClass).add(task);
		}
		
		double[][] extraUpBand = new double[K][];
		double[][] extraDownBand = new double[K][];
		for (int i = 0; i < K; i++) {
			extraUpBand[i] = new double[Simulator.config.numOfHosts];
			Arrays.fill(extraUpBand[i], 0);
			extraDownBand[i] = new double[Simulator.config.numOfHosts];
			Arrays.fill(extraDownBand[i], 0);
		}
		int sum = 0;
		int weightedSum = 0;
		for (int i = 0; i < K; i++) {
			if (!taskQueues.get(i).isEmpty()) {
				sum++;
				weightedSum += getWeight(i);
			}
		}
		for (int i = 0; i < K; i++) {
			if (!taskQueues.get(i).isEmpty()) {
				// schedule
				int[] numOfHosts = new int[1];
				numOfHosts[0] = Simulator.config.numOfHosts;
				FairSharingTopology tempTopology = new FairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
				tempTopology.resetRemainBand(Simulator.config.bandPerLayer[0] * getWeight(i) / weightedSum);
				Collections.sort(taskQueues.get(i), new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.FIFO));
				for (Task task : taskQueues.get(i)) {
					tempTopology.setRates(task.getFlows());
				}
				
				// allocate remaining bandwidth to other queues
				double[] remainUpBand = new double[Simulator.config.numOfHosts];
				double[] remainDownBand = new double[Simulator.config.numOfHosts];
				Arrays.fill(remainUpBand, Simulator.config.bandPerLayer[0] * getWeight(i) / weightedSum);
				Arrays.fill(remainDownBand, Simulator.config.bandPerLayer[0] * getWeight(i) / weightedSum);
				for (Task task : taskQueues.get(i)) {
					for (Flow flow : task.getFlows()) {
						remainUpBand[flow.src] -= flow.getRate();
						remainDownBand[flow.dst] -= flow.getRate();
					}
				}
				for (int j = 0; j < Simulator.config.numOfHosts; j++) {
					remainUpBand[j] = Utils.checkRate(remainUpBand[j]);
					remainDownBand[j] = Utils.checkRate(remainDownBand[j]);
					if (weightedSum != getWeight(i)) {  // avoid special cases when i is the only active queue
						for (int k = 0; k < K; k++) {
							if (!taskQueues.get(k).isEmpty() && k != i) {  // k != i solve the allocation error
								//extraUpBand[k][j] += remainUpBand[j] * getWeight(k) / (weightedSum - getWeight(i));
								//extraDownBand[k][j] += remainDownBand[j] * getWeight(k) / (weightedSum - getWeight(i));
								extraUpBand[k][j] += remainUpBand[j] / (sum - 1);
								extraDownBand[k][j] += remainDownBand[j] / (sum - 1);
							}
						}
					}
				}
			}
		}
		
		// backup allocated rates
		for (Task task : traffic.tasks) {
			for (Flow flow : task.getFlows()) {
				flow.setAllocRate(flow.getRate());
			}
		}
		
		// allocating extra rates
		for (int i = 0; i < K; i++) {
			if (!taskQueues.get(i).isEmpty()) {
				int[] numOfHosts = new int[1];
				numOfHosts[0] = Simulator.config.numOfHosts;
				FairSharingTopology tempTopology = new FairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
				tempTopology.resetRemainBand(extraUpBand[i], extraDownBand[i]);
				for (Task task : taskQueues.get(i)) {
					tempTopology.setRates(task.getFlows());
				}
			}
		}
		
		// compute final rate
		for (Task task : traffic.tasks) {
			for (Flow flow : task.getFlows()) {
				flow.setRate(flow.getAllocRate() + flow.getRate());
			}
		}
		
		if (!Topology.checkRateValidityTask(traffic.tasks)) {
			System.out.println("rate allocation error");
		}
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