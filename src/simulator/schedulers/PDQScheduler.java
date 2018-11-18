package simulator.schedulers;

import java.util.Collections;

import simulator.simulators.Simulator;
import simulator.topology.PriorityTopology;
import simulator.topology.Topology;
import simulator.traffic.Flow;
import simulator.traffic.Flow.FlowComparator;
import simulator.traffic.PerFlowSepratedTraffic;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;
import simulator.utils.Utils;

public class PDQScheduler extends Scheduler {
	private PriorityTopology topology;
	private PerFlowSepratedTraffic traffic;
	private double nextDeadlineEvent;
	private double nextNormalEvent;
	
	public PDQScheduler() {
		super();
		name = "pdq";
		traffic = new PerFlowSepratedTraffic();
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
		if (traffic.deadlineFlows.isEmpty()) {
			nextDeadlineEvent = Parameters.INFINITY;
		}
		else {
			Collections.sort(traffic.deadlineFlows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.DEADLINE));
			topology.setRates(traffic.deadlineFlows);
			nextDeadlineEvent = Simulator.getTime() + traffic.deadlineFlows.peek().getExpectedCompletionTime();
		}
		
		if (traffic.normalFlows.isEmpty()) {
			nextNormalEvent = Parameters.INFINITY;
		}
		else {
			Collections.sort(traffic.normalFlows, new Flow.FlowComparator(FlowComparator.FLOW_COMPARATOR_TYPE.REMAINING_SIZE));
			topology.setRates(traffic.normalFlows);
			nextNormalEvent = Simulator.getTime() + traffic.normalFlows.peek().getExpectedCompletionTime();
//			System.out.println(nextNormalEvent);
//			System.out.println(traffic.normalFlows.peek().id);
//			System.out.println(traffic.normalFlows.peek().dst);
//			System.out.println(traffic.normalFlows.peek().task.type.id);
//			System.out.println(traffic.normalFlows.peek().getReaminingSize());
//			for (int i = 0; i < traffic.normalFlows.size(); i++) {
//				System.out.println(traffic.normalFlows.get(i).task.type.id);
//			}
//			System.out.println(traffic.normalFlows.peek().getRate());
//			System.out.println("\n");
			
		}
	}
	
	protected void setNextEvent() {
		nextEvent = Utils.min(nextDeadlineEvent, nextNormalEvent);
	}
}