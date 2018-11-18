package simulator.schedulers;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Task.TaskComparator;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class BaraatScheduler extends Scheduler {
	//private static final int HEAVY_TASK_THRES_NAME = Integer.MAX_VALUE;  // FIFO
	//private static final int HEAVY_TASK_THRES_NAME = 1;  // 1GB for sim , Ran 15/6/25
	//private static final int HEAVY_TASK_THRES_NAME = 9;  // 9GB for Baraat, 80%
	//private static final int HEAVY_TASK_THRES_NAME = 13;  // 13GB for Baraat, 90%
	//private static final int HEAVY_TASK_THRES_NAME = 0.5;  // 500MB for Varys, 80%
	private static final double HEAVY_TASK_THRES_NAME = 0.08;  // 4GB for Varys, 90%
	private static final double HEAVY_TASK_THRES = (double)HEAVY_TASK_THRES_NAME * 1000.0 * 1000.0 * 1000.0;
	private FairSharingTopology topology;
	private PerTaskMixedTraffic traffic;
	
	public BaraatScheduler () {
		super();
		name = "baraat-"+HEAVY_TASK_THRES_NAME;
		traffic = new PerTaskMixedTraffic();
		topology = new FairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
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
		Collections.sort(traffic.tasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.FIFO));
		for (Iterator<Task> taskIter = traffic.tasks.iterator(); taskIter.hasNext(); ) {
			LinkedList<Flow> flows = new LinkedList<Flow>();
			Task task;
			do {
				task = taskIter.next();
				flows.addAll(task.getFlows());
			} while (task.getSentSize() >= HEAVY_TASK_THRES && taskIter.hasNext());
			topology.setRates(flows);
			//topology.setRatesLink(flows);
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
	}
}