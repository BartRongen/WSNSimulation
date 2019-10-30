import java.util.ArrayList;
import java.util.Random;

public class Node {

    private int GTS;
    private BaseStation bs;
    private ArrayList<Integer> generating; //stores the messages, with pair(slot, message), slot determines when the message will be generated.
    private ArrayList<Message> messages;
    private Random random;
    private int totalSlots;
    private double PER;
    private int seqNumber = 0;
    private int id;
    private boolean clearMessages;
    private int memory = 20; //we can store 20 messages

    public Node(int totalSlots, double PER, int id){
        messages = new ArrayList<>();
        generating = new ArrayList<>();
        random = new Random();
        this.totalSlots = totalSlots;
        this.PER = PER;
        this.id = id;
    }

    public void assignBaseStation(BaseStation bs){
        this.bs = bs;
    }

    public void setup(int GTS){
        //sets the new GTS
        this.GTS = GTS;
        //generates the messages
        generating.clear();

        for (int i=0; i<random.nextInt(21); i++){
            //creates the to time slot for the generated message
            generating.add(random.nextInt(totalSlots+1));
        }
    }

    //processes the node, called each slot
    public void process(int frame, int slot, long time){
        //calculate current slot;
        int cS = frame*16 + slot;

        //check whether we can clear the current messages
        if (clearMessages){
            messages.clear();
            clearMessages = false;
        }

        //check if we need to generate a message
        for (int timeSlot: generating){
            if (cS == timeSlot){
                //when there are more than 20 messages, we can't store them in our memory.
                if (messages.size() > memory){
                    messages.remove(0);
                }
                messages.add(new Message(id, seqNumber, time));
                seqNumber++;
            }
        }
        //check whether it is this node's GTS
        if (cS == GTS){
            //determines whether the sending is successful according to PER
            if (random.nextDouble() <= PER){
                boolean success = bs.send(messages, this, time);
                if (success){
                    clearMessages = true;
                }
            }
        }

        //TODO CSMA

    }

    //called by the base station when multiple nodes try to send in the same time slot
    public void notReceived(){
        clearMessages = false;
    }

    public int getSeqNumber(){
        return seqNumber;
    }
}
