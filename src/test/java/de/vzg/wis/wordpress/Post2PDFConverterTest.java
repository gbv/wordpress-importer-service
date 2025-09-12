package de.vzg.wis.wordpress;

import static org.junit.jupiter.api.Assertions.*;

import de.vzg.wis.wordpress.model.Post;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Post2PDFConverterTest {

  @Autowired
  private PostFetcher postFetcher;



  @Test
  public void testPostConversionToPDF() throws IOException, URISyntaxException, TransformerException {
    String blog = "https://verfassungsblog.de/";
    Post post = postFetcher.fetchPost(blog, 85365);

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    new Post2PDFConverter()
        .getPDF(post, bout,blog, null,
            "");

    assertTrue(bout.size() > 1000);
    // Files.write(new File("test.pdf").toPath(), bout.toByteArray());
  }

}