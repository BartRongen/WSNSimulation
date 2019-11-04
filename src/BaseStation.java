import java.util.*;

public class BaseStation {

    private Node[] nodes;
    private Random random;
    private ArrayList<Message> received; //the successfully received messages
    private ArrayList<Message> awaiting; //the messages discovered this slot, but they can be corrupted if two nodes send during the same slot
    private Queue<PriceUpdate> updates; //the messages discovered this slot, but they can be corrupted if two nodes send during the same slot
    boolean alreadyReceived = false; //boolean to track whether or not the base station already received a message this slot
    boolean didCollide;
    private Node currentSender;

    private int highestUpdatesCount;

    public int getHighestUpdatesCount() {
        return highestUpdatesCount;
    }

    private long collisions;

    private int seqNumber = 0;

    public BaseStation() {
        received = new ArrayList<>();
        awaiting = new ArrayList<>();
        updates = new LinkedList<>();
        random = new Random();
    }

    public void setupGeneration(long time) {
        int numNewUpdates = random.nextInt(Config.maxUpdatesPerMinute + 1);
        for (int i = 0; i < numNewUpdates; i++) {
            updates.add(new PriceUpdate(random.nextInt(Config.numNodes), ++seqNumber, time));
        }
        highestUpdatesCount = Math.max(highestUpdatesCount, updates.size());
    }

    public void assignNodes(Node[] nodes) {
        this.nodes = nodes;
    }

    public void beacon(int frame) {
        //setup all nodes for the frame
        for (int i = 0; i < nodes.length; i++) {
            int gts = -1;
            if (i >= frame * 7 && i < (frame + 1) * 7) {
                gts = 9 + i - frame * 7;
                // Use PRR to simulate beacon not being received by node
                if ((random.nextDouble() > Config.PRR)) {
                    gts = -1;
                }
            }
            nodes[i].beacon(gts);
        }
    }

    public void update(int frame, int slot, long time) {
        if (slot < 9 || updates.size() == 0) return;
        int cS = frame * 7 + slot - 9;
        if (cS < Config.numNodes) return;

        PriceUpdate update = updates.peek();
        Node node = nodes[update.destination];
        node.send(update, time);

    }

    public void process(long time) {
        alreadyReceived = false;
        if (didCollide) {
            collisions++;
        }
        didCollide = false;

        if (awaiting.size() > 0) {
            for (Message message : awaiting) {
                if (message.recievedAt == -1) {
                    message.recievedAt = time;
                    received.add(message);
                }
            }
            currentSender.ack();
            awaiting.clear();
        }
    }

    //nodes can send data to the base station with this method
    public void send(List<Message> messages, Node node, int slot) {
        if ((random.nextDouble() > Config.PRR)) {
            return;
        }
        if (!alreadyReceived) {
            //stores the current sender, so it can be send an ack
            currentSender = node;
            //adds the message with arrived timestamp.
            for (Message message : messages) {
                awaiting.add(message);
            }
            alreadyReceived = true;
        } else {
            if (slot > 8) {
                System.err.println("Collision happened in guaranteed slot period!");
                System.exit(1);
            }
            didCollide = true;
            awaiting.clear();
        }
    }

    public ArrayList<Message> getReceived() {
        return received;
    }

    public long getCollisions() {
        return collisions;
    }

    public void ack() {
        if ((random.nextDouble() > Config.PRR)) {
            return;
        }
        updates.poll();
    }
}
