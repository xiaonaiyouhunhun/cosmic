package liu1.liu11.liu1.liu1.plugin.list;

import java.math.BigDecimal;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Locale;

import kd.bos.context.RequestContext;
import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;

public class WeeklyReportListPlugin extends AbstractListPlugin {

    private static final String CREATE_WEEKLY_REPORT_BUTTON = "fyl6_baritemap";
    private static final String WEEKLY_REPORT_TABLE = "tk_fyl6_zhoubao";
    private static final String PAYMENT_WORK_TABLE = "tk_fyl6_zhoubaohkgz";
    private static final String OPPORTUNITY_FOLLOW_TABLE = "tk_fyl6_zhoubaoxjgj";
    private static final String ACTION_RECORD_TABLE = "tk_fyl6_zhoubaobzxdjl";
    private static final String RECEIVABLE_TABLE = "tk_ahkd_receivable";
    private static final String BUSINESS_OPPORTUNITY_TABLE = "tk_ahkd_busopportunity";
    private static final String VISIT_RECORD_TABLE = "tk_ahkd_visitrecord";
    private static final String EXTEND_DB_ROUTE = "secd";
    private static final String DEFAULT_TEXT_VALUE = " ";

    @Override
    public void registerListener(EventObject e) {
        super.registerListener(e);
        this.addItemClickListeners(CREATE_WEEKLY_REPORT_BUTTON);
    }

    @Override
    public void itemClick(ItemClickEvent evt) {
        super.itemClick(evt);
        if (!CREATE_WEEKLY_REPORT_BUTTON.equals(evt.getItemKey())) {
            return;
        }

        WeeklyDate weeklyDate = getWeeklyDate(LocalDate.now());
        long currentUserId = getCurrentUserId();
        if (existsWeeklyReport(weeklyDate, currentUserId)) {
            this.getView().showErrorNotification(String.format(
                    "\u5f53\u524d\u767b\u5f55\u4eba%s\u5df2\u5b58\u5728%s\u5e74%s\u6708%s\u5468\u6b21\u5468\u62a5\uff0c\u4e0d\u5141\u8bb8\u91cd\u590d\u751f\u6210",
                    getCurrentUserDisplayName(currentUserId), weeklyDate.year, weeklyDate.month,
                    weeklyDate.weekOfMonth));
            return;
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        long fid = DB.genLongId(WEEKLY_REPORT_TABLE);
        String billNo = "ZB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sql = "insert into " + WEEKLY_REPORT_TABLE
                + " (fid, fbillno, fbillstatus, fcreatetime, fmodifytime, fcreatorid, fmodifierid, fauditorid,"
                + " fk_fyl6_year, fk_fyl6_month, fk_fyl6_week, fk_fyl6_newweek, fk_fyl6_textareafield1, fk_fyl6_wenti)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] params = new Object[] { fid, billNo, "A", now, now, currentUserId, currentUserId, 0L,
                String.valueOf(weeklyDate.year), String.valueOf(weeklyDate.month),
                String.valueOf(weeklyDate.weekOfMonth), DEFAULT_TEXT_VALUE, DEFAULT_TEXT_VALUE, DEFAULT_TEXT_VALUE };

        int rows = DB.update(DBRoute.of(EXTEND_DB_ROUTE), sql, params);
        if (rows > 0) {
            int paymentRows = createPaymentWorkRows(fid, now, currentUserId);
            int followRows = createOpportunityFollowRows(fid, now, currentUserId);
            int actionRows = createActionRecordRows(fid, now, LocalDate.now(), currentUserId);
            this.getView().showSuccessNotification(String.format(
                    "\u5df2\u751f\u6210%s\u5e74%s\u6708\u7b2c%s\u5468\u5468\u62a5\uff0c\u56de\u6b3e\u5de5\u4f5c%s\u6761\uff0c\u5546\u673a\u8ddf\u8fdb%s\u6761\uff0c\u672c\u5468\u884c\u52a8%s\u6761",
                    weeklyDate.year, weeklyDate.month, weeklyDate.weekOfMonth, paymentRows, followRows, actionRows));
            this.reload();
            return;
        }

        this.getView().showErrorNotification(
                "\u751f\u6210\u5468\u62a5\u5931\u8d25\uff1a\u672a\u5199\u5165\u6570\u636e");
    }

    private long getCurrentUserId() {
        RequestContext requestContext = RequestContext.get();
        return requestContext == null ? 0L : requestContext.getCurrUserId();
    }

    private String getCurrentUserDisplayName(long currentUserId) {
        RequestContext requestContext = RequestContext.get();
        if (requestContext != null && requestContext.getUserName() != null
                && !requestContext.getUserName().trim().isEmpty()) {
            return requestContext.getUserName().trim();
        }
        return String.valueOf(currentUserId);
    }

    private boolean existsWeeklyReport(WeeklyDate weeklyDate, long creatorId) {
        String sql = "select count(1) as record_count from " + WEEKLY_REPORT_TABLE
                + " where fk_fyl6_year = ? and fk_fyl6_month = ? and fk_fyl6_week = ? and fcreatorid = ?";
        Object[] params = new Object[] { String.valueOf(weeklyDate.year), String.valueOf(weeklyDate.month),
                String.valueOf(weeklyDate.weekOfMonth), creatorId };
        Integer count = DB.query(DBRoute.of(EXTEND_DB_ROUTE), sql, params, rs -> {
            if (rs.next()) {
                return rs.getInt("record_count");
            }
            return 0;
        });
        return count != null && count > 0;
    }

    private int createPaymentWorkRows(long weeklyReportId, Timestamp now, long currentUserId) {
        List<PaymentWorkInfo> paymentWorkInfos = queryPaymentWorkInfos(currentUserId);
        if (paymentWorkInfos.isEmpty()) {
            return 0;
        }

        long[] entryIds = DB.genLongIds(PAYMENT_WORK_TABLE, paymentWorkInfos.size());
        List<Object[]> batchParams = new ArrayList<>(paymentWorkInfos.size());
        for (int i = 0; i < paymentWorkInfos.size(); i++) {
            PaymentWorkInfo paymentWorkInfo = paymentWorkInfos.get(i);
            batchParams.add(new Object[] { weeklyReportId, paymentWorkInfo.receivableAmount, DEFAULT_TEXT_VALUE, 0L,
                    paymentWorkInfo.overdueAmount, i + 1, now, DEFAULT_TEXT_VALUE, entryIds[i],
                    paymentWorkInfo.projectName });
        }

        String sql = "insert into " + PAYMENT_WORK_TABLE
                + " (fid, fk_fyl6_ysje, fk_fyl6_hk, fk_fyl6_modifierfield, fk_fyl6_yqje, fseq,"
                + " fk_fyl6_modifydatefield, fk_fyl6_mx, fentryid, fk_fyl6_xmmc)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return countBatchSuccess(DB.executeBatch(DBRoute.of(EXTEND_DB_ROUTE), sql, batchParams));
    }

    private List<PaymentWorkInfo> queryPaymentWorkInfos(long currentUserId) {
        String sql = "select fk_ahkd_decimalfield5 as receivable_amount,"
                + " fk_ahkd_decimalfield6 as overdue_amount, fk_ahkd_project as project_name from " + RECEIVABLE_TABLE
                + " where fk_ahkd_decimalfield5 > 0 and fk_ahkd_salesmanager = ? order by fid";
        Object[] params = new Object[] { currentUserId };
        return DB.query(DBRoute.of(EXTEND_DB_ROUTE), sql, params, rs -> {
            List<PaymentWorkInfo> paymentWorkInfos = new ArrayList<>();
            while (rs.next()) {
                paymentWorkInfos.add(new PaymentWorkInfo(formatAmount(rs.getBigDecimal("receivable_amount")),
                        formatAmount(rs.getBigDecimal("overdue_amount")), rs.getString("project_name")));
            }
            return paymentWorkInfos;
        });
    }

    private int createOpportunityFollowRows(long weeklyReportId, Timestamp now, long currentUserId) {
        List<OpportunityFollowInfo> followInfos = queryOpportunityFollowInfos(currentUserId);
        if (followInfos.isEmpty()) {
            return 0;
        }

        long[] entryIds = DB.genLongIds(OPPORTUNITY_FOLLOW_TABLE, followInfos.size());
        List<Object[]> batchParams = new ArrayList<>(followInfos.size());
        for (int i = 0; i < followInfos.size(); i++) {
            OpportunityFollowInfo followInfo = followInfos.get(i);
            batchParams.add(new Object[] { weeklyReportId, entryIds[i], i + 1, 0L, now, DEFAULT_TEXT_VALUE,
                    followInfo.status, followInfo.stage, followInfo.opportunityName });
        }

        String sql = "insert into " + OPPORTUNITY_FOLLOW_TABLE
                + " (fid, fentryid, fseq, fmodifierfield, fmodifydatefield, fk_fyl6_mx,"
                + " fk_fyl6_zt, fk_fyl6_jd, fk_fyl6_sjmc)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return countBatchSuccess(DB.executeBatch(DBRoute.of(EXTEND_DB_ROUTE), sql, batchParams));
    }

    private int createActionRecordRows(long weeklyReportId, Timestamp now, LocalDate currentDate, long currentUserId) {
        List<ActionRecordInfo> actionRecordInfos = queryActionRecordInfos(currentDate, currentUserId);
        if (actionRecordInfos.isEmpty()) {
            return 0;
        }

        long[] entryIds = DB.genLongIds(ACTION_RECORD_TABLE, actionRecordInfos.size());
        List<Object[]> batchParams = new ArrayList<>(actionRecordInfos.size());
        for (int i = 0; i < actionRecordInfos.size(); i++) {
            ActionRecordInfo actionRecordInfo = actionRecordInfos.get(i);
            batchParams.add(new Object[] { weeklyReportId, actionRecordInfo.visitMethod, actionRecordInfo.visitTheme,
                    i + 1, 0L, actionRecordInfo.customer, actionRecordInfo.visitDate,
                    actionRecordInfo.communicationContent, now, entryIds[i], actionRecordInfo.projectName });
        }

        String sql = "insert into " + ACTION_RECORD_TABLE
                + " (fid, fk_fyl6_bfxs, fk_fyl6_bfzt, fseq, fk_fyl6_modifierfield1, fk_fyl6_kh,"
                + " fk_fyl6_bfrq, fk_fyl6_jlnr, fk_fyl6_modifydatefield1, fentryid, fk_fyl6_xmmc)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return countBatchSuccess(DB.executeBatch(DBRoute.of(EXTEND_DB_ROUTE), sql, batchParams));
    }

    private List<ActionRecordInfo> queryActionRecordInfos(LocalDate currentDate, long currentUserId) {
        LocalDate weekStart = currentDate.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        Timestamp startTime = Timestamp.valueOf(weekStart.atStartOfDay());
        Timestamp endTime = Timestamp.valueOf(weekStart.plusWeeks(1).atStartOfDay());
        String sql = "select fk_ahkd_visitmethod as visit_method, fk_ahkd_visittheme as visit_theme,"
                + " fk_ahkd_customer as customer, fk_ahkd_visitdate as visit_date,"
                + " fk_ahkd_textareafield1 as communication_content, fk_ahkd_xmmc as project_name"
                + " from " + VISIT_RECORD_TABLE
                + " where fk_ahkd_visitdate >= ? and fk_ahkd_visitdate < ? and fcreatorid = ?"
                + " order by fk_ahkd_visitdate, fid";
        Object[] params = new Object[] { startTime, endTime, currentUserId };
        return DB.query(DBRoute.of(EXTEND_DB_ROUTE), sql, params, rs -> {
            List<ActionRecordInfo> actionRecordInfos = new ArrayList<>();
            while (rs.next()) {
                actionRecordInfos.add(new ActionRecordInfo(rs.getString("visit_method"),
                        rs.getString("visit_theme"), rs.getString("customer"), rs.getTimestamp("visit_date"),
                        rs.getString("communication_content"), rs.getString("project_name")));
            }
            return actionRecordInfos;
        });
    }

    private int countBatchSuccess(int[] result) {
        int successCount = 0;
        for (int updateCount : result) {
            if (updateCount >= 0) {
                successCount += updateCount;
            } else if (updateCount == Statement.SUCCESS_NO_INFO) {
                successCount++;
            }
        }
        return successCount;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? DEFAULT_TEXT_VALUE : amount.stripTrailingZeros().toPlainString();
    }

    private static String normalizeText(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) {
            return DEFAULT_TEXT_VALUE;
        }
        String value = text.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private List<OpportunityFollowInfo> queryOpportunityFollowInfos(long currentUserId) {
        String sql = "select fname as opportunity_name, " + businessStatusExpression() + " as biz_status, "
                + businessStageExpression() + " as biz_stage from " + BUSINESS_OPPORTUNITY_TABLE
                + " where fk_ahkd_currentstage in (?, ?, ?) and fcreatorid = ? order by fid";
        Object[] params = new Object[] { "1", "2", "3", currentUserId };
        return DB.query(DBRoute.of(EXTEND_DB_ROUTE), sql, params, rs -> {
            List<OpportunityFollowInfo> followInfos = new ArrayList<>();
            while (rs.next()) {
                followInfos.add(new OpportunityFollowInfo(rs.getString("opportunity_name"),
                        rs.getString("biz_status"), rs.getString("biz_stage")));
            }
            return followInfos;
        });
    }

    private String businessStatusExpression() {
        return "case fk_ahkd_currentstage when '1' then 'RFQ' when '2' then 'TBS' when '3' then 'TBA' "
                + "else fk_ahkd_currentstage end";
    }

    private String businessStageExpression() {
        return "case fk_ahkd_state when '2' then 'S1-Leads' when '3' then 'S2-NBO Prep' "
                + "when '4' then 'S3-NBO Neg' when '5' then 'S4-BO Prep' when '6' then 'S5-BO Neg' "
                + "when '7' then 'S6-Bidding' when '8' then 'S7-Award' else fk_ahkd_state end";
    }

    private WeeklyDate getWeeklyDate(LocalDate date) {
        WeekFields weekFields = WeekFields.of(Locale.CHINA);
        int year = date.getYear();
        int month = date.getMonthValue();
        int weekOfMonth = date.get(weekFields.weekOfMonth());
        return new WeeklyDate(year, month, weekOfMonth);
    }

    private static class WeeklyDate {
        private final int year;
        private final int month;
        private final int weekOfMonth;

        private WeeklyDate(int year, int month, int weekOfMonth) {
            this.year = year;
            this.month = month;
            this.weekOfMonth = weekOfMonth;
        }
    }

    private static class PaymentWorkInfo {
        private final String receivableAmount;
        private final String overdueAmount;
        private final String projectName;

        private PaymentWorkInfo(String receivableAmount, String overdueAmount, String projectName) {
            this.receivableAmount = normalizeText(receivableAmount, 50);
            this.overdueAmount = normalizeText(overdueAmount, 50);
            this.projectName = normalizeText(projectName, 50);
        }
    }

    private static class ActionRecordInfo {
        private final String visitMethod;
        private final String visitTheme;
        private final String customer;
        private final Timestamp visitDate;
        private final String communicationContent;
        private final String projectName;

        private ActionRecordInfo(String visitMethod, String visitTheme, String customer, Timestamp visitDate,
                String communicationContent, String projectName) {
            this.visitMethod = normalizeText(visitMethod, 50);
            this.visitTheme = normalizeText(visitTheme, 50);
            this.customer = normalizeText(customer, 50);
            this.visitDate = visitDate;
            this.communicationContent = normalizeText(communicationContent, 50);
            this.projectName = normalizeText(projectName, 50);
        }
    }

    private static class OpportunityFollowInfo {
        private final String opportunityName;
        private final String status;
        private final String stage;

        private OpportunityFollowInfo(String opportunityName, String status, String stage) {
            this.opportunityName = normalizeText(opportunityName, 50);
            this.status = normalizeText(status, 50);
            this.stage = normalizeText(stage, 50);
        }
    }
}
