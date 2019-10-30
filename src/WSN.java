import javafx.util.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class WSN {

    static long slotLength = 1000/(12*16);
    static int simulationTime = 10000;

    BaseStation bs;
    Node[] nodes;

    private int frames = 12;
    private int slots = 16;
    private double PER = 0.99;

    private int totalSlots = frames*slots;
    private int secondsPassed = 0;

    public WSN(){
        bs = new BaseStation(PER);
        nodes = new Node[80];
        ArrayList<Node> firstHalf = new ArrayList<>();
        ArrayList<Node> secondHalf = new ArrayList<>();
        for (int i=0; i<80; i++){
            ArrayList<Node> half = i < 40 ? firstHalf : secondHalf;
            nodes[i] = new Node(totalSlots, PER, i, half);
            half.add(nodes[i]);
            nodes[i].assignBaseStation(bs);
        }
        bs.assignNodes(nodes);
    }

    int count = 0;
    public void startSimulation(){
        long time = 0;
        long startLap = 0;
        //Instant start = Instant.now().truncatedTo(ChronoUnit.MICROS);
        //Instant secondLap = Instant.now().truncatedTo(ChronoUnit.MICROS);
        int frame = 0; //current frame, resets when it reaches 12
        int slot = 0; //current slot, resets when it reaches 16
        int tS = 0; //counter for total slots in a frame
        int GTS = 0; //counter for total GTS in a frame
        int slotTime = 1; //total slots elapsed (to measure the time for comparing), never resets

        while(secondsPassed <= simulationTime){
            //check if a second has passed
            if (time > (secondsPassed+1) * 1000){
                secondsPassed++;
            }
            //processing of each node in the network

            bs.process(frame, slot, time);
            for (int i=0; i<nodes.length; i++){
                nodes[i].process(frame, slot, time);
            }



            //update counters
            tS++;
            slot++;
            slotTime++;
            //check whether we've reached the end of this frame
            if (slot>15){
                //update frame counter and reset slot counter
                frame++;
                slot = 0;
                //check whether we've reached the end of the second (full 12 frames)
                if (frame>11){
                    //reset frame counter
                    frame = 0;
                    //reset totalSlots
                    tS = 0;
                    GTS = 0;
                }
            } else if (slot>8){
                //when slot>8, we've reached the GTS
                GTS++;
            }
            //update the time
            time += slotLength;
        }

        analyze();
    }

    public void analyze(){
        int[] nodeSQN = new int[80];
        int expectedMessages = 0;
        int expectedAway = 0;

        for (int i=0; i<nodeSQN.length; i++){
            nodeSQN[i] = 0;
            expectedMessages += nodes[i].getSeqNumber();
        }

        int lostMessages = 0;
        int receivedMessages = 0;
        long totalLatency = 0;
        long worstLatency = 0;

        ArrayList<Pair<Message, Long>> obtained = bs.getObtained();
        for (Pair<Message, Long> pair : obtained){
            Message message = pair.getKey();
            long received = pair.getValue();
            int id = message.node;
            int sqn = message.seqNumber;
            long generated = message.timeStamp;
            if (nodeSQN[id] < sqn){
                lostMessages += (sqn - nodeSQN[id]);
                nodeSQN[id] = sqn+1;
            } else if (nodeSQN[id] == sqn) {
                receivedMessages++;
                long currentLatency = received - generated;
                if (worstLatency < currentLatency){
                    worstLatency = currentLatency;
                }
                totalLatency+= currentLatency;
                nodeSQN[id] = sqn+1;
            }
        }

        System.out.println("Expected messages: " + expectedMessages);
        System.out.println("Lost messages: " + lostMessages);
        System.out.println("Received messages: " + receivedMessages);
        System.out.println("Average latency: " + totalLatency/receivedMessages);
        System.out.println("Worst latency: " + worstLatency);

    }
}
