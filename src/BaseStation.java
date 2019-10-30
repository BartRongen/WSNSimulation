import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Random;

public class BaseStation {

    private Node[] nodes;
    private double PER;
    private Random random;
    private ArrayList<Pair<Message, Long>> obtained; //the successfully obtained messages
    private ArrayList<Pair<Message, Long>> awaiting; //the messages discovered this slot, but they can be corrupted if two nodes send during the same slot
    boolean alreadyReceived = false; //boolean to track whether or not the base station already received a message this slot
    private Node currentSender;

    public BaseStation(double PER){
        this.PER = PER;
        obtained = new ArrayList<>();
        awaiting = new ArrayList<>();
        random = new Random();
    }

    public void assignNodes(Node[] nodes){
        this.nodes = nodes;
    }

    //processes the BaseStation, called each slot
    public void process(int frame, int slot, long time){
        //calculate current slot;
        int cS = frame*16 + slot;

        //resets already received
        alreadyReceived = false;

        //add all awaiting messages to obtained
        obtained.addAll(awaiting);
        //clears the awaiting messages
        awaiting.clear();

        //setup all nodes for the following second (not entirely how beacon-enabled works, but it does represent it on high level)
        if (cS == 0){
            for (int i=0; i<nodes.length; i++){
                nodes[i].setup(i);
            }
        }
    }

    //nodes can send data to the base station with this method
    public boolean send(ArrayList<Message> messages, Node node, long time){
        if (!alreadyReceived){
            //stores the current sender, maybe it needs to be updated that the messages didn't arrive
            currentSender = node;
            //adds the message with arrived timestamp.
            for(Message m: messages){
                Pair<Message, Long> pair = new Pair<>(m, time);
                awaiting.add(pair);
            }
            alreadyReceived = true;
            //return the ack message with PER
            return (random.nextDouble() <= PER);
        } else {
            System.out.println("two at the same time");
            awaiting.clear();
            //sends the error back to the current sender
            currentSender.notReceived();
            return false;
        }
    }

    public ArrayList<Pair<Message, Long>> getObtained() {
        return obtained;
    }
}
