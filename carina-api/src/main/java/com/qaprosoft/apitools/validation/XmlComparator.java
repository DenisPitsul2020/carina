package com.qaprosoft.apitools.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

public class XmlComparator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private XmlComparator() {
    }

    /**
     * comparison with strict array ordering.
     */
    public static void strictCompare(String actualXmlData, String expectedXmlData) {
        XmlAssert.assertThat(actualXmlData).and(expectedXmlData)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .areIdentical();
    }

    /**
     * comparison with non-strict array ordering.
     */
    public static void nonStrictOrderCompare(String actualXmlData, String expectedXmlData) {
        Diff differences = DiffBuilder.compare(expectedXmlData).withTest(actualXmlData)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withDifferenceEvaluator((comparison, outcome) -> {
                    if (outcome == ComparisonResult.DIFFERENT) {
                        outcome = ascendingBruteForceParentNodesComparison(comparison, outcome);
                    }
                    return outcome;
                }).checkForSimilar().build();
        Assert.assertFalse(differences.hasDifferences());
    }

    private static ComparisonResult ascendingBruteForceParentNodesComparison(Comparison comparison, ComparisonResult outcome) {
        List<Node> parentTestNodes = constructParentNodesHierarchy(comparison.getTestDetails().getTarget());
        Node controlNode = comparison.getControlDetails().getTarget();
        for (Node parentTestNode : parentTestNodes) {
            Node foundNode = findEqualNodeInHierarchy(controlNode, parentTestNode);
            if (foundNode != null && areNodesAtTheSameHierarchyLevel(controlNode, foundNode)) {
                return ComparisonResult.SIMILAR;
            }
        }
        throw new AssertionError("Unable to find testNode '" + controlNode.getNodeName() + "'.");
    }

    private static boolean areNodesAtTheSameHierarchyLevel(Node controlNode, Node testNode) {
        List<Node> parentControlNodes = constructParentNodesHierarchy(controlNode);
        List<Node> parentTestNodes = constructParentNodesHierarchy(testNode);
        if (parentControlNodes.size() != parentTestNodes.size()) {
            LOGGER.info("Size of parent test nodes: " + parentTestNodes.size() +
                    ", size of parent control nodes: " + parentControlNodes.size()
                    + ". XML files are considered different because of different target nodes" +
                    " placement in the hierarchy.");
            return false;
        }
        for (int i = 0; i < parentControlNodes.size(); ++i) {
            Node parentControlNode = parentControlNodes.get(i);
            Node parentTestNode = parentTestNodes.get(i);
            if (!parentControlNode.getNodeName().equals(parentTestNode.getNodeName())) {
                throw new AssertionError(("Parent control node '" + parentControlNode.getNodeName()
                        + "' at URI: " + parentControlNode.getBaseURI() + " is different than parent test node '"
                        + parentTestNode.getNodeName() + "' at URI: " + parentTestNode.getBaseURI()
                        + ". Comparison failed."));
            }
        }
        return true;
    }

    private static Node findEqualNodeInHierarchy(Node controlNode, Node testNode) {
        if (controlNode.isEqualNode(testNode)) {
            return testNode;
        }
        NodeList children = testNode.getChildNodes();
        if (children == null) {
            return null;
        }
        for (int i = 0; i < children.getLength(); ++i) {
            Node childNode = children.item(i);
            Node foundNode = findEqualNodeInHierarchy(controlNode, childNode);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }

    private static List<Node> constructParentNodesHierarchy(Node node) {
        List<Node> parentNodes = new ArrayList<>();
        Node localParentNode = node.getParentNode();
        while (localParentNode != null) {
            parentNodes.add(localParentNode);
            localParentNode = localParentNode.getParentNode();
        }
        return parentNodes;
    }
}

