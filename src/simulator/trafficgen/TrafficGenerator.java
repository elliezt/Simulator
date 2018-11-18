package simulator.trafficgen;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;
import simulator.traffic.Task;
import simulator.traffic.TaskType;
import simulator.trafficgen.TrafficGenerator.TRAFFIC_EVENT_TYPE;
import simulator.utils.Parameters;

public abstract class TrafficGenerator {
	public enum TRAFFIC_EVENT_TYPE {
		TASK_ARRIVAL,
		FLOW_ARRIVAL
	} 
	public String name;
	public double nextEvent;
	protected String[] taskTypes;
	protected boolean predictionEnable = false;
	public TRAFFIC_EVENT_TYPE trafficEventType;
	
	public TrafficGenerator() {
		nextEvent = Parameters.INFINITY;
		setTaskTypes();
		String[] path = Simulator.config.trafficPath.split("/");
		name = path[path.length-1];
	}
	
	public final Task nextTask() {
		Task task = getNextTask();
		setNextEvent();
		return task;
	}
	
	public final void setPredictionEnable(boolean predictionEnable) {
		this.predictionEnable = predictionEnable;
	}
	
	protected abstract void setTaskTypes();
	
	protected abstract Task getNextTask();
	
	protected abstract void setNextEvent();
	
	protected final void setTaskTypes(String[] types) {
		TaskType.types = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			TaskType.types[i] = types[i];
		}
	}
}