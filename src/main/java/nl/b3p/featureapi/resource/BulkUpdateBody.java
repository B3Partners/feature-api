package nl.b3p.featureapi.resource;

import java.util.Map;

public class BulkUpdateBody {
    private String filter;

    private boolean useSQLFiltering;

    private Map<String, String> updatedFields;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Map<String, String> getUpdatedFields() {
        return updatedFields;
    }

    public void setUpdatedFields(Map<String, String> updatedFields) {
        this.updatedFields = updatedFields;
    }

    public boolean isUseSQLFiltering() {
        return useSQLFiltering;
    }

    public void setUseSQLFiltering(boolean useSQLFiltering) {
        this.useSQLFiltering = useSQLFiltering;
    }
}
