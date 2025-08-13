package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import rfx.core.util.StringUtil;

/**
 * The data model for Cytoscape.js to visualize touchpoint flow network
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class CytoscapeData {

	public static final class Node {
		public static final int MAX_LENGTH_LABEL = 60;

		public final String id;
		public String fullLabel = "";
		public String label = "";
		public String url = "";
		public int weight = 0;
		public long profileCount = 0;

		public Node(String id, String label, String url, long profileCount) {
			super();
			this.id = id;
			this.profileCount = profileCount;
			this.url = url;
			setLabel(label);
		}

		public Node(String id, String label, String url) {
			super();
			this.id = id;
			this.url = url;
			setLabel(label);
		}

		public long getWeight() {
			return weight;
		}

		public void updateOutputWeight() {
			this.weight += 1;
		}
		
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public long getProfileCount() {
			return profileCount;
		}

		public void setProfileCount(long profileCount) {
			this.profileCount += profileCount;
		}

		void setLabel(String label) {
			this.fullLabel = label;
			this.label = StringUtil.truncate(label, MAX_LENGTH_LABEL);
		}
	}

	public static final class Edge {
		public final String id;
		public long weight = 0;
		public final String source;
		public final String target;

		public Edge(String edgeId, String sourceId, String targetId, long weight) {
			super();
			this.id = edgeId;
			this.source = sourceId;
			this.target = targetId;
			this.weight = weight;
		}

		public long getWeight() {
			return weight;
		}

		public void setWeight(long weight) {
			this.weight += weight;
		}

		public String getId() {
			return id;
		}

	}

	public static final class DataNode implements Comparable<DataNode> {
		Node data;

		public DataNode(Node data) {
			super();
			this.data = data;
		}

		public Node getData() {
			return data;
		}

		public void setData(Node data) {
			this.data = data;
		}

		@Override
		public int compareTo(DataNode o) {
			if (data.weight > o.data.weight) {
				return -1;
			} else if (data.weight < o.data.weight) {
				return 1;
			}
			return 0;
		}
	}

	public static final class DataEdge {
		Edge data;

		public DataEdge(Edge data) {
			super();
			this.data = data;
		}

		public Edge getData() {
			return data;
		}

		public void setData(Edge data) {
			this.data = data;
		}

	}

	protected String journeyMapId = "";
	protected List<DataNode> nodes;
	protected List<DataEdge> edges;
	protected String rootNodeId = "";

	public CytoscapeData(List<DataNode> nodes, List<DataEdge> edges) {
		super();
		this.nodes = nodes;
		this.edges = edges;
	}

	public CytoscapeData(String journeyMapId, List<TouchpointFlowReport> reports, boolean groupByDomain) {
		super();
		this.journeyMapId = journeyMapId;
		this.nodes = new ArrayList<>();
		this.edges = new ArrayList<>();
		transformToGraphData(reports, groupByDomain);
	}

	protected void transformToGraphData(List<TouchpointFlowReport> reports, boolean groupByDomain) {
		if (reports == null) {
			return;
		}
		Map<String, DataNode> mapNode = new HashMap<>();
		Map<String, DataEdge> mapEdge = new HashMap<>();
		
		for (TouchpointFlowReport report : reports) {
			long profileCount = report.getProfileCount();
			
			String sourceDomain = report.getRefTouchpointDomain();
			String sourceId = groupByDomain ? sourceDomain : report.getRefTouchpointId();
			String sourceLabel = groupByDomain ? sourceDomain :report.getRefTouchpointName();
			String sourceUrl = groupByDomain ? ("https://"+sourceDomain) : report.getRefTouchpointUrl();
			
			String targetDomain = report.getSrcTouchpointDomain();
			String targetId = groupByDomain ? targetDomain : report.getSrcTouchpointId();
			String targetLabel = groupByDomain ? targetDomain : report.getSrcTouchpointName();
			String targetUrl = groupByDomain ? ("https://"+targetDomain) : report.getSrcTouchpointUrl();

			// sourceNodes
			DataNode sourceNode = mapNode.get(sourceId);
			if (sourceNode == null) {
				sourceNode = new DataNode(new Node(sourceId, sourceLabel, sourceUrl, profileCount));
				this.nodes.add(sourceNode);
				mapNode.put(sourceId, sourceNode);
			}
			else {
				sourceNode.data.setProfileCount(profileCount);
			}

			// targetNodes
			DataNode targetNode = mapNode.get(targetId);
			if (targetNode == null) {
				targetNode = new DataNode(new Node(targetId, targetLabel, targetUrl , profileCount));
				this.nodes.add(targetNode);
				mapNode.put(targetId, targetNode);
			}
			else {
				targetNode.data.setProfileCount(profileCount);
			}

			// dataEdges
			String edgeId = sourceId + "_" + targetId;
			long eventCount = report.getEventCount();
			DataEdge dataEdge = mapEdge.get(edgeId);
			if (dataEdge == null) {
				dataEdge = new DataEdge(new Edge(edgeId, sourceId, targetId, eventCount));
				mapEdge.put(edgeId, dataEdge);
				this.edges.add(dataEdge);
				
				// output event, to sort most referral touchpoint
				sourceNode.getData().updateOutputWeight();
			} else {
				dataEdge.data.setWeight(eventCount);
			}
		}

		// sort by the weight of node
		Collections.sort(this.nodes);
		if (nodes.size() > 0) {
			// the highest ranked node , the first node in list, is the root node
			rootNodeId = nodes.get(0).data.id;
		}
	}

	public List<DataNode> getNodes() {
		return nodes;
	}

	public void setNodes(List<DataNode> nodes) {
		this.nodes = nodes;
	}

	public List<DataEdge> getEdges() {
		return edges;
	}

	public void setEdges(List<DataEdge> edges) {
		this.edges = edges;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
