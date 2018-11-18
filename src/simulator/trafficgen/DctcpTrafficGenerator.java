package simulator.trafficgen;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import simulator.simulators.Simulator;
import simulator.traffic.Task;
import simulator.utils.Parameters;
import simulator.utils.RunTime;

public class DctcpTrafficGenerator extends TrafficGenerator {
	private static final int QUERY_SIZE = 20480;
	public static enum TASK_TYPE {
		QUERY,
		BACK,
	}
	
	private class TaskMeta {
		private TASK_TYPE type;
		private double startTime;
		private int src;
		private int dst;
		private double size;
		
		public TaskMeta(TASK_TYPE type, double startTime, int src, int dst, double size) {
			this.type = type;
			this.startTime = startTime;
			this.src = src;
			this.dst = dst;
			this.size = size;
		}
	}
	
	public static class TaskMetaComparator implements Comparator<LinkedList<TaskMeta>> {
		public int compare(LinkedList<TaskMeta> list1, LinkedList<TaskMeta> list2) {
			return list1.peek().startTime > list2.peek().startTime ? 1 : (list1.peek().startTime == list2.peek().startTime ? 0 : -1);
		}
	}
	
	private PriorityQueue<LinkedList<TaskMeta>> tasks;
	
	public DctcpTrafficGenerator() {
		super();
		RunTime runtime = new RunTime();
		tasks = new PriorityQueue<LinkedList<TaskMeta>>(Simulator.config.numOfHosts * 2, new TaskMetaComparator());
		
		for (int i = 0; i < Simulator.config.numOfHosts; i++) {
			//LinkedList<Task> queryTasks = new LinkedList<Task>();
			//LinkedList<Task> backTasks = new LinkedList<Task>();
			LinkedList<TaskMeta> queryTasks = new LinkedList<TaskMeta>();
			LinkedList<TaskMeta> backTasks = new LinkedList<TaskMeta>();
			try {
				BufferedReader bufferedReader = new BufferedReader(new FileReader(Simulator.config.trafficPath + String.valueOf(i) + ".query"));
				String s;
				double time = 0;
				while ((s = bufferedReader.readLine()) != null) {
					String[] args = s.split(" ");
					time += new Double(args[0]);
					queryTasks.add(new TaskMeta(TASK_TYPE.QUERY, time, new Integer(args[1]), i, 0));
				}
				bufferedReader.close();
				bufferedReader = new BufferedReader(new FileReader(Simulator.config.trafficPath + String.valueOf(i) + ".back"));
				time = 0;
				while ((s = bufferedReader.readLine()) != null) {
					String[] args = s.split(" ");
					time += new Double(args[0]);
					backTasks.add(new TaskMeta(TASK_TYPE.BACK, time, i, new Integer(args[1]), new Double(args[2])));
				}
				bufferedReader.close();
			} catch (IOException e) {
				Simulator.output.error("dctcp traffic reading exception", e);
			}
			if (!queryTasks.isEmpty()) {
				tasks.add(queryTasks);
			}
			if (!backTasks.isEmpty()) {
				tasks.add(backTasks);
			}
		}
		
		setNextEvent();
		System.out.println("traffic load duration of DCTCP traffic generator is " + runtime.getRunTime("ms-us"));
	}
	
	protected void setTaskTypes() {
		taskTypes = new String[2];
		taskTypes[0] = "query";
		taskTypes[1] = "back";
		setTaskTypes(taskTypes);
	}
	
	protected void setNextEvent() {
		if (!tasks.isEmpty()) {
			nextEvent = tasks.peek().peek().startTime;
		}
		else {
			nextEvent = Parameters.INFINITY;
		}
	}
	
	protected Task getNextTask() {
		LinkedList<TaskMeta> hostTasks = tasks.poll();
		TaskMeta taskMeta = hostTasks.poll();
		Task task;
		double deadline = Parameters.INFINITY;
		
		if (taskMeta.type == TASK_TYPE.QUERY) {
			task = new Task(taskTypes[0], taskMeta.startTime, taskMeta.startTime, deadline);
			int taskSrc = new Integer(taskMeta.src);
			if (taskSrc > 0) {
				for (int dst = taskSrc * Simulator.config.childrenPerNode[0]; dst < (taskSrc + 1) * Simulator.config.childrenPerNode[0]; dst++) {
					task.addFlow(taskMeta.startTime, dst, taskMeta.dst, QUERY_SIZE, Simulator.config);
				}
			}
			else {
//				for (int dst = taskMeta.dst / Simulator.config.childrenPerNode[0] * Simulator.config.childrenPerNode[0]; dst < (taskMeta.dst / Simulator.config.childrenPerNode[0] + 1) * Simulator.config.childrenPerNode[0]; dst++) {
				for (int dst = taskMeta.dst / Simulator.config.nodesPerLayer[0] * Simulator.config.nodesPerLayer[0]; dst < (taskMeta.dst / Simulator.config.nodesPerLayer[0] + 1) * Simulator.config.nodesPerLayer[0]; dst++) {
					if (dst != taskMeta.dst) {  // exclude MLA
						task.addFlow(taskMeta.startTime, dst, taskMeta.dst, QUERY_SIZE, Simulator.config);
						//System.out.println("new query, src: "+dst+" dst: "+task.hostId+" task id: "+nextTask.id);
					}
				}
			}
		}
		else {
			task = new Task(taskTypes[1], taskMeta.startTime, taskMeta.startTime, deadline);
			task.addFlow(taskMeta.startTime, taskMeta.src, taskMeta.dst, taskMeta.size, Simulator.config);
		}
		if (!hostTasks.isEmpty()) {
			tasks.add(hostTasks);
		}
		return task;
	}
}