package linbactsim.simulation;

// Tracks the live state of an in-progress simulation run.
// Source: fields from SURE.Main anonymous ActionListener (~lines 1238–1254)
// and SURE.Main#time, SURE.Main#dt.
public class SimulationTracking {

    // Source: SURE.Main anonymous ActionListener fields: step, maxSteps, dt
    private int currentStep;
    private int maxSteps;
    private int dt;
    private boolean running;

    public SimulationTracking(int maxSteps, int dt) {
        this.maxSteps = maxSteps;
        this.dt = dt;
        this.currentStep = 0;
        this.running = false;
    }

    public void start()  { running = true; }
    public void stop()   { running = false; }
    public void reset()  { currentStep = 0; running = false; }
    public void step()   { currentStep++; }
    public boolean isDone() { return currentStep >= maxSteps; }

    public int  getCurrentStep() { return currentStep; }
    public int  getMaxSteps()    { return maxSteps; }
    public int  getDt()          { return dt; }
    public boolean isRunning()   { return running; }

    // Source: SURE.Main#getNumStep()
    public int getNumStep() { return maxSteps; }
}
