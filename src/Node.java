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
    private ArrayList<Node> overhearableNodes;
    private boolean csmaCheckedPreviousSlot;
    private int csmaSkipSlots = 0;
    private int BE;
    private int maxBE = 5;

    private boolean sending;
    public boolean isSending() {
        return sending;
    }

    public Node(int totalSlots, double PER, int id, ArrayList<Node> overhearableNodes, int BE){
        messages = new ArrayList<>();
        generating = new ArrayList<>();
        random = new Random();
        this.totalSlots = totalSlots;
        this.PER = PER;
        this.id = id;
        this.overhearableNodes = overhearableNodes;
        this.BE = BE;
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
        sending = false;

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
        if (messages.size() > 0) {
            if (slot == GTS) {
                //determines whether the sending is successful according to PER
                if (random.nextDouble() <= PER) {
                    sending = true;
                    boolean success = bs.send(messages, this, time);
                    if (success) {
                        clearMessages = true;
                    }
                }
            } else if (slot < 9) {
                // CSMA
                if (csmaSkipSlots > 0) {
                    csmaSkipSlots--;
                } else if (csmaCheckedPreviousSlot) {
                    csmaCheckedPreviousSlot = false;
                    sending = true;
                    if (bs.send(messages, this, time)) {
                        clearMessages = true;
                    } else {
                        csmaSkipSlots = random.nextInt((int) Math.pow(2, BE));
                    }
                } else if (slot < 8) {
                    // cannot send in next slot if slot is 8
                    boolean anySending = false;
                    for (Node node : overhearableNodes) {
                        if (node.isSending()) {
                            anySending = true;
                            break;
                        }
                    }
                    if (!anySending) {
                        csmaCheckedPreviousSlot = true;
                    } else {
                        BE = Math.min(BE+1, 5);
                        csmaSkipSlots = random.nextInt((int) Math.pow(2, BE));
                    }
                } else {
                    csmaSkipSlots = 0;
                    csmaCheckedPreviousSlot = false;
                }
            }
        }

    }

    //called by the base station when multiple nodes try to send in the same time slot
    public void notReceived(){
        clearMessages = false;
    }

    public int getSeqNumber(){
        return seqNumber;
    }
}
