package de.vzg.wis;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.xml.transform.TransformerException;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.vzg.wis.configuration.ConfigNotFoundException;
import de.vzg.wis.configuration.ImporterConfiguration;
import de.vzg.wis.configuration.ImporterConfigurationPart;

import de.vzg.wis.mycore.LocalMyCoReObjectStore;
import de.vzg.wis.wordpress.LocalPostStore;
import de.vzg.wis.wordpress.Post2PDFConverter;
import de.vzg.wis.wordpress.PostFetcher;
import de.vzg.wis.wordpress.model.Post;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.XMLOutputter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", exposedHeaders = "Content-Disposition")
public class ServiceResource {

    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String getConfigurations() {
        final Map<String, ImporterConfigurationPart> configParts = ImporterConfiguration.getConfiguration().getParts();
        GsonBuilder g = new GsonBuilder();
        g.addSerializationExclusionStrategy(new ExclusionStrategy() {

            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getName().equals("password") || f.getName().equals("username");
            }

            @Override public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        });
        return g.create().toJson(configParts);
    }


    @GetMapping(value = "/compare/{config}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody String compare(@PathVariable("config") String config) throws IOException, JDOMException, ConfigNotFoundException {
        final Map<String, ImporterConfigurationPart> configParts = ImporterConfiguration.getConfiguration().getParts();
        if(!configParts.containsKey(config)){
            throw new ConfigNotFoundException("There is not configuration " + config);
        }

        final WordpressMyCoReCompare wordpressMyCoReCompare = new WordpressMyCoReCompare(configParts.get(config));
        return new Gson().toJson(wordpressMyCoReCompare.compare());
    }

    @GetMapping(value = "convert/mods/{config}/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody String convertBlogPostXML(@PathVariable("config") String configName, @PathVariable("id") int postID) {
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


    @GetMapping(value = "revalidate/{config}/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public @ResponseBody String revalidateMyCoReID(@PathVariable("config") String configName, String id){
        final ImporterConfigurationPart config = ImporterConfiguration.getConfiguration().getParts()
            .get(configName);
        LocalMyCoReObjectStore.getInstance(config.getRepository()).update(true, config.getUsername(),
            config.getPassword());
        return "{}";
    }


    @RequestMapping("convert/pdf/{config}/{id}")
    public void convertBlogPostPDF(@PathVariable("config") String configName, @PathVariable("id") int postID, HttpServletResponse response)
        throws IOException {
        final ImporterConfigurationPart config = ImporterConfiguration.getConfiguration().getParts()
            .get(configName);
        Post post = PostFetcher.fetchPost(config.getBlog(), postID);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + Utils.getTitleFileName(post) + "\"");
        ServletOutputStream outputStream = response.getOutputStream();

        try {
                new Post2PDFConverter().getPDF(post, outputStream, config.getBlog(), config.getLicense());
            } catch (TransformerException | URISyntaxException e) {
                throw new RuntimeException("Error while generating PDF!", e);
            }



    }



}
