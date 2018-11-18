package simulator.main;

import simulator.simulators.Simulator;
import simulator.utils.Config;
import simulator.utils.RunTime;

public class TaskSimulator {
	public static void main(String[] args) {
		//String trafficGenerator = "simple-task";
		//String trafficPaths[] = {"traffics/motivation/"};
		//String trafficPaths[] = {"traffics/m2ms-hosts-24-duration-107200-load-21.6/"};
		String trafficGenerator = "varys-task";
		//String trafficGenerator = args[0];
		String trafficPaths[] = {"traffics/m2ms-hosts-16-12-duration-200-load-192.0/"};
		//String trafficPaths[] = {args[1]};
		//String trafficGenerator = "vl2";
		//String trafficGenerator = args[0];
		//String trafficPaths[] = {"traffics/vl2-hosts-144-duration-100-load-0.9/"};
		//String trafficPaths[] = {args[1]};
		//String trafficGenerator = "varys-task";
		//String trafficPaths[] = {"traffics/varysm2m-hosts-48-duration-300-load-0.8/"};
		//String trafficPaths[] = {"traffics/varysm2m-96-1000-seed-34/varysm2m-hosts-96-duration-1000-load-0.5/"};
		  //String trafficPaths[] = {"traffics/varysm2m-hosts-24-2-duration-200-load-0.4/"};
		//String trafficPaths[] = {"traffics/varysm2m-24-4-1000-seed-34/varysm2m-hosts-24-4-duration-1000-load-1.0/"};
		//String trafficPath = "traffics/varysm2m-24-4-1000-seed-34/varysm2m-hosts-24-4-duration-1000-load-";
		/*String trafficPath = "traffics/varysm2m-96-1000-seed-34/varysm2m-hosts-96-duration-1000-load-";
		int size = 10;
		String[] trafficPaths = new String[size];
		for (int i = 0; i < size; i++) {
			if (i != 9) {
				trafficPaths[i] = trafficPath + "0." + (i * 1 + 1) + "/";
			}
			else {
				trafficPaths[i] = trafficPath + "1.0/";
			}
		}*/
		
		//String[] schedulers = {"pdq", "fair", "flow-fifo", "baraat", "varys", "elastic"};
		//String[] schedulers = {"pdq", "baraat", "varys", "bridge"};
		//String[] schedulers = {"pdq", "baraat", "varys", "bridge-fsize", "bridge-crowd", "bridge-stand"};
		//String[] schedulers = {"baraat", "varys", "bridge-stand"};
		//String[] schedulers = {"balas"};
		String[] schedulers = {"dba-ideal"};
		//String[] schedulers = {args[2]};
		//String[] schedulers = {"baraat"};
		//String[] schedulers = {"varys-sebf-enhance"};
		//String[] schedulers = {"varys-sebf", "varys-sebf-enhance", "varys-size", "varys-size-enhance"};
		//String[] schedulers = {"bridge-tsize", "bridge-width", "bridge-length", "bridge-bot"};
		//String[] schedulers = {"bridge-remain-tsize", "bridge-remain-width", "bridge-remain-length", "bridge-remain-bot"};
		//String[] schedulers = {"bridge-fair", "bridge-pfair"};
		//String[] schedulers = {"bridge-crowd"};
		//String[] schedulers = {"bridge-tsize-enhance"};
		//String[] schedulers = {"bridge-bot-enhance"};
		//String[] schedulers = {"basrpt"};
		//String[] schedulers = {"pdq", "sif1", "sif2", "sif3", "sif4", "sif5"};
		//String[] schedulers = {"sif0", "sif1", "sif2", "sif4", "sif5", "sif6"};
		//String[] schedulers = {"sif12", "sif13", "sif14", "sif15", "sif16"};
		
		RunTime runtime = new RunTime();
		Simulator simulator;
		for (String tp : trafficPaths) {
			Config config = new Config(tp);
			for (String s : schedulers) {
				simulator = new Simulator(s, trafficGenerator, config, "logs/");
				simulator.simulate();
			}
		}
		System.out.println("total run time is " + runtime.getRunTime("s-us"));
	}
}