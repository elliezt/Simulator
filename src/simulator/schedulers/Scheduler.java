package simulator.schedulers;

import simulator.topology.Topology;
import simulator.traffic.Flow;
//import simulator.topology.LeafSpineTopology;
import simulator.traffic.Task;
import simulator.traffic.Traffic;
import simulator.utils.Parameters;

public abstract class Scheduler {
	public String name;
	public double nextEvent;
	
	public Scheduler() {
		nextEvent = Parameters.INFINITY;
	}
	
	public abstract Traffic getTraffic();
	
	public abstract Topology getTopology();
	
//	public abstract LeafSpineTopology getTopology();
	
	public abstract void addTask(Task task);
		
	public final void updateSchedule() {
//		getTopology().resetRemainBand();
		setNextSchedule();
		setNextEvent();
	}
	
	protected abstract void setNextSchedule();
	
	protected abstract void setNextEvent();
}