package simulator.traffic;

import java.util.LinkedList;

public abstract class Traffic {
	public Traffic() { }
	
	public abstract void addTask(Task task);
	
	public abstract void updateTraffic(double time);
	
	public abstract LinkedList<Flow> getFlows();
	
	public abstract double[] getQueueLen();

	public abstract double[] getFlowRate();
	
}