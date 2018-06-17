package org.magcode.daikin;

import java.util.Arrays;
import java.util.List;

public class Constants {
	public final static String PR_TARGETTEMP = "targettemp";
	public final static String PR_OUTTEMP = "otemp";
	public final static String PR_INTEMP = "intemp";
	public final static String PR_SETTABLE = "/$settable";
	public final static String PR_FAN = "fan";
	public final static String PR_FANDIR = "fandirection";
	public final static String PR_MODE = "mode";
	public final static String PR_POWER = "power";

	public final static List<String> PROPERTIES = Arrays.asList(PR_TARGETTEMP, PR_OUTTEMP, PR_INTEMP, PR_FAN, PR_FANDIR,
			PR_MODE, PR_POWER);
}