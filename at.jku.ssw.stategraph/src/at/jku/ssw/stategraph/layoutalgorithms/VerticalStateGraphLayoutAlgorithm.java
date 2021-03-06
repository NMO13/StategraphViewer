package at.jku.ssw.stategraph.layoutalgorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.ManhattanConnectionRouter;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.swt.graphics.Color;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.layouts.LayoutEntity;
import org.eclipse.zest.layouts.algorithms.AbstractLayoutAlgorithm;
import org.eclipse.zest.layouts.dataStructures.DisplayIndependentPoint;
import org.eclipse.zest.layouts.dataStructures.DisplayIndependentRectangle;
import org.eclipse.zest.layouts.dataStructures.InternalNode;
import org.eclipse.zest.layouts.dataStructures.InternalRelationship;

import at.jku.ssw.stategraph.testmodel.StateModelNode;
import at.jku.ssw.stategraph.widgets.LeftSideAnchor;
import at.jku.ssw.stategraph.widgets.RightSideAnchor;
import at.jku.ssw.stategraph.widgets.StateGraphConnection;
import at.jku.ssw.stategraph.widgets.StateGraphConnectionRouter;
import at.jku.ssw.stategraph.widgets.StateGraphLabel;
import at.jku.ssw.stategraph.widgets.StateGraphNode;

// Comment: Nodes-Names have to be unique!!!!
public class VerticalStateGraphLayoutAlgorithm extends AbstractLayoutAlgorithm{

	private double minNodeHeight; // a node can't be smaller in heigth than minNodeHeight
	private double minNodeWidth;  // a node can't be smaller in width than minNodeWidth
	private double offsetXToParent;
	private double offsetYToParent;
	private double offsetYToSibling;
	private InternalLayoutStateNode root;
	private ModelLayoutStyle style;
	private int maxDepth;
	private DisplayIndependentRectangle layoutBounds;
	
	public ModelLayoutStyle getModelLayoutStyle() {
		return style;
	}

	public void setModelLayoutStyle(ModelLayoutStyle style) {
		this.style = style;
	}

	public VerticalStateGraphLayoutAlgorithm(int styles) {
		super(styles);
		maxDepth = -1;
		minNodeHeight = 40;
		minNodeWidth = 200;
		offsetXToParent = 30;
		offsetYToParent = 10;
		offsetYToSibling = 10;
		style = ModelLayoutStyle.Horizontal;
	}

	@Override
	public void setLayoutArea(double x, double y, double width, double height) {
	
	}

	@Override
	protected boolean isValidConfiguration(boolean asynchronous,
			boolean continuous) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected void postLayoutAlgorithm(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int getTotalNumberOfLayoutSteps() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int getCurrentLayoutStep() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	protected void applyLayoutInternal(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider, double boundsX,
			double boundsY, double boundsWidth, double boundsHeight) {
		
		layoutNodes();
		// set graph position to the middle of window
		alignGraph(entitiesToLayout);
		updateLayoutLocations(entitiesToLayout);
		// set connections
		deleteConnectionPoints(entitiesToLayout);
		setConnections(entitiesToLayout);
	}
	
	protected void alignGraph(InternalNode[] entitiesToLayout) {
		double newPos = (layoutBounds.width - root.getInternalNode().getInternalWidth()) / 2;
		for(InternalNode i : entitiesToLayout) {
			i.setInternalLocation(i.getInternalX() + newPos, i.getInternalY());
		}
		int i = 0;
	}

	void deleteConnectionPoints(InternalNode[] entitiesToLayout) {
		// set all points to 0 so that no connection gets drawn that
		// must no be drawn
		for(InternalNode i : entitiesToLayout) {
			StateGraphNode sgn = getStateGraphNodeFromInternalNode(i);
			List<StateGraphConnection> sources = sgn.getSourceConnections();
			List<StateGraphConnection> targets = sgn.getTargetConnections();
			for(int j = 0; j < sources.size(); j++){
				StateGraphConnection sourceCon = (StateGraphConnection) sources.get(j);
				int p[] = {0, 0, 0, 0};
				sourceCon.getConnectionFigure().setPoints(new PointList(p));
			}
		}
	}

	private void setConnections(InternalNode[] entitiesToLayout) {
		// draw the appropriate connections by setting the length
		// do that by performing a breath first search from root downwards
		List<InternalLayoutStateNode> fringe = new ArrayList<InternalLayoutStateNode>();
		fringe.add(root);
		// every common parent gets an entry into the list -> key
		// value: a list of all connections lines on this parent
		// every list saves connection lines sorted by their vertical lengths
		HashMap<GraphNode, List<InternalLayoutStateConnection>> internalConnectionList = new HashMap<GraphNode, List<InternalLayoutStateConnection>>();
		while(fringe.size() > 0) {
			InternalLayoutStateNode n = fringe.remove(0);
			StateGraphNode sgn = getStateGraphNodeFromInternalNode(n.getInternalNode());
			List<GraphConnection> sources = sgn.getSourceConnections();
			for(int j = 0; j < sources.size(); j++){
				// search for common parent and register it
				// create internalconnection class for convenience
				GraphNode commonParent = findCommonParent(sources.get(j).getSource(), sources.get(j).getDestination(), entitiesToLayout);
				InternalLayoutStateConnection ilsc = new InternalLayoutStateConnection(sources.get(j));
				ilsc.setCommonParent(commonParent);
				calcDistance(sources.get(j), ilsc);
				// sorting criteria: vert. length of each connection line
				insertSortedIntoConnectionList(internalConnectionList, ilsc);
			}	
			for(InternalLayoutStateNode k : n.getChildren())
				fringe.add(k);
		}
		
		
		Collection<List<InternalLayoutStateConnection>> c = internalConnectionList.values();
		Iterator<List<InternalLayoutStateConnection>> it = c.iterator();
		while(it.hasNext()) {
			List<InternalLayoutStateConnection> l = (List<InternalLayoutStateConnection>) it.next();
			double oldLengthRight = 0;
			double oldLengthLeft = 0;
			int increaserRight = 5;
			int increaserLeft = 5;
			for(int j = 0; j < l.size(); j++) {
				// calc distance from node to root
				// do that for source and destination node
				
				if(l.get(j).isTargetConOnRightSide) {
					if(oldLengthRight != l.get(j).distance) {
						oldLengthRight = l.get(j).distance;
						increaserRight += 7;
					}
					setConnectionOnRightSide(l, j, increaserRight);
				}
				else {
					if(oldLengthLeft != l.get(j).distance) {
						oldLengthLeft = l.get(j).distance;
						increaserLeft += 7;
					}
					setConnectionOnLeftSide(l, j, increaserLeft);
				}
				
			}
		}	
	}
	
	private void setConnectionOnLeftSide(
			List<InternalLayoutStateConnection> l, int j, int verticalLength) {
		LayoutEntity s = l.get(j).getGraphConnection().getSource().getLayoutEntity();
		LayoutEntity d = l.get(j).getGraphConnection().getDestination().getLayoutEntity();
		LayoutEntity parent = l.get(j).getCommonParent().getLayoutEntity();
		// source
		double pos = s.getXInLayout();
		double diff = pos - parent.getXInLayout();
		assert(diff > 0);
		l.get(j).setWidthSourceSide(verticalLength);
		double ratio = verticalLength/diff;
		assert(ratio > 0 && ratio < 1);
		double anchorPosY = s.getHeightInLayout() - s.getHeightInLayout()*ratio;
		StateGraphConnection stateGraphCon = (StateGraphConnection) l.get(j).getGraphConnection();
		stateGraphCon.setLineColor(ColorConstants.black);
		stateGraphCon.setHighlightColor(ColorConstants.red);
		stateGraphCon.getConnectionFigure().setSourceAnchor(new LeftSideAnchor(stateGraphCon.getSource().getNodeFigure(), anchorPosY));
	
		// destination
		pos = d.getXInLayout() + d.getWidthInLayout();
		diff = parent.getXInLayout()+parent.getWidthInLayout() - pos;
		assert(diff > 0);
		l.get(j).setWidthSourceSide(verticalLength);
		ratio = verticalLength/diff;
		assert(ratio > 0 && ratio < 1);
		anchorPosY = s.getHeightInLayout()*ratio;
		stateGraphCon.getConnectionFigure().setTargetAnchor(new LeftSideAnchor(stateGraphCon.getDestination().getNodeFigure(), anchorPosY));
		
		// points
		//int p[] = {0, 0,  (int) (s.getXInLayout()-length), 0, (int) (s.getXInLayout()-length), 0, 0, 0};
		int p[] = {0, 0,  (int) (parent.getXInLayout() + offsetXToParent - verticalLength), 0, (int) (parent.getXInLayout() + offsetXToParent -verticalLength), 0, 0, 0};
		stateGraphCon.getConnectionFigure().setPoints(new PointList(p));
		
		// arrow
		PolygonDecoration decoration = new PolygonDecoration();
		((PolylineConnection)stateGraphCon.getConnectionFigure()).setTargetDecoration(decoration);
	}
	
	private void setConnectionOnRightSide(
			List<InternalLayoutStateConnection> l, int j, int length) {
		
		LayoutEntity s = l.get(j).getGraphConnection().getSource().getLayoutEntity();
		LayoutEntity d = l.get(j).getGraphConnection().getDestination().getLayoutEntity();
		LayoutEntity parent = l.get(j).getCommonParent().getLayoutEntity();
		//LayoutEntity parent = l.get(j).getCommonParent().getLayoutEntity();
		// source
		double pos = s.getXInLayout() + s.getWidthInLayout();
		double diff = parent.getXInLayout()+parent.getWidthInLayout() - pos;
		assert(diff > 0);
		l.get(j).setWidthSourceSide(length);
		double ratio = length/diff;
		assert(ratio > 0 && ratio < 1);
		double anchorPosY = s.getHeightInLayout()*ratio;
		StateGraphConnection stateGraphCon = (StateGraphConnection) l.get(j).getGraphConnection();
		stateGraphCon.setLineColor(ColorConstants.black);
		stateGraphCon.setHighlightColor(ColorConstants.red);
		stateGraphCon.getConnectionFigure().setSourceAnchor(new RightSideAnchor(stateGraphCon.getSource().getNodeFigure(), anchorPosY));
		
		// destination
		pos = d.getXInLayout() + d.getWidthInLayout();
		diff = parent.getXInLayout()+parent.getWidthInLayout() - pos;
		assert(diff > 0);
		l.get(j).setWidthSourceSide(length);
		ratio = length/diff;
		assert(ratio > 0 && ratio < 1);
		anchorPosY = s.getHeightInLayout() - s.getHeightInLayout()*ratio;
		stateGraphCon.getConnectionFigure().setTargetAnchor(new RightSideAnchor(stateGraphCon.getDestination().getNodeFigure(), anchorPosY));
		
		// points
		int p[] = {0, 0,  (int) (parent.getXInLayout()+parent.getWidthInLayout()-offsetXToParent+length), 0, (int) (parent.getXInLayout()+parent.getWidthInLayout()-offsetXToParent+length), 0, 0, 0};
		stateGraphCon.getConnectionFigure().setPoints(new PointList(p));
		
		// arrow
		PolygonDecoration decoration = new PolygonDecoration();
		((PolylineConnection)stateGraphCon.getConnectionFigure()).setTargetDecoration(decoration);
	}

	private void insertSortedIntoConnectionList(
			HashMap<GraphNode, List<InternalLayoutStateConnection>> internalConnectionList,
			InternalLayoutStateConnection ilsc) {
		List<InternalLayoutStateConnection> l = internalConnectionList.get(ilsc.getCommonParent());
		if(l == null) {
			ArrayList<InternalLayoutStateConnection> il = new ArrayList<VerticalStateGraphLayoutAlgorithm.InternalLayoutStateConnection>();
			il.add(ilsc);
			internalConnectionList.put(ilsc.getCommonParent(), il);
		}
		else {
			int i = 0;
			while(i < l.size() && l.get(i).getDistance() < ilsc.getDistance())
				i++;
			l.add(i, ilsc);
		}
		return;
		
	}

	private GraphNode findCommonParent(GraphNode graphNode, GraphNode graphNode2, InternalNode[] entitiesToLayout) {
		
		StateModelNode commonParent = null;
		StateModelNode source = (StateModelNode) graphNode.getData();
		StateModelNode dest = (StateModelNode) graphNode2.getData();
		List<StateModelNode> sList = new ArrayList<StateModelNode>();
		List<StateModelNode> dList = new ArrayList<StateModelNode>();
		StateModelNode temp = source.getParent();
		while(temp != null) {
			sList.add(temp);
			temp = temp.getParent();
			
		}
		temp = dest.getParent();
		while(temp != null) {
			dList.add(temp);
			temp = temp.getParent();
		}
		int iter1 = 0, iter2 = 0;
		while(iter1 < sList.size()) {
			iter2 = 0;
			while(iter2 < dList.size()) {
				if(sList.get(iter1) == dList.get(iter2)) {
					commonParent =  sList.get(iter1);
					System.out.println("Common parent found: " + commonParent);
					return findGraphNodeInInternalNodeList(commonParent, entitiesToLayout);
				}
				iter2++;
			}
			iter1++;
		}
		return null;
	}
	
	private void calcDistance(GraphConnection stateGraphConnection, InternalLayoutStateConnection ilsc) {
		GraphNode s = stateGraphConnection.getSource();
		GraphNode d = stateGraphConnection.getDestination();
		double diff = s.getLayoutEntity().getYInLayout() - d.getLayoutEntity().getYInLayout();
		if(diff > 0)
			ilsc.setConIsRight(true);
		else
			ilsc.setConIsRight(false);
		ilsc.setDistance(Math.abs(diff));
	}
		
	private GraphNode findGraphNodeInInternalNodeList(StateModelNode n, InternalNode[] entitiesToLayout) {
		if(n == null)
			return null;
		for(InternalNode in : entitiesToLayout) {
			GraphNode gn = getStateGraphNodeFromInternalNode(in);
			StateModelNode smn = (StateModelNode) gn.getData();
			if(smn == n)
				return gn;
		}
		return null;
	}
	
	private DisplayIndependentPoint setHeightAndYPosition(InternalLayoutStateNode node, DisplayIndependentPoint p) {
		DisplayIndependentPoint size = new DisplayIndependentPoint(0, 0);
		DisplayIndependentPoint pointer = new DisplayIndependentPoint(p.x, p.y);
		for(int i = 0; i < node.getChildren().length; i++) {
			double gapHeight = 0.0;
			if(i == 0) {
				gapHeight = node.getChildren()[i].offsetYToParent;
				if(node.children.size() != 0)
					gapHeight += node.getStateNameHeight();
			}
			else
				gapHeight = node.getChildren()[i].offsetYToSibling;
			pointer.y += gapHeight;
			size.y += gapHeight;
			
			node.getChildren()[i].getInternalNode().setInternalLocation(0.0, pointer.y);
			DisplayIndependentPoint tsize = setHeightAndYPosition(node.getChildren()[i], pointer);
			size.y += tsize.y;
		}
		
		double heightVal = 0.0;
		if(node.getChildren().length == 0) {
			node.getInternalNode().setInternalSize(0.0, minNodeHeight);
			heightVal = minNodeHeight;
			pointer.y += heightVal;
		}
		else {
			double y = size.y + node.getChildren()[0].offsetYToParent;
			node.getInternalNode().setInternalSize(0.0, y);
			heightVal = y;
			pointer.y += node.getChildren()[0].offsetYToParent;
		}
		size.y = heightVal;
		p.y = pointer.y;
		return size;
		
	}

	protected void layoutNodes() {
		if(root != null) {
			// sets the height and y-position of nodes
			root.getInternalNode().setInternalLocation(0, 0);
			setHeightAndYPosition(root, new DisplayIndependentPoint(0.0, 0.0));
			
			// set width
			setWidth(root, 0);
			
			setXPosition(root);
		}
	}
	
	private void setXPosition(InternalLayoutStateNode node) {
		for(int i = 0; i < node.getChildren().length; i++) {
			double posX = (node.getInternalNode().getInternalWidth() - node.getChildren()[i].getInternalNode().getInternalWidth()) / 2 + node.getInternalNode().getInternalX();
			node.getChildren()[i].getInternalNode().setInternalLocation(posX, node.getChildren()[i].getInternalNode().getInternalY());
			if(node.getChildren()[i].getChildren().length != 0)
				setXPosition(node.getChildren()[i]);
		}
	}
	
	private double setWidth(InternalLayoutStateNode node, int curLevel) {
		double biggestNodeWidth = 0;

		for(int i = 0; i < node.getChildren().length; i++) {
			double w = setWidth(node.getChildren()[i], curLevel++);
			if( w > biggestNodeWidth) {
				biggestNodeWidth = w;
			}
		}
		
		if(biggestNodeWidth == 0)
			biggestNodeWidth = minNodeWidth;
		
		// adjust all child nodes to fill empty space
		for(int i = 0; i < node.getChildren().length; i++) {
			// if child is a leave node
			// set child's node width to biggest node width
//			if(node.getChildren()[i].getChildren().length == 0)
//				node.getChildren()[i].getInternalNode().setInternalSize(biggestNodeWidth - 2 * offsetXToParent, node.getChildren()[i].getInternalNode().getInternalHeight());
//			else
				// make recursive adjustment for a non leave node
				adjustNonLeaveNode(node.getChildren()[i], biggestNodeWidth - 2 * offsetXToParent);
		}
		// set node width
		node.getInternalNode().setInternalSize(biggestNodeWidth, node.getInternalNode().getInternalHeight());
		return biggestNodeWidth + 2 * offsetXToParent;
	}

	private void adjustNonLeaveNode(InternalLayoutStateNode node, double biggestNodeWidth) {
		for(int i = 0; i < node.getChildren().length; i++) {
			adjustNonLeaveNode(node.getChildren()[i], biggestNodeWidth - 2 * offsetXToParent);
		}
		node.getInternalNode().setInternalSize(biggestNodeWidth, node.getInternalNode().getInternalHeight());
	}


	@Override
	protected void preLayoutAlgorithm(InternalNode[] entitiesToLayout,
			InternalRelationship[] relationshipsToConsider, double x, double y,
			double width, double height) {
		layoutBounds = new DisplayIndependentRectangle(x, y, width, height);
		InternalNode root = findRoot(entitiesToLayout, /*"Coolant_temp"*/"TestStateModel" /*"Outside_temp"*//*"Key_pos"*/);
		try {
			this.root = createInternalLayoutNode(root, null, offsetXToParent, offsetYToParent, offsetYToSibling, 0.0);
			rebuildTree(entitiesToLayout, this.root, getStateModelNodeFromInternalNode(root));
		} catch (NullPointerException e) {
			System.out.println("-------------------");
			System.out.println("Root not found!");
			System.out.println("-------------------");
		}
	}

	/**
	 * entitiesToLayout is just a plain array. This method rebuilds a tree from the given array
	 * recusively. 
	 * 
	 * @param entitiesToLayout
	 */
	private void rebuildTree(InternalNode[] entitiesToLayout, InternalLayoutStateNode ilnParent, StateModelNode smnParent) {		
		// find all internal nodes that belong to the child nodes of parent
		int j = 0;
		for(int i = 0; i < entitiesToLayout.length; i++) {
			if(j == smnParent.getChildren().length)
				break;
			StateModelNode smNode = getStateModelNodeFromInternalNode(entitiesToLayout[i]);
			if(smNode == smnParent.getChildren()[j]) {
				InternalLayoutStateNode iNode = createInternalLayoutNode(entitiesToLayout[i], ilnParent,
						offsetXToParent, offsetYToParent, offsetYToSibling, 10);
				ilnParent.addChild(iNode);
				j++;
				rebuildTree(entitiesToLayout, iNode, smNode);
			}
		}
	}

	private InternalLayoutStateNode createInternalLayoutNode(
			InternalNode entity, InternalLayoutStateNode parent,
			double offXPar, double offYPar, double offXSib, double offYSib) {
		InternalLayoutStateNode node = new InternalLayoutStateNode(entity, parent);
		StateModelNode smNode = getStateModelNodeFromInternalNode(entity);
		node.setDepth(smNode.getDepth());
		if(smNode.getDepth() > maxDepth)
			maxDepth = smNode.getDepth();
		node.setValue((String) smNode.getValue().toString());
		node.updateOffsetToParent(offXPar, offYPar);
		node.updateOffsetToSibling(offXSib, offYSib);
		
		
		StateGraphLabel sgl = (StateGraphLabel) ((GraphNode)entity.getLayoutEntity().getGraphData()).getNodeFigure();
		node.setStateNameHeight(sgl.getStateNameHeight());
		node.setStateNameWidth(sgl.getStateNameWidth());
		return node;
	}

	private InternalNode findRoot(InternalNode[] entitiesToLayout, String name) {
		for(int i = 0; i < entitiesToLayout.length; i++) {
			StateModelNode node = getStateModelNodeFromInternalNode(entitiesToLayout[i]);
			if(node.getValue().equals(name))
				return entitiesToLayout[i];
		}
		return null;
	}
	private InternalNode findRoot(InternalNode[] entitiesToLayout) {
		for(int i = 0; i < entitiesToLayout.length; i++) {
			StateModelNode node = getStateModelNodeFromInternalNode(entitiesToLayout[i]);
			if(node.getParent() == null)
				return entitiesToLayout[i];
		}
		return null;
	}
	
	private StateModelNode getStateModelNodeFromInternalNode(InternalNode node) {
		return (StateModelNode) ((GraphNode)node.getLayoutEntity().getGraphData()).getData();
	}
	
	private StateGraphNode getStateGraphNodeFromInternalNode(InternalNode node) {
		return (StateGraphNode) node.getLayoutEntity().getGraphData();
	}
	
	class InternalLayoutStateConnection {
		private GraphConnection con;
		private GraphNode commonParent;
		private boolean isTargetConOnRightSide;
		private double distance;
		
		public GraphNode getCommonParent() {
			return commonParent;
		}

		public double getDistance() {
			return distance;
		}

		public void setWidthSourceSide(int i) {
			// TODO Auto-generated method stub
			
		}

		public void setDistance(double abs) {
			distance = abs;
			
		}

		public void setCommonParent(GraphNode commonParent) {
			this.commonParent = commonParent;
		}

		InternalLayoutStateConnection(GraphConnection graphConnection) {
			this.con = graphConnection;
		}
		
		public GraphConnection getGraphConnection() {
			return con;
		}

		public void setConIsRight(boolean targetConIsBelow) {
			this.isTargetConOnRightSide = targetConIsBelow;
		}

		public boolean isConOnRightSide() {
			return isTargetConOnRightSide;
		}
	}
	
	class InternalLayoutStateNode {
		private InternalLayoutStateNode parent;
		private ArrayList<InternalLayoutStateNode> children = new ArrayList<InternalLayoutStateNode>();
		private ArrayList<InternalLayoutStateNode> siblings = new ArrayList<InternalLayoutStateNode>();
		private InternalNode internalNode;
		private int depth = -1;
		private double offsetXToParent = 0;
		private double offsetYToParent = 0;
		private double offsetXToSibling = 0;
		private double offsetYToSibling = 0;
		private String value;
		private double stateNameHeight = 0.0;
		private double stateNameWidth;
		

		public InternalLayoutStateNode(InternalNode n, InternalLayoutStateNode parent) {
			internalNode = n;
			this.parent = parent;
		}

		public void addChild(InternalLayoutStateNode iNode) {
			setSibling(iNode, children);
			children.add(iNode);
		}
		
		public void addSibling(InternalLayoutStateNode iNode) {
			siblings.add(iNode);
		}
		
		public InternalLayoutStateNode getParent() {
			return parent;
		}

		public InternalLayoutStateNode[] getChildren() {
			return children.toArray(new InternalLayoutStateNode[children.size()]);
		}

		public InternalLayoutStateNode[] getSiblings() {
			return siblings.toArray(new InternalLayoutStateNode[siblings.size()]);
		}

		public int getDepth() {
			return depth;
		}

		public void updateOffsetToParent(double x, double y) {
			offsetXToParent = x;
			offsetYToParent = y;
		}
		
		public void updateOffsetToSibling(double x, double y) {
			offsetXToSibling = x;
			offsetYToSibling = y;
		}

		public boolean isRoot() {
			return parent == null ? true : false;
		}
		
		public void setDepth(int d) {
			depth = d;
		}

		public InternalNode getInternalNode() {
			return internalNode;
		}

		public void setValue(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
		
		public double getStateNameHeight() {
			return stateNameHeight;
		}
		
		public double getStateNameWidth() {
			return stateNameWidth;
		}
		
		public void setStateNameHeight(double stateNameHeight) {
			this.stateNameHeight = stateNameHeight;
		}
		
		public void setStateNameWidth(double stateNameWidth) {
			this.stateNameWidth = stateNameWidth;
		}
		
		private void setSibling(InternalLayoutStateNode sibling,
				List<InternalLayoutStateNode> nodes) {
			for(int i = 0; i < nodes.size(); i++) {
				nodes.get(i).addSibling(sibling);
			}
		}
		
		@Override
		public String toString() {
			return value.toString();
		}
	}
}
