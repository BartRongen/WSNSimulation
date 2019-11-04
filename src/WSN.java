
import java.util.*;

public class WSN {

    static final long slotLength = 1000 / (12 * 16);

    BaseStation bs;
    Node[] nodes;

    static final int frames = 12;
    static final int slots = 16;

    static final int totalSlots = frames * slots;
    private int secondsPassed = 0;

    public HashMap<String, String> data;

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
                    bs.setupGeneration(time);
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
            bs.update(frame, slot, time);
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
        System.out.println();
        int expectedMessages = 0;
        ArrayList<PriceUpdate> receivedUpdates = new ArrayList<>();

        for (Node node : nodes) {
            expectedMessages += node.getSeqNumber();
            receivedUpdates.addAll(node.getReceived());
        }
        System.out.println("Expected messages: " + expectedMessages);
        System.out.println("Lost messages: " + Node.getLostMessages());
        System.out.println("Collisions: " + bs.getCollisions());
        System.out.println("Messages sent using CSMA: " + Node.getCsmaMessagesSent());

        long totalLatency = 0;
        long totalLatencyMiddle = 0;
        int numUnder5 = 0;

        ArrayList<Message> received = bs.getReceived();
        System.out.println("Received messages: " + received.size());
        ArrayList<Integer> latencies = new ArrayList<>(received.size());
        for (int i = 0; i < received.size(); i++) {
            Message message = received.get(i);
            int latency = (int) (message.recievedAt - message.createdAt);
            if (latency <= 5000) {
                numUnder5++;
            }
            latencies.add(latency);
            totalLatency += latency;
            if (i > received.size() / 100 && i < received.size() - received.size() / 100) {
                totalLatencyMiddle += latency;
            }
        }
        latencies.sort(Comparator.naturalOrder());

        int bestLatency = latencies.get(0);
        int bestLatency1 = latencies.get(latencies.size() / 100);
        long avgLatency = totalLatency / received.size();
        long avgLatency98 = totalLatencyMiddle / received.size();
        int worstLatency = latencies.get(latencies.size() - 1);
        int worstLatency1 = latencies.get(latencies.size() - 1 - latencies.size() / 100);
        int medianLatency = latencies.get(latencies.size() / 2);
        double stdDev = standardDeviation(latencies, totalLatency / received.size());
        double percentUnder5 = (double) numUnder5 / (double) received.size() * 100.0;
        int highestUpdatesCount = bs.getHighestUpdatesCount();

        latencies.clear();
        totalLatency = 0;
        numUnder5 = 0;
        for (PriceUpdate update : receivedUpdates) {
            int latency = (int) (update.recievedAt - update.createdAt);
            totalLatency += latency;
            latencies.add(latency);
            if (latency < 5000) {
                numUnder5++;
            }
        }
        latencies.sort(Comparator.naturalOrder());

        int bestUpdatesLatency = latencies.get(0);
        long avgUpdatesLatency = totalLatency / receivedUpdates.size();
        int worstUpdatesLatency = latencies.get(latencies.size() - 1);
        int medianUpdatesLatency = latencies.get(latencies.size() / 2);
        double percentUpdatesUnder5 = (double) numUnder5 / (double) receivedUpdates.size() * 100.0;


        System.out.println("Best latency: " + bestLatency + " ms");
        System.out.println("Best latency (1%): " + bestLatency1 + " ms");
        System.out.println("Average latency: " + avgLatency + " ms");
        System.out.println("Average latency (middle 98%): " + avgLatency98 + " ms");
        System.out.println("Worst  latency: " + worstLatency + " ms");
        System.out.println("Worst  latency (1%): " + worstLatency1 + " ms");
        System.out.println("Median latency: " + medianLatency + " ms");
        System.out.println("Standard deviation: " + stdDev + " ms");
        System.out.println("Under 5s: " + percentUnder5 + " %");

        System.out.println();

        System.out.println("Updates received: " + receivedUpdates.size());
        System.out.println("Highest updates count: " + highestUpdatesCount);
        System.out.println("Updates best latency: " + bestUpdatesLatency + " ms");
        System.out.println("Updates average latency: " + avgUpdatesLatency + " ms");
        System.out.println("Updates worst  latency: " + worstUpdatesLatency + " ms");
        System.out.println("Updates median latency: " + medianUpdatesLatency + " ms");
        System.out.println("Updates under 5s: " + percentUpdatesUnder5 + " %");


        System.out.println();
        System.out.println("Done!");

        //When we want to generate data we store the data obtained from this simulation in a hashmap
        if (Config.generateData) {
            data = new HashMap<>();
            data.put("Expected Messages", String.valueOf(expectedMessages));
            data.put("Lost Messages", String.valueOf(Node.getLostMessages()));
            data.put("Collisions", String.valueOf(bs.getCollisions()));
            data.put("Messages (CSMA)", String.valueOf(Node.getCsmaMessagesSent()));
            data.put("Received Messages", String.valueOf(received.size()));
            data.put("Best Latency (ms)", String.valueOf(bestLatency));
            data.put("Best Latency (1%) (ms)", String.valueOf(bestLatency1));
            data.put("Average Latency", String.valueOf(avgLatency));
            data.put("Average Latency (middle 98%) (ms)", String.valueOf(avgLatency98));
            data.put("Worst Latency (ms)", String.valueOf(worstLatency));
            data.put("Worst Latency (1%) (ms)", String.valueOf(worstLatency1));
            data.put("Median Latency (ms)", String.valueOf(medianLatency));
            data.put("Standard Deviation (ms)", String.valueOf(stdDev).replace(".", ","));
            data.put("Under 5s (%)", String.valueOf(percentUnder5).replace(".", ","));

            data.put("Updates received", String.valueOf(receivedUpdates.size()).replace(".", ","));
            data.put("Highest updates count", String.valueOf(highestUpdatesCount).replace(".", ","));
            data.put("Updates best latency (ms)", String.valueOf(bestUpdatesLatency).replace(".", ","));
            data.put("Updates average latency (ms)", String.valueOf(avgUpdatesLatency).replace(".", ","));
            data.put("Updates worst  latency (ms)", String.valueOf(worstUpdatesLatency).replace(".", ","));
            data.put("Updates median latency (ms) ", String.valueOf(medianUpdatesLatency).replace(".", ","));
            data.put("Updates under 5s (%)", String.valueOf(percentUpdatesUnder5).replace(".", ","));
        }
    }


    private double standardDeviation(List<Integer> latencies, long average) {
        long runningSum = 0;
        for (int latency : latencies) {
            long val = average - latency;
            runningSum += val * val;
        }
        int avg = (int) (runningSum / latencies.size());
        return Math.sqrt(avg);
    }

    public HashMap<String, String> getData() {
        return data;
    }
}
