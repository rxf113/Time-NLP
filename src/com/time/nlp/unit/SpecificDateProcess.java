package com.time.nlp.unit;

import com.time.nlp.CusTimeModel;
import com.time.nlp.TimeUnit;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static com.time.util.DateUtil.*;

/**
 * 具体时间处理
 */
public enum SpecificDateProcess {

    INSTANCE;

    public static SpecificDateProcess getInstance() {
        return INSTANCE;
    }

    public CusTimeModel process(String time, TimeUnit[] units) {
        CusTimeModel timeModel = new CusTimeModel();
        if (units.length != 1) {
            return timeModel;
        }
        timeModel.setOriginTime(units[0].Time_Expression);
        Date date = units[0].getTime();
        Instant instant = date.toInstant();
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

        String unit = units[0].Time_Norm.substring(units[0].Time_Norm.length() - 1);

        //前后内
        if (time.matches(".*[前后内]$")) {


            if (time.endsWith("前")) {
                timeModel.setTimeEnd(localDateTime.plusDays(1).format(ISO_8601_FORMATTER_START));
                timeModel.setTime(localDateTime.minusYears(10).format(ISO_8601_FORMATTER_START));
            } else if (time.endsWith("后")) {
                timeModel.setTime(localDateTime.format(ISO_8601_FORMATTER_START));
                timeModel.setTimeEnd(localDateTime.plusYears(10).format(ISO_8601_FORMATTER_START));
            } else {
                //内
                //判断单位
                if (unit.equals("日")) {
                    timeModel.setTimeEnd(localDateTime.plusDays(1).format(ISO_8601_FORMATTER_START));
                    timeModel.setTime(localDateTime.format(ISO_8601_FORMATTER_START));
                } else if (unit.equals("月")) {
                    timeModel.setTimeEnd(localDateTime.plusMonths(1).format(ISO_8601_FORMATTER_START));
                    timeModel.setTime(localDateTime.format(ISO_8601_FORMATTER_START));
                } else if (unit.equals("年")) {
                    timeModel.setTimeEnd(localDateTime.plusYears(1).format(ISO_8601_FORMATTER_START));
                    timeModel.setTime(localDateTime.format(ISO_8601_FORMATTER_START));
                }
            }
            return timeModel;
        }

        //上午 中午 下午 晚上
        String replaceTime = time.replace("晚上", "晚");
        //上午：6:00 至 12:00
        //中午：12:00 至 13:00
        //下午：13:00 至 18:00
        //晚上：18:00 至 24:00
        //凌晨：00:00 至 06:00
        if (replaceTime.matches(".*(上午|中午|下午|晚上|凌晨)$")) {
            String start, end;
            if (replaceTime.endsWith("上午")) {
                start = "06:00:00";
                end = "12:00:00";
            } else if (replaceTime.endsWith("中午")) {
                start = "12:00:00";
                end = "13:00:00";
            } else if (replaceTime.endsWith("下午")) {
                start = "13:00:00";
                end = "18:00:00";
            } else if (replaceTime.endsWith("晚上")) {
                start = "18:00:00";
                end = "24:00:00";
            } else {
                start = "00:00:00";
                end = "06:00:00";
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            timeModel.setTime(localDateTime.toLocalDate().atTime(LocalTime.parse(start)).format(formatter));
            timeModel.setTimeEnd(localDateTime.toLocalDate().atTime(LocalTime.parse(end)).format(formatter));
            return timeModel;
        }

        //春天：3月1日至5月31日
        //夏天：6月1日至8月31日
        //秋天：9月1日至11月30日
        //冬天：12月1日至次年2月28日或29日
        if (time.matches(".*年[春夏秋冬][天季]?$")) {
            String start = "";
            String end = "";
            int endYear = localDateTime.getYear();
            if (time.endsWith("春天") || time.endsWith("春季")) {
                start = "03-01";
                end = "05-31";
            } else if (time.endsWith("夏天") || time.endsWith("夏季")) {
                start = "06-01";
                end = "08-31";
            } else if (time.endsWith("秋天") || time.endsWith("秋季")) {
                start = "09-01";
                end = "11-30";
            } else if (time.endsWith("冬天") || time.endsWith("冬季")) {
                start = "12-01";
                endYear += 1; // 结束年份是下一年
                // 判断是否是闰年，设置2月28日或2月29日
                if (endYear % 4 == 0 && (endYear % 100 != 0 || endYear % 400 == 0)) {
                    end = "02-29";
                } else {
                    end = "02-28";
                }
            }

            if (!start.isEmpty() && !end.isEmpty()) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate startDate = LocalDate.parse(localDateTime.getYear() + "-" + start, dateFormatter);
                LocalDate endDate = LocalDate.parse(endYear + "-" + end, dateFormatter);

                timeModel.setTime(startDate.atStartOfDay().format(ISO_8601_FORMATTER_START));
                timeModel.setTimeEnd(endDate.plusDays(1).format(ISO_8601_FORMATTER_START));
            }
            return timeModel;
        }

        return timeModel;
    }

}
