package simulator.schedulers;

import java.util.Collections;
import java.util.LinkedList;


import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerFlowMixedTraffic;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class LASScheduler extends Scheduler {
	private FairSharingTopology topology;
	private PerFlowMixedTraffic traffic;
	
	public LASScheduler() {
		super();
		name = "las";
		traffic = new PerFlowMixedTraffic();
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
		Collections.sort(traffic.flows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.LAS));	
		//System.out.println(traffic.flows.size());
		LinkedList<Flow> copyflows = new LinkedList<Flow> ();
		while (!traffic.flows.isEmpty()) {
			LinkedList<Flow> LASflows = new LinkedList<Flow> ();
			Flow LASflow = traffic.flows.poll();
			LASflows.add(LASflow);
			while (!traffic.flows.isEmpty() && traffic.flows.getFirst().getSentSize() == LASflow.getSentSize()) {
				LASflow = traffic.flows.poll();
				LASflows.add(LASflow);
			}
			topology.setRates(LASflows);
			copyflows.addAll(LASflows);
		}
		traffic.flows = copyflows;
		//System.out.println(traffic.flows.get(0).getRate());
	}

	protected void setNextEvent() {
		double minCompletionTime = Parameters.INFINITY;
		for (Flow flow : traffic.flows) {
			if (flow.getExpectedCompletionTime() < minCompletionTime) {
				minCompletionTime = flow.getExpectedCompletionTime();
			}
		}
		//System.out.println("minCompletionTime = " + traffic.flows.get(0).getExpectedCompletionTime());
		nextEvent = Simulator.getTime() + minCompletionTime;
	}
}