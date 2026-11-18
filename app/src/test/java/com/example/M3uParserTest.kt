package com.example

import com.example.data.model.ChannelCategory
import com.example.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {

    @Test
    fun parse_extractsChannelsLogosCategoriesAndDeduplicatesUrls() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/news.png" group-title="News",شبکه خبر
            https://example.com/live/news.m3u8
            #EXTINF:-1 group-title="Sport",ورزش زنده
            http://example.com/live/sport.m3u8
            #EXTINF:-1 group-title="Sport",Duplicate Sport
            http://example.com/live/sport.m3u8
            #EXTINF:-1,NoLogoChannel
            https://example.com/live/general.m3u8
            #EXTINF:-1,InvalidUrl
            ftp://example.com/invalid.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(sampleM3u)

        assertEquals(3, channels.size)
        assertEquals("شبکه خبر", channels[0].name)
        assertEquals("https://example.com/news.png", channels[0].logoUrl)
        assertEquals(ChannelCategory.NEWS.labelFa, channels[0].category)

        assertEquals("ورزش زنده", channels[1].name)
        assertNull(channels[1].logoUrl)
        assertEquals(ChannelCategory.SPORTS.labelFa, channels[1].category)

        assertEquals("NoLogoChannel", channels[2].name)
        assertEquals(ChannelCategory.GENERAL.labelFa, channels[2].category)
    }

    @Test
    fun parse_handlesOver12000ChannelsViaStreamingReader() {
        val builder = StringBuilder("#EXTM3U\n")
        for (i in 1..12_500) {
            builder.append("#EXTINF:-1 group-title=\"News\",Channel $i\n")
            builder.append("https://example.com/stream_$i.m3u8\n")
        }

        val parsed = M3uParser.parse(builder.toString().reader().buffered(), maxChannels = 12_000)
        assertEquals(12_000, parsed.size)
        assertEquals("Channel 1", parsed.first().name)
        assertEquals("Channel 12000", parsed.last().name)
    }
}
