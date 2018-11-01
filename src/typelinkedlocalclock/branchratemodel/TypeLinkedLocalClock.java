package typelinkedlocalclock.branchratemodel;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.tree.MultiTypeTree;
import beast.evolution.tree.Node;

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
    protected RealParameter meanRate;
    protected double [] meanBranchRates;
    protected boolean recompute = false;

    @Override
    public void initAndValidate() {

        // Read tree
        m_tree = treeInput.get();


        // Check limits and dimension of rates associated with types
        RealParameter rates = rateParamInput.get();
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


    }

    @Override
    public double getRateForBranch(Node node) {

        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads
        synchronized (this) {
            if (recompute) {
                recalculateMeanBranchRates();
                recompute = false;
            }
        }

        return meanBranchRates[getNodeNumber(node)];
    }


    /**
     * For each branch in the tree calculate the mean rate across a branch in a MultiTypeTree
     *
     * i.e. Calculate the duration spent in each type along the branch, associate rates and get average.
     */
    private void recalculateMeanBranchRates() {

    }


    private int getNodeNumber(Node node) {
        int nodeNr = node.getNr();  // Why would node ever have a number greater than the root?
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
