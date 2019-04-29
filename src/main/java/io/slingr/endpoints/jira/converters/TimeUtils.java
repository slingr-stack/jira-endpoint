package io.slingr.endpoints.jira.converters;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dgaviola on 4/6/15.
 */
public class TimeUtils {
    private static SimpleDateFormat jiraSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static SimpleDateFormat versionSdf = new SimpleDateFormat("dd/MMM/yy");
    private static SimpleDateFormat standardSdf = new SimpleDateFormat("yyyy-MM-dd");

    public static Date parseJiraDate(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return jiraSdf.parse(text);
        } catch (ParseException e) {
            return null;
        }
    }


    public static Long parseSeconds(Integer seconds) {
        if (seconds == null) {
            return 0l;
        }
        return new Long(seconds) * 1000;
    }

    public static String parseVersionDate(String versionDate) throws ParseException {
        if (StringUtils.isBlank(versionDate)) {
            return null;
        }
        Date date = versionSdf.parse(versionDate);
        return standardSdf.format(date);
    }

    public static String formatJiraDate(Long millis) {
        if (millis == null) {
            return null;
        }
        Date date = new Date(millis);
        return jiraSdf.format(date);
    }
}

