package typelinkedlocalclock.branchratemodel;

import beast.evolution.tree.MultiTypeNode;
import beast.evolution.tree.MultiTypeTreeFromNewick;
import junit.framework.TestCase;
import org.junit.Test;
import test.beast.BEASTTestCase;

import java.util.Arrays;

public class TypeLinkedLocalClockTest extends TestCase {


    @Test
    public void testTwoTypeTreeTimesinTypes() {

        System.out.println("Test calculateTimesInTypes on a 2-type MTT");

        // Assemble test MultiTypeTree:
        String newickStr = "(((3[&location=1]:0.5)[&location=0,reaction=Migration]:2.0)[&location=1,reaction=Migration]:0.8,(((((6[&location=0]:0.25,4[&location=0]:0.2)[&location=0,reaction=Coalescence]:0.25,(5[&location=0]:0.2,((1[&location=1]:0.05,2[&location=1]:0.05)[&location=1,reaction=Coalescence]:0.25)[&location=0,reaction=Migration]:0.1)[&location=0,reaction=Coalescence]:0.1)[&location=0,reaction=Coalescence]:0.1)[&location=1,reaction=Migration]:0.5)[&location=0,reaction=Migration]:0.15)[&location=1,reaction=Migration]:1.5)[&location=1,reaction=Coalescence]:0.0;";

        MultiTypeTreeFromNewick mtt = new MultiTypeTreeFromNewick();
        mtt.initByName(
                "value", newickStr,
                "typeLabel", "location");

        TypeLinkedLocalClock clock = new TypeLinkedLocalClock();
        clock.initByName("tree", mtt, "rates", "1 2");

        // Calculated by hand
        double [][] expectedTimes = new double [][] {{0.0,  0.05},
                                                     {0.0,  0.05},
                                                     {2.0,  1.3},
                                                     {0.2,  0.0},
                                                     {0.2,  0.0},
                                                     {0.25, 0.0},
                                                     {0.25, 0.0},
                                                     {0.1,  0.25},
                                                     {0.1,  0.0},
                                                     {0.25, 2.0},
                                                     {0.0,  0.0}};
        double [] typeTimes = new double[2];


        for (int i = 0; i < mtt.getNodeCount(); i++) {
            clock.calculateTimesInTypes( (MultiTypeNode) mtt.getNode(i), typeTimes, false);

            //System.out.println(i+":\t"+Arrays.toString(typeTimes));
            for (int j = 0; j < typeTimes.length; j++) {
                assertEquals(expectedTimes[i][j], typeTimes[j], BEASTTestCase.PRECISION);
            }
        }
    }


    @Test
    public void testThreeTypeTreeTimesinTypes() {

        System.out.println("Test calculateTimesInTypes on a 3-type MTT");

        // Assemble test MultiTypeTree:
        String newickStr = "((((((1[&location=1]:0.04)[&location=0,reaction=Migration]:0.05)[&location=1,reaction=Migration]:0.06)[&location=2,reaction=Migration]:0.3)[&location=1,reaction=Migration]:0.15,(((3[&location=0]:0.3)[&location=1,reaction=Migration]:0.05)[&location=0,reaction=Migration]:0.25)[&location=1,reaction=Migration]:0.1)[&location=1,reaction=Coalescence]:0.525,(((2[&location=1]:0.6)[&location=2,reaction=Migration]:0.125)[&location=0,reaction=Migration]:0.4)[&location=1,reaction=Migration]:0.25)[&location=1,reaction=Coalescence]:0.0;";

        MultiTypeTreeFromNewick mtt = new MultiTypeTreeFromNewick();
        mtt.initByName(
                "value", newickStr,
                "typeLabel", "location");

        TypeLinkedLocalClock clock = new TypeLinkedLocalClock();
        clock.initByName("tree", mtt, "rates", "1 2 4");

        // Calculated by hand
        double [][] expectedTimes = new double [][] {{0.05, 0.25,  0.3},
                                                     {0.4,  0.85,  0.125},
                                                     {0.55, 0.15,  0.0},
                                                     {0.0,  0.525, 0.0},
                                                     {0.0,  0.0,   0.0}};
        double [] typeTimes = new double[3];


        for (int i = 0; i < mtt.getNodeCount(); i++) {
            clock.calculateTimesInTypes( (MultiTypeNode) mtt.getNode(i), typeTimes, false);

            //System.out.println(i+":\t"+Arrays.toString(typeTimes));
            for (int j = 0; j < typeTimes.length; j++) {
                assertEquals(expectedTimes[i][j], typeTimes[j], BEASTTestCase.PRECISION);
            }
        }
    }



}
