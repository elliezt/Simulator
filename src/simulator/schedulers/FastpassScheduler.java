package simulator.schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.PriorityTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.Task.TaskComparator;
import simulator.traffic.PerTaskSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class FastpassScheduler extends Scheduler {
	private PriorityTopology topology;
	private PerTaskSepratedTraffic traffic;
	private LinkedList<Task> newTasks;
	private double nextNormalEvent;
	private boolean TOPOLOGY_RESTRICT;
	private double[][] RemainUpBand;
	private double[][] RemainDownBand;
	
	public FastpassScheduler() {
		super();
		name = "fastpass";
		traffic = new PerTaskSepratedTraffic();
		topology = new PriorityTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer);
		newTasks = new LinkedList<Task>();
		TOPOLOGY_RESTRICT = Simulator.config.layers > 1;
		int max = 0;
		for (int i = 0; i < Simulator.config.layers; i++) {
			if (max < topology.linksPerLayer[i]) {
				max = topology.linksPerLayer[i];
			}
		}	
		RemainUpBand = new double[Simulator.config.layers][max];
		RemainDownBand = new double[Simulator.config.layers][max];
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
		// Smallest Bottleneck First schedule
		for(int i = 0; i < Simulator.config.layers; i++) {
			Arrays.fill(RemainUpBand[i], Simulator.config.bandPerLayer[i]);
			Arrays.fill(RemainDownBand[i], Simulator.config.bandPerLayer[i]);
		}
		for (Iterator<Task> iter = newTasks.iterator(); iter.hasNext(); ) {
			Task task = iter.next();
			traffic.addTask(task);
			iter.remove();
		}		
		if (traffic.normalTasks.isEmpty()) {
			nextNormalEvent = Parameters.INFINITY;
		}
		else {
			for (Task task : traffic.normalTasks) {
				task.calculateEffectiveBottleneckArray(RemainUpBand, RemainDownBand);
			}
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.EFFECTIVE_BOTTLENECK));
//			System.out.println(traffic.normalTasks.size());
			LinkedList<Flow> flows = new LinkedList<Flow>();
			for (Task task : traffic.normalTasks) {
				flows.addAll(task.getFlows());
			}
//			Collections.sort(flows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.REMAINING_SIZE));
			topology.setRates(flows);
			nextNormalEvent = Simulator.getTime() + 0.000012;
//			nextNormalEvent = Simulator.getTime() + flows.peek().getExpectedCompletionTime();
//			System.out.println(nextNormalEvent);
//			System.out.println("\n");
		}
	}
	
	protected void setNextEvent() {
		nextEvent = nextNormalEvent;
	}
}