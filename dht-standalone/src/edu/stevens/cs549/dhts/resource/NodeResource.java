package edu.stevens.cs549.dhts.resource;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import edu.stevens.cs549.dhts.activity.DHT;
import edu.stevens.cs549.dhts.activity.NodeInfo;

@Path("/dht")
public class NodeResource {

	/*
	 * Web service API.
	 * 
	 * TODO: Fill in the missing operations.
	 */

	Logger log = Logger.getLogger(NodeResource.class.getCanonicalName());
	private static final String ns = "http://www.stevens.edu/cs549/dht";
	public static final QName nsString = new QName(ns, "String[]");
	
	@Context
	UriInfo uriInfo;

	@Context
	HttpHeaders headers;

	@GET
	@Path("info")
	@Produces("application/xml")
	public Response getNodeInfoXML() {
		return new NodeService(headers, uriInfo).getNodeInfo();
	}

	@GET
	@Path("info")
	@Produces("application/json")
	public Response getNodeInfoJSON() {
		return new NodeService(headers, uriInfo).getNodeInfo();
	}

	@GET
	@Path("pred")
	@Produces("application/xml")
	public Response getPred() {
		return new NodeService(headers, uriInfo).getPred();
	}

	@PUT
	@Path("notify")
	@Consumes("application/xml")
	@Produces("application/xml")
	/*
	 * Actually returns a TableRep (annotated with @XmlRootElement)
	 */
	public Response putNotify(TableRep predDb) {
		/*
		 * See the comment for WebClient::notify (the client side of this logic).
		 */
		return new NodeService(headers, uriInfo).notify(predDb);
		// NodeInfo p = predDb.getInfo();
	}

	@GET
	@Path("find")
	@Produces("application/xml")
	public Response findSuccessor(@QueryParam("id") String index) {
		int id = Integer.parseInt(index);
		return new NodeService(headers, uriInfo).findSuccessor(id);
	}
	/*
	 * 
	 * TODO: Start
	 */
	@GET
	@Path("succ")
	@Produces("application/xml")
	public Response getSucc(){
		return new NodeService(headers, uriInfo).getSucc();
	}
	

	
	@GET
	@Path("get")
	@Produces("application/xml")
	public Response get(@QueryParam("key") String k){
		return new NodeService(headers, uriInfo).get(k);
	}
	

	@GET
	@Path("getClosest")
	@Produces("application/xml")
	public Response closestPrecedingFinger(@QueryParam("id") int id){
		return new NodeService(headers, uriInfo).closestPrecedingFinger(id);
	}
	
	@PUT
	@Path("add")
	//@Produces("application/xml")
	public void add(@QueryParam("key") String k, @QueryParam("value") String v){
		new NodeService(headers, uriInfo).add(k,v);
	}
	
	@DELETE
	@Path("delete")
	//@Produces("application/xml")
	public void delete(@QueryParam("key") String k, @QueryParam("value") String v){
		new NodeService(headers, uriInfo).delete(k,v);
	}

}
