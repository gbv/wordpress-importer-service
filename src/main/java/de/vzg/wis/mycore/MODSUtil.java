package de.vzg.wis.mycore;

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

    private static final String CHILDREN_XPATH = "/mycoreobject/structure/children/child";

    public static List<String> getChildren(Document mods) {
        XPathExpression<Element> childrenXPath = XPathFactory.instance().compile(CHILDREN_XPATH, Filters.element());
        return childrenXPath.evaluate(mods).stream()
            .map(child -> child.getAttributeValue("href", XLINK_NAMESPACE))
            .collect(Collectors.toList());
    }

    public static Optional<String> getFulltextURL(Document mods){
        XPathExpression<Element> fulltextXPath = XPathFactory.instance().compile(FULLTEXT_URL_XPATH, Filters.element(), null, MODS_NAMESPACE);
        final Element element = fulltextXPath.evaluateFirst(mods);
        return Optional.ofNullable(element).map(Element::getTextTrim).map(Utils::getFixedURL);
    }

    public static boolean isLockedOrDeleted(Document childDoc) {
        XPathExpression<Element> fulltextXPath = XPathFactory.instance()
            .compile(LOCKED_DELETED_XPATH, Filters.element(), null, MODS_NAMESPACE);
        return fulltextXPath.evaluate(childDoc).size() > 0;

    }

}
