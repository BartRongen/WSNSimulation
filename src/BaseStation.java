import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaseStation {

    private Node[] nodes;
    private Random random;
    private ArrayList<Message> received; //the successfully received messages
    private ArrayList<Message> awaiting; //the messages discovered this slot, but they can be corrupted if two nodes send during the same slot
    boolean alreadyReceived = false; //boolean to track whether or not the base station already received a message this slot
    boolean didCollide;
    private Node currentSender;

    private long collisions;

    public BaseStation() {
        received = new ArrayList<>();
        awaiting = new ArrayList<>();
        random = new Random();
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
            if ((random.nextDouble() <= Config.PRR)) {
                currentSender.ack();
            }
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
}
