package liu1.liu11.liu1.liu1.plugin.report;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import kd.bos.algo.DataSet;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.AbstractReportColumn;
import kd.bos.entity.report.DecimalReportColumn;
import kd.bos.entity.report.FilterInfo;
import kd.bos.entity.report.ReportColumn;
import kd.bos.entity.report.ReportColumnGroup;
import kd.bos.entity.report.ReportQueryParam;

public class SecUserDateReportQueryPlugin extends AbstractReportListDataPlugin {

    private static final String BUSINESS_OPPORTUNITY_TABLE = "tk_ahkd_busopportunity";
    private static final String EXTEND_DB_ROUTE = "secd";
    private static final String PROJECT_COUNT_GROUP_TITLE = "\u7acb\u9879\u6570\u636e\u7edf\u8ba1";
    private static final String PROJECT_COUNT_GROUP_FIELD = "project_count_group";
    private static final String PROJECT_COUNT_TITLE = PROJECT_COUNT_GROUP_TITLE;
    private static final String DEPARTMENT_ENTITY = "bos_adminorg";
    private static final String SALES_ENTITY = "bos_user";

    private static final String DEPARTMENT_FIELD = "dept_id";
    private static final String SALES_FIELD = "sales_id";

    private static final String AWARDED_COUNT_FIELD = "awarded_count";
    private static final String TBS_COUNT_FIELD = "tbs_count";
    private static final String RFQ_COUNT_FIELD = "rfq_count";
    private static final String RFQ_S4_S6_COUNT_FIELD = "rfq_s4_s6_count";
    private static final String PENDING_COUNT_FIELD = "pending_count";
    private static final String LOST_COUNT_FIELD = "lost_count";
    private static final String TOTAL_COUNT_FIELD = "total_count";
    private static final String RFQ_RATE_FIELD = "rfq_rate";
    private static final String RFQ_S4_S6_RATE_FIELD = "rfq_s4_s6_rate";
    private static final String LOST_RATE_FIELD = "lost_rate";
    private static final String AWARDED_RATE_FIELD = "awarded_rate";

    @Override
    public List<AbstractReportColumn> getColumns(List<AbstractReportColumn> columns) throws Throwable {
        List<AbstractReportColumn> reportColumns = columns == null ? new ArrayList<>() : columns;
        reportColumns.clear();
        reportColumns.add(createBaseDataColumn(DEPARTMENT_FIELD, "部门", DEPARTMENT_ENTITY, 150));
        reportColumns.add(createBaseDataColumn(SALES_FIELD, "销售", SALES_ENTITY, 120));
        reportColumns.add(createNumberColumn(AWARDED_COUNT_FIELD, "Awarded", 0, 90, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(TBS_COUNT_FIELD, "TBS", 0, 80, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(RFQ_COUNT_FIELD, "RFQ", 0, 80, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(RFQ_S4_S6_COUNT_FIELD, "RFQ其中S4-S6", 0, 120, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(PENDING_COUNT_FIELD, "Pending", 0, 90, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(LOST_COUNT_FIELD, "Lost", 0, 80, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(TOTAL_COUNT_FIELD, "总计", 0, 80, false, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(RFQ_RATE_FIELD, "RFQ比例", 4, 90, true, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(RFQ_S4_S6_RATE_FIELD, "RFQ(S4-S6)比例", 4, 130, true, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(LOST_RATE_FIELD, "Lost比例/丢单率", 4, 130, true, PROJECT_COUNT_TITLE));
        reportColumns.add(createNumberColumn(AWARDED_RATE_FIELD, "Awarded比例/赢单率", 4, 140, true, PROJECT_COUNT_TITLE));
        ReportColumnGroup reportColumnGroup = new ReportColumnGroup();
        reportColumnGroup.setFieldKey(PROJECT_COUNT_GROUP_FIELD);
        reportColumnGroup.setCaption(new LocaleString(PROJECT_COUNT_GROUP_TITLE));
        reportColumnGroup.getChildren().addAll(new ArrayList<>(reportColumns.subList(2, reportColumns.size())));
        reportColumns.subList(2, reportColumns.size()).clear();
        reportColumns.add(reportColumnGroup);
        return reportColumns;
    }

    @Override
    public DataSet query(ReportQueryParam reportQueryParam, Object selectedObj) {
        FilterInfo filter = reportQueryParam == null ? null : reportQueryParam.getFilter();
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("select ");
        sql.append("bo.fk_ahkd_basedatafield_bo as ").append(DEPARTMENT_FIELD).append(", ");
        sql.append("bo.fk_ahkd_recmanager as ").append(SALES_FIELD).append(", ");
        sql.append(sumStage("4")).append(" as ").append(AWARDED_COUNT_FIELD).append(", ");
        sql.append(sumStage("2")).append(" as ").append(TBS_COUNT_FIELD).append(", ");
        sql.append(sumStage("1")).append(" as ").append(RFQ_COUNT_FIELD).append(", ");
        sql.append("sum(case when bo.fk_ahkd_currentstage = '1' and bo.fk_ahkd_state in ('5', '6', '7') then 1 else 0 end) as ")
                .append(RFQ_S4_S6_COUNT_FIELD).append(", ");
        sql.append(sumStage("5")).append(" as ").append(PENDING_COUNT_FIELD).append(", ");
        sql.append(sumStage("6")).append(" as ").append(LOST_COUNT_FIELD).append(", ");
        sql.append("count(1) as ").append(TOTAL_COUNT_FIELD).append(", ");
        sql.append(rateExpr("bo.fk_ahkd_currentstage = '1'")).append(" as ").append(RFQ_RATE_FIELD).append(", ");
        sql.append(rateExpr("bo.fk_ahkd_currentstage = '1' and bo.fk_ahkd_state in ('5', '6', '7')")).append(" as ")
                .append(RFQ_S4_S6_RATE_FIELD).append(", ");
        sql.append(rateExpr("bo.fk_ahkd_currentstage = '6'")).append(" as ").append(LOST_RATE_FIELD).append(", ");
        sql.append(rateExpr("bo.fk_ahkd_currentstage = '4'")).append(" as ").append(AWARDED_RATE_FIELD).append(" ");
        sql.append("from ").append(BUSINESS_OPPORTUNITY_TABLE).append(" bo ");
        sql.append("where 1 = 1");

        appendDateFilter(sql, params, filter);
        appendInFilter(sql, params, filter, "bo.fk_ahkd_basedatafield_bo",
                "dept", "department", "org", "dept_id", "fk_ahkd_basedatafield_bo");
        appendInFilter(sql, params, filter, "bo.fk_ahkd_recmanager",
                "sales", "salesman", "user", "sales_id", "fk_ahkd_recmanager");
        appendInFilter(sql, params, filter, "bo.fk_ahkd_xmcplx",
                "producttype", "product_type", "xmcplx", "fk_ahkd_xmcplx");
        appendInFilter(sql, params, filter, "bo.fk_ahkd_bussource",
                "bussource", "datasource", "source", "data_source", "fk_ahkd_bussource");

        sql.append(" group by bo.fk_ahkd_basedatafield_bo, bo.fk_ahkd_recmanager");
        sql.append(" order by bo.fk_ahkd_basedatafield_bo, bo.fk_ahkd_recmanager");
        return DB.queryDataSet(this.getClass().getName(), DBRoute.of(EXTEND_DB_ROUTE), sql.toString(), params.toArray());
    }

    private ReportColumn createBaseDataColumn(String fieldKey, String caption, String entityId, int width) {
        ReportColumn column = ReportColumn.createBaseDataColumn(fieldKey, entityId);
        column.setCaption(new LocaleString(caption));
        column.setWidth(new LocaleString(String.valueOf(width)));
        return column;
    }

    private DecimalReportColumn createNumberColumn(String fieldKey, String caption, int scale, int width, boolean percent,
            String titleReport) {
        DecimalReportColumn column = new DecimalReportColumn();
        column.setFieldKey(fieldKey);
        column.setCaption(new LocaleString(caption));
        column.setFieldType(scale == 0 ? ReportColumn.TYPE_INTEGER : ReportColumn.TYPE_DECIMAL);
        column.setScale(scale);
        column.setWidth(new LocaleString(String.valueOf(width)));
        column.setXTitleReport(titleReport);
        if (percent) {
            column.setDisplayFormatString("#,##0.00%");
        }
        return column;
    }

    private String sumStage(String stage) {
        return "sum(case when bo.fk_ahkd_currentstage = '" + stage + "' then 1 else 0 end)";
    }

    private String rateExpr(String condition) {
        return "case when count(1) = 0 then 0 else round(sum(case when " + condition
                + " then 1 else 0 end) * 1.0000 / count(1), 4) end";
    }

    private void appendDateFilter(StringBuilder sql, List<Object> params, FilterInfo filter) {
        Date startDate = getFilterDate(filter, "startdate", "begindate", "fromdate", "start_date", "fstartdate");
        Date endDate = getFilterDate(filter, "enddate", "todate", "end_date", "fenddate");
        if (startDate == null && endDate == null) {
            Integer year = getFilterYear(filter, "year", "fyear", "filteryear", "bizyear");
            if (year != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.clear();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, Calendar.JANUARY);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                startDate = calendar.getTime();
                calendar.add(Calendar.YEAR, 1);
                endDate = calendar.getTime();
            }
        } else if (endDate != null) {
            endDate = nextDay(endDate);
        }

        if (startDate != null) {
            sql.append(" and bo.fcreatetime >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" and bo.fcreatetime < ?");
            params.add(endDate);
        }
    }

    private Date getFilterDate(FilterInfo filter, String... filterKeys) {
        if (filter == null) {
            return null;
        }

        for (String filterKey : filterKeys) {
            Date date = getFilterDateValue(filter, filterKey);
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    private Integer getFilterYear(FilterInfo filter, String... filterKeys) {
        if (filter == null) {
            return null;
        }

        for (String filterKey : filterKeys) {
            Object value = getFilterValue(filter, filterKey);
            Integer year = parseYear(value);
            if (year != null) {
                return year;
            }
        }
        return null;
    }

    private Integer parseYear(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date) value);
            return calendar.get(Calendar.YEAR);
        }
        if (value instanceof Number) {
            int year = ((Number) value).intValue();
            return isValidYear(year) ? year : null;
        }
        String text = String.valueOf(value).trim();
        if (text.length() >= 4) {
            try {
                int year = Integer.parseInt(text.substring(0, 4));
                return isValidYear(year) ? year : null;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private boolean isValidYear(int year) {
        return year >= 1900 && year <= 2999;
    }

    private Date nextDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private void appendInFilter(StringBuilder sql, List<Object> params, FilterInfo filter, String columnName,
            String... filterKeys) {
        List<Object> values = getFilterValues(filter, filterKeys);
        if (values.isEmpty()) {
            return;
        }

        if (values.size() == 1) {
            sql.append(" and ").append(columnName).append(" = ?");
            params.add(values.get(0));
            return;
        }

        sql.append(" and ").append(columnName).append(" in (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
            params.add(values.get(i));
        }
        sql.append(")");
    }

    private List<Object> getFilterValues(FilterInfo filter, String... filterKeys) {
        List<Object> values = new ArrayList<>();
        if (filter == null) {
            return values;
        }

        Set<Object> distinctValues = new LinkedHashSet<>();
        for (String filterKey : filterKeys) {
            addFilterValue(distinctValues, getFilterValue(filter, filterKey));
            addFilterValue(distinctValues, getFilterDynamicObject(filter, filterKey));
            addFilterValue(distinctValues, getFilterDynamicObjectCollection(filter, filterKey));
            if (!distinctValues.isEmpty()) {
                break;
            }
        }
        values.addAll(distinctValues);
        return values;
    }

    private Object getFilterValue(FilterInfo filter, String filterKey) {
        try {
            return filter.getValue(filterKey);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Date getFilterDateValue(FilterInfo filter, String filterKey) {
        try {
            return filter.getDate(filterKey);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private DynamicObject getFilterDynamicObject(FilterInfo filter, String filterKey) {
        try {
            return filter.getDynamicObject(filterKey);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private DynamicObjectCollection getFilterDynamicObjectCollection(FilterInfo filter, String filterKey) {
        try {
            return filter.getDynamicObjectCollection(filterKey);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void addFilterValue(Set<Object> values, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof DynamicObjectCollection) {
            DynamicObjectCollection collection = (DynamicObjectCollection) value;
            for (Object item : collection) {
                addFilterValue(values, item);
            }
            return;
        }
        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                addFilterValue(values, item);
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addFilterValue(values, Array.get(value, i));
            }
            return;
        }
        Object normalizedValue = normalizeFilterValue(value);
        if (normalizedValue != null && !"".equals(String.valueOf(normalizedValue).trim())) {
            values.add(normalizedValue);
        }
    }

    private Object normalizeFilterValue(Object value) {
        if (!(value instanceof DynamicObject)) {
            return value;
        }

        DynamicObject dynamicObject = (DynamicObject) value;
        Object idValue = getDynamicValue(dynamicObject, "id");
        if (idValue != null) {
            return idValue;
        }
        idValue = getDynamicValue(dynamicObject, "fid");
        if (idValue != null) {
            return idValue;
        }
        idValue = getDynamicValue(dynamicObject, "number");
        if (idValue != null) {
            return idValue;
        }
        return dynamicObject.toString();
    }

    private Object getDynamicValue(DynamicObject dynamicObject, String fieldKey) {
        try {
            if (dynamicObject.containsProperty(fieldKey)) {
                return dynamicObject.get(fieldKey);
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return null;
    }
}
