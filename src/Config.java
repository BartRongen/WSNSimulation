class Config {
    //Packet reception ration
    static final double PRR = 0.95;
    //Number of hours to simulate
    static final int hours = 1;
    static final int simulationTime = hours * 60 * 60;
    //Total number of nodes in the network
    static final int numNodes = 80;
    //Number of messages a node can store
    static final int memory = 60;

    //Whether we save data to .csv and for how many simulations
    static final boolean generateData = true;
    static final int numberOfSimulations = 20;

    static final int maxUpdatesPerMinute = 20;
}
