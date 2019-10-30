import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Random;

public class BaseStation {

    private Node[] nodes;
    private double PRR;
    private Random random;
    private ArrayList<Pair<Message, Long>> obtained; //the successfully obtained messages
    private ArrayList<Pair<Message, Long>> awaiting; //the messages discovered this slot, but they can be corrupted if two nodes send during the same slot
    boolean alreadyReceived = false; //boolean to track whether or not the base station already received a message this slot
    private Node currentSender;

    public BaseStation(double PRR) {
        this.PRR = PRR;
        obtained = new ArrayList<>();
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
                if ((random.nextDouble() > PRR)) {
                    gts = -1;
                }
            }
            nodes[i].setup(gts);
        }
    }

    public void process(int frame, int slot) {
        alreadyReceived = false;

        if (awaiting.size() > 0) {
            obtained.addAll(awaiting);
            if ((random.nextDouble() <= PRR)) {
                currentSender.ack();
            }
            awaiting.clear();
        }
    }

    //nodes can send data to the base station with this method
    public void send(ArrayList<Message> messages, Node node, long time) {
        if ((random.nextDouble() > PRR)) {
            return;
        }
        if (!alreadyReceived) {
            //stores the current sender, so it can be send an ack
            currentSender = node;
            //adds the message with arrived timestamp.
            for (Message m : messages) {
                Pair<Message, Long> pair = new Pair<>(m, time);
                awaiting.add(pair);
            }
            alreadyReceived = true;
        } else {
//            System.out.println("two at the same time");
            awaiting.clear();
        }
    }

    public ArrayList<Pair<Message, Long>> getObtained() {
        return obtained;
    }
}
