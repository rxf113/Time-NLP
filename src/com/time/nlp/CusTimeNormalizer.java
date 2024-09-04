package com.time.nlp;

import com.time.nlp.unit.*;
import com.time.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.time.util.StringNumberUtil.replaceCapitalNumberWithNumber;

public enum CusTimeNormalizer {


    INSTANCE;

    public static CusTimeNormalizer getINSTANCE() {
        return INSTANCE;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(CusTimeNormalizer.class);

    /**
     * 单个时间，闹钟业务
     *
     * @param time
     * @param clockSlotsRepeat
     * @return
     */
    public CusTimeModel singleTime(String query, String time, String clockSlotsRepeat) {
        clockSlotsRepeat = extractRepeat(query, clockSlotsRepeat);
        CusTimeModel timeModel = processRepeat(clockSlotsRepeat);

        //兜底模型对 每周五下午三点 提取的是 每周5 和 下午三点
        if (clockSlotsRepeat.startsWith("每") && !time.contains(clockSlotsRepeat.substring(1))) {
            time = StringUtils.defaultString(clockSlotsRepeat, "") + time;
            time = time.replaceFirst("每", "");
        }

        //模型兜底对 定一个半小时后的闹钟 提取的 一个半小时 去掉一个
        if (query.startsWith("定一个") || query.startsWith("设置一个")) {
            time = time.replaceFirst("一个", "");
        }

        //兜底模型对 例如: 1.5小时后 只提取了1.5小时
        time = correctTime(query, time);

        //对于time_delay 用 range处理
        Matcher matcher;
        if ((matcher = TIME_DELAY_PATTERN.matcher(time)).find()) {
            //timeDelay
            String direction = matcher.group(3);

            int model;
            if ("前".equals(direction)) {
                model = 1;
            } else if ("后".equals(direction)) {
                model = 2;
            } else {
                model = 3;
            }
            CusTimeModel cusTimeModel = CusRangeTimeUtil.getInstance().processTime(time, timeModel, model);
            CusTimeModel res = new CusTimeModel();
            res.setRepeat(cusTimeModel.getRepeat());
            res.setTime(cusTimeModel.getOriginTime());
            return res;
        }

        //20240724: 用新的方式补充
        TimeNormalizer normalizer = new TimeNormalizer();
        normalizer.setPreferFuture(true);
        TimeUnit[] units = normalizer.parse(time);
        if (units.length > 0) {
            //将 Time_Expression 做为originTime
            timeModel.setOriginTime(units[0].Time_Expression);
            timeModel.setTime(DateUtil.formatDateChoose(units[0].getTime()));
        } else {
            //尝试使用固定词匹配
            CusTimeModel cusTimeModel = FixWordTimeProcess.getInstance().process(time);
            if (cusTimeModel.getTime() != null) {
                return cusTimeModel;
            }
            LOGGER.warn("time: {}, 获取时间失败", time);
            //兜底
            cusTimeModel.setTime("");
            return cusTimeModel;

        }
        return timeModel;
    }


    private static final Pattern TIME_DELAY_PATTERN = Pattern.compile("^(.+)(年|个月|周|日|天|小时|分钟)(前|后|内).*");

    private static final Pattern RANGE_TIME_DELAY_PATTERN = Pattern.compile("^之?(上|前|后|下)(.+)(年|月|周|日|天|小时|分钟|个月).*");

    private CusTimeModel postRangeTime(CusTimeModel cusTimeModel, String time) {
        if (StringUtils.isBlank(cusTimeModel.getTime()) && StringUtils.isBlank(cusTimeModel.getTimeEnd())) {
            //扩展 特殊case等
            CusTimeModel exModel = ExtendTimeNormalizeUtil.getInstance().rangeTime(time);
            exModel.setOriginTime(cusTimeModel.getOriginTime());
            exModel.setRepeat(cusTimeModel.getRepeat());
            return exModel;
        }
        return cusTimeModel;
    }

    public CusTimeModel rangeTime(String query, String time, String clockSlotsRepeat) {
        CusTimeModel cusTimeModel = rangeTimeProcess(query, time, clockSlotsRepeat);
        return postRangeTime(cusTimeModel, time);
    }

    public CusTimeModel rangeTime(String query, String time) {
        CusTimeModel cusTimeModel = rangeTimeProcess(query, time);
        return postRangeTime(cusTimeModel, time);
    }


    public CusTimeModel rangeTimeProcess(String query, String time, String clockSlotsRepeat) {
        clockSlotsRepeat = extractRepeat(query, clockSlotsRepeat);
        CusTimeModel repeatModel = processRepeat(clockSlotsRepeat);

        CusTimeModel cusTimeModel = rangeTimeProcess(query, time);

        //扩展 特殊case等
        if (StringUtils.isBlank(cusTimeModel.getTime()) && StringUtils.isBlank(cusTimeModel.getTimeEnd())) {
            cusTimeModel = ExtendTimeNormalizeUtil.getInstance().rangeTime(time);
        }

        cusTimeModel.setRepeat(repeatModel.getRepeat());
        return cusTimeModel;
    }

    /**
     * @param query
     * @param time
     * @return
     */
    public CusTimeModel rangeTimeProcess(String query, String time) {
        time = replaceCapitalNumberWithNumber(time);
        time = timeReplace(time);

        query = replaceCapitalNumberWithNumber(query);
        query = timeReplace(query);

        //解决nlp 槽位提取错误 例如: 1.5小时后 只提取了1.5小时
        time = correctTime(query, time);

        CusTimeModel cusTimeModel = new CusTimeModel();
        TimeNormalizer normalizer = new TimeNormalizer();
        normalizer.setPreferFuture(false);
        TimeUnit[] units = normalizer.parse(time);
        if (units.length == 2) {
            cusTimeModel.setTime(DateUtil.formatDateStartDefault(units[0].getTime()));
            cusTimeModel.setTimeEnd(DateUtil.formatDateEndDefault(units[1].getTime()));
            //将 Time_Expression 做为originTime
            cusTimeModel.setOriginTime(units[0].Time_Expression + "到" + units[1].Time_Expression);
            return cusTimeModel;
        }
        //判断是否是 某个时间前或者后, 一个月前(的那天)
        Matcher matcher;
        if ((matcher = TIME_DELAY_PATTERN.matcher(time)).find()) {
            //timeDelay
            String direction = matcher.group(3);

            int model;
            if ("前".equals(direction)) {
                model = 1;
            } else if ("后".equals(direction)) {
                model = 2;
            } else {
                model = 3;
            }
            CusTimeModel timeModel = CusRangeTimeUtil.getInstance().processTime(time, cusTimeModel, model);
            if (timeModel.getTime() != null && timeModel.getTimeEnd() != null) {
                return timeModel;
            }
        }

        //判断是否是 前/后 某个时间段
        if ((matcher = RANGE_TIME_DELAY_PATTERN.matcher(time)).find()) {
            //timeDelay
            String direction = matcher.group(1);

            int model = 1;
            if ("前".equals(direction) || "上".equals(direction)) {
                model = 1;
            } else if ("后".equals(direction) || "下".equals(direction)) {
                model = 2;
            } else {
                //暂未遇到这种情况
            }
            CusTimeModel timeModel = CusRangeTimeUtil.getInstance().processTimePrior(time, cusTimeModel, model);
            if (timeModel.getTime() != null && timeModel.getTimeEnd() != null) {
                return timeModel;
            }
        }

        //处理具体日期
        cusTimeModel = SpecificDateProcess.getInstance().process(time, units);
        if (cusTimeModel.getTime() != null && cusTimeModel.getTimeEnd() != null) {
            return cusTimeModel;
        }

        //其它情况 按单位处理
        if (units.length == 0) {
            LOGGER.warn("time: {}, 获取时间失败", time);
            cusTimeModel.setTime("");
            return cusTimeModel;
        }

        //将 Time_Expression 做为originTime
        cusTimeModel.setOriginTime(units[0].Time_Expression);

        if (time.matches(".*[天日号]$") || units[0].Time_Expression.endsWith("日")) {
            return DayProcess.getInstance().process(time, units);
        } else if (time.matches(".*[周]$") || units[0].Time_Expression.endsWith("周")) {
            return WeekProcess.getInstance().process(time, units);
        } else if (time.matches(".*月$") || units[0].Time_Expression.endsWith("月")) {
            return MonthProcess.getInstance().process(time, units);
        } else if (time.matches(".*年$") || units[0].Time_Expression.endsWith("年")) {
            return YearProcess.getInstance().process(time, units);
        }

        if (units.length == 1 && (units[0].Time_Norm.endsWith("时") || units[0].Time_Norm.endsWith("分") || units[0].Time_Norm.endsWith("秒"))) {
            //如果是精确到小时的时间，没有范围
            cusTimeModel.setTime(DateUtil.formatDateDefault(units[0].getTime()));
            return cusTimeModel;
        }

        //其余时间 默认按固定时间 按天算
        Date date = units[0].getTime();
        cusTimeModel.setTime(DateUtil.formatDateStartDefault(date));
        cusTimeModel.setTimeEnd(DateUtil.formatDateEndDefault(date));

        return cusTimeModel;
    }

    private String timeReplace(String originTime) {
        return originTime
                .replace("十", "10").replace("两", "2")
                .replace("星期", "周")
                .replace("周日", "周7")
                .replace("周天", "周7")
                .replace("个半", ".5")
                .replace("半个", "0.5")
                .replace("天半", "天12小时")
                .replace("分半", "分30秒")
                .replace("点半", "点30分")
                .replace("年半", "年6月")
                .replace("半", "0.5")
                .replace("月份", "月")
                .replace("个礼拜", "周")
                .replace("个星期", "周");
    }

    private CusTimeModel processRepeat(String clockSlotsRepeat) {
        CusTimeModel timeModel = new CusTimeModel();
        if (StringUtils.isBlank(clockSlotsRepeat)) {
            return timeModel;
        }
        if (clockSlotsRepeat.contains("年")) {
            timeModel.setRepeat("year");
        } else if (clockSlotsRepeat.contains("月")) {
            timeModel.setRepeat("month");
        } else if (clockSlotsRepeat.contains("周") || clockSlotsRepeat.contains("星期")) {
            timeModel.setRepeat("week");
        } else if (clockSlotsRepeat.contains("工作日")) {
            timeModel.setRepeat("workday");
        } else if (clockSlotsRepeat.contains("天") || clockSlotsRepeat.contains("日")) {
            timeModel.setRepeat("day");
        }
        return timeModel;
    }

    private String correctTime(String query, String time) {
        if (time.matches(".*[前后内].*")) {
            return time;
        }
        if (!query.matches(".*[前后内].*")) {
            return time;
        }
        if (query.matches(".*" + time + "前.*")) {
            return time + "前";
        } else if (query.matches(".*" + time + "内.*")) {
            return time + "内";
        } else if (query.matches(".*" + time + "后.*")) {
            return time + "后";
        }
        return time;
    }


    /**
     * 提取repeat槽位
     *
     * @param query
     * @param clockSlotsRepeat
     * @return
     */
    private String extractRepeat(String query, String clockSlotsRepeat) {
        //已经有槽位了
        if (StringUtils.isNotBlank(clockSlotsRepeat)) {
            return clockSlotsRepeat;
        }
        //没有槽位，从query中提取出来
        Matcher matcher = EVERY_WEEK.matcher(query);
        if (matcher.find()) {
            return "每" + matcher.group(1);
        }
        matcher = EVERY_Other.matcher(query);
        if (matcher.find()) {
            return "每" + matcher.group(1);
        }
        return "";
    }

    Pattern EVERY_WEEK = Pattern.compile(".*每[个一]?((?:周|星期)[一二三四五六日1234567]?).*");

    Pattern EVERY_Other = Pattern.compile(".*每[个一]?([天月年]).*");

    /**
     * 定时器转换
     *
     * @param time
     * @return
     */
    public List<TimerModel> timerConvert(String time) {
        //提取时分秒
        String withNumber = replaceCapitalNumberWithNumber(time);
        withNumber = replaceSpecialWord(withNumber);
        withNumber = timeReplace(withNumber);
        TimeNumModel[] timeNumberArray = extractTimeNumber(withNumber);
        String hour = StringUtils.defaultString(timeNumberArray[HOUR_IDX].getTime(), "0");
        String minute = StringUtils.defaultString(timeNumberArray[MINUTE_IDX].getTime(), "0");
        String second = StringUtils.defaultString(timeNumberArray[SECS_IDX].getTime(), "0");

        return timerModelsFormat(hour, minute, second);
    }

    private TimeNumModel[] extractTimeNumber(String fullTime) {
        TimeNumModel[] timeNums = new TimeNumModel[PATTERN_ARRAY.length];

        for (int i = 0; i < PATTERN_ARRAY.length; i++) {
            Matcher matcher = PATTERN_ARRAY[i].matcher(fullTime);
            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    timeNums[i] = new TimeNumModel(matcher.group(1));
                } else if (matcher.group(2) != null) {
                    timeNums[i] = new TimeNumModel(matcher.group(2));
                }
            } else {
                timeNums[i] = new TimeNumModel(null);
            }
        }
        return timeNums;
    }

    private String replaceSpecialWord(String origin) {
        return origin.replace("十", "10").replace("两", "2");
    }

    private List<TimerModel> timerModelsFormat(String hour, String minute, String second) {
// Parse input strings to doubles
        double hours = Double.parseDouble(hour);
        double minutes = Double.parseDouble(minute);
        double seconds = Double.parseDouble(second);

        // Convert hours to hours and minutes
        int h = (int) hours;
        double remainingMinutes = (hours - h) * 60;

        // Add the input minutes to the remaining minutes from hours conversion
        double totalMinutes = remainingMinutes + minutes;
        int m = (int) totalMinutes;
        double remainingSeconds = (totalMinutes - m) * 60;

        // Add the input seconds to the remaining seconds from minutes conversion
        double totalSeconds = remainingSeconds + seconds;
        int s = (int) totalSeconds;

        int finalMinutes = m + (s / 60);
        s = s % 60;
        return Arrays.asList(new TimerModel("hour", Integer.toString(h)), new TimerModel("minute", Integer.toString(finalMinutes)), new TimerModel("second", Integer.toString(s)));
    }

    private static final Pattern YEAR_COMPILE = Pattern.compile("(\\d+)年");
    private static final Pattern MONTH_COMPILE = Pattern.compile("(\\d+)月|(\\d+)个月");
    private static final Pattern DAY_COMPILE = Pattern.compile("(?:\\d+月)?(\\d+)[日号]|(\\d+)天");
    //    private static final Pattern HOUR_COMPILE = Pattern.compile("(\\d+)点|(\\d+)小时");
    private static final Pattern HOUR_COMPILE = Pattern.compile("(\\d+(?:\\.\\d+)?)点|(\\d+(?:\\.\\d+)?)个?小时");
    private static final Pattern MINUTE_COMPILE = Pattern.compile("\\d+点(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)(?:分钟|分)");
    private static final Pattern SECS_COMPILE = Pattern.compile("\\d+分(\\d+)|(\\d+)秒");
    private static final Pattern WEEK_COMPILE = Pattern.compile("[周|星期](\\d+)");
    private static final Pattern[] PATTERN_ARRAY = {YEAR_COMPILE, MONTH_COMPILE, DAY_COMPILE, HOUR_COMPILE, MINUTE_COMPILE,
            SECS_COMPILE, WEEK_COMPILE};

    private static final Integer YEAR_IDX = 0;
    private static final Integer MONTH_IDX = 1;
    private static final Integer DAY_IDX = 2;
    private static final Integer HOUR_IDX = 3;
    private static final Integer MINUTE_IDX = 4;
    private static final Integer SECS_IDX = 5;
    private static final Integer WEEK_IDX = 6;
}
