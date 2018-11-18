package simulator.traffic;

import java.util.LinkedList;

import simulator.simulators.Simulator;

public class PerFlowMixedTraffic extends PerFlowTraffic {
	public LinkedList<Flow> flows;
	
	public PerFlowMixedTraffic() {
		flows = new LinkedList<Flow>();
	}
	
	public void addTask(Task task) {
		flows.addAll(task.getFlows());
	}
	
	public void updateTraffic(double time) {
		updateTraffic(flows, time);
	}
	
	public LinkedList<Flow> getFlows() {
		if (flows == null) {
			return new LinkedList<Flow>();
		}
		return new LinkedList<Flow>(flows);
	}
	
	public double[] getQueueLen() {
		double[] queueLen = new double[Simulator.config.numOfHosts];
		for (Flow flow : flows) {
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