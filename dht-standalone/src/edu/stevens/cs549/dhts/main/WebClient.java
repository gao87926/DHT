package edu.stevens.cs549.dhts.main;

import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBElement;

import edu.stevens.cs549.dhts.activity.DHTBase;
import edu.stevens.cs549.dhts.activity.NodeInfo;
import edu.stevens.cs549.dhts.resource.TableRep;
import edu.stevens.cs549.dhts.resource.TableRow;


public class WebClient {

	private Logger log = Logger.getLogger(WebClient.class.getCanonicalName());
	/*
	 * Jersey way of dealing with JAXB client-side: wrap with run-time type
	 * information.
	 */
	private GenericType<JAXBElement<NodeInfo>> nodeInfoType = new GenericType<JAXBElement<NodeInfo>>() {
	};
	private GenericType<JAXBElement<TableRow>> tableRowType = new GenericType<JAXBElement<TableRow>>() { 
		
	};

	private void error(String msg) {
		log.severe(msg);
	}

	/*
	 * Encapsulate Web client operations here.
	 * 
	 * TODO: Fill in missing operations.
	 */
	/*
	 * Get the Successor pointer at a node.
	 */
	// GX
	public NodeInfo getSucc(NodeInfo info) throws DHTBase.Failed{
		URI succPath = UriBuilder.fromUri(info.addr).path("succ").build();
		info("client getSucc(" + succPath + ")");
		Response response = getRequest(succPath);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /succ");
		} else {
			NodeInfo succ = response.readEntity(nodeInfoType).getValue();
			return succ;
		}
	}
	/*
	 * Get the predecessor pointer at a node.
	 */
	public NodeInfo getPred(NodeInfo node) throws DHTBase.Failed {
		URI predPath = UriBuilder.fromUri(node.addr).path("pred").build();
		info("client getPred(" + predPath + ")");
		Response response = getRequest(predPath);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /pred");
		} else {
			NodeInfo pred = response.readEntity(nodeInfoType).getValue();
			return pred;
		}
	}
	public String[] get(NodeInfo info, String k)throws DHTBase.Failed{
		URI uri = UriBuilder.fromUri(info.addr).path("get").queryParam("key",k).build();
		info("client string(" + k + ")");
		Response response = getRequest(uri);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /key");
		} else {
			String[] tab = response.readEntity(tableRowType).getValue().vals;
			return tab;
		}
	}
	public void add(NodeInfo n, String k, String v){
		URI uri = UriBuilder.fromUri(n.addr).path("add").queryParam("key",k)
				.queryParam("value",v).build();
		info("client put(" + k + ", "+ v+")");
		String[] sv ={v};
		TableRow tr = new TableRow(k,sv);
		TableRep tRep = new TableRep(null,null, 1);
		tRep.entry[0]=tr;
		putRequest(uri,Entity.xml(tRep));
	}
	
	public void delete(NodeInfo n, String k, String v){
		URI uri = UriBuilder.fromUri(n.addr).path("delete").queryParam("key", k)
				.queryParam("value",v).build();
		info("client delete(key:" + k +")");
		client.target(uri).request(MediaType.APPLICATION_XML_TYPE)
		.header(Time.TIME_STAMP, Time.advanceTime())
		.delete();
		}

	public NodeInfo closestPrecedingFinger(NodeInfo info, int id){
		URI uri = UriBuilder.fromUri(info.addr).path("getClosest")
				.queryParam("id", id).build();
		Response response = getRequest(uri);
		NodeInfo closestNode = response.readEntity(nodeInfoType).getValue();
		return closestNode;
	}
	/*
	 * Creation of client instances is expensive, so just create one.
	 */
	protected Client client;

	public WebClient() {
		client = ClientBuilder.newClient();
	}

	private void info(String mesg) {
		Log.info(mesg);
	}

	private Response getRequest(URI uri) {
		try {
			Response cr = client.target(uri).request(MediaType.APPLICATION_XML_TYPE)
					.header(Time.TIME_STAMP, Time.advanceTime()).get();
			processResponseTimestamp(cr);
			return cr;
		} catch (Exception e) {
			error("Exception during GET request: " + uri.toString() +"---"+e);
			return null;
		}
	}

	private Response putRequest(URI uri, Entity<?> entity) {
		// TODO
		//GX
		try {
			Response cr = client.target(uri).request(MediaType.APPLICATION_XML_TYPE)
					.header(Time.TIME_STAMP, Time.advanceTime())
					.put(entity);
			return cr;
		} catch (Exception e) {
			error("Exception during PUT request: " + uri.toString() +entity.toString());
			return null;
		}
	}

	private Response putRequest(URI uri) {
		return putRequest(uri, Entity.text(""));
	}

	private void processResponseTimestamp(Response cr) {
		Time.advanceTime(Long.parseLong(cr.getHeaders().getFirst(Time.TIME_STAMP).toString()));
	}

	/*
	 * Ping a remote site to see if it is still available.
	 */
	public boolean isFailed(URI base) {
		URI uri = UriBuilder.fromUri(base).path("info").build();
		Response c = getRequest(uri);
		return c.getStatus() >= 300;
	}



	/*
	 * Notify node that we (think we) are its predecessor.
	 */
	public TableRep notify(NodeInfo node, TableRep predDb) throws DHTBase.Failed {
		/*
		 * The protocol here is more complex than for other operations. We
		 * notify a new successor that we are its predecessor, and expect its
		 * bindings as a result. But if it fails to accept us as its predecessor
		 * (someone else has become intermediate predecessor since we found out
		 * this node is our successor i.e. race condition that we don't try to
		 * avoid because to do so is infeasible), it notifies us by returning
		 * null. This is represented in HTTP by RC=304 (Not Modified).
		 */
		NodeInfo thisNode = predDb.getInfo();
		UriBuilder ub = UriBuilder.fromUri(node.addr).path("notify");
		URI notifyPath = ub.queryParam("id", thisNode.id).build();
		info("client notify(" + notifyPath + ")");
		Response response = putRequest(notifyPath, Entity.xml(predDb));
		if (response != null && response.getStatusInfo() == Response.Status.NOT_MODIFIED) {
			/*
			 * Do nothing, the successor did not accept us as its predecessor.
			 */
			return null;
		} else if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("PUT /notify?id=ID");
		} else {
			TableRep bindings = response.readEntity(TableRep.class);
			return bindings;
		}
	}

	/*
	 * Find successor of an id. Used by join protocol
	 */
	public NodeInfo findSuccessor(URI addr, int id) throws DHTBase.Failed {
		UriBuilder ub = UriBuilder.fromUri(addr).path("find");
		URI findPath = ub.queryParam("id", id).build();
		info("client findSuccessor(" + findPath + ")");
		Response response = getRequest(findPath);
		if (response == null || response.getStatus() >= 300) {
			throw new DHTBase.Failed("GET /find?id=ID");
		} else {
			return response.readEntity(nodeInfoType).getValue();
		}
	}

}
