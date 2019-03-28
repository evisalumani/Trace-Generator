import com.google.gson.annotations.SerializedName;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

// A call represents a single request from a service to another, as part of a trace.
// TODO: Make fields private
public class Call {
    @SerializedName("start")
    public DateTime startTimestamp;
    @SerializedName("end")
    public DateTime endTimestamp;
    @SerializedName("service")
    public String serviceName;
    public transient String callerSpan; // parent span
    @SerializedName("span")
    public String span; // span generated in the current call
    public transient boolean isRoot;
    public transient boolean isVisited; // needed only during search
    @SerializedName("calls")
    public List<Call> calls; // composite pattern

    public Call(String serviceName, DateTime startTimestamp, DateTime endTimestamp, String callerSpan, String span) {
        super();
        this.serviceName = serviceName;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.callerSpan = callerSpan;
        this.span = span;
        this.calls = new ArrayList<>();
    }

    public DateTime getStartTimestamp() {
        return startTimestamp;
    }

    @Override
    public String toString() {
        return "Call [serviceName=" + serviceName + ", startTimestamp=" + startTimestamp + ", endTimestamp="
                + endTimestamp + ", callerSpan=" + callerSpan + ", span=" + span + "]";
    }
}