package app;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import static dev.nklab.jl2.web.gcp.datastore.Extentions.*;
import dev.nklab.jl2.web.profile.Trace;
import dev.nklab.jl2.web.profile.WebTrace;
import dev.nklab.kuda.core.Trigger;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class EventResource {

    @ConfigProperty(name = "slide4vr.gcp.projectid")
    String projectId;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.slide")
    String slideBucket;

    @Inject
    Trigger trigger;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    // @Authenticated
    public Response invoke(@Context HttpHeaders headers, @Context SecurityContext ctx, Map<String, String> params)
            throws IOException {
        var key = params.get("key");
        // var userId = ctx.getUserPrincipal().getName();
        var userId = params.get("userId");
        System.err.println(key);

        updateUploadStatus(userId, key);

        trigger.callTrigger(
                Map.of(
                    "userId", userId, 
                    "key", key, 
                    "targetParams", Map.of("args", userId + "," + key)),
                "always");

        return Response.ok().build();
    }

    @Trace
    void updateUploadStatus(String userId, String key) {
        var baseUrl = "https://storage.googleapis.com";
        var storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        var bucket = storage.get(slideBucket);
        var option = Storage.BlobListOption.prefix(userId + "/" + key);
        var items = bucket.list(option).iterateAll();
        var url = baseUrl + "/" + slideBucket + "/" + items.iterator().next().getName();

        var datastore = DatastoreOptions.getDefaultInstance().getService();
        var slideKey = datastore.newKeyFactory().addAncestors(PathElement.of("User", userId)).setKind("Slide")
                .newKey(key);

        var slide = datastore.get(slideKey);
        if (slide != null) {
            var entity = Entity.newBuilder(slide).set("is_uploaded", noindex(true)).set("thumbnail", url).build();
            datastore.update(entity);
        }
    }
}
