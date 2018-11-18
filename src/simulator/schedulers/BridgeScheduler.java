package simulator.schedulers;

import java.util.Collections;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.PriorityTopology;
import simulator.topology.ProportionalSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerTaskSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.Task.TaskComparator;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class BridgeScheduler extends Scheduler {
	public static final int SIZE_ENHANCE_FACTOR_NAME = 3000;
	public static final double SIZE_ENHANCE_FACTOR = (double)SIZE_ENHANCE_FACTOR_NAME * 1000.0 * 1000.0;
	public static final int BOTTLENECK_ENHANCE_FACTOR_NAME = 40;
	public static final double BOTTLENECK_ENHANCE_FACTOR = (double)BOTTLENECK_ENHANCE_FACTOR_NAME / 1000.0;
	
	private static enum INTER_TASK_TYPE {
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
	}
	private static enum INTRA_TASK_TYPE {
		FAIR,
		SIZE,
		CROWDEDNESS,
		PROPORTIONAL_FAIR,
	}
	
	private INTER_TASK_TYPE inter_task_type = INTER_TASK_TYPE.BOTTLENECK;
	private INTRA_TASK_TYPE intra_task_type = INTRA_TASK_TYPE.FAIR;
	
	private PriorityTopology priorityTopology;
	private FairSharingTopology fairSharingTopology;
	private ProportionalSharingTopology proportionalSharingTopology;
	private PerTaskSepratedTraffic traffic;
	
	public BridgeScheduler(String type) {
		super();
		name = "bridge-" + type;
		if (type.equalsIgnoreCase("tsize")) {
			inter_task_type = INTER_TASK_TYPE.SIZE;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("remain-tsize")) {
			inter_task_type = INTER_TASK_TYPE.REMAIN_SIZE;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("tsize-enhance")) {
			inter_task_type = INTER_TASK_TYPE.SIZE_ENHANCE;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
			name += "-" + SIZE_ENHANCE_FACTOR_NAME;
		}
		else if (type.equalsIgnoreCase("width")) {
			inter_task_type = INTER_TASK_TYPE.WIDTH;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("remain-width")) {
			inter_task_type = INTER_TASK_TYPE.REMAIN_WIDTH;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("length")) {
			inter_task_type = INTER_TASK_TYPE.LENGTH;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("remain-length")) {
			inter_task_type = INTER_TASK_TYPE.REMAIN_LENGTH;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("bot")) {
			inter_task_type = INTER_TASK_TYPE.BOTTLENECK;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("remain-bot")) {
			inter_task_type = INTER_TASK_TYPE.REMAIN_BOTTLENECK;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
		}
		else if (type.equalsIgnoreCase("bot-enhance")) {
			inter_task_type = INTER_TASK_TYPE.BOTTLENECK_ENHANCE;
			intra_task_type = INTRA_TASK_TYPE.SIZE;
			name += "-" + BOTTLENECK_ENHANCE_FACTOR_NAME;
		}
		else if (type.equalsIgnoreCase("fair")) {
			//inter_task_type = INTER_TASK_TYPE.SIZE;
			inter_task_type = INTER_TASK_TYPE.SIZE_ENHANCE;
			intra_task_type = INTRA_TASK_TYPE.FAIR;
		}
		else if (type.equalsIgnoreCase("crowd")) {
			//inter_task_type = INTER_TASK_TYPE.SIZE;
			inter_task_type = INTER_TASK_TYPE.SIZE_ENHANCE;
			intra_task_type = INTRA_TASK_TYPE.CROWDEDNESS;
		}
		else if (type.equalsIgnoreCase("pfair")) {
			//inter_task_type = INTER_TASK_TYPE.SIZE;
			inter_task_type = INTER_TASK_TYPE.SIZE_ENHANCE;
			intra_task_type = INTRA_TASK_TYPE.PROPORTIONAL_FAIR;
		}
		traffic = new PerTaskSepratedTraffic();
		if (intra_task_type == INTRA_TASK_TYPE.FAIR) {
			fairSharingTopology = new FairSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer);
		}
		else if (intra_task_type == INTRA_TASK_TYPE.PROPORTIONAL_FAIR) {
			proportionalSharingTopology = new ProportionalSharingTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer, Simulator.config.nodesPerLayer, ProportionalSharingTopology.PROPORTION_TYPE.REMAIN_SIZE);
		}
		else {
			priorityTopology = new PriorityTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer);
		}
	}
	
	public Traffic getTraffic() {
		return traffic;
	}
	
	public Topology getTopology() {
		if (intra_task_type == INTRA_TASK_TYPE.FAIR) {
			return fairSharingTopology;
		}
		else if (intra_task_type == INTRA_TASK_TYPE.PROPORTIONAL_FAIR) {
			return proportionalSharingTopology;
		}
		else {
			return priorityTopology;
		}
	}
	
	public void addTask(Task task) {
		traffic.addTask(task);
	}
	
	protected void setNextSchedule() {
		Collections.sort(traffic.deadlineTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.DEADLINE));
		for (Task task : traffic.deadlineTasks) {
			setRates(task);
		}
		
		if (inter_task_type == INTER_TASK_TYPE.BOTTLENECK) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.BOTTLENECK));
		}
		else if (inter_task_type == INTER_TASK_TYPE.SIZE) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.SIZE));
		}
		else if (inter_task_type == INTER_TASK_TYPE.REMAIN_SIZE) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.REMAIN_SIZE));
		}
		else if (inter_task_type == INTER_TASK_TYPE.SIZE_ENHANCE) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.SIZE_ENHANCE));
		}
		else if (inter_task_type == INTER_TASK_TYPE.WIDTH) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.WIDTH));
		}
		else if (inter_task_type == INTER_TASK_TYPE.REMAIN_WIDTH) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.REMAIN_WIDTH));
		}
		else if (inter_task_type == INTER_TASK_TYPE.LENGTH) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.LENGTH));
		}
		else if (inter_task_type == INTER_TASK_TYPE.REMAIN_LENGTH) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.REMAIN_LENGTH));
		}
		else if (inter_task_type == INTER_TASK_TYPE.REMAIN_BOTTLENECK) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.REMAIN_BOTTLENECK));
		}
		else if (inter_task_type == INTER_TASK_TYPE.BOTTLENECK_ENHANCE) {
			Collections.sort(traffic.normalTasks, new Task.TaskComparator(TaskComparator.TASK_COMPARATOR_TYPE.BOTTLENECK_ENHANCE));
		}
		for (Task task : traffic.normalTasks) {
			setRates(task);
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
	
	private void setRates(Task task) {
		if (intra_task_type == INTRA_TASK_TYPE.FAIR) {
			fairSharingTopology.setRates(task.getFlows());
			//fairSharingTopology.setRatesLink(task.getFlows());
			//fairSharingTopology.setRatesSearch(task.getFlows());
			//fairSharingTopology.setRatesSearchImproved(task.getFlows());
		}
		else if (intra_task_type == INTRA_TASK_TYPE.PROPORTIONAL_FAIR) {
			proportionalSharingTopology.setRates(task.getFlows());
		}
		else {
			if (intra_task_type == INTRA_TASK_TYPE.SIZE) {
				Collections.sort(task.getFlows(), new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.SIZE));
			}
			else if (intra_task_type == INTRA_TASK_TYPE.CROWDEDNESS) {
				Collections.sort(task.getFlows(), new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.BRIDGE_PRIORITY));
			}
			priorityTopology.setRates(task.getFlows());
		}
	}
}