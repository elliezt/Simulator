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

public class Vl2TrafficGenerator extends TrafficGenerator {
	private class TaskMeta {
		private double startTime;
		private int src;
		private int dst;
		private double size;
		
		public TaskMeta(double startTime, int src, int dst, double size) {
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
	
	public Vl2TrafficGenerator() {
		super();
		RunTime runtime = new RunTime();
		tasks = new PriorityQueue<LinkedList<TaskMeta>>(Simulator.config.numOfHosts, new TaskMetaComparator());
		
		for (int i = 0; i < Simulator.config.numOfHosts; i++) {
			LinkedList<TaskMeta> hostTasks = new LinkedList<TaskMeta>(); 
			try {
				BufferedReader bufferedReader = new BufferedReader(new FileReader(Simulator.config.trafficPath + String.valueOf(i) + ".dm"));
				String s;
				double time = 0;
				while ((s = bufferedReader.readLine()) != null) {
					String[] args = s.split(" ");
					time += new Double(args[0]);
					hostTasks.add(new TaskMeta(time, i, new Integer(args[1]), new Double(args[2])));
				}
				bufferedReader.close();
			} catch (IOException e) {
				Simulator.output.error("vl2 traffic reading exception", e);
			}
			if (!hostTasks.isEmpty()) {
				tasks.add(hostTasks);
			}
		}
		
		setNextEvent();
		System.out.println("traffic load duration of VL2 traffic generator is " + runtime.getRunTime("ms-us"));
	}
	
	protected void setTaskTypes() {
		taskTypes = new String[1];
		taskTypes[0] = "dm";
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
		double deadline = Parameters.INFINITY;
		Task task = new Task("dm", taskMeta.startTime, taskMeta.startTime, deadline);
		task.addFlow(taskMeta.startTime, taskMeta.src, taskMeta.dst, taskMeta.size, Simulator.config);
		if (!hostTasks.isEmpty()) {
			tasks.add(hostTasks);
		}
		return task;
	}
}