import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node {

    private int GTS;
    private BaseStation bs;
    private ArrayList<Integer> generating; //stores the messages, with pair(slot, message), slot determines when the message will be generated.
    private ArrayList<Message> messages;
    private ArrayList<PriceUpdate> received;
    private Random random;
    private int totalSlots;
    private int seqNumber = 0;
    private int id;
    private boolean clearMessages;
    private int memory = Config.memory; //the amount of messages we can store
    private ArrayList<Node> overhearableNodes;
    private boolean csmaCheckedPreviousSlot;
    private int csmaSkipSlots = 0;
    private int BE;
    private int maxBE = 5;
    private boolean csmaDidSend;

    private boolean sending;

    private static int lostMessages;
    private static int csmaMessagesSent;

    public static int getCsmaMessagesSent() {
        return csmaMessagesSent;
    }

    public static int getLostMessages() {
        return lostMessages;
    }


    public static void reset(){
        lostMessages = 0;
        csmaMessagesSent = 0;
    }

    public boolean isSending() {
        return sending;
    }

    public Node(int totalSlots, int id, ArrayList<Node> overhearableNodes, int BE) {
        messages = new ArrayList<>();
        generating = new ArrayList<>();
        received = new ArrayList<>();
        random = new Random();
        this.totalSlots = totalSlots;
        this.id = id;
        this.overhearableNodes = overhearableNodes;
        this.BE = BE;
    }

    public void assignBaseStation(BaseStation bs) {
        this.bs = bs;
    }

    public void beacon(int GTS) {
        this.GTS = GTS;

    }

    public void setupGeneration() {
        //generates the messages
        generating.clear();

        for (int i = 0; i < random.nextInt(21); i++) {
            //creates the to createdAt slot for the generated message
            generating.add(random.nextInt(totalSlots));
        }
    }

    //processes the node, called each slot
    public void process(int frame, int slot, long time) {
        //calculate current slot;
        int cS = frame * WSN.slots + slot;
        sending = false;

        if (csmaDidSend) {
            csmaDidSend = false;
            csmaBackoff();
        }

        //check whether we can clear the current messages
        if (clearMessages) {
            if (messages.size() > 20) {
                List<Message> remaining = messages.subList(20, messages.size());
                messages = new ArrayList<>(remaining.size());
                messages.addAll(remaining);
            } else {
                messages.clear();
            }
            clearMessages = false;
        }

        //check if we need to generate a message
        for (int timeSlot : generating) {
            if (cS == timeSlot) {
                //when there are more than 60 messages, we can't store them in our memory.
                if (messages.size() > memory) {
                    messages.remove(0);
                    lostMessages++;
                }
                messages.add(new Message(id, seqNumber, time));
                seqNumber++;
            }
        }
        if (messages.size() == 0) return;

        //check whether it is this node's GTS
        if (slot == GTS) {
            sending = true;
            sendPacket(slot);
        } else if (slot < 9) {
            // CSMA
            if (csmaSkipSlots > 0) {
                csmaSkipSlots--;
            } else if (csmaCheckedPreviousSlot) {
                csmaCheckedPreviousSlot = false;
                sending = true;
                csmaDidSend = true;
                sendPacket(slot);
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
                    csmaBackoff();
                }
            }
        }

    }

    private void sendPacket(int slot) {
        bs.send(messages.subList(0, Math.min(messages.size(), 20)), this, slot);
    }

    private void csmaBackoff() {
        csmaSkipSlots = random.nextInt((int) Math.pow(2, BE));
    }

    public void ack() {
        if ((random.nextDouble() > Config.PRR)) {
            return;
        }
        clearMessages = true;
        if (csmaDidSend) {
            csmaMessagesSent += Math.min(messages.size(), 20);
            csmaDidSend = false;
            csmaSkipSlots = 0;
        }
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public ArrayList<PriceUpdate> getReceived() {
        return received;
    }

    public void send(PriceUpdate update, long time) {
        if ((random.nextDouble() > Config.PRR)) {
            return;
        }
        if (update.recievedAt == -1) {
            update.recievedAt = time + WSN.slotLength;
            received.add(update);
        }
        bs.ack();
    }

}
