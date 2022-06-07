package de.vzg.service.resources;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.vzg.service.Post2ModsConverter;
import de.vzg.service.Utils;
import de.vzg.service.WordpressMyCoReCompare;
import de.vzg.service.configuration.ImporterConfiguration;
import de.vzg.service.configuration.ImporterConfigurationPart;
import de.vzg.service.mycore.LocalMyCoReObjectStore;
import de.vzg.service.wordpress.LocalPostStore;
import de.vzg.service.wordpress.Post2PDFConverter;
import de.vzg.service.wordpress.PostFetcher;
import de.vzg.service.wordpress.model.Post;

@Path("/")
public class ServiceResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("config")
    public String getConfigurations() throws NoSuchFieldException {
        final Map<String, ImporterConfigurationPart> configParts = ImporterConfiguration.getConfiguration().getParts();
        GsonBuilder g = new GsonBuilder();
        g.addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override public boolean shouldSkipField(FieldAttributes f) {
                return f.getName().equals("password") || f.getName().equals("username");
            }

            @Override public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        });
        return g.create().toJson(configParts);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("compare/{config}")
    public String compare(@PathParam("config") String config) throws IOException, JDOMException {
        final Map<String, ImporterConfigurationPart> configParts = ImporterConfiguration.getConfiguration().getParts();
        if(!configParts.containsKey(config)){
            throw new NotFoundException("There is not configuration " + config);
        }

        final WordpressMyCoReCompare wordpressMyCoReCompare = new WordpressMyCoReCompare(configParts.get(config));
        return new Gson().toJson(wordpressMyCoReCompare.compare());
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("convert/mods/{config}/{id}")
    public String convertBlogPostXML(@PathParam("config") String configName, @PathParam("id") int postID) {
        final ImporterConfigurationPart config = ImporterConfiguration.getConfiguration().getParts()
            .get(configName);
        final LocalPostStore postStore = LocalPostStore.getInstance(config.getBlog());
        final Post post = postStore.getPost(postID);
        final Document mods = new Post2ModsConverter(post, config.getParentObject(), config.getBlog(),
            config.getPostTemplate(),
            config.getLicense()
        ).getMods();

        return new XMLOutputter().outputString(mods);
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("revalidate/{config}/{id}")
    public String revalidateMyCoReID(@PathParam("config") String configName, String id){
        final ImporterConfigurationPart config = ImporterConfiguration.getConfiguration().getParts()
            .get(configName);
        LocalMyCoReObjectStore.getInstance(config.getRepository()).update(true);
        return "{}";
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("convert/pdf/{config}/{id}")
    public Response convertBlogPostPDF(@PathParam("config") String configName, @PathParam("id") int postID)
        throws IOException {
        final ImporterConfigurationPart config = ImporterConfiguration.getConfiguration().getParts()
            .get(configName);
        Post post = PostFetcher.fetchPost(config.getBlog(), postID);
        return Response.ok((StreamingOutput) outputStream -> {
            try {
                new Post2PDFConverter().getPDF(post, outputStream, config.getBlog(), config.getLicense());
            } catch (FOPException | TransformerException | URISyntaxException e) {
                throw new RuntimeException("Error while generating PDF!", e);
            }
        }).header("Content-Disposition", "attachment; filename=\"" + Utils.getTitleFileName(post) + "\"").build();
    }



}
