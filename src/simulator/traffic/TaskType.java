package simulator.traffic;

import simulator.simulators.Simulator;

public class TaskType {
	public static String[] types;
	public int id;
	
	public TaskType(String name) {
		this.id = getTypeId(name);
	}
	
	private int getTypeId(String name) {
		for (int i = 0; i < types.length; i++) {
			if (name.equalsIgnoreCase(types[i])) {
				return i;
			}
		}
		Simulator.output.error("task type not found error");
		return -1;
	}
}