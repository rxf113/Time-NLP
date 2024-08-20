package com.time.nlp.unit;

import com.time.nlp.CusTimeModel;
import com.time.nlp.TimeNormalizer;
import com.time.nlp.TimeUnit;
import com.time.util.DateUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.time.util.DateUtil.*;

public enum DayProcess {
    INSTANCE;

    public static DayProcess getInstance() {
        return INSTANCE;
    }

    private final Pattern NUM_DAYS_PATTERN = Pattern.compile(".*([前上后])(\\d+)[天日].*");

    public CusTimeModel process(String time, TimeUnit[] units) {

        CusTimeModel cusTimeModel = new CusTimeModel();

        Matcher matcher = NUM_DAYS_PATTERN.matcher(time);
        if (matcher.find()) {
            String dir = matcher.group(1);
            String interval = matcher.group(2);

            LocalDateTime now = LocalDateTime.now();
            if (dir.equals("前") || dir.equals("上")) {
                cusTimeModel.setTimeEnd(now.plusDays(1).format(ISO_8601_FORMATTER_START));
                LocalDateTime preDay = now.minusDays(Integer.parseInt(interval));
                cusTimeModel.setTime(preDay.format(ISO_8601_FORMATTER_START));
            } else {
                cusTimeModel.setTime(now.format(ISO_8601_FORMATTER_START));
                LocalDateTime preDay = now.plusDays(Integer.parseInt(interval));
                cusTimeModel.setTimeEnd(preDay.plusDays(1).format(ISO_8601_FORMATTER_START));
            }
            return cusTimeModel;
        }

        //其它时间，单天，添加首位
        Date date = units[0].getTime();
        cusTimeModel.setTime(DateUtil.formatDateStartDefault(date));
        cusTimeModel.setTimeEnd(DateUtil.formatDateEndDefault(date));
        //将 Time_Expression 做为originTime
        cusTimeModel.setOriginTime(units[0].Time_Expression);
        return cusTimeModel;
    }
}
