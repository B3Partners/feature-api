package nl.b3p.featureapi.resource;

import nl.tailormap.viewer.config.services.FeatureTypeRelation;
import nl.tailormap.viewer.config.services.FeatureTypeRelationKey;
import nl.tailormap.viewer.config.services.SimpleFeatureType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TailormapCQL {

    private static final Log LOG = LogFactory.getLog(TailormapCQL.class);

    private final static String BEGIN_APPLAYER_PART = "APPLAYER(";
    public final static String BEGIN_RELATED_FEATURE_PART = "RELATED_FEATURE(";

    private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    public static Filter toFilter(String filter, EntityManager em) throws CQLException {
        filter = processFilter(filter, em);

        return getFilter(filter, em);
    }

    private static String processFilter(String filter, EntityManager em) throws CQLException {
        if (filter.contains(BEGIN_APPLAYER_PART)) {
            throw new IllegalArgumentException("Layer has no featuretype configured");
        }
        return filter;
    }

    private static Filter getFilter(String filter, EntityManager em) throws CQLException {
        Filter f = null;

        if (filter.contains(BEGIN_RELATED_FEATURE_PART)) {
            f = replaceSubselectsFromFilter(filter, em);
        } else {
            f = ECQL.toFilter(filter);
        }

        return f;
    }

    private static Filter replaceSubselectsFromFilter(String filter, EntityManager em) throws CQLException {

        String remainingFilter = filter;
        Filter f = null;
        Filter current = null;

        if (startsWithRelatedLayer(remainingFilter)) {
            int startIndex = remainingFilter.indexOf(BEGIN_RELATED_FEATURE_PART) + BEGIN_RELATED_FEATURE_PART.length();
            int endIndex = findIndexOfClosingBracket(startIndex - 1, remainingFilter);
            String filterPart = BEGIN_RELATED_FEATURE_PART + remainingFilter.substring(startIndex, endIndex + 1);
            current = createSubselect(filterPart, em);

            remainingFilter = remainingFilter.substring(0, remainingFilter.indexOf(BEGIN_RELATED_FEATURE_PART)) + remainingFilter.substring(endIndex + 1);
        } else {
            int endAnd = Math.max(0, remainingFilter.toLowerCase().indexOf(" and "));
            int endOR = Math.max(0, remainingFilter.toLowerCase().indexOf(" or "));
            int end = Math.max(endAnd, endOR);
            String filterPart = remainingFilter.substring(0, end);
            current = ECQL.toFilter(filterPart);
            remainingFilter = remainingFilter.substring(end);
        }
        remainingFilter = remainingFilter.trim();

        remainingFilter = removeUnnecessaryParens(remainingFilter);

        if (!remainingFilter.isEmpty()) {
            remainingFilter = remainingFilter.trim();
            String nextFilter = remainingFilter.substring(remainingFilter.indexOf(" "));
            f = getBinaryLogicOperator(remainingFilter, current, getFilter(nextFilter, em));
        } else {
            f = current;
        }

        return f;
    }

    private static boolean startsWithRelatedLayer(String filter) {
        int threshold = 6;
        return filter.substring(0, BEGIN_RELATED_FEATURE_PART.length() + threshold).contains(BEGIN_RELATED_FEATURE_PART);
    }

    private static int findIndexOfClosingBracket(int startIndex, String filter) {
        int openBrackets = 0, closingBrackets = 0, endIndex = 0;
        for (int i = startIndex; i < filter.length(); i++) {
            char c = filter.charAt(i);
            if (c == '(') {
                openBrackets++;
            }
            if (c == ')') {
                closingBrackets++;
            }
            if (openBrackets == closingBrackets && c != ' ') {
                endIndex = i;
                break;
            }
        }
        return endIndex;
    }

    private static FeatureTypeRelation retrieveFeatureTypeRelation(String filter, EntityManager em) throws CQLException {
             /*
          RELATED_LAYER(<SIMPLEFEATURETYPEID_MAIN>, <SIMPLEFEATURETYPEID_SUB>, <FILTER>)
                SIMPLEFEATURETYPEID_MAIN number  id of simplefeaturetype (not applayer ID!)  main layer in tailormap db: on this featuretype the filter will be set
                SIMPLEFEATURETYPEID_SUB number  id of related simplefeaturetype in tailormap db
                FILTER: string  TailormapCQL filter

			haal featuretype op
                haal relations op
                 check of in relations of LAYERID_MAIN er is (zo nee, crash)

                haal met behulp van de relatie de kolom uit main op waar de relatie op ligt: kolom_main
                haal met behulp van de relatie de kolom uit sub op waar de relatie op ligt: kolom_sub
                maak filter op LAYER_SUB, en haal alle values voor kolom_sub op: values
         */
        int beginPartLength = filter.indexOf(BEGIN_RELATED_FEATURE_PART) + BEGIN_RELATED_FEATURE_PART.length();
        int endMainFeatureType = filter.indexOf(",", beginPartLength + 1);
        int endSimpleFeatureIdSub = filter.indexOf(",", endMainFeatureType + 1);
        if (endMainFeatureType == -1 || endSimpleFeatureIdSub == -1) {
            throw new CQLException("Related layer filter incorrectly formed. Must be of form: RELATED_LAYER(<SIMPLEFEATURETYPEID_MAIN>, <SIMPLEFEATURETYPEID_SUB>, <FILTER>)");
        }
        String simpleFeatureTypeIdMain = filter.substring(beginPartLength, endMainFeatureType);
        String simpleFeatureIdSub = filter.substring(endMainFeatureType + 1, endSimpleFeatureIdSub);

        if (simpleFeatureTypeIdMain.isEmpty() || simpleFeatureIdSub.isEmpty()) {
            throw new CQLException("Related layer filter incorrectly formed. Must be of form: RELATED_LAYER(<SIMPLEFEATURETYPEID_MAIN>, <SIMPLEFEATURETYPEID_SUB>, <FILTER>)");
        }
        simpleFeatureTypeIdMain = simpleFeatureTypeIdMain.trim();
        simpleFeatureIdSub = simpleFeatureIdSub.trim();
        try {

            SimpleFeatureType sub = em.find(SimpleFeatureType.class, Long.parseLong(simpleFeatureIdSub));
            SimpleFeatureType main = em.find(SimpleFeatureType.class, Long.parseLong(simpleFeatureTypeIdMain));

            AtomicReference<FeatureTypeRelation> atomRel = new AtomicReference<>();

            List<FeatureTypeRelation> mainRelations = main.getRelations();
            mainRelations.forEach(rel -> {
                if (rel.getForeignFeatureType().getId().equals(sub.getId())) {
                    atomRel.set(rel);
                }
            });

            if(atomRel.get() == null) {
                List<FeatureTypeRelation> subRelations = sub.getRelations();
                subRelations.forEach(rel -> {
                    if (rel.getForeignFeatureType().getId().equals(main.getId())) {
                        atomRel.set(rel);
                    }
                });
                // the relation is backwards, so we have to switch the foreignfeaturetype and the keys
                if (atomRel.get() != null) {
                    FeatureTypeRelation old = atomRel.get();
                    FeatureTypeRelation switched = new FeatureTypeRelation();

                    switched.setFeatureType(old.getForeignFeatureType());
                    switched.setForeignFeatureType(old.getFeatureType());
                    switched.setRelationKeys(old.getRelationKeys().stream().map(featureTypeRelationKey -> {
                        FeatureTypeRelationKey tmp = new FeatureTypeRelationKey();
                        tmp.setLeftSide(featureTypeRelationKey.getRightSide());
                        tmp.setRightSide(featureTypeRelationKey.getLeftSide());
                        return tmp;
                    }).collect(Collectors.toList()));
                    atomRel.set(switched);
                }
            }

            if (atomRel.get() == null) {
                throw new CQLException("featuretypes do not have a relation");
            }
            return atomRel.get();
        } catch (NumberFormatException nfe) {
            throw new CQLException("Related layer filter incorrectly formed. Ids are not parsable to Longs. Must be of form: RELATED_LAYER(<LAYERID_MAIN>, <SIMPLEFEATURETYPEID_SUB>, <FILTER>)");
        }
    }

    private static String retrieveRelatedFilter(String filter) {
        int endSubFilter = StringUtils.ordinalIndexOf(filter, ",", 2) + 1;

        int endIndex = findIndexOfClosingBracket(endSubFilter, filter);
        if (endIndex == endSubFilter || (endIndex - 1) == endSubFilter) {
            endIndex = filter.indexOf(")", endSubFilter) - 1;
        }
        String relatedFilterString = filter.substring(endSubFilter, endIndex + 1);
        return relatedFilterString;
    }

    private static Subselect createSubselect(String filter, EntityManager em) throws CQLException {
        FeatureTypeRelation relation = TailormapCQL.retrieveFeatureTypeRelation(filter, em);
        SimpleFeatureType subSft = relation.getForeignFeatureType();
        FeatureTypeRelationKey key = relation.getRelationKeys().get(0);
        String relatedColumn = key.getRightSide().getName();
        String mainColumn = key.getLeftSide().getName();
        String relatedTable = subSft.getTypeName();

        Filter relatedFilter = TailormapCQL.toFilter(retrieveRelatedFilter(filter), em);
        Subselect s = new Subselect(relatedFilter, relatedColumn, mainColumn, relatedTable);
        return s;
    }

    public static String removeUnnecessaryParens(String filter){
        String newFilter = removeAdjoiningParens(filter);
        newFilter = removeEnclosingParens(newFilter);
        return newFilter;
    }

    public static String removeAdjoiningParens(String filter) {
        if (filter.isEmpty()) {
            return filter;
        }
        String cur = "";

        for (int i = 0; i < filter.length() ; i++) {
            char c = filter.charAt(i);
            if (i + 1 < filter.length()) {
                char next = filter.charAt(i + 1);
                if (c == '(' && next == ')') {
                    i++;
                } else {
                    cur += c;
                }
            } else {
                cur += c;
            }
        }

        if (filter.length() > cur.length() && cur.length() >= 2) {
            cur = removeAdjoiningParens(cur);
        }
        return cur;
    }

    public static String removeEnclosingParens(String filter){
        String cur = filter;
        while(cur.startsWith("(") && cur.endsWith(")")){
            cur = cur.substring(1, cur.length() - 1);

        }
        return cur;
    }

    private static BinaryLogicOperator getBinaryLogicOperator(String filter, Filter prev, Filter current) {
        int endIndex = filter.indexOf(" ");
        String logicPart = filter.substring(0, endIndex);
        return logicPart.contains("AND") ? ff.and(prev, current) : ff.or(prev, current);
    }
}
