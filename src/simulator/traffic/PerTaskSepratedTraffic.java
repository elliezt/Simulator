package simulator.traffic;

import java.util.LinkedList;

import simulator.simulators.Simulator;

public class PerTaskSepratedTraffic extends PerTaskTraffic {
	public LinkedList<Task> deadlineTasks;
	public LinkedList<Task> normalTasks;
	
	public PerTaskSepratedTraffic() {
		deadlineTasks = new LinkedList<Task>();
		normalTasks = new LinkedList<Task>();
	}
	
	public void addTask(Task task) {
		if (task.hasDeadline) {
			deadlineTasks.add(task);
		}
		else {
			normalTasks.add(task);
		}
	}
	
	public void updateTraffic(double time) {
		updateTraffic(deadlineTasks, time);
		updateTraffic(normalTasks, time);
	}
	
	public LinkedList<Flow> getFlows() {
		LinkedList<Flow> flows = new LinkedList<Flow>();
		for (Task task : deadlineTasks) {
			flows.addAll(task.getFlows());
		}
		for (Task task : normalTasks) {
			flows.addAll(task.getFlows());
		}
		return flows;
	}
	
	public double[] getQueueLen() {
		double[] queueLen = new double[Simulator.config.numOfHosts];
		for (Task task : deadlineTasks) {
			for (Flow flow : task.getFlows()) {
				queueLen[flow.src] += flow.getReaminingSize();
			}
		}
		for (Task task : normalTasks) {
			for (Flow flow : task.getFlows()) {
				queueLen[flow.src] += flow.getReaminingSize();
//				queueLen[flow.src] += (flow.getReaminingSize() / 8 / 1500) * 1460 * 8; //normal packet
//				queueLen[flow.src] += (flow.getReaminingSize() / 8 / 1508) * 1448 * 8; //DBA packet
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