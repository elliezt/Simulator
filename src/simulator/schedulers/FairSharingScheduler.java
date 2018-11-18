package simulator.schedulers;

import simulator.simulators.Simulator;
import simulator.topology.FairSharingTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerFlowMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public class FairSharingScheduler extends Scheduler {
	private FairSharingTopology topology;
	private PerFlowMixedTraffic traffic;
	
	public FairSharingScheduler() {
		super();
		name = "fair";
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
		topology.setRates(traffic.flows);
		//topology.setRatesSort(traffic.flows);
		//topology.setRatesSearch(traffic.flows);
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