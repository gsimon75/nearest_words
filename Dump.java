public class Dump {
	public static String indent = "";

	public static void inc() { indent = "  " + indent; }
	public static void dec() { indent = indent.substring(2); }
}
