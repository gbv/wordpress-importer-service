package de.vzg.wis;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;

import de.vzg.wis.wordpress.model.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;


public class Utils {

    private static final Logger LOGGER = LogManager.getLogger();

    public static String getFixedURL(String urlToInstance) {
        if(urlToInstance.startsWith("http:")){
            urlToInstance = urlToInstance.replace("http:", "https:");
        }
        urlToInstance = appendSlashIfNotPresent(urlToInstance);
        return urlToInstance;
    }

    public static String appendSlashIfNotPresent(String urlToInstance) {
        if (!urlToInstance.endsWith("/")) {
            urlToInstance += "/";
        }
        return urlToInstance;
    }


    public static String encodeURI(String uriString) throws URISyntaxException, MalformedURLException {
        try {
            new URI(uriString);
            return uriString;
        } catch (URISyntaxException ex) {
            URL url = new URL(uriString);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
                url.getQuery(), url.getRef());
            url = uri.toURL();
            return url.toString();
        }
    }

    private static SimpleDateFormat WP_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    public static String getTitleFileName(Post post) {
        return Jsoup.parseBodyFragment(post.getTitle().getRendered()).text()
            .replaceAll("[ ]", "_")
            .replaceAll("[^a-zA-Z0-9_]", "")+".pdf";
    }

    public static OffsetDateTime getWPDate(String date) {
        try {
            Instant instant = WP_DATE.parse(date).toInstant();
            return instant.atOffset(ZoneOffset.UTC);
        } catch (ParseException e) {
            LOGGER.error("Could not parse date: {}", date);
        }
        return null;
    }

    public static String getUserAgent() {
        return "WordpressAutoImporter/1.0";
    }
}
