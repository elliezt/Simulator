package simulator.utils;

public class Utils {
	public static double min (double num1, double num2) {
		return num1 < num2 ? num1 : num2;
	}
	
	public static double checkRate(double rate) {
		if (rate < 0) {
			if (rate > -Parameters.RATE_GRANULARITY) {
				rate = 0;
			}
			else {
				return -1;
			}
		}
		return rate;
	}
}