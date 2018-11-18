package simulator.trafficgen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

import simulator.simulators.Simulator;
import simulator.traffic.Task;
import simulator.utils.Parameters;

public class SimpleTaskTrafficGenerator extends TrafficGenerator {
	private LinkedList<Task> tasks;
	
	public SimpleTaskTrafficGenerator() {
		super();
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(Simulator.config.trafficPath+"traffic"));
			int numOfTasks = new Integer(bufferedReader.readLine());
			tasks = new LinkedList<Task>();
			bufferedReader.readLine();
			for (int i = 0; i < numOfTasks; i++) {
				String[] timeInfos = bufferedReader.readLine().split(" ");
				double awareTime = new Double(timeInfos[0]);
				double availableTime = new Double(timeInfos[1]);
				double deadline = new Double(timeInfos[2]);
				if (deadline == 0) {
					deadline = Parameters.INFINITY;
				}
				int numOfFlows = new Integer(bufferedReader.readLine());
				if (numOfFlows != 0) {
					Task task = new Task("normal", awareTime, availableTime, deadline);
					for (int j = 0; j < numOfFlows; j++) {
						String flowInfos[] = bufferedReader.readLine().split(" ");
						task.addFlow(new Double(flowInfos[3]), new Integer(flowInfos[0]), new Integer(flowInfos[1]), new Double(flowInfos[2]), Simulator.config);
					}
					tasks.add(task);
				}
				bufferedReader.readLine();
			}
			bufferedReader.close();
			setNextEvent();
		} catch (FileNotFoundException e) {
			Simulator.output.error("traffic file not found!!!", e);
		} catch (IOException e) {
			Simulator.output.error("traffic file io error!!!", e);
		}
	}
	
	public void setName() {
		name = "simple-task";
	}
	
	protected Task getNextTask() {
		Task task = tasks.poll();
		return task;
	}
	
	protected void setNextEvent() {
		if (!tasks.isEmpty()) {
			if (predictionEnable) {
				nextEvent = tasks.peek().awareTime;
			}
			else {
				nextEvent = tasks.peek().availableTime;
			}
		}
		else {
			nextEvent = Parameters.INFINITY;
		}
	}
	
	protected void setTaskTypes() {
		String[] taskTypes = new String[1];
		taskTypes[0] = "normal";
		setTaskTypes(taskTypes);
	}
}