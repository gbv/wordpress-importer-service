package de.vzg.wis.mycore;

import de.vzg.wis.jpa.MyCoReObjectInfo;
import de.vzg.wis.jpa.MyCoReObjectInfoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;

@Service
public class MyCoReObjectInfoUpdater {

    @Autowired
    private MyCoReObjectInfoRepository repo;

    @Autowired
    private ObjectFetcher objectFetcher;

    private static final Logger LOGGER = LogManager.getLogger();

    public void updateMyCoReObjectInfo(String repositoryURL, String username, String password, String parentId) throws IOException,
            URISyntaxException, JDOMException {
        Document parentDocument = objectFetcher.fetchObject(repositoryURL, username, password, parentId);
        var childrenList = MODSUtil.getChildren(parentDocument);
        var childrenSet = new HashSet<>(childrenList);

        Document lastModifiedDocument = objectFetcher.fetchObject(repositoryURL, username, password, "");
        for (Element mycoreobjectElement : lastModifiedDocument.getRootElement().getChildren("mycoreobject")) {
            var id = mycoreobjectElement.getAttributeValue("id");
            var lastModifiedStr = mycoreobjectElement.getAttributeValue("lastModified");

            Instant inst = Instant.parse(lastModifiedStr);
            OffsetDateTime lastModified = inst.atOffset(ZoneOffset.UTC);

            if (childrenSet.contains(id)) {
                updateMyCoReObject(repositoryURL, username, password, id, lastModified);
            }
        }
    }

    public void updateMyCoReObject(String repositoryURL,
                                    String username,
                                    String password,
                                    String id,
                                    OffsetDateTime lastModified) throws IOException, JDOMException, URISyntaxException {
        var dbInfo = repo.findFirstByMycoreIdAndRepository(id, repositoryURL);
        if (dbInfo != null && lastModified != null) {
            if (dbInfo.getLastModified().equals(lastModified)) {
                // do nothing, lastModified is the same
                return;
            }
            LOGGER.info("Updating object " + id);
        } else {
            dbInfo = new MyCoReObjectInfo();
            dbInfo.setRepository(repositoryURL);
        }

        // update object
        var modsDocument = objectFetcher.fetchObject(repositoryURL, username, password, id);
        if(extractInfo(modsDocument, dbInfo)) {
            repo.save(dbInfo);
        } else {
            LOGGER.warn("Could not extract info from " + id);
        }
    }


    public boolean extractInfo(Document document, MyCoReObjectInfo info) {
        String state = MODSUtil.getState(document);
        if(state == null) {
            LOGGER.warn("Could not extract state from " + info.getMycoreId());
            return false;
        }

        String id = MODSUtil.getID(document);
        if(id == null) {
            LOGGER.warn("Could not extract id from " + info.getMycoreId());
            return false;
        }

        String parent = MODSUtil.getParent(document);
        if(parent == null) {
            LOGGER.warn("Could not extract parent from " + info.getMycoreId());
            return false;
        }

        info.setMycoreId(id);
        info.setFulltext(MODSUtil.getFulltextURL(document).orElse(null));
        info.setCreated(MODSUtil.getCreateDate(document));
        info.setLastModified(MODSUtil.getLastModified(document));
        info.setParentMycoreId(parent);
        info.setState(state);
        MODSUtil.MODSRecordInfo recordInfo = MODSUtil.getRecordInfo(document);
        if(recordInfo!= null) {
            info.setWordpressId(recordInfo.id());
            info.setWordpressURL(recordInfo.url());
        }
        return true;
    }



}
