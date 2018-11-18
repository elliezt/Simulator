package simulator.trafficgen;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.PriorityQueue;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;
import simulator.traffic.Task;
import simulator.traffic.Flow.FlowComparator.FLOW_COMPARATOR_TYPE;
import simulator.utils.Parameters;

public class VarysTaskTrafficGenerator extends TrafficGenerator {
	private static final int NARROW_TASK_THRES = 50;
		
	private LinkedList<TaskMeta> tasks;
//	private PriorityQueue<FlowMeta> flows;
		
	private class FlowMeta{
		private int src;
		private int dst;
		private double size;
		private double awareTime;
		
		public FlowMeta(int src, int dst, double size, double awareTime) {
			this.src = src;
			this.dst = dst;
			this.size = size;
			this.awareTime = awareTime;
		}
	}
	
	private class FlowMetaComparator implements Comparator<FlowMeta> {
		public int compare(FlowMeta flow1, FlowMeta flow2) {
			return flow1.awareTime > flow2.awareTime ? 1 : (flow1.awareTime == flow2.awareTime ? 0 : -1);
		}
	}
	private class TaskMeta {
		private double awareTime;
		private double availableTime;
		private double deadline;
		private double length;
		private int width;
		private ArrayList<FlowMeta> flows;
		
		public TaskMeta(double awareTime, double availableTime, double deadline, int numOfFlows) {
			this.awareTime = awareTime;
			this.availableTime = availableTime;
			this.deadline = deadline;
			length = 0;
			width = numOfFlows;
			flows = new ArrayList<FlowMeta>(numOfFlows);
		}
		
		public void addFlow(FlowMeta flow) {
			flows.add(flow);
			if (flow.size > length)
				length = flow.size;
		}
	}
	
	public VarysTaskTrafficGenerator() {
		super();
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(Simulator.config.trafficPath+"traffic"));
			int numOfTasks = new Integer(bufferedReader.readLine());
			tasks = new LinkedList<TaskMeta>();
//			flows = new PriorityQueue<FlowMeta>(new FlowMetaComparator());
			bufferedReader.readLine();
			for (int i = 0; i < numOfTasks; i++) {
				String[] timeInfo = bufferedReader.readLine().split(" ");
				int numOfFlows = new Integer(bufferedReader.readLine());
				if (numOfFlows != 0) {
					TaskMeta task = new TaskMeta(new Double(timeInfo[0]), new Double(timeInfo[1]), new Double(timeInfo[2]), numOfFlows);
					for (int j = 0; j < numOfFlows; j++) {
						String flowInfo[] = bufferedReader.readLine().split(" ");
						task.addFlow(new FlowMeta(new Integer(flowInfo[0]), new Integer(flowInfo[1]), new Double(flowInfo[2]), new Double(flowInfo[3])));
//						System.out.println(flowInfo[2]);
					}
					tasks.add(task);
//					flows.addAll(task.flows);
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
	
	private static final double SHORT_TASK_THRES = 5 * 1024 *1024;

	public void setName() {
		name = "varys-task";
	}
	
	protected Task getNextTask() {
		TaskMeta taskMeta = tasks.poll();
		Task task;
		if (taskMeta.deadline == 0) {
			taskMeta.deadline = Parameters.INFINITY;
		}
		if (taskMeta.width <= NARROW_TASK_THRES) {
			if (taskMeta.length <= SHORT_TASK_THRES) {
				task = new Task("SN", taskMeta.awareTime, taskMeta.availableTime, taskMeta.deadline);
			}
			else {
				task = new Task("LN", taskMeta.awareTime, taskMeta.availableTime, taskMeta.deadline);
			}
		}
		else {
			if (taskMeta.length <= SHORT_TASK_THRES) {
				task = new Task("SW", taskMeta.awareTime, taskMeta.availableTime, taskMeta.deadline);
			}
			else {
				task = new Task("LW", taskMeta.awareTime, taskMeta.availableTime, taskMeta.deadline);
			}
		}
		for (FlowMeta flow : taskMeta.flows) {
			task.addFlow(flow.awareTime, flow.src, flow.dst, flow.size, Simulator.config);
		}
		return task;
	}
	
	protected void setNextEvent() {
		if (!tasks.isEmpty()) {
			if (predictionEnable) {
				nextEvent = tasks.peek().awareTime;
//				nextEvent = flows.peek().awareTime;
//				if (nextEvent == tasks.peek().awareTime) {
//					trafficEventType = TRAFFIC_EVENT_TYPE.TASK_ARRIVAL;
//				}
//				else {
//					trafficEventType = TRAFFIC_EVENT_TYPE.FLOW_ARRIVAL;
//				}
			}
			else {
				nextEvent = tasks.peek().availableTime;
//				nextEvent = flows.peek().awareTime;
//				if (nextEvent == tasks.peek().awareTime) {
//					trafficEventType = TRAFFIC_EVENT_TYPE.TASK_ARRIVAL;
//				}
//				else {
//					trafficEventType = TRAFFIC_EVENT_TYPE.FLOW_ARRIVAL;
//				}
			}
		}
		else {
			nextEvent = Parameters.INFINITY;
		}
	}
	
	protected void setTaskTypes() {
		taskTypes = new String[4];
		taskTypes[0] = "SN";
		taskTypes[1] = "LN";
		taskTypes[2] = "SW";
		taskTypes[3] = "LW";
		setTaskTypes(taskTypes);
	}
}