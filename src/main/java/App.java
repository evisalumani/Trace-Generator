public class App {
    private static String LOG_INPUT_FILE;
    private static String TRACE_OUTPUT_FILE;

    // main requires 2 arguments for the input and output files
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("Incorrect arguments specified");
            return;
        }
        LOG_INPUT_FILE = args[0];
        TRACE_OUTPUT_FILE = args[1];

        System.out.println("Starting program...");
        TraceGenerator.generateTraces(LOG_INPUT_FILE, TRACE_OUTPUT_FILE);
    }
}