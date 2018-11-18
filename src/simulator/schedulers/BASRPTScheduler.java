package simulator.schedulers;

import java.util.Collections;

import simulator.simulators.Simulator;
import simulator.topology.PriorityTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.PerFlowBAMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class BASRPTScheduler extends Scheduler {
	public static final int V = 5000;
	
	private PriorityTopology topology;
	private PerFlowBAMixedTraffic traffic;
	
	public BASRPTScheduler() {
		super();
		name = "basrpt-" + V;
		traffic = new PerFlowBAMixedTraffic();
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
		for (Flow flow : traffic.flows) {
			flow.setBarpt(traffic.getQueueLenSep(flow));
		}
		Collections.sort(traffic.flows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.BASRPT));
		//System.out.println("flows.size = " + traffic.flows.size());
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