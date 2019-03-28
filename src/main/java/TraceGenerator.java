import com.google.gson.*;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TraceGenerator {
    // Variables needed for statistics
    private static long TIME_STARTED;
    private static long TIME_LOGS_READ;
    private static long TIME_TRACES_GENERATED;
    private static long TIME_TRACES_WRITTEN;
    private static long DURATION_READING;
    private static long DURATION_GENERATING;
    private static long DURATION_WRITING;

    // A map between a traceId and a list of corresponding service calls (spans)
    private static Map<String, LinkedList<Call>> map = new ConcurrentHashMap<>();
    // A list of json traces generated for reach trace
    private static List<String> jsonTraces = new ArrayList<String>();

    private static void appendJsonTraces(String jsonTrace) {
        // Access to shared jsonTraces list must be synchronized among threads
        synchronized(jsonTraces) {
            jsonTraces.add(jsonTrace);
        }
    }

    public static void generateTraces(String inputFile, String outputFile) {
        TIME_STARTED = System.currentTimeMillis();

        readInputLogs(inputFile);

        TIME_LOGS_READ = System.currentTimeMillis();
        DURATION_READING = TIME_LOGS_READ - TIME_STARTED;

        buildTraceTrees(map);

        TIME_TRACES_GENERATED = System.currentTimeMillis();
        DURATION_GENERATING = TIME_TRACES_GENERATED - TIME_LOGS_READ;

        writeOutputTraces(outputFile);

        TIME_TRACES_WRITTEN = System.currentTimeMillis();
        DURATION_WRITING = TIME_TRACES_WRITTEN - TIME_TRACES_GENERATED;

        // Print statistics
        System.out.println("Duration reading input: " + DURATION_READING + " ms\n" +
                        "Duration generating traces: " + DURATION_GENERATING + " ms\n" +
                        "Duration writing output: " + DURATION_WRITING + " ms\n" +
                        "Traces generated: " + jsonTraces.size());
    }

    // Read logs from file
    private static void readInputLogs(String inputFile) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(inputFile));
            scanner.useDelimiter("\n");

            while (scanner.hasNext()) {
                // Process single line
                String token = scanner.next();
                String[] parts = token.split(" |->"); // Regex OR operator to split on whitespace or "->"

                // 6 parts of information are required per line: startTimestamp, endTimestamp, traceId, serviceName, callerSpan, span
                if (parts == null || parts.length != 6) {
                    // Line was malformed, either no split occurred or not enough data per trace => Ignore
                    continue;
                }

                try {
                    DateTime startTimestamp = DateTime.parse(parts[0]);
                    DateTime endTimestamp = DateTime.parse(parts[1]);
                    String traceId = parts[2];
                    String serviceName = parts[3];
                    String callerSpan = parts[4];
                    String span = parts[5];

                    Call newCall = new Call(serviceName, startTimestamp, endTimestamp, callerSpan, span);

                    // Update the map
                    if (!map.containsKey(traceId)) {
                        map.put(traceId, new LinkedList<>(Arrays.asList(newCall)));
                    } else {
                        LinkedList<Call> callsList = map.get(traceId);
                        if (callsList == null) callsList = new LinkedList<Call>();

                        // Root span is appended as 1st element of the list
                        if (callerSpan.equals("null")) {
                            newCall.isRoot = true;
                            callsList.addFirst(newCall);
                        } else {
                            callsList.add(newCall);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }

            } // End of looping through file contents
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (scanner!=null) scanner.close();
        }
    }

    private static void buildTraceTrees(Map<String, LinkedList<Call>> map) {
        // Parallel processing of the map's entries
        // Each entry (traceId, list of calls) is processed in a separate thread
        map.entrySet()
                .parallelStream()
                .forEach(buildOneTraceTree);
    }

    private static Consumer<Map.Entry<String, LinkedList<Call>>> buildOneTraceTree = (Map.Entry<String, LinkedList<Call>> entry) -> {
        String traceId = entry.getKey();
        LinkedList<Call> calls = entry.getValue();

        Call rootCall = calls.poll(); // rootCall is the first element on the list of calls
        rootCall.isVisited = true;
        LinkedList<Call> queue = new LinkedList<Call>(); // queue is used to keep track of visited nodes (i.e. calls)
        queue.add(rootCall);

        // Create the Trace object and assign its root Call
        Trace trace = new Trace(traceId);
        trace.root = rootCall;

        // Breadth-first search for building the tree of Calls for the current Trace
        while (queue.size() != 0) {
            Call sourceCall = queue.poll();
            calls.stream() // Calls are searched for sequentially
                    // Search for calls that are not yet visited and are called by the current (parent) span
                    .filter((Call call) -> call.callerSpan.equals(sourceCall.span) && !call.isVisited)
                    // Sort (children) calls by their startTimestamp
                    .sorted(Comparator.comparing(Call::getStartTimestamp))
                    .forEach((Call call) -> {
                        call.isVisited = true; // Mark call as visited
                        sourceCall.calls.add(call); // Add call as a child of current (parent) span
                        queue.add(call); // Add call to the queue (in order to search next for its children)
                    });
        }

        String jsonTrace = generateJsonForTrace(trace);
        appendJsonTraces(jsonTrace);
    };

    private static String generateJsonForTrace(Trace trace) {
        // Custom gson for serializing dates
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(DateTime.class, new JsonSerializer<DateTime>(){
                    @Override
                    public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(ISODateTimeFormat.dateTime().print(src));
                    }
                })
                .create();

        String jsonTrace = gson.toJson(trace) + "\n"; // Append a newline
        return jsonTrace;
    }

    private static void writeOutputTraces(String outputFile) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,false));
            jsonTraces.stream() // Json traces are written to file sequentially
                    .forEach((s) -> {
                        try {
                            writer.write(s);
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    });

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}