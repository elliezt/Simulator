package simulator.schedulers;

import java.util.Collections;

import simulator.simulators.Simulator;
import simulator.topology.PriorityTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerFlowMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.traffic.Flow.FlowComparator;
import simulator.utils.Parameters;

public class FlowFifoScheduler extends Scheduler {
	private PriorityTopology topology;
	private PerFlowMixedTraffic traffic;
	
	public FlowFifoScheduler() {
		super();
		name = "flow-fifo";
		traffic = new PerFlowMixedTraffic();
		topology = new PriorityTopology(Simulator.config.childrenPerNode, Simulator.config.bandPerLayer);
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
		Collections.sort(traffic.flows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.FIFO));
		topology.setRates(traffic.flows);
	}
	
	protected void setNextEvent() {
		double minCompletionTime = Parameters.INFINITY;
		for (Flow flow : traffic.flows) {
			if (flow.getExpectedCompletionTime() < minCompletionTime) {
				minCompletionTime = flow.getExpectedCompletionTime();
			}
		}
		nextEvent = Simulator.getTime() + minCompletionTime;
	}
}