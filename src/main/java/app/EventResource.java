package app;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import static dev.nklab.jl2.web.gcp.datastore.Extentions.*;
import dev.nklab.jl2.web.profile.Trace;
import dev.nklab.jl2.web.profile.WebTrace;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class EventResource {

    @ConfigProperty(name = "slide4vr.gcp.projectid")
    String projectId;
    @ConfigProperty(name = "slide4vr.gcp.bucketname.slide")
    String slideBucket;


    @GET
    @Path("/healthcheck")
    public Response healthcheck() {
        var options = DatastoreOptions.getDefaultInstance();
        var datastore = options.getService();

        return Response.ok("success" + ":" + datastore.getClass().getName()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @WebTrace
    public Response invoke(Map<String, Object> params) throws IOException {
        var targetParams = (Map<String, String>) params.get("targetParams");
        System.out.println("params:" + params);
        System.out.println("targetParams:" + targetParams);
        var key = params.get("key").toString();
        var userId = params.get("userId").toString();

        updateUploadStatus(userId, key);

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
