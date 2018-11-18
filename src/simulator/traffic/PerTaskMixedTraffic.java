package simulator.traffic;

import java.util.LinkedList;

import simulator.simulators.Simulator;

public class PerTaskMixedTraffic extends PerTaskTraffic {
	public LinkedList<Task> tasks;
	
	public PerTaskMixedTraffic() {
		tasks = new LinkedList<Task>();
	}
	
	public void addTask(Task task) {
		tasks.add(task);
	}
	
	public void updateTraffic(double time) {
		updateTraffic(tasks, time);
	}
	
	public LinkedList<Flow> getFlows() {
		LinkedList<Flow> flows = new LinkedList<Flow>();
		for (Task task : tasks) {
			flows.addAll(task.getFlows());
		}
		return flows;
	}
	
	public double[] getQueueLen() {
		double[] queueLen = new double[Simulator.config.numOfHosts];
		for (Task task : tasks) {
			for (Flow flow : task.getFlows()) {
				queueLen[flow.src] += flow.getReaminingSize();
			}
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