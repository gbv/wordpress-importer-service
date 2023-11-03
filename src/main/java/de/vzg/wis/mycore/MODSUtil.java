package de.vzg.wis.mycore;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.vzg.wis.Utils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;


public class MODSUtil {

    public static final Namespace XLINK_NAMESPACE = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    public static final Namespace MODS_NAMESPACE = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    private static final String FULLTEXT_URL_XPATH = "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url";
    private static final String LOCKED_DELETED_XPATH = "/mycoreobject/service/servstates/servstate[@categid='blocked' or @categid='deleted']";
    private static final String STATE_XPATH = "/mycoreobject/service/servstates/servstate";

    private static final String MODIFY_DATE_XPATH
        = "/mycoreobject/service/servdates[@class='MCRMetaISO8601Date']/servdate[@type='modifydate']";
    private static final String CREATE_DATE_XPATH
        = "/mycoreobject/service/servdates[@class='MCRMetaISO8601Date']/servdate[@type='createdate']";
    private static final String RECORD_INFO_XPATH
        = "/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/mods:recordInfo";

    private static final String PARENT_XPATH = "/mycoreobject/structure/parents/parent";

    private static final String CHILDREN_XPATH = "/mycoreobject/structure/children/child";

    public static List<String> getChildren(Document mycoreObject) {
        XPathExpression<Element> childrenXPath = XPathFactory.instance().compile(CHILDREN_XPATH, Filters.element());
        return childrenXPath.evaluate(mycoreObject).stream()
            .map(child -> child.getAttributeValue("href", XLINK_NAMESPACE))
            .collect(Collectors.toList());
    }

    public static Optional<String> getFulltextURL(Document mycoreObject) {
        XPathExpression<Element> fulltextXPath = XPathFactory.instance().compile(FULLTEXT_URL_XPATH, Filters.element(), null, MODS_NAMESPACE);
        final Element element = fulltextXPath.evaluateFirst(mycoreObject);
        return Optional.ofNullable(element).map(Element::getTextTrim).map(Utils::getFixedURL);
    }

    public static String getID(Document mycoreObject) {
        return mycoreObject.getRootElement().getAttributeValue("ID");
    }

    public static OffsetDateTime getLastModified(Document mycoreObject) {
        XPathExpression<Element> modifyDateXP
            = XPathFactory.instance().compile(MODIFY_DATE_XPATH, Filters.element(), null, MODS_NAMESPACE);
        final Element element = modifyDateXP.evaluateFirst(mycoreObject);
        if (element == null) {
            return null;
        }
        String text = element.getText();
        return Instant.parse(text).atOffset(ZoneOffset.UTC);
    }

    public static OffsetDateTime getCreateDate(Document mycoreObject) {
        XPathExpression<Element> createDateXP
            = XPathFactory.instance().compile(CREATE_DATE_XPATH, Filters.element(), null, MODS_NAMESPACE);
        final Element element = createDateXP.evaluateFirst(mycoreObject);
        if (element == null) {
            return null;
        }
        String text = element.getText();
        return Instant.parse(text).atOffset(ZoneOffset.UTC);
    }

    public static String getParent(Document mycoreObject) {
        XPathExpression<Element> parentXPath = XPathFactory.instance().compile(PARENT_XPATH, Filters.element());
        final Element element = parentXPath.evaluateFirst(mycoreObject);
        if (element == null) {
            return null;
        }
        return element.getAttributeValue("href", XLINK_NAMESPACE);
    }

    public static String getState(Document mycoreObject) {
        XPathExpression<Element> stateXPath = XPathFactory.instance().compile(STATE_XPATH, Filters.element());
        final Element element = stateXPath.evaluateFirst(mycoreObject);
           if (element == null) {
                return null;
            }
        return element.getAttributeValue("categid");
    }

    public static MODSRecordInfo getRecordInfo(Document mycoreObject) {
        XPathExpression<Element> recordInfoXPath
            = XPathFactory.instance().compile(RECORD_INFO_XPATH, Filters.element(), null, MODS_NAMESPACE);
        final Element element = recordInfoXPath.evaluateFirst(mycoreObject);
        if (element == null) {
            return null;
        }
        String id = element.getChildTextTrim("recordIdentifier", MODS_NAMESPACE);
        String url = element.getChildTextTrim("recordContentSource", MODS_NAMESPACE);
        return new MODSRecordInfo(id == null ? null : Long.parseLong(id), url);
    }

    public static boolean isLockedOrDeleted(Document childDoc) {
        XPathExpression<Element> fulltextXPath = XPathFactory.instance()
            .compile(LOCKED_DELETED_XPATH, Filters.element(), null, MODS_NAMESPACE);
        return fulltextXPath.evaluate(childDoc).size() > 0;
    }

    public record MODSRecordInfo(Long id, String url) {
    }
}
