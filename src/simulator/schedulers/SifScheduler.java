package simulator.schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.topology.PriorityTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.PerFlowMixedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.traffic.Flow.FlowComparator;
import simulator.utils.Parameters;

public class SifScheduler extends Scheduler {
	private PriorityTopology topology;
	private PerFlowMixedTraffic traffic;
	private int SIF_TYPE = 1;
	
	public SifScheduler(int type) {
		super();
		name = "sif";
		SIF_TYPE = type;
		name += type;
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
		topology.initRelatedFlow(task.getFlows());
	}
	
	protected void setNextSchedule() {
		LinkedList<Flow> flows = new LinkedList<Flow>(traffic.flows);
		for (Flow flow : flows) {
			if (SIF_TYPE == 1) {
				flow.setInfluence(flow.getReaminingSize() * flow.getRelatedFlows().size());
			}
			else if (SIF_TYPE == 12) {
				int relatedFlowCount = flow.getRelatedFlows().size();
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					if (relatedFlow.getReaminingSize() <= flow.getReaminingSize() * 10) {
						relatedFlowCount++;
					}
				}
				flow.setInfluence(flow.getReaminingSize() * relatedFlowCount);
			}
			else if (SIF_TYPE == 13) {
				flow.setInfluence(flow.getReaminingSize() * flow.getRelatedFlows().size() * flow.hops);
			}
			else if (SIF_TYPE == 14) {
				flow.setInfluence(flow.getReaminingSize() * flow.getRelatedFlows().size() / flow.hops);
			}
			else if (SIF_TYPE == 15) {
				double influence = 0;
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					influence += flow.getReaminingSize() * relatedFlow.hops;
				}
				flow.setInfluence(influence);
			}
			else if (SIF_TYPE == 16) {
				double influence = 0;
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					influence += flow.getReaminingSize() / relatedFlow.hops;
				}
				flow.setInfluence(influence);
			}
			else if (SIF_TYPE == 2) {
				double influence = 0;
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					influence += flow.getReaminingSize() / relatedFlow.getReaminingSize();
				}
				flow.setInfluence(influence);
			}
			/*else if (SIF_TYPE == 3) {
				ArrayList<HashSet<Flow>> relatedFlows = topology.getRelatedFlowsByHop(flow);
				double influence = 0;
				for (HashSet<Flow> flows : relatedFlows) {
					if (!flows.isEmpty()) {
						influence += flow.getReaminingSize();
					}
				}
				flow.setInfluence(influence);
			}*/
			else if (SIF_TYPE == 4) {
				double size = 0;
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					size += relatedFlow.getReaminingSize();
				}
				size /= flow.getRelatedFlows().size();
				if (flow.getReaminingSize() > size / flow.getRelatedFlows().size()) {
					flow.setInfluence(flow.getReaminingSize() * flow.getRelatedFlows().size());
				}
				else {
					flow.setInfluence(flow.getReaminingSize());
				}
			}
			else if (SIF_TYPE == 5) {
				double max = Parameters.INFINITY;
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					if (max > relatedFlow.getReaminingSize()) {
						max = relatedFlow.getReaminingSize();
					}
				}
				if (flow.getReaminingSize() > max / flow.getRelatedFlows().size()) {
					flow.setInfluence(flow.getReaminingSize() * flow.getRelatedFlows().size());
				}
				else {
					flow.setInfluence(flow.getReaminingSize());
				}
			}
			else if (SIF_TYPE == 6) {
				double influence = flow.getReaminingSize() * flow.getRelatedFlows().size();
				double avg = 0;
				for (Flow relatedFlow : flow.getRelatedFlows()) {
					if (relatedFlow.getReaminingSize() > flow.getReaminingSize()) {
						avg += relatedFlow.getReaminingSize();
					}
				}
				if (!flow.getRelatedFlows().isEmpty()) {
					avg /= flow.getRelatedFlows().size();
				}
				else {
					avg = 0;
				}
				influence -= avg;
				flow.setInfluence(influence);
				//System.out.println(influence);
			}
			/*double influence = 0;
			for (Flow relatedFlow : flow.getRelatedFlows()) {
				influence += 10000 / relatedFlow.getReaminingSize();
			}
			flow.setInfluence(influence);*/
		}
		Collections.sort(flows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.INFLUENCE));
		topology.setRates(flows);
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