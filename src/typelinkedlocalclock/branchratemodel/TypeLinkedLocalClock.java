package typelinkedlocalclock.branchratemodel;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.tree.MultiTypeNode;
import beast.evolution.tree.MultiTypeTree;
import beast.evolution.tree.Node;

import java.util.Arrays;

/**
 * @author Louis du Plessis
 * @date   2018/11/01
 */
public class TypeLinkedLocalClock extends BranchRateModel.Base {

    final public Input<RealParameter> rateParamInput =
            new Input<>("rates",
                        "the rate parameters associated with types in the tree for sampling of individual rates among branches.",
                         Input.Validate.REQUIRED);

    final public Input<MultiTypeTree> treeInput =
            new Input<>("tree", "the multitypetree this local clock is associated with.", Input.Validate.REQUIRED);


    protected MultiTypeTree m_tree;
    protected RealParameter rates,
                            meanRate;

    protected double [] meanBranchRates;
    protected boolean recompute = true;

    @Override
    public void initAndValidate() {

        // Read tree
        m_tree = treeInput.get();

        // Check limits and dimension of rates associated with types
        rates = rateParamInput.get();
        if (rates.lowerValueInput.get() == null || rates.lowerValueInput.get() < 0.0) {
            rates.setLower(0.0);
        }
        if (rates.upperValueInput.get() == null || rates.upperValueInput.get() < 0.0) {
            rates.setUpper(Double.MAX_VALUE);
        }
        if (rates.getDimension() != m_tree.getTypeSet().getNTypes()) {
            Log.warning.println("TypeLinkedLocalClock::Setting dimension of rates to " + (m_tree.getTypeSet().getNTypes()));
            rates.setDimension(m_tree.getTypeSet().getNTypes());
        }

        // Do we want to allow a meanRate (across types and branches) to be set and fixed?
        meanRate = meanRateInput.get();
        if (meanRate == null) {
            meanRate = new RealParameter("1.0");
        }

        // Initialise mean rates for each branch
        meanBranchRates = new double[m_tree.getNodeCount()];
        System.out.println(meanBranchRates.length);

    }


    @Override
    public double getRateForBranch(Node node) {

        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads
        synchronized (this) {
            if (recompute) {
                recalculateMeanBranchRatesIterative(m_tree, rates, meanBranchRates);
                //recalculateMeanBranchRatesRecursive(m_tree, rates, meanBranchRates);
                recompute = false;
            }
        }

        return meanBranchRates[node.getNr()];
    }


    /**
     * Fill the array typeTimes with the amount of time that the branch leading from MultiTypeNode node spends in
     * each type.
     *
     * Note that typeTimes needs to be the same length as the number of types or else an ArrayIndexOutOfBoundsException
     * will be thrown. (Throwing an exception is less well-behaved, but is faster than checking the number of types and
     * there is no easy way to check the number of types from MultiTypeNode.
     * (method getChangeTypes() does not exist and changeTypes ArrayList is private).
     *
     * Array is passed by pointer instead of initialized and returned at each method call to save on
     * garbage collection.
     *
     * @param node
     * @param typeTimes
     */
    public void calculateTimesInTypes(MultiTypeNode node, double [] typeTimes, boolean normalize) throws ArrayIndexOutOfBoundsException {

        double  increment,
                totalHeight = 0,
                currHeight,
                prevHeight = node.getHeight();
        int currType = node.getNodeType();

        // Clear array
        for (int i = 0; i < typeTimes.length; i++)
            typeTimes[i] = 0;

        // Add time spent in each type
        for (int i = 0; i < node.getChangeCount(); i++) {
            currHeight = node.getChangeTime(i);
            increment  = (currHeight - prevHeight);

            totalHeight += increment;
            typeTimes[currType] += increment;

            currType   = node.getChangeType(i);
            prevHeight = currHeight;
        }
        typeTimes[node.getFinalType()] += (node.getLength() - totalHeight);

        // Normalize to proportion of branch length spent in each type
        if (normalize) {
            double branchLength = node.getLength();

            if (branchLength == 0.0) {
                // Zero-length branches require special treatment or else NaNs will result

                // If any type spends more than 0.0 time in a zero-length branch throw an exception
                for (int i = 0; i < typeTimes.length; i++) {
                    if (typeTimes[i] > 0.0) {
                        throw new RuntimeException("ERROR: Zero-length branch cannot spend more than 0.0 time units in any type trait.");
                    }
                }

                // Assign type of the node to the complete branch (of length 0.0)
                typeTimes[node.getNodeType()] = 1;

            } else {
                // Calculate a weighted average if branchlength > 0
                for (int i = 0; i < typeTimes.length; i++) {
                    typeTimes[i] = typeTimes[i]/branchLength;
                }
            }

        }

    }

    /**
     * Recursive implementation
     *
     * @param tree
     * @param rates
     */
    private void recalculateMeanBranchRatesRecursive(MultiTypeTree tree, RealParameter rates, double [] meanBranchRates) {

        MultiTypeNode root = (MultiTypeNode) tree.getRoot();
        double [] typeTimes = new double[rates.getDimension()];

        calculateMeanBranchRate(root, rates, typeTimes, meanBranchRates);
    }


    private void calculateMeanBranchRate(MultiTypeNode node, RealParameter rates,
                                         double [] typeTimes, double [] meanBranchRates) {

        calculateTimesInTypes(node, typeTimes, true);

        int nodeNr = node.getNr();
        meanBranchRates[nodeNr] = 0;
        for (int j = 0; j < typeTimes.length; j++) {
            meanBranchRates[nodeNr] += typeTimes[nodeNr]*rates.getValue(nodeNr);
        }

        // Leaves don't have children, so no need to check if node is a leaf
        for (int j = 0; j < node.getChildCount(); j++) {
            calculateMeanBranchRate( (MultiTypeNode) node.getChild(j), rates, typeTimes, meanBranchRates);
        }

        /*
        if (!node.isLeaf()) {
            calculateMeanBranchRate( (MultiTypeNode) node.getLeft(), rates, typeTimes, meanBranchRates);
            calculateMeanBranchRate( (MultiTypeNode) node.getRight(), rates, typeTimes, meanBranchRates);
        }
        */

    }



    /**
     * Iterative implementation
     * d
     * For each branch in the tree calculate the mean rate across a branch in a MultiTypeTree
     *
     * i.e. Calculate the duration spent in each type along the branch, associate rates and get average.
     */
    private void recalculateMeanBranchRatesIterative(MultiTypeTree tree, RealParameter rates, double [] meanBranchRates) {

        double [] typeTimes = new double[rates.getDimension()];

        for (int i = 0; i < tree.getNodeCount(); i++) {
            calculateTimesInTypes( (MultiTypeNode) tree.getNode(i), typeTimes, true);

            meanBranchRates[i] = 0;
            for (int j = 0; j < typeTimes.length; j++) {
                meanBranchRates[i] += typeTimes[j]*rates.getValue(j);
            }
        }
    }

    /**
     * Inherited from RandomLocalClockModel.java:
     *
     * This method is not useful - why would node ever have a number greater than the root?
     * (not used any longer)
     *
     * @param node
     * @return
     */
    private int getNodeNumber(Node node) {
        int nodeNr = node.getNr();
        if (nodeNr > m_tree.getRoot().getNr()) {
            nodeNr--;
        }
        return nodeNr;
    }

    /* Calculation node methods */

    @Override
    protected boolean requiresRecalculation() {
        // this is only called if any of its inputs is dirty, hence we need to recompute
        recompute = true;
        return true;
    }

    @Override
    protected void store() {
        recompute = true;
        super.store();
    }

    @Override
    protected void restore() {
        recompute = true;
        super.restore();
    }
}
