package Messages;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VectorClock {
    private final Map<String, Integer> clock;

    public VectorClock(Set<String> participants, String peer) {
        clock = new HashMap<>();

        // Put itself
        clock.put(peer, 0);

        // Put participants
        for (String participant : participants) {
            clock.put(participant, 0);
        }

    }

    public VectorClock(String vectorClock) {
        this.clock = new HashMap<>();

        vectorClock = vectorClock.substring(1, vectorClock.length()-1); // remove the curly braces at the beginning and end
        String[] pairs = vectorClock.split(", "); // split the string into key-value pairs

        for (String pair : pairs) {
            String[] parts = pair.split("="); // split the pair into key and value
            String key = parts[0];
            Integer value = Integer.parseInt(parts[1]);
            this.clock.put(key, value); // add the key-value pair to the map
        }

    }

    public void increment(String participant) {
        this.clock.put(participant, this.clock.get(participant) + 1);
    }

    public Map<String, Integer> getClock() {
        return clock;
    }

    @Override
    public String toString() {
        return this.clock.toString();
    }

}
