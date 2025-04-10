package com.manorrock.assistant.cli;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer for Markdown text in the terminal with ANSI colors.
 */
public class MarkdownRenderer {

    private static final Parser PARSER;
    private static final HtmlRenderer HTML_RENDERER;

    static {
        // Initialize Flexmark parser with options
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.PARSE_INNER_HTML_COMMENTS, true);
        options.set(HtmlRenderer.SOFT_BREAK, "\n");
        
        PARSER = Parser.builder(options).build();
        HTML_RENDERER = HtmlRenderer.builder(options).build();
    }

    /**
     * Initialize Jansi for ANSI color support in the terminal.
     */
    public static void init() {
        AnsiConsole.systemInstall();
    }

    /**
     * Cleanup Jansi when the application is done.
     */
    public static void shutdown() {
        AnsiConsole.systemUninstall();
    }

    /**
     * Render Markdown text to the terminal with ANSI colors.
     *
     * @param markdown The markdown text to render
     * @return The rendered text with ANSI escape sequences
     */
    public static String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        // Parse the markdown to AST
        Document document = PARSER.parse(markdown);
        
        // Convert to a simplified representation
        String plainText = document.getChars().toString();

        // Process specific markdown elements
        return processMarkdown(plainText);
    }

    /**
     * Process markdown elements and apply ANSI colors.
     *
     * @param markdown The markdown text
     * @return Formatted text with ANSI colors
     */
    private static String processMarkdown(String markdown) {
        // Create a new Ansi builder
        Ansi ansi = Ansi.ansi();
        
        // Replace the content with processed version
        String result = markdown;
        
        // Process headers (# Header, ## Header, etc.)
        result = processHeaders(result);
        
        // Process bold and italic
        result = processBoldAndItalic(result);
        
        // Process code blocks and inline code
        result = processCodeBlocks(result);
        
        // Process lists
        result = processLists(result);
        
        // Process links
        result = processLinks(result);
        
        return result;
    }

    /**
     * Process Markdown headers and apply ANSI colors.
     *
     * @param text The input text
     * @return Text with headers highlighted
     */
    private static String processHeaders(String text) {
        // Match headers (# Header, ## Header, etc.)
        Pattern pattern = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String headerMarker = matcher.group(1);
            String headerContent = matcher.group(2);
            
            // Format based on header level
            String replacement = Ansi.ansi()
                .bold()
                .fg(Ansi.Color.CYAN)
                .a(headerContent)
                .reset()
                .toString();
                
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    /**
     * Process bold and italic text and apply ANSI styles.
     *
     * @param text The input text
     * @return Text with bold and italic styled
     */
    private static String processBoldAndItalic(String text) {
        // Process bold (surrounded by ** or __)
        String result = text;
        
        // Bold: **text** or __text__
        Pattern boldPattern = Pattern.compile("(\\*\\*|__)(.+?)(\\*\\*|__)", Pattern.DOTALL);
        Matcher boldMatcher = boldPattern.matcher(result);
        
        StringBuffer boldSb = new StringBuffer();
        while (boldMatcher.find()) {
            String content = boldMatcher.group(2);
            String replacement = Ansi.ansi()
                .bold()
                .a(content)
                .reset()
                .toString();
            
            boldMatcher.appendReplacement(boldSb, Matcher.quoteReplacement(replacement));
        }
        boldMatcher.appendTail(boldSb);
        result = boldSb.toString();
        
        // Italic: *text* or _text_
        Pattern italicPattern = Pattern.compile("(\\*|_)(.+?)(\\*|_)", Pattern.DOTALL);
        Matcher italicMatcher = italicPattern.matcher(result);
        
        StringBuffer italicSb = new StringBuffer();
        while (italicMatcher.find()) {
            String content = italicMatcher.group(2);
            String replacement = Ansi.ansi()
                .a(Ansi.Attribute.ITALIC)
                .a(content)
                .reset()
                .toString();
            
            italicMatcher.appendReplacement(italicSb, Matcher.quoteReplacement(replacement));
        }
        italicMatcher.appendTail(italicSb);
        
        return italicSb.toString();
    }

    /**
     * Process code blocks and inline code.
     *
     * @param text The input text
     * @return Text with code highlighted
     */
    private static String processCodeBlocks(String text) {
        String result = text;
        
        // Code blocks (triple backticks)
        Pattern codeBlockPattern = Pattern.compile("```(?:[a-zA-Z0-9]+)?\\s*([\\s\\S]*?)```", Pattern.DOTALL);
        Matcher codeBlockMatcher = codeBlockPattern.matcher(result);
        
        StringBuffer codeBlockSb = new StringBuffer();
        while (codeBlockMatcher.find()) {
            String codeContent = codeBlockMatcher.group(1);
            String replacement = Ansi.ansi()
                .fg(Ansi.Color.GREEN)
                .a("\n" + codeContent + "\n")
                .reset()
                .toString();
            
            codeBlockMatcher.appendReplacement(codeBlockSb, Matcher.quoteReplacement(replacement));
        }
        codeBlockMatcher.appendTail(codeBlockSb);
        result = codeBlockSb.toString();
        
        // Inline code (single backticks)
        Pattern inlineCodePattern = Pattern.compile("`([^`]+)`");
        Matcher inlineCodeMatcher = inlineCodePattern.matcher(result);
        
        StringBuffer inlineCodeSb = new StringBuffer();
        while (inlineCodeMatcher.find()) {
            String codeContent = inlineCodeMatcher.group(1);
            String replacement = Ansi.ansi()
                .fg(Ansi.Color.GREEN)
                .a(codeContent)
                .reset()
                .toString();
            
            inlineCodeMatcher.appendReplacement(inlineCodeSb, Matcher.quoteReplacement(replacement));
        }
        inlineCodeMatcher.appendTail(inlineCodeSb);
        
        return inlineCodeSb.toString();
    }

    /**
     * Process Markdown lists.
     *
     * @param text The input text
     * @return Text with lists highlighted
     */
    private static String processLists(String text) {
        String result = text;
        
        // Unordered lists
        Pattern ulPattern = Pattern.compile("^(\\s*)([-*+])\\s+(.+)$", Pattern.MULTILINE);
        Matcher ulMatcher = ulPattern.matcher(result);
        
        StringBuffer ulSb = new StringBuffer();
        while (ulMatcher.find()) {
            String indent = ulMatcher.group(1);
            String marker = ulMatcher.group(2);
            String content = ulMatcher.group(3);
            
            String replacement = indent + Ansi.ansi()
                .fg(Ansi.Color.YELLOW)
                .a(marker)
                .reset()
                .a(" " + content)
                .toString();
            
            ulMatcher.appendReplacement(ulSb, Matcher.quoteReplacement(replacement));
        }
        ulMatcher.appendTail(ulSb);
        result = ulSb.toString();
        
        // Ordered lists
        Pattern olPattern = Pattern.compile("^(\\s*)(\\d+)\\.(\\s+)(.+)$", Pattern.MULTILINE);
        Matcher olMatcher = olPattern.matcher(result);
        
        StringBuffer olSb = new StringBuffer();
        while (olMatcher.find()) {
            String indent = olMatcher.group(1);
            String number = olMatcher.group(2);
            String spacing = olMatcher.group(3);
            String content = olMatcher.group(4);
            
            String replacement = indent + Ansi.ansi()
                .fg(Ansi.Color.YELLOW)
                .a(number + ".")
                .reset()
                .a(spacing + content)
                .toString();
            
            olMatcher.appendReplacement(olSb, Matcher.quoteReplacement(replacement));
        }
        olMatcher.appendTail(olSb);
        
        return olSb.toString();
    }

    /**
     * Process Markdown links.
     *
     * @param text The input text
     * @return Text with links highlighted
     */
    private static String processLinks(String text) {
        // Match [link text](url)
        Pattern pattern = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(text);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String linkText = matcher.group(1);
            String url = matcher.group(2);
            
            String replacement = Ansi.ansi()
                .fg(Ansi.Color.BLUE)
                .a(linkText)
                .reset()
                .a(" (" + url + ")")
                .toString();
            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}
