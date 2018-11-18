package simulator.schedulers;

import java.util.Collections;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerFlowBAMixedTraffic;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class BALASScheduler extends Scheduler {
	public static final int V = 20000;
	private FairSharingTopology topology;
	private PerFlowBAMixedTraffic traffic;
	
	public BALASScheduler() {
		super();
		name = "balas-" + V;
		traffic = new PerFlowBAMixedTraffic();
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
		for (Flow flow : traffic.flows) {
			flow.setBalas(traffic.getQueueLenSep(flow));
		}
		Collections.sort(traffic.flows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.BALAS));	
		//System.out.println(traffic.flows.size());
		LinkedList<Flow> copyflows = new LinkedList<Flow> ();
		while (!traffic.flows.isEmpty()) {
			LinkedList<Flow> LASflows = new LinkedList<Flow> ();
			Flow LASflow = traffic.flows.poll();
			LASflows.add(LASflow);
			while (!traffic.flows.isEmpty() && traffic.flows.getFirst().getBalas() == LASflow.getBalas()) {
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