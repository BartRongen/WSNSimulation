import java.util.ArrayList;
import java.util.Comparator;

public class WSN {

    static long slotLength = 1000 / (12 * 16);

    BaseStation bs;
    Node[] nodes;

    private int frames = 12;
    private int slots = 16;

    private int totalSlots = frames * slots;
    private int secondsPassed = 0;

    public WSN() {
        bs = new BaseStation();
        nodes = new Node[Config.numNodes];
        ArrayList<Node> firstHalf = new ArrayList<>();
        ArrayList<Node> secondHalf = new ArrayList<>();
        for (int i = 0; i < Config.numNodes; i++) {
            ArrayList<Node> half = i < 40 ? firstHalf : secondHalf;
            nodes[i] = new Node(totalSlots, i, half, 2);
            half.add(nodes[i]);
            nodes[i].assignBaseStation(bs);
        }
        bs.assignNodes(nodes);
    }

    public void startSimulation() {
        long time = 0;
        int frame = 0; //current frame, resets when it reaches 12
        int slot = 0; //current slot, resets when it reaches 16

        System.out.println("Starting simulation of " + (Config.simulationTime / 60) + " minutes");


        while (secondsPassed <= Config.simulationTime) {
            //check if a second has passed
            if (time > (secondsPassed + 1) * 1000) {
                secondsPassed++;
                if (secondsPassed % (60 * 5) == 0) {
                    System.out.println("Simulated " + (secondsPassed / 60) + "/" + (Config.simulationTime / 60) + " minutes");
                }
                for (Node node : nodes) {
                    node.setupGeneration();
                }
            }
            //processing of each node in the network

            if (slot == 0) {
                bs.beacon(frame);
            }
            for (Node node : nodes) {
                node.process(frame, slot, time);
            }
            //update the createdAt
            time += slotLength;

            bs.process(time);


            //update counters
            slot++;
            //check whether we've reached the end of this frame
            if (slot > 15) {
                //update frame counter and reset slot counter
                frame++;
                slot = 0;
                //check whether we've reached the end of the second (full 12 frames)
                if (frame > 11) {
                    //reset frame counter
                    frame = 0;
                }
            }
        }

        analyze();
    }

    public void analyze() {
        System.out.println("Gathering results...");
        int expectedMessages = 0;

        for (Node node : nodes) {
            expectedMessages += node.getSeqNumber();
        }

        int lostMessages = 0;
        long totalLatency = 0;

        ArrayList<Message> received = bs.getReceived();
        ArrayList<Long> latencies = new ArrayList<>(received.size());
        for (Message message : received) {
            long latency = message.recievedAt - message.createdAt;
            latencies.add(latency);
            totalLatency += latency;
        }
        latencies.sort(Comparator.naturalOrder());

        System.out.println("Expected messages: " + expectedMessages);
        System.out.println("Lost messages: " + lostMessages);
        System.out.println("Received messages: " + received.size());
        System.out.println("Best latency: " + latencies.get(0) + " ms");
        System.out.println("Best latency (1%): " + latencies.get(latencies.size() / 100) + " ms");
        System.out.println("Average latency: " + totalLatency / received.size() + " ms");
        System.out.println("Worst  latency: " + latencies.get(latencies.size() - 1) + " ms");
        System.out.println("Worst  latency (1%): " + latencies.get(latencies.size() - 1 - latencies.size() / 100) + " ms");
        System.out.println("Median latency: " + latencies.get(latencies.size() / 2) + " ms");
        System.out.println("Collisions: " + bs.getCollisions());

    }
}
