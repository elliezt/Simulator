package simulator.traffic;

import java.util.LinkedList;

import simulator.simulators.Simulator;

public class PerFlowSepratedTraffic extends PerFlowTraffic {
	public LinkedList<Flow> deadlineFlows;
	public LinkedList<Flow> normalFlows;
	
	public PerFlowSepratedTraffic() {
		deadlineFlows = new LinkedList<Flow>();
		normalFlows = new LinkedList<Flow>();
	}
	
	public void addTask(Task task) {
		if (task.hasDeadline) {
			deadlineFlows.addAll(task.getFlows());
		}
		else {
			normalFlows.addAll(task.getFlows());
		}
	}
	
	public void updateTraffic(double time) {
		updateTraffic(deadlineFlows, time);
		updateTraffic(normalFlows, time);
	}
	
	public LinkedList<Flow> getFlows() {
		LinkedList<Flow> flows = new LinkedList<Flow>(deadlineFlows);
		flows.addAll(normalFlows);
		return flows;
	}
	
	public double[] getQueueLen() {
		double[] queueLen = new double[Simulator.config.numOfHosts];
		for (Flow flow : deadlineFlows) {
			queueLen[flow.src] += flow.getReaminingSize();
		}
		for (Flow flow : normalFlows) {
			queueLen[flow.src] += flow.getReaminingSize();
		}
		return queueLen;
	}
	public double[] getFlowRate() {
		LinkedList<Flow> flows = getFlows();
		double[] flowRates = new double[flows.size()];
		int i = 0;
		for (Flow flow : flows) {
			flowRates[i] = flow.getRate();
			i++;
		}
		return flowRates;
	}
}