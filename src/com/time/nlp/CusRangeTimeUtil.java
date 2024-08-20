package com.time.nlp;

import com.time.util.DateUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.time.util.DateUtil.ISO_8601_FORMATTER_START;
import static com.time.util.StringNumberUtil.replaceCapitalNumberWithNumber;

/**
 * 只支持向前或者向后的移动
 * 一小时后，半天后 两天前 这种，
 */
public class CusRangeTimeUtil {


    // 私有构造函数，防止外部通过new创建实例
    private CusRangeTimeUtil() {
    }

    // 私有静态内部类
    private static class SingletonHelper {
        // 内部类中创建一个私有的静态实例
        private static final CusRangeTimeUtil INSTANCE = new CusRangeTimeUtil();
    }

    // 公有的静态方法，返回Singleton实例
    public static CusRangeTimeUtil getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final Pattern YEAR_COMPILE = Pattern.compile("(\\d+)年");
    private static final Pattern MONTH_COMPILE = Pattern.compile("(\\d+(?:\\.\\d+)?)个?月");
    private static final Pattern DAY_COMPILE = Pattern.compile("(\\d+(?:\\.\\d+)?)天");
    private static final Pattern HOUR_COMPILE = Pattern.compile("(\\d+(?:\\.\\d+)?)个?小时");
    private static final Pattern MINUTE_COMPILE = Pattern.compile("(\\d+(?:\\.\\d+)?)(?:分钟|分)");
    private static final Pattern SECS_COMPILE = Pattern.compile("(\\d+)秒");
    private static final Pattern WEEK_COMPILE = Pattern.compile("(\\d+)[周|星期]");
    private static final Pattern[] PATTERN_ARRAY = {YEAR_COMPILE, MONTH_COMPILE, DAY_COMPILE, HOUR_COMPILE, MINUTE_COMPILE,
            SECS_COMPILE, WEEK_COMPILE};

    private static final Integer YEAR_IDX = 0;
    private static final Integer MONTH_IDX = 1;
    private static final Integer DAY_IDX = 2;
    private static final Integer HOUR_IDX = 3;
    private static final Integer MINUTE_IDX = 4;
    private static final Integer SECS_IDX = 5;
    private static final Integer WEEK_IDX = 6;


    /**
     * 替换汉字为具体时间
     *
     * @param originTime
     * @return
     */
    private String timeReplace(String originTime) {
        return originTime
                .replace("星期", "周")
                .replace("周日", "周7")
                .replace("周天", "周7")
                .replace("个半", ".5")
                .replace("半个", "0.5")
                .replace("天半", "天12小时")
                .replace("分半", "分30秒")
                .replace("点半", "点30分")
                .replace("年半", "年6月")
                .replace("半", "0.5");
    }

    /**
     * 处理 前一个月，后十年
     *
     * @param time
     * @param timeModel
     * @param model     1 前 2后
     * @return
     */
    public CusTimeModel processTimePrior(String time, CusTimeModel timeModel, int model) {
        LocalDateTime target = getTargetTime(time, timeModel, model);
        if (target == null) {
            return timeModel;
        }
        timeModel.setOriginTime(DateUtil.formatLocalDateChoose(target));
        LocalDateTime now = LocalDateTime.now();
        if (model == 1) {
            timeModel.setTimeEnd(now.format(ISO_8601_FORMATTER));
            timeModel.setTime(target.format(ISO_8601_FORMATTER_START));
        } else if (model == 2) {
            timeModel.setTime(now.format(ISO_8601_FORMATTER));
            timeModel.setTimeEnd(target.format(ISO_8601_FORMATTER_START));
        }
        //判断range时长，
        return timeModel;
    }


    /**
     * @param time
     * @param timeModel
     * @param model     1 前 2后 3内
     * @return
     */
    public CusTimeModel processTime(String time, CusTimeModel timeModel, int model) {

        if (StringUtils.isBlank(time)) {
            return timeModel;
        }


//        if (time.matches("一个.+(年|月|日|天|小时|点|分|分钟|秒)")) {
//            time = time.replaceFirst("一个", "");
//        }

        String fullTime = time;
        fullTime = timeReplace(fullTime);

        String withNumber = replaceCapitalNumberWithNumber(fullTime);
        withNumber = replaceSpecialWord(withNumber);

        TimeNumModel[] timeNumberArray = extractTimeNumber(withNumber);

        Optional<TimeNumModel> any = Arrays.stream(timeNumberArray).filter(it -> it.getTime() != null).findAny();
        if (!any.isPresent()) {
            return timeModel;
        }

        /**
         * 大于10年 (例如: 2024年) 直接跳出
         */
        if (timeNumberArray[YEAR_IDX].getTime() != null && Integer.parseInt(timeNumberArray[YEAR_IDX].getTime()) > 10) {
            return timeModel;
        }


        //1.5月转为1月15天，1.5天转为一天12小时  1.5这种情况的小时转为分钟
        double month = Double.parseDouble(StringUtils.defaultString(timeNumberArray[MONTH_IDX].getTime(), "0"));
        double day = Double.parseDouble(StringUtils.defaultString(timeNumberArray[DAY_IDX].getTime(), "0"));
        double hour = Double.parseDouble(StringUtils.defaultString(timeNumberArray[HOUR_IDX].getTime(), "0"));
        double min = Double.parseDouble(StringUtils.defaultString(timeNumberArray[MINUTE_IDX].getTime(), "0"));
        double sec = Double.parseDouble(StringUtils.defaultString(timeNumberArray[SECS_IDX].getTime(), "0"));

        String[] res = convertTime(month, day, hour, min, sec);


        timeNumberArray[MONTH_IDX].setTime(res[0]);
        timeNumberArray[DAY_IDX].setTime(res[1]);
        timeNumberArray[HOUR_IDX].setTime(res[2]);
        timeNumberArray[MINUTE_IDX].setTime(res[3]);
        timeNumberArray[SECS_IDX].setTime(res[4]);


        LocalDateTime target = timeDelay(timeNumberArray, (model == 1 || model == 3));
        timeModel.setOriginTime(DateUtil.formatLocalDateChoose(target));
        LocalDateTime now = LocalDateTime.now();
        if (model == 1) {
            timeModel.setTimeEnd(target.plusDays(1).format(ISO_8601_FORMATTER));
            //设置一个固定起始最小时间 (10年前)
//                cusTimeModel.setTime("min");
            timeModel.setTime(target.minusYears(10).format(ISO_8601_FORMATTER_START));
        } else if (model == 2) {
            timeModel.setTime(target.format(ISO_8601_FORMATTER));
            //设置一个固定终结最大时间 (10年后)
//                cusTimeModel.setTimeEnd("max");
            timeModel.setTimeEnd(target.plusYears(10).plusDays(1).format(ISO_8601_FORMATTER_START));
        } else {
            timeModel.setTimeEnd(now.format(ISO_8601_FORMATTER));
            timeModel.setTime(target.format(ISO_8601_FORMATTER_START));
        }
        //判断range时长，
        return timeModel;
    }

    private LocalDateTime getTargetTime(String time, CusTimeModel timeModel, int model) {

        if (StringUtils.isBlank(time)) {
            return null;
        }


//        if (time.matches("一个.+(年|月|日|天|小时|点|分|分钟|秒)")) {
//            time = time.replaceFirst("一个", "");
//        }

        String fullTime = time;
        fullTime = timeReplace(fullTime);

        String withNumber = replaceCapitalNumberWithNumber(fullTime);
        withNumber = replaceSpecialWord(withNumber);

        TimeNumModel[] timeNumberArray = extractTimeNumber(withNumber);

        Optional<TimeNumModel> any = Arrays.stream(timeNumberArray).filter(it -> it.getTime() != null).findAny();
        if (!any.isPresent()) {
            return null;
        }

        /**
         * 大于10年 (例如: 2024年) 直接跳出
         */
        if (timeNumberArray[YEAR_IDX].getTime() != null && Integer.parseInt(timeNumberArray[YEAR_IDX].getTime()) > 10) {
            return null;
        }


        //1.5月转为1月15天，1.5天转为一天12小时  1.5这种情况的小时转为分钟
        double month = Double.parseDouble(StringUtils.defaultString(timeNumberArray[MONTH_IDX].getTime(), "0"));
        double day = Double.parseDouble(StringUtils.defaultString(timeNumberArray[DAY_IDX].getTime(), "0"));
        double hour = Double.parseDouble(StringUtils.defaultString(timeNumberArray[HOUR_IDX].getTime(), "0"));
        double min = Double.parseDouble(StringUtils.defaultString(timeNumberArray[MINUTE_IDX].getTime(), "0"));
        double sec = Double.parseDouble(StringUtils.defaultString(timeNumberArray[SECS_IDX].getTime(), "0"));

        String[] res = convertTime(month, day, hour, min, sec);


        timeNumberArray[MONTH_IDX].setTime(res[0]);
        timeNumberArray[DAY_IDX].setTime(res[1]);
        timeNumberArray[HOUR_IDX].setTime(res[2]);
        timeNumberArray[MINUTE_IDX].setTime(res[3]);
        timeNumberArray[SECS_IDX].setTime(res[4]);


        return timeDelay(timeNumberArray, (model == 1 || model == 3));
    }


    private static String[] convertTime(double months, double days,
                                        double hours,
                                        double minutes,
                                        double seconds) {
        // 将小数部分转为秒
        days += (months % 1) * 30;
        hours += (days % 1) * 24;
        minutes += (hours % 1) * 60;
        seconds += (minutes % 1) * 60;

        // 去掉小数部分
        months = (int) months;
        days = (int) days;
        hours = (int) hours;
        minutes = (int) minutes;
        seconds = (int) seconds;

        // 调用转换函数
        return convertTime((int) months, (int) days, (int) hours, (int) minutes, (int) seconds);
    }

    public static String[] convertTime(int months, int days, int hours, int minutes, int seconds) {
        // 处理秒到分钟的转换
        minutes += seconds / 60;
        seconds = seconds % 60;

        // 处理分钟到小时的转换
        hours += minutes / 60;
        minutes = minutes % 60;

        // 处理小时到天的转换
        days += hours / 24;
        hours = hours % 24;

        // 处理天到月的转换（假设一个月为30天）
        months += days / 30;
        days = days % 30;

        return new String[]{String.valueOf(months), String.valueOf(days), String.valueOf(hours),
                String.valueOf(minutes), String.valueOf(seconds)};
    }


    private LocalDateTime timeDelay(TimeNumModel[] timeNumberArray, boolean forward) {
        String tYear = StringUtils.defaultString(timeNumberArray[YEAR_IDX].getTime(), "0");
        String tMonth = StringUtils.defaultString(timeNumberArray[MONTH_IDX].getTime(), "0");
        String tDay = StringUtils.defaultString(timeNumberArray[DAY_IDX].getTime(), "0");
        String tHour = StringUtils.defaultString(timeNumberArray[HOUR_IDX].getTime(), "0");
        String tMinute = StringUtils.defaultString(timeNumberArray[MINUTE_IDX].getTime(), "0");
        String tSec = StringUtils.defaultString(timeNumberArray[SECS_IDX].getTime(), "0");
        String tWeek = StringUtils.defaultString(timeNumberArray[WEEK_IDX].getTime(), "0");

        LocalDateTime now = LocalDateTime.now();

        if (forward) {
            //向前减
            now = now.minusYears(Integer.parseInt(tYear))
                    .minusMonths(Integer.parseInt(tMonth))
                    .minusDays(Integer.parseInt(tDay))
                    .minusHours(Integer.parseInt(tHour))
                    .minusMinutes(Integer.parseInt(tMinute))
                    .minusSeconds(Integer.parseInt(tSec))
                    .minusWeeks(Integer.parseInt(tWeek));
        } else {
            now = now.plusYears(Integer.parseInt(tYear))
                    .plusMonths(Integer.parseInt(tMonth))
                    .plusDays(Integer.parseInt(tDay))
                    .plusHours(Integer.parseInt(tHour))
                    .plusMinutes(Integer.parseInt(tMinute))
                    .plusSeconds(Integer.parseInt(tSec))
                    .plusWeeks(Integer.parseInt(tWeek));
        }

        return now;
    }

    private String replaceSpecialWord(String origin) {
        return origin.replace("十", "10").replace("两", "2");
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

    public static void main(String[] args) {

        CusTimeModel cusTimeModel = new CusRangeTimeUtil().processTime("一个月20天", new CusTimeModel(), 3);
        System.out.println(cusTimeModel);


    }

}
