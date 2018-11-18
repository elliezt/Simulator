package simulator.schedulers;

import java.util.Collections;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.traffic.Task.TaskComparator;
import simulator.utils.Parameters;

public class ElasticScheduler extends Scheduler {
	private FairSharingTopology topology;
	private PerTaskSepratedTraffic traffic;
	private LinkedList<Task> newTasks;
	
	public ElasticScheduler() {
		super();
		name = "elastic";
		traffic = new PerTaskSepratedTraffic();
		topology = new FairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
		newTasks = new LinkedList<Task>();
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
		// handle new tasks
		for (Task task : newTasks) {
			if (task.hasDeadline) {
				
				if (true) {  // admission control
					traffic.addTask(task);
				}
				else {
					task.terminate();
				}
			}
			else {
				traffic.addTask(task);
			}
		}
		newTasks = new LinkedList<Task>();
		
		// deadline schedule
		LinkedList<Flow> flows = new LinkedList<Flow>();
		for (Task task : traffic.deadlineTasks) {
			flows.addAll(task.getFlows());
			topology.setRates(flows);
		}
		
		// other task schedule
		// guarantee minimum bandwidth
		// allocate remaining bandwidth
		Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.FIFO));
		for (Task task : traffic.normalTasks) {
			topology.setRates(task.getFlows());
		}
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
	}
}