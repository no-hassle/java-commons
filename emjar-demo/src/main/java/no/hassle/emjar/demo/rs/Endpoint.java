package no.hassle.emjar.demo.rs;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class Endpoint {
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String htmlHandler() {
        return "<html><body><h1>OK</h1></body></html>";
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Map<String,String> jsonHandler() {
        final Map<String,String> map = new HashMap<>();
        map.put("key", "value");
        return map;
    }
}
