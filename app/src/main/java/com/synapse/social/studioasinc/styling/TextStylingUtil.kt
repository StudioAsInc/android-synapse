package com.synapse.social.studioasinc.styling

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.ProfileActivity
import java.util.regex.MatchResult
import java.util.regex.Pattern

class TextStylingUtil(private val context: Context) {

    private class ProcessedSpan

    private enum class StyleType {
        BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, BOLD_ITALIC_UNDERLINE,
        HIGHLIGHT, LINK, HEADING, QUOTE,
        SUPERSCRIPT, SUBSCRIPT, TEXT_COLOR, MENTION_HASHTAG
    }

    private data class StylingMatch(val result: MatchResult, val type: StyleType)

    // Ordered patterns - most specific first
    private val stylingRules: Map<Pattern, StyleType> = linkedMapOf(
        // Links must come first to handle nested formatting
        Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)") to StyleType.LINK,
        // Mentions and hashtags
        Pattern.compile("(?<![^\\s])([@#])([A-Za-z0-9_.-]+)") to StyleType.MENTION_HASHTAG,
        // Code blocks
        Pattern.compile("`(.+?)`") to StyleType.HIGHLIGHT,
        // Headings
        Pattern.compile("^(#{1,5})\\s+(.+?)(?=\\n|$)", Pattern.MULTILINE) to StyleType.HEADING,
        // Quotes
        Pattern.compile("^>\\s+(.+?)(?=\\n|$)", Pattern.MULTILINE) to StyleType.QUOTE,
        // Combined bold+italic+underline (///text///)
        Pattern.compile("///(.*?)///") to StyleType.BOLD_ITALIC_UNDERLINE,
        // Bold (**text**)
        Pattern.compile("\\*\\*(.*?)\\*\\*") to StyleType.BOLD,
        // Italic (__text__)
        Pattern.compile("__(.*?)__") to StyleType.ITALIC,
        // Strikethrough (~~text~~)
        Pattern.compile("~~(.*?)~~") to StyleType.STRIKETHROUGH,
        // Alternate italic (*text*)
        Pattern.compile("\\*(.*?)\\*") to StyleType.ITALIC,
        // Underline (_text_)
        Pattern.compile("_(.*?)_") to StyleType.UNDERLINE,
        // Superscript (^text^)
        Pattern.compile("\\^(.*?)\\^") to StyleType.SUPERSCRIPT,
        // Subscript (~text~)
        Pattern.compile("~(.*?)~") to StyleType.SUBSCRIPT,
        // Colored text ({#RRGGBB}(text))
        Pattern.compile("\\{#([0-9a-fA-F]{6})\\}\\((.*?)\\)") to StyleType.TEXT_COLOR
    )

    fun applyStyling(text: String, textView: TextView) {
        textView.movementMethod = LinkMovementMethod.getInstance()
        val ssb = SpannableStringBuilder(text)

        while (true) {
            var lastMatch: StylingMatch? = null

            for ((pattern, type) in stylingRules) {
                val matcher = pattern.matcher(ssb)
                while (matcher.find()) {
                    if (ssb.getSpans(matcher.start(), matcher.end(), ProcessedSpan::class.java).isNotEmpty()) {
                        continue
                    }
                    if (lastMatch == null || matcher.start() < lastMatch.result.start()) {
                        lastMatch = StylingMatch(matcher.toMatchResult(), type)
                    }
                }
            }

            val matchToProcess = lastMatch ?: break

            val result = matchToProcess.result
            val start = result.start()
            var end = result.end()
            var finalEnd = end

            when (matchToProcess.type) {
                StyleType.BOLD -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.ITALIC -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.UNDERLINE -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(UnderlineSpan(), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.BOLD_ITALIC_UNDERLINE -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    // Apply all three styles
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(UnderlineSpan(), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.STRIKETHROUGH -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(StrikethroughSpan(), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.HIGHLIGHT -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(CodeBlockSpan(Color.parseColor("#E0E0E0"), 6f), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.LINK -> {
                    val linkText = result.group(1)!!
                    val linkUrl = result.group(2)!!
                    ssb.replace(start, end, linkText)
                    finalEnd = start + linkText.length
                    
                    // Check if the link is inside bold text
                    val existingSpans = ssb.getSpans(start, finalEnd, StyleSpan::class.java)
                    val isBold = existingSpans.any { it.style == Typeface.BOLD }
                    
                    if (Patterns.WEB_URL.matcher(linkUrl).matches()) {
                        ssb.setSpan(LinkClickableSpan(linkUrl, context, isBold), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                StyleType.HEADING -> {
                    val headingText = result.group(2)!!
                    val headingLevel = result.group(1)!!.length
                    val size = when (headingLevel) {
                        1 -> 1.5f
                        2 -> 1.4f
                        3 -> 1.3f
                        4 -> 1.2f
                        5 -> 1.1f
                        else -> 1.0f
                    }
                    ssb.replace(start, end, headingText)
                    finalEnd = start + headingText.length
                    ssb.setSpan(RelativeSizeSpan(size), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.QUOTE -> {
                    val quoteText = result.group(1)!!
                    ssb.replace(start, end, quoteText)
                    finalEnd = start + quoteText.length
                    ssb.setSpan(LeadingMarginSpan.Standard(60, 10), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.SUPERSCRIPT -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(SuperscriptSpan(), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.75f), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.SUBSCRIPT -> {
                    val content = result.group(1)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    ssb.setSpan(SubscriptSpan(), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(RelativeSizeSpan(0.75f), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                StyleType.TEXT_COLOR -> {
                    val colorString = result.group(1)!!
                    val content = result.group(2)!!
                    ssb.replace(start, end, content)
                    finalEnd = start + content.length
                    try {
                        val color = Color.parseColor("#$colorString")
                        ssb.setSpan(ForegroundColorSpan(color), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    } catch (e: IllegalArgumentException) {
                        Log.w("TextStylingUtil", "Invalid hex color: #$colorString")
                    }
                }
                StyleType.MENTION_HASHTAG -> {
                    val fullMatch = result.group(0)!!
                    val symbol = result.group(1)!!
                    val span = if (symbol == "@") ProfileSpan(context, fullMatch) else HashtagSpan(fullMatch)
                    ssb.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            ssb.setSpan(ProcessedSpan(), start, finalEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        ssb.getSpans(0, ssb.length, ProcessedSpan::class.java).forEach { ssb.removeSpan(it) }
        textView.text = ssb
    }

    private class CodeBlockSpan(
        private val backgroundColor: Int,
        private val cornerRadius: Float,
        private val padding: Int = 6
    ) : LineBackgroundSpan {
        
        private val rect = RectF()
        private val paint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        override fun drawBackground(
            c: Canvas, p: Paint, left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int, lnum: Int
        ) {
            val textWidth = p.measureText(text, start, end)
            rect.set(
                left.toFloat() - padding,
                top.toFloat(),
                left.toFloat() + textWidth + padding,
                bottom.toFloat()
            )

            // Only round corners for single-line blocks
            val isSingleLine = lnum == 0 && (end - start == text.length)
            if (isSingleLine) {
                c.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            } else {
                c.drawRect(rect, paint)
            }
        }
    }

    private class LinkClickableSpan(
        private val url: String,
        private val context: Context,
        private val preserveBold: Boolean = false
    ) : ClickableSpan() {

        override fun onClick(view: View) {
            try {
                CustomTabsIntent.Builder()
                    .build()
                    .launchUrl(context, Uri.parse(url))
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    Log.e("LinkClickableSpan", "No activity found for URL: $url")
                }
            }
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.parseColor("#445E91")
            ds.isUnderlineText = true
            if (preserveBold) {
                ds.typeface = Typeface.create(ds.typeface, Typeface.BOLD)
            }
        }
    }

    private class ProfileSpan(private val context: Context, private val handle: String) : ClickableSpan() {
        override fun onClick(view: View) {
            val username = handle.substring(1)
            val db = FirebaseDatabase.getInstance().getReference("synapse/username").child(username)
            db.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val uid = snapshot.child("uid").getValue(String::class.java)
                    if (uid != null && uid != "null") {
                        context.startActivity(
                            Intent(context, ProfileActivity::class.java)
                                .putExtra("uid", uid)
                        )
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileSpan", "Database error", error.toException())
                }
            })
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.parseColor("#445E91")
            ds.isUnderlineText = false
            ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    private class HashtagSpan(private val hashtag: String) : ClickableSpan() {
        override fun onClick(view: View) {
            Log.d("HashtagSpan", "Clicked: $hashtag")
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = Color.parseColor("#445E91")
            ds.isUnderlineText = false
            ds.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }
}