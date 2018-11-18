package simulator.utils;

public class RunTime {
	private long startTime;
	
	public RunTime() {
		startTime = System.nanoTime();
	}
	
	public String getRunTime(String form) {
		long runTime = System.nanoTime() - startTime;
		String s = "";
		String[] forms = form.split("-");
		int i = 0;
		if (forms[i].equalsIgnoreCase("s")) {
			s += runTime / 1000000000L + " s ";
			runTime %= 1000000000L;
			i++;
		}
		if (i < forms.length && forms[i].equalsIgnoreCase("ms")) {
			s += runTime / 1000000L + " ms ";
			runTime %= 1000000L;
			i++;
		}
		if (i < forms.length && forms[i].equalsIgnoreCase("us")) {
			s += runTime / 1000L + " us ";
			runTime %= 1000L;
			i++;
		}
		if (i < forms.length && forms[i].equalsIgnoreCase("ns")) {
			s += runTime + " ns ";
			i++;
		}
		if (i != forms.length) {
			return "wrong time format";
		}
		return s;
	}
}