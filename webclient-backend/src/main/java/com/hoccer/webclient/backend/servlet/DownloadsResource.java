package com.hoccer.webclient.backend.servlet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.client.model.TalkClientDownload;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;

@Path("downloads")
public class DownloadsResource {

    private ObjectMapper mJsonMapper;

    @Inject private XoClientDatabase mDatabase;

    public DownloadsResource() {
        mJsonMapper = new ObjectMapper(new JsonFactory());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownloads(@QueryParam("mediaType") String mediaType) throws IOException, SQLException {
        List<TalkClientDownload> downloads;

        if (mediaType != null) {
            downloads = mDatabase.findClientDownloadsByMediaType(mediaType);
        } else {
            downloads = mDatabase.findAllClientDownloads();
        }

        StringWriter writer = new StringWriter();
        mJsonMapper.writeValue(writer, downloads);

        return writer.toString();
    }

    @GET
    @Path("/{downloadId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDownload(@PathParam("downloadId") Integer downloadId) throws IOException, SQLException {
        TalkClientDownload download = mDatabase.findClientDownloadById(downloadId);

        if (download == null) {
            throw new WebApplicationException(404);
        }

        StringWriter writer = new StringWriter();
        mJsonMapper.writeValue(writer, download);
        return writer.toString();
    }

    @PATCH
    @Path("/{downloadId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void putDownload(@PathParam("downloadId") Integer downloadId, String json) throws IOException, SQLException {
        TalkClientDownload download = mDatabase.findClientDownloadById(downloadId);

        if (download == null) {
            throw new WebApplicationException(404);
        }

        ObjectReader reader = mJsonMapper.readerForUpdating(download);
        TalkClientDownload updatedDownload = reader.readValue(json);
        mDatabase.saveClientDownload(updatedDownload);
    }
}
