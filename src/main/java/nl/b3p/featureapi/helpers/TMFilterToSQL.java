package nl.b3p.featureapi.helpers;

import nl.b3p.featureapi.resource.fla.Subselect;
import org.geotools.data.jdbc.FilterToSQL;
import org.geotools.data.jdbc.FilterToSQLException;
import org.geotools.filter.FilterCapabilities;
import org.geotools.jdbc.BasicSQLDialect;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.io.StringWriter;

public class TMFilterToSQL extends FilterToSQL {
    private FilterToSQL base;

    public TMFilterToSQL(JDBCDataStore dataStore, String tableName) throws IOException {
        this.featureType = dataStore.getSchema(tableName);
        this.base = ((BasicSQLDialect) dataStore.getSQLDialect()).createFilterToSQL();
    }

    public TMFilterToSQL() {

    }

    @Override
    public FilterCapabilities createFilterCapabilities() {
        FilterCapabilities caps = super.createFilterCapabilities();
        caps.addType(Subselect.class);
        if (this.base != null) {
            caps.addAll(this.base.getCapabilities());
        } else {
            caps.addAll(BasicSQLDialect.BASE_DBMS_CAPABILITIES);
        }
        return caps;
    }


    public String encodeToString(Filter filter) throws FilterToSQLException {
        StringWriter out = new StringWriter();
        this.out = out;
        this.encode(filter);

        return out.getBuffer().toString();
    }

    public Object visit(Subselect filter, Object extraData) {
        try {
            this.out.write(filter.getMainColumn() + " IN (");
            this.out.write("SELECT " + filter.getRelatedColumn() + " FROM " + filter.getRelatedTable() + " ");
            this.encode(filter.getRelatedFilter());
            this.out.write(") ");

            return extraData;
        } catch (IOException | FilterToSQLException var4) {
            throw new RuntimeException("io problem writing filter", var4);
        }
    }
}
