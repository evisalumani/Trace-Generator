import com.google.gson.annotations.SerializedName;

// A trace represents an end-to-end collection of calls starting from the initial request on the first service to all other downstream services
public class Trace {
    @SerializedName("id")
    public String traceId;
    @SerializedName("root")
    public Call root; // root request call in the trace

    public Trace(String traceId) {
        this.traceId = traceId;
    }
}