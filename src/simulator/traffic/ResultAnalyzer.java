package simulator.traffic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import simulator.simulators.Simulator;
import simulator.utils.Parameters;
import simulator.utils.RunTime;

public class ResultAnalyzer {
	private ArrayList<ArrayList<Result>> results;
	
	public class Result {
		public double completionTime;
		
		public Result(Flow flow) {
			completionTime = flow.getCompletionTime();
		}
		
		public Result(Task task) {
			completionTime = task.getCompletionTime();
		}
	}
	
	public static class ResultComparator implements Comparator<Result> {
		public int compare(Result r1, Result r2) {
			return r1.completionTime > r2.completionTime ? 1 : (r1.completionTime == r2.completionTime ? 0 : -1);
		}
	}
	
	public ResultAnalyzer() {
		results = new ArrayList<ArrayList<Result>>();
		for (int i = 0; i < TaskType.types.length; i++) {
			results.add(new ArrayList<Result>());
		}
	}
	
	public void flowCompletion(Flow flow) {
		results.get(flow.task.type.id).add(new Result(flow));
	}
	
	public void taskCompletion(Task task) {
		results.get(task.type.id).add(new Result(task));
	}
	
	public void flowTermination(Flow flow) {
		//TODO
	}
	
	public void taskTermination(Task task) {
		//TODO
	}
	
	public void analyze() {
		RunTime runtime = new RunTime();
		int size = 0;
		for (int i = 0; i < TaskType.types.length; i++) {
			if (!results.get(i).isEmpty()) {
				analyzeResult(results.get(i), TaskType.types[i]);
			}
			size += results.get(i).size();
		}
		ArrayList<Result> allResults = new ArrayList<Result>(size);
		for (ArrayList<Result> resultsElement : results) {
			allResults.addAll(resultsElement);
		}
		analyzeResult(allResults, "all");
		System.out.println("result analyze duration is " + runtime.getRunTime("ms-us"));
	}
	
	private void analyzeResult(ArrayList<Result> results, String type) {
		double completionTimes = 0;
		Collections.sort(results, new ResultComparator());
		for (Result result : results) {
			completionTimes += result.completionTime;
		}
		double percentage1 = results.get((int)Math.ceil((double)results.size() * 0.01) - 1).completionTime;
		double percentage5 = results.get((int)Math.ceil((double)results.size() * 0.05) - 1).completionTime;
		double percentage10 = results.get((int)Math.ceil((double)results.size() * 0.1) - 1).completionTime;
		double percentage15 = results.get((int)Math.ceil((double)results.size() * 0.15) - 1).completionTime;
		double percentage20 = results.get((int)Math.ceil((double)results.size() * 0.2) - 1).completionTime;
		double percentage25 = results.get((int)Math.ceil((double)results.size() * 0.25) - 1).completionTime;
		double percentage30 = results.get((int)Math.ceil((double)results.size() * 0.3) - 1).completionTime;
		double percentage35 = results.get((int)Math.ceil((double)results.size() * 0.35) - 1).completionTime;
		double percentage40 = results.get((int)Math.ceil((double)results.size() * 0.4) - 1).completionTime;
		double percentage45 = results.get((int)Math.ceil((double)results.size() * 0.45) - 1).completionTime;
		double percentage50 = results.get((int)Math.ceil((double)results.size() * 0.5) - 1).completionTime;
		double percentage55 = results.get((int)Math.ceil((double)results.size() * 0.55) - 1).completionTime;
		double percentage60 = results.get((int)Math.ceil((double)results.size() * 0.6) - 1).completionTime;
		double percentage65 = results.get((int)Math.ceil((double)results.size() * 0.65) - 1).completionTime;
		double percentage70 = results.get((int)Math.ceil((double)results.size() * 0.7) - 1).completionTime;
		double percentage75 = results.get((int)Math.ceil((double)results.size() * 0.75) - 1).completionTime;
		double percentage80 = results.get((int)Math.ceil((double)results.size() * 0.8) - 1).completionTime;
		double percentage85 = results.get((int)Math.ceil((double)results.size() * 0.85) - 1).completionTime;
		double percentage90 = results.get((int)Math.ceil((double)results.size() * 0.9) - 1).completionTime;
		double percentage95 = results.get((int)Math.ceil((double)results.size() * 0.95) - 1).completionTime;
		double percentage99 = results.get((int)Math.ceil((double)results.size() * 0.99) - 1).completionTime;
		double percentage999 = results.get((int)Math.ceil((double)results.size() * 0.999) - 1).completionTime;
		double percentage9999 = results.get((int)Math.ceil((double)results.size() * 0.9999) - 1).completionTime;
		completionTimes /= (double)results.size();
		if (!Parameters.COMPUTE_TCT) {
			Simulator.output.output(type + " flow completion time : average: " + completionTimes + " 95th: " + percentage95
					+ " 99th: " + percentage99 + " 999th: " + percentage999 + " 9999th: " + percentage9999);
		}
		else {
			Simulator.output.output(type + " task completion time : average: " + completionTimes + " 1th: " + percentage1 + " 5th: " + percentage5 + " 10th: " + percentage10 + " 15th: " + percentage15 + " 20th: " + percentage20 + " 25th: " + percentage25 + " 30th: " + percentage30 + " 35th: " + percentage35 + " 40th: " + percentage40 + " 45th: " + percentage45 + " 50th: " + percentage50 + " 55th: " + percentage55 + " 60th: " + percentage60 + " 65th: " + percentage65 + " 70th: " + percentage70 + " 75th: " + percentage75 + " 80th: " + percentage80 + " 85th: " + percentage85 + " 90th: " + percentage90 + " 95th: " + percentage95
					+ " 99th: " + percentage99 + " 999th: " + percentage999 + " 9999th: " + percentage9999);
		}
	}
}