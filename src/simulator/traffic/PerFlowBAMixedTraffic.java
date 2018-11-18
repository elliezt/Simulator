package simulator.traffic;

import java.util.Arrays;

import simulator.simulators.Simulator;

public class PerFlowBAMixedTraffic extends PerFlowMixedTraffic {
	private double[][] queueLenSep;
	
	public PerFlowBAMixedTraffic() {
		super();
		queueLenSep = new double[Simulator.config.numOfHosts][];
		for (int i = 0; i < Simulator.config.numOfHosts; i++) {
			queueLenSep[i] = new double[Simulator.config.numOfHosts];
			Arrays.fill(queueLenSep[i], 0);
		}
	}
	
	public void addTask(Task task) {
		flows.addAll(task.getFlows());
		for (Flow flow : task.getFlows()) {
			queueLenSep[flow.src][flow.dst] += flow.size;
		}
	}
	
	public void updateTraffic(double time) {
		updateTraffic(flows, time);
		for (Flow flow : flows) {
			queueLenSep[flow.src][flow.dst] -= flow.getRate() * time;
		}
	}
	
	public double getQueueLenSep(Flow flow) {
		return queueLenSep[flow.src][flow.dst];
	}
}