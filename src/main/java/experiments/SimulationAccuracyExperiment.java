/*
 * Copyright (c) 2019 Nico Kuijpers
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal 
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is furnished 
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR I
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR 
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package experiments;

import ephemeris.EphemerisAccurate;
import ephemeris.IEphemeris;
import ephemeris.SolarSystemParameters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import particlesystem.Particle;
import particlesystem.ParticleSystem;
import util.Vector3D;

/**
 * Experiment to determine accuracy of simulation results for the following bodies of
 * the Solar System: Mercury, Venus, Earth, Moon, Mars, Jupiter, Saturn, Uranus, and
 * Pluto.
 * Simulation results using Newton Mechanics and General Relativity are compared to
 * ephemeris data from DE405 over a period of 580 years. Simulation time step is 1 hour.
 * 
 * @author Nico Kuijpers
 */
public class SimulationAccuracyExperiment {
    
    // Ephemeris
    private final IEphemeris ephemeris;

    // Particle system
    private ParticleSystem particleSystem;
    
    // Names of bodies to be simulated (except Sun)
    private final List<String> bodyNames;
    
    // Simulation date/time
    private GregorianCalendar simulationDateTime;
    
    // Simulation time step [seconds]
    private final long deltaT;
    
    // Store deviations in position to compute average
    private Map<String,List<Double>> deviations;
    
    /**
     * Constructor.
     * Set ephemeris, names of Solar System bodies, and simulation time step.
     */
    public SimulationAccuracyExperiment() {
        // Set ephemeris
        ephemeris = EphemerisAccurate.getInstance();
        
        // Define the bodies of the Solar System to be simulated (except Sun)
        bodyNames = new ArrayList<>();
        bodyNames.add("Mercury");
        bodyNames.add("Venus");
        bodyNames.add("Earth");
        bodyNames.add("Moon");
        bodyNames.add("Mars");
        bodyNames.add("Jupiter");
        bodyNames.add("Saturn");
        bodyNames.add("Uranus");
        bodyNames.add("Neptune");
        bodyNames.add("Pluto");

        // Set simulation time step to 1 hour
        deltaT = (long) (60 * 60);
    }

    /**
     * Initialize simulation.
     * Simulation will start January 1st, 1620.
     */
    private void initSimulation() {
        // Start simulation at January 1st, 1620
        // Note that January is month 0
        simulationDateTime = new GregorianCalendar(1620,0,1);
        
        // https://www.timeanddate.com/time/aboututc.html
        // Use Coordinated Universal Time (UTC) to avoid 
        // sudden changes in ephemeris due to changes from 
        // winter time to summer time and vice versa
        simulationDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));

        // Store deviations in position to compute average
        deviations = new HashMap<>();
        for (String name : bodyNames) {
            deviations.put(name, new ArrayList<>());
        }
        
        // Create the Solar System
        particleSystem = new ParticleSystem();
        
        // Create the Sun 
        Vector3D positionSun = new Vector3D(); // Origin
        Vector3D velocitySun = new Vector3D(); // Zero velocity
        double massSun = SolarSystemParameters.getInstance().getMass("Sun");
        double muSun   = SolarSystemParameters.getInstance().getMu("Sun");
        particleSystem.addParticle("Sun",massSun,muSun,positionSun,velocitySun);
        
        // Create the remaining bodies of the Solar System
        for (String name : bodyNames) {
            createBody(name,simulationDateTime);
        } 
    }

    
    /**
     * Run simulation for given number of years.
     * Simulation time step is 1 hour. Position of Solar System bodies is
     * checked against ephemeris data each day.
     * @param nrYears number of years
     */
    private void runSimulation(int nrYears) {
        // End date/time of the simulation
        GregorianCalendar simulationEndDateTime = (GregorianCalendar) simulationDateTime.clone();
        simulationEndDateTime.setTimeZone(TimeZone.getTimeZone("UTC"));
        simulationEndDateTime.add(Calendar.YEAR, nrYears);
        
        // Check initial position and velocity
        int hour = 0;
        int day = 0;
        checkPosition(simulationDateTime,day);
        
        // Run simulation
        while (simulationDateTime.before(simulationEndDateTime)) {
            // Advance 1 hour
            particleSystem.advanceRungeKutta(deltaT);
            
            // Correction for position of Sun
            Particle sun = particleSystem.getParticle("Sun");
            particleSystem.correctDrift(sun.getPosition(),sun.getVelocity());
            
            // Update simulation date/time
            simulationDateTime.add(Calendar.SECOND, (int) deltaT);
            
            // Check position of Solar System bodies each day
            hour++;
            if (hour == 24) {
                hour = 0;
                day++;
                checkPosition(simulationDateTime,day);
            }   
        }
    }

    /**
     * Run simulation using Newton Mechanics for given number of years.
     * @param nrYears number of years
     */
    public void simulateNewtonMechanics(int nrYears) {
        // Initialize simulation
        initSimulation();
        
        // Do not apply General Relativity when computing acceleration
        particleSystem.setGeneralRelativityFlag(false);
        
        // Show message on screen
        System.out.println("Running simulation using Newton Mechanics for " + nrYears + " years");
        
        // Run simulation
        runSimulation(nrYears);
    }

    /**
     * Run simulation using General Relativity for given number of years.
     * @param nrYears number of years
     */
    public void simulateGeneralRelativity(int nrYears) {
        // Initialize simulation
        initSimulation();
        
        // Apply General Relativity when computing acceleration
        particleSystem.setGeneralRelativityFlag(true);
        
        // Show message on screen
        System.out.println("Running simulation using General Relativity for " + nrYears + " years");
        
        // Run simulation
        runSimulation(nrYears);
    }
    
    /**
     * Check position of Solar System bodies against ephemeris data.
     * @param dateTime simulation date/time
     * @param day      current day of simulation
     */
    private void checkPosition(GregorianCalendar dateTime, int day) {
        
        // Position [m] of the Sun
        Particle sun = particleSystem.getParticle("Sun");
        Vector3D positionSun = sun.getPosition();

        // Position [m] of the other bodies
        for (String name : bodyNames) {
            // Expected position [m] of body with respect to the Sun
            Vector3D positionExpected = ephemeris.getBodyPosition(name,dateTime);

            // Actual position [m] of body
            Particle planet = particleSystem.getParticle(name);
            Vector3D positionBody = planet.getPosition();

            // Actual position of body with respect to the Sun
            Vector3D positionActual = positionBody.minus(positionSun);

            // Deviation [m] between actual position to expected position
            double deviationPosition = positionActual.euclideanDistance(positionExpected);
            
            // Store deviation in position to compute average
            (deviations.get(name)).add(deviationPosition);
            
            // Show average deviation after 365 days of simulation
            if (day % 365 == 0) {
                // Compute average deviation in position over the past 365 days
                double sumDeviations = 0.0;
                for (double deviation : deviations.get(name)) {
                    sumDeviations += deviation;
                }
                double averageDeviation = sumDeviations / (deviations.get(name)).size();
                (deviations.get(name)).clear();
                
                // Show average deviation on screen
                System.out.println("Year: " + day/365 + " body: " + name +
                        " average deviation: " + averageDeviation/1000 + " km");
            }
        }
    }
    
    /**
     * Create body of the Solar System corresponding to given name.
     * Create and initialize a new particle with mass, standard gravitational
     * parameter, position, and velocity corresponding to given date/time.
     * The new particle will be added to the particle system.
     * @param name     name of the body
     * @param dateTime date/time to initialize position and velocity
     */
    private void createBody(String name, GregorianCalendar dateTime) {
        // Obtain mass of body
        double mass = SolarSystemParameters.getInstance().getMass(name);
        
        // Obtain parameter mu = G * m, where G = Gravitational constant and m = mass
        double mu = SolarSystemParameters.getInstance().getMu(name);
         
        // Obtain initial (x,y,z) position [m] from ephemeris
        Vector3D position = ephemeris.getBodyPosition(name, dateTime);
        
        // Obtain intial (x,y,z) velocity [m/s] from ephemeris
        Vector3D velocity = ephemeris.getBodyVelocity(name, dateTime);
        
        // Add particle for simulation
        particleSystem.addParticle(name, mass, mu, position, velocity);
    }
    
    /**
     * Main method.
     * Run two simulations for 580 years using Newton Mechanics and General Relativity, 
     * respectively. Simulation results are compared to Ephemeris DE405 data. 
     * Simulation time step is 1 hour.
     * @param args input arguments (not used)
     */
    public static void main (String[] args) {
        // Experiment set-up
        SimulationAccuracyExperiment experiment = new SimulationAccuracyExperiment();
        
        // Run simulation using Newton Mechanics for 580 years
        experiment.simulateNewtonMechanics(580);
        
        // Run simulation using General Relativity for 580 years
        experiment.simulateGeneralRelativity(580);
    }

    /*
        Results
        Running simulation using Newton Mechanics for 580 years
        Year: 100 body: Mercury average deviation: 18651.33266516227 km
        Year: 100 body: Venus average deviation: 8765.861319718528 km
        Year: 100 body: Earth average deviation: 5876.194184110724 km
        Year: 100 body: Moon average deviation: 5954.964737423869 km
        Year: 100 body: Mars average deviation: 4310.3778523784285 km
        Year: 100 body: Jupiter average deviation: 322.86561863498906 km
        Year: 100 body: Saturn average deviation: 211.27603175520917 km
        Year: 100 body: Uranus average deviation: 130.38127790103223 km
        Year: 100 body: Neptune average deviation: 130.37451857219347 km
        Year: 100 body: Pluto average deviation: 76.56052959502074 km

        Year: 200 body: Mercury average deviation: 37334.994528035764 km
        Year: 200 body: Venus average deviation: 17659.125134044905 km
        Year: 200 body: Earth average deviation: 11782.6703749294 km
        Year: 200 body: Moon average deviation: 11930.2845950075 km
        Year: 200 body: Mars average deviation: 8763.842498742983 km
        Year: 200 body: Jupiter average deviation: 765.6649533061166 km
        Year: 200 body: Saturn average deviation: 490.2357013808898 km
        Year: 200 body: Uranus average deviation: 268.82379553188906 km
        Year: 200 body: Neptune average deviation: 155.20752612085852 km
        Year: 200 body: Pluto average deviation: 80.90969553453951 km

        Year: 300 body: Mercury average deviation: 56126.26170909272 km
        Year: 300 body: Venus average deviation: 26380.666884768943 km
        Year: 300 body: Earth average deviation: 17688.801566007525 km
        Year: 300 body: Moon average deviation: 17850.258548978094 km
        Year: 300 body: Mars average deviation: 12635.25477551177 km
        Year: 300 body: Jupiter average deviation: 1067.8319959382754 km
        Year: 300 body: Saturn average deviation: 758.9715054905869 km
        Year: 300 body: Uranus average deviation: 412.8778486521169 km
        Year: 300 body: Neptune average deviation: 296.58095600046363 km
        Year: 300 body: Pluto average deviation: 79.892085908439 km

        Year: 400 body: Mercury average deviation: 75193.92505375914 km
        Year: 400 body: Venus average deviation: 35347.3627263678 km
        Year: 400 body: Earth average deviation: 23594.15404376493 km
        Year: 400 body: Moon average deviation: 23844.274469388267 km
        Year: 400 body: Mars average deviation: 15619.399167669719 km
        Year: 400 body: Jupiter average deviation: 1369.1251469186761 km
        Year: 400 body: Saturn average deviation: 880.1168349115511 km
        Year: 400 body: Uranus average deviation: 576.3068529989906 km
        Year: 400 body: Neptune average deviation: 362.2490932572205 km
        Year: 400 body: Pluto average deviation: 171.6697647027669 km

        Year: 500 body: Mercury average deviation: 94672.01057168366 km
        Year: 500 body: Venus average deviation: 44043.75967633921 km
        Year: 500 body: Earth average deviation: 29501.424067084543 km
        Year: 500 body: Moon average deviation: 29753.6331329433 km
        Year: 500 body: Mars average deviation: 18217.78199617646 km
        Year: 500 body: Jupiter average deviation: 1969.1700119982634 km
        Year: 500 body: Saturn average deviation: 1322.2480764623965 km
        Year: 500 body: Uranus average deviation: 762.4007095214944 km
        Year: 500 body: Neptune average deviation: 423.1528083756864 km
        Year: 500 body: Pluto average deviation: 122.65355960848973 km

        Year: 580 body: Mercury average deviation: 110576.45578072112 km
        Year: 580 body: Venus average deviation: 51135.78074113667 km
        Year: 580 body: Earth average deviation: 34226.325843627725 km
        Year: 580 body: Moon average deviation: 34325.29483906366 km
        Year: 580 body: Mars average deviation: 24951.228781056438 km
        Year: 580 body: Jupiter average deviation: 2226.8322096002103 km
        Year: 580 body: Saturn average deviation: 1300.9794381850418 km
        Year: 580 body: Uranus average deviation: 867.0276991301828 km
        Year: 580 body: Neptune average deviation: 540.7895744365629 km
        Year: 580 body: Pluto average deviation: 172.1702850993744 km

        Results General Relativity
        Running simulation using General Relativity for 580 years
        Year: 100 body: Mercury average deviation: 7.876617744671853 km
        Year: 100 body: Venus average deviation: 0.5797896085099321 km
        Year: 100 body: Earth average deviation: 22.191175313772103 km
        Year: 100 body: Moon average deviation: 1783.252870559056 km
        Year: 100 body: Mars average deviation: 28.266877941832046 km
        Year: 100 body: Jupiter average deviation: 179.39455742359206 km
        Year: 100 body: Saturn average deviation: 2.199832482472122 km
        Year: 100 body: Uranus average deviation: 59.936292502430504 km
        Year: 100 body: Neptune average deviation: 91.09698535096354 km
        Year: 100 body: Pluto average deviation: 90.59573660388463 km

        Year: 200 body: Mercury average deviation: 4.5557545795435495 km
        Year: 200 body: Venus average deviation: 0.5298465213087015 km
        Year: 200 body: Earth average deviation: 42.54926340419992 km
        Year: 200 body: Moon average deviation: 3414.3846739692835 km
        Year: 200 body: Mars average deviation: 86.67608999429952 km
        Year: 200 body: Jupiter average deviation: 390.75966220722785 km
        Year: 200 body: Saturn average deviation: 17.962489540802537 km
        Year: 200 body: Uranus average deviation: 128.2043390494327 km
        Year: 200 body: Neptune average deviation: 97.44155324012468 km
        Year: 200 body: Pluto average deviation: 91.2618254675843 km

        Year: 300 body: Mercury average deviation: 13.166291238412539 km
        Year: 300 body: Venus average deviation: 0.9989073430790822 km
        Year: 300 body: Earth average deviation: 60.93651553876523 km
        Year: 300 body: Moon average deviation: 4876.721668385543 km
        Year: 300 body: Mars average deviation: 182.7277328792074 km
        Year: 300 body: Jupiter average deviation: 567.7453086040297 km
        Year: 300 body: Saturn average deviation: 28.112875213998578 km
        Year: 300 body: Uranus average deviation: 206.85901528760562 km
        Year: 300 body: Neptune average deviation: 188.03843309294976 km
        Year: 300 body: Pluto average deviation: 98.01771237515533 km

        Year: 400 body: Mercury average deviation: 41.540828741604074 km
        Year: 400 body: Venus average deviation: 1.8803910150678844 km
        Year: 400 body: Earth average deviation: 77.68561505849131 km
        Year: 400 body: Moon average deviation: 6229.46593466047 km
        Year: 400 body: Mars average deviation: 269.54096758235505 km
        Year: 400 body: Jupiter average deviation: 752.5826936416289 km
        Year: 400 body: Saturn average deviation: 15.151973679578928 km
        Year: 400 body: Uranus average deviation: 291.0502447185643 km
        Year: 400 body: Neptune average deviation: 232.69346857956293 km
        Year: 400 body: Pluto average deviation: 228.33138194093328 km

        Year: 500 body: Mercury average deviation: 82.33255075577321 km
        Year: 500 body: Venus average deviation: 2.4916572061458773 km
        Year: 500 body: Earth average deviation: 92.7542032267337 km
        Year: 500 body: Moon average deviation: 7414.6207324557945 km
        Year: 500 body: Mars average deviation: 355.44890905384597 km
        Year: 500 body: Jupiter average deviation: 974.3847270468424 km
        Year: 500 body: Saturn average deviation: 7.612857667101683 km
        Year: 500 body: Uranus average deviation: 374.7127778548194 km
        Year: 500 body: Neptune average deviation: 265.11169862251234 km
        Year: 500 body: Pluto average deviation: 130.44837219131017 km

        Year: 580 body: Mercury average deviation: 123.95238105408244 km
        Year: 580 body: Venus average deviation: 2.3875733413687117 km
        Year: 580 body: Earth average deviation: 103.78842965597421 km
        Year: 580 body: Moon average deviation: 8248.56100279333 km
        Year: 580 body: Mars average deviation: 472.1275574218172 km
        Year: 580 body: Jupiter average deviation: 1120.283228868307 km
        Year: 580 body: Saturn average deviation: 26.33411600747299 km
        Year: 580 body: Uranus average deviation: 431.18499103652096 km
        Year: 580 body: Neptune average deviation: 343.5266490381035 km
        Year: 580 body: Pluto average deviation: 237.01167498695162 km
     */
}
