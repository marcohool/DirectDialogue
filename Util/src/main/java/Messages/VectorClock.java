package Messages;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock {
    private final Map<String, Integer> clock;

    public VectorClock(Set<String> participants) {
        clock = new HashMap<>();
        for (String participant : participants) {
            clock.put(participant, 0);
        }
    }

    public VectorClock(String vectorClock) {
        this.clock = new HashMap<>();
        System.out.println("got clock " + vectorClock);
    }

    @Override
    public String toString() {
        return this.clock.toString();
    }

}
