package liu1.liu11.liu1.liu1.plugin.list;

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

import kd.bos.db.DB;
import kd.bos.db.DBRoute;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;

public class WeeklyReportListPlugin extends AbstractListPlugin {

    private static final String CREATE_WEEKLY_REPORT_BUTTON = "fyl6_baritemap";
    private static final String WEEKLY_REPORT_TABLE = "tk_fyl6_zhoubao";
    private static final String OPPORTUNITY_FOLLOW_TABLE = "tk_fyl6_zhoubaoxjgj";
    private static final String BUSINESS_OPPORTUNITY_TABLE = "tk_ahkd_busopportunity";
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
        if (existsWeeklyReport(weeklyDate)) {
            this.getView().showErrorNotification(String.format(
                    "\u5df2\u5b58\u5728%s\u5e74%s\u6708%s\u5468\u6b21\u5468\u62a5\uff0c\u4e0d\u5141\u8bb8\u91cd\u590d\u751f\u6210",
                    weeklyDate.year, weeklyDate.month, weeklyDate.weekOfMonth));
            return;
        }

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        long fid = DB.genLongId(WEEKLY_REPORT_TABLE);
        String billNo = "ZB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sql = "insert into " + WEEKLY_REPORT_TABLE
                + " (fid, fbillno, fbillstatus, fcreatetime, fmodifytime, fcreatorid, fmodifierid, fauditorid,"
                + " fk_fyl6_year, fk_fyl6_month, fk_fyl6_week, fk_fyl6_newweek, fk_fyl6_textareafield1, fk_fyl6_wenti)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Object[] params = new Object[] { fid, billNo, "A", now, now, 0L, 0L, 0L,
                String.valueOf(weeklyDate.year), String.valueOf(weeklyDate.month),
                String.valueOf(weeklyDate.weekOfMonth), DEFAULT_TEXT_VALUE, DEFAULT_TEXT_VALUE, DEFAULT_TEXT_VALUE };

        int rows = DB.update(DBRoute.of(EXTEND_DB_ROUTE), sql, params);
        if (rows > 0) {
            int followRows = createOpportunityFollowRows(fid, now);
            this.getView().showSuccessNotification(String.format(
                    "\u5df2\u751f\u6210%s\u5e74%s\u6708\u7b2c%s\u5468\u5468\u62a5\uff0c\u5546\u673a\u8ddf\u8fdb%s\u6761",
                    weeklyDate.year, weeklyDate.month, weeklyDate.weekOfMonth, followRows));
            this.reload();
            return;
        }

        this.getView().showErrorNotification(
                "\u751f\u6210\u5468\u62a5\u5931\u8d25\uff1a\u672a\u5199\u5165\u6570\u636e");
    }

    private boolean existsWeeklyReport(WeeklyDate weeklyDate) {
        String sql = "select count(1) as record_count from " + WEEKLY_REPORT_TABLE
                + " where fk_fyl6_year = ? and fk_fyl6_month = ? and fk_fyl6_week = ?";
        Object[] params = new Object[] { String.valueOf(weeklyDate.year), String.valueOf(weeklyDate.month),
                String.valueOf(weeklyDate.weekOfMonth) };
        Integer count = DB.query(DBRoute.of(EXTEND_DB_ROUTE), sql, params, rs -> {
            if (rs.next()) {
                return rs.getInt("record_count");
            }
            return 0;
        });
        return count != null && count > 0;
    }

    private int createOpportunityFollowRows(long weeklyReportId, Timestamp now) {
        List<OpportunityFollowInfo> followInfos = queryOpportunityFollowInfos();
        if (followInfos.isEmpty()) {
            return 0;
        }

        long[] entryIds = DB.genLongIds(OPPORTUNITY_FOLLOW_TABLE, followInfos.size());
        List<Object[]> batchParams = new ArrayList<>(followInfos.size());
        for (int i = 0; i < followInfos.size(); i++) {
            OpportunityFollowInfo followInfo = followInfos.get(i);
            batchParams.add(new Object[] { weeklyReportId, entryIds[i], i + 1, 0L, now, DEFAULT_TEXT_VALUE,
                    followInfo.status, followInfo.stage });
        }

        String sql = "insert into " + OPPORTUNITY_FOLLOW_TABLE
                + " (fid, fentryid, fseq, fmodifierfield, fmodifydatefield, fk_fyl6_mx, fk_fyl6_zt, fk_fyl6_jd)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?)";
        int[] result = DB.executeBatch(DBRoute.of(EXTEND_DB_ROUTE), sql, batchParams);
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

    private List<OpportunityFollowInfo> queryOpportunityFollowInfos() {
        String sql = "select " + businessStatusExpression() + " as biz_status, "
                + businessStageExpression() + " as biz_stage from " + BUSINESS_OPPORTUNITY_TABLE
                + " where fk_ahkd_currentstage in (?, ?, ?) order by fid";
        Object[] params = new Object[] { "1", "2", "3" };
        return DB.query(DBRoute.of(EXTEND_DB_ROUTE), sql, params, rs -> {
            List<OpportunityFollowInfo> followInfos = new ArrayList<>();
            while (rs.next()) {
                followInfos.add(new OpportunityFollowInfo(rs.getString("biz_status"), rs.getString("biz_stage")));
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

    private static class OpportunityFollowInfo {
        private final String status;
        private final String stage;

        private OpportunityFollowInfo(String status, String stage) {
            this.status = status == null || status.trim().isEmpty() ? DEFAULT_TEXT_VALUE : status;
            this.stage = stage == null || stage.trim().isEmpty() ? DEFAULT_TEXT_VALUE : stage;
        }
    }
}
