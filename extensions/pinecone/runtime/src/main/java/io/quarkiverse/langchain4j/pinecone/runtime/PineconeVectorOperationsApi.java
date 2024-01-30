package io.quarkiverse.langchain4j.pinecone.runtime;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
@RegisterProvider(PineconeExceptionMapper.class)
public interface PineconeVectorOperationsApi {

    @POST
    @Path("/vectors/upsert")
    UpsertResponse upsert(UpsertRequest vector);

    @POST
    @Path("/query")
    QueryResponse query(QueryRequest request);

    @POST
    @Path("/vectors/delete")
    void delete(DeleteRequest request);

}
