package com.github.kasper_gram.somefetcher.feed

import com.github.kasper_gram.somefetcher.data.FeedSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object OPMLManager {

    /**
     * Generates a valid OPML 2.0 XML string from [sources].
     */
    fun exportOpml(sources: List<FeedSource>): String {
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<opml version=\"2.0\">")
        sb.appendLine("  <head>")
        sb.appendLine("    <title>SoMeFetcher Subscriptions</title>")
        sb.appendLine("  </head>")
        sb.appendLine("  <body>")
        for (source in sources) {
            val escapedTitle = escapeXml(source.name)
            val escapedUrl = escapeXml(source.url)
            sb.appendLine(
                "    <outline type=\"rss\" text=\"$escapedTitle\" title=\"$escapedTitle\" xmlUrl=\"$escapedUrl\"/>"
            )
        }
        sb.appendLine("  </body>")
        sb.append("</opml>")
        return sb.toString()
    }

    /**
     * Parses an OPML [inputStream] and returns a list of (title, xmlUrl) pairs
     * for every `<outline>` element that has a non-blank `xmlUrl` attribute.
     */
    fun parseOpml(inputStream: InputStream): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, null)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG &&
                parser.name.equals("outline", ignoreCase = true)
            ) {
                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                if (!xmlUrl.isNullOrBlank()) {
                    val title = parser.getAttributeValue(null, "title")
                        ?: parser.getAttributeValue(null, "text")
                        ?: xmlUrl
                    results.add(title to xmlUrl)
                }
            }
            eventType = parser.next()
        }
        return results
    }

    private fun escapeXml(text: String): String = buildString(text.length + 16) {
        for (c in text) {
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(c)
            }
        }
    }
}
