import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class WSN {

    long sL = 1000/(12*16);

    BaseStation bs;
    Node[] nodes;

    private int frames = 12;
    private int slots = 16;
    private double PER = 0.9;

    private int totalSlots = frames*slots;
    private int secondsPassed = 0;

    public WSN(){
        bs = new BaseStation(PER);
        nodes = new Node[80];
        for (int i=0; i<80; i++){
            nodes[i] = new Node(totalSlots, PER, i);
            nodes[i].assignBaseStation(bs);
        }
        bs.assignNodes(nodes);
    }

    public void startSimulation(){
        Instant start = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant secondLap = Instant.now().truncatedTo(ChronoUnit.MICROS);
        int frame = 0; //current frame, resets when it reaches 12
        int slot = 0; //current slot, resets when it reaches 16
        int tS = 0; //counter for total slots in a frame
        int GTS = 0; //counter for total GTS in a frame
        int slotTime = 1; //total slots elapsed (to measure the time for comparing), never resets

        while(secondsPassed < 10){
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            //check if a second has passed
            if (secondLap.compareTo(now.minusMillis(1000)) < 0){
                secondsPassed++;
                System.out.println("1 second");
                secondLap = now;
            }
            //check if a new slot begins
            if (start.compareTo(now.minusMillis(slotTime*sL)) < 0){
                //processing of each node in the network
                bs.process(frame, slot);
                for (int i=0; i<nodes.length; i++){
                    nodes[i].process(frame, slot);
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
                        System.out.println("totalsSlots: " + totalSlots + ", GTS: " + GTS);
                        //reset totalSlots
                        tS = 0;
                        GTS = 0;
                    }
                } else if (slot>8){
                    //when slot>8, we've reached the GTS
                    GTS++;
                }
            }
        }
        analyze();
    }

    public void analyze(){

    }
}
