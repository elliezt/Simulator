package simulator.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import simulator.simulators.Simulator;
import simulator.traffic.Flow;
import simulator.traffic.Task;
import simulator.traffic.TaskType;

public class Output {
	private BufferedWriter logWriter;
	private ArrayList<BufferedWriter> completionWriters;
	private ArrayList<BufferedWriter> terminationWriters;
	private BufferedWriter queueWriter;
	private BufferedWriter rateWriter;
	
	public Output(String path, String trafficName, String schedulerName) {
		completionWriters = new ArrayList<BufferedWriter>();
		terminationWriters = new ArrayList<BufferedWriter>();
		path += trafficName + "/";
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
		path += schedulerName + "-";
		try {
			logWriter = new BufferedWriter(new FileWriter(path+"results.log"));
			for (int i = 0; i < TaskType.types.length; i++) {
				if (Parameters.REC_COMP) {
					completionWriters.add(new BufferedWriter(new FileWriter(path + TaskType.types[i] + "-" + "ct.log")));
				}
				if(Parameters.REC_TERM) {
					terminationWriters.add(new BufferedWriter(new FileWriter(path + TaskType.types[i] + "-" + "termination.log")));
				}
			}
			if (Parameters.REC_QUEUE_LEN) {
				queueWriter = new BufferedWriter(new FileWriter(path + "queue.log"));
			}
			if (Parameters.REC_FLOW_RATE) {
				rateWriter = new BufferedWriter(new FileWriter(path + "rate.log"));
			}
		} catch (IOException e) {
			error("create output file error", e);
		}
	}
	
	public void closeAll() {
		if (Parameters.OUTPUT_TO_FILE) {
			try {
				logWriter.close();
				if (!completionWriters.isEmpty()) {
					for (int i = 0; i < TaskType.types.length; i++) {
						completionWriters.get(i).flush();  // TODO
					}
				}
				if (!terminationWriters.isEmpty()) {
					for (int i = 0; i < TaskType.types.length; i++) {
						terminationWriters.get(i).flush();
					}
				}
				if (Parameters.REC_QUEUE_LEN) {
					queueWriter.close();
				}
				if (Parameters.REC_FLOW_RATE) {
					rateWriter.close();
				}
			} catch (IOException e) {
				error("flush log file exception", e);
			}
		}
	}
	
	public void output(String s) {
		if (Parameters.OUTPUT_TO_FILE) {
			try {
				logWriter.write(s+"\n");
			} catch (IOException e) {
				error("write to log file exception", e);
			}
		}
		else {
			System.out.println(s);
		}
	}
	
	public void completion(int typeId, String s) {
		if (Parameters.REC_COMP) {
			try {
				completionWriters.get(typeId).write(s+"\n");
			} catch (IOException e) {
				error("write to completion time file exception", e);
			}
		}
	}
	
	public void termination(int typeId, String s) {
		if (Parameters.REC_TERM) {
			try {
				terminationWriters.get(typeId).write(s+"\n");
			} catch (IOException e) {
				error("write to termination file exception", e);
			}
		}
	}
	
	public void recQueueLen(double time, double[] queueLen) {
		try {
			queueWriter.write("" + time);
			for (int i = 0; i < queueLen.length; i++) {
				queueWriter.write(" " + queueLen[i] / 8192);  // queue length in KB
			}
			queueWriter.write("\n");
			//queueWriter.flush();
		} catch (IOException e) {
			
		}
	}
	
	public void recFlowRate(double time, double[] flowRates) {
		try {
			rateWriter.write("" + time);
			for (int i = 0; i < flowRates.length; i++) {
				rateWriter.write(" " + flowRates[i] / 8192);  // queue length in KB
			}
			rateWriter.write("\n");
			//rateWriter.flush();
		} catch (IOException e) {
			
		}
	}
	
	public void error(String s) {
		System.out.println(s);
		System.exit(0);
	}
	
	public void error(String s, Exception e) {
		System.out.println(s);
		e.printStackTrace();
		System.exit(0);
	}
	
	public void printFlow(Flow flow) {
		System.out.println("Flow id: "+flow.id+" time: "+Simulator.getTime()+" rate: "+flow.getRate());
	}
	
	public void printTask(Task task) {
		System.out.println("Task id: "+task.id+" time: "+Simulator.getTime());
		for (Flow flow : task.getFlows()) {
			System.out.println("Flow id: "+flow.id+" rate: "+flow.getRate());
		}
	}
}