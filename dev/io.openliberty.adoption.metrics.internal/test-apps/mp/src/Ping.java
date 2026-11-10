import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/ping")
public class Ping {
    @GET
    public String ping() { return "pong"; }
}
