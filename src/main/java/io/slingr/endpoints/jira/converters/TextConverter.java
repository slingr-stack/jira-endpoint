package io.slingr.endpoints.jira.converters;

import net.htmlparser.jericho.Source;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.eclipse.mylyn.wikitext.core.util.ServiceLocator;

import java.io.StringWriter;

/**
 * Created by dgaviola on 4/6/15.
 */
public class TextConverter {
    public static String convertWikiToHtml(String wikiText) {
        try {
            StringWriter writer = new StringWriter();
            HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
            MarkupParser parser = new MarkupParser(ServiceLocator.getInstance().getMarkupLanguage("Confluence"), builder);
            parser.parse(wikiText);
            String html = writer.toString();
            int start = html.indexOf("<body>");
            int end = html.indexOf("</body>");
            if (start >= 0 && end >= 0) {
                return html.substring(start + "<body>".length(), end);
            } else {
                return html;
            }
        } catch (Exception e) {
            return wikiText;
        }
    }

    public static String convertWikiToText(String wikiText) {
        String html = convertWikiToHtml(wikiText);
        String text = convertHtmlToText(html);
        if (text != null) {
            return text.trim();
        }
        return null;
    }

    public static String convertTextToHtml(String text) {
        try {
            String escapedText = StringEscapeUtils.escapeHtml(text);
            return escapedText.replaceAll("\\n", "<br>");
        } catch (Exception e) {
            return text;
        }
    }

    public static String convertTextToWiki(String text) {
        return text;
    }

    public static String convertHtmlToText(String html) {
        try {
            Source htmlBody = new Source(StringEscapeUtils.unescapeHtml(html));
            String textBody = htmlBody.getRenderer().toString();
            return textBody;
        } catch (Exception e) {
            return html;
        }
    }

    public static String convertHtmlToWiki(String html) {
        // probably not the best thing, but better than sending the HTML code
        return convertHtmlToText(html);
    }
}
