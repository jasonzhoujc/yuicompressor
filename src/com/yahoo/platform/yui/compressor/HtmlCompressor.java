package com.yahoo.platform.yui.compressor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
/**
 * 对html内嵌的Javascript/Css进行压缩
 * @author jason.zhou 
 * @createdate2012.3.5
 * @updatedate2012.12.18 更新了获取css js的正则
 */
public class HtmlCompressor {

	public HtmlCompressor(Reader in, ErrorReporter reporter)
			throws IOException, EvaluatorException {
		int c;
		while ((c = in.read()) != -1) {
			srcsb.append((char) c);
		}
		this.reporter = reporter;
	}

	/**
	 * 转换流为字符串
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public StringBuffer convertStreamToString(Reader in) throws IOException {
		StringBuffer sb = new StringBuffer();
		String line;
		BufferedReader reader = new BufferedReader(in);
		while ((line = reader.readLine()) != null) {
			sb.append(line).append("\n");
		}
		return sb;
	}

	/**
	 * 压缩html中内嵌的Javascript、Css
	 * @param out
	 * @param linebreak
	 * @param munge
	 * @param verbose
	 * @param preserveAllSemiColons
	 * @param disableOptimizations
	 * @throws IOException
	 */
	public void compress(Writer out, int linebreak, boolean munge,
			boolean verbose, boolean preserveAllSemiColons,
			boolean disableOptimizations) throws IOException {
		String html = this.srcsb.toString();
		html = this.jsCompress(html, linebreak, munge, verbose, preserveAllSemiColons, disableOptimizations);
		html = this.cssCompress(html, linebreak);
		out.write(html.toString());
	}
	
	/**
	 * 返回压缩内嵌css压缩后的html字符串
	 * @param html 要压缩的html
	 * @param linebreak
	 * @return
	 * @throws IOException
	 */
	public String cssCompress(String html, int linebreak) throws IOException {
		StringBuffer htm = new StringBuffer();
		Pattern cssp = Pattern.compile(STYLE_REG, Pattern.DOTALL|Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher cssm = cssp.matcher(html);
		while (cssm.find()) {
			String css = cssm.group(2);
			StringReader cssReader = new StringReader(css);
			CssCompressor cssCompressor = new CssCompressor(cssReader);
			String replaceSb = cssCompressor.getCompressedCss(linebreak);
			replaceSb = replaceSb.replaceAll(ESCAPE_CHARACTER, ESCAPE_CHARACTER_REPLACEMENT);
			replaceSb = replaceSb.replaceAll($_REG, $_REPLACEMENT);
			cssm.appendReplacement(htm, cssm.group(1) + replaceSb + cssm.group(3));
		}
		cssm.appendTail(htm);
		return htm.toString();
	}
	
	/**
	 * 返回压缩内嵌javascript压缩后的html字符串
	 * @param html 要压缩的html
	 * @param linebreak
	 * @param munge
	 * @param verbose
	 * @param preserveAllSemiColons
	 * @param disableOptimizations
	 * @return
	 * @throws EvaluatorException
	 * @throws IOException
	 */
	public String jsCompress(String html, int linebreak, boolean munge,
			boolean verbose, boolean preserveAllSemiColons,
			boolean disableOptimizations) throws EvaluatorException, IOException {
		StringBuffer htm = new StringBuffer();
		Pattern jsp = Pattern.compile(SCRIPT_REG, Pattern.DOTALL|Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher jsm = jsp.matcher(html);
		Pattern p = Pattern.compile(CLEAR_BLANK_REG, Pattern.DOTALL|Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		while (jsm.find()) {
			String script = jsm.group(2);
			String replaceSb = "";
			script = p.matcher(script).replaceAll("");
			if (!script.isEmpty()) {
				StringReader scriptReader = new StringReader(script);
				JavaScriptCompressor jsCompressor = new JavaScriptCompressor(scriptReader, this.reporter);
				replaceSb = jsCompressor.getCompressedJavasCript(linebreak, munge,
						verbose, preserveAllSemiColons, disableOptimizations);
				replaceSb = replaceSb.replaceAll(ESCAPE_CHARACTER, ESCAPE_CHARACTER_REPLACEMENT);
				replaceSb = replaceSb.replaceAll($_REG, $_REPLACEMENT);
			}
			jsm.appendReplacement(htm, jsm.group(1) + replaceSb + jsm.group(3));
		}
		jsm.appendTail(htm);
		return htm.toString();
	}
	// 源文件字符串
	private StringBuffer srcsb = new StringBuffer();
	// 处理JavaScript异常类
	private ErrorReporter reporter = null;
	// 首尾空白替换
	private static final String CLEAR_BLANK_REG = "^\\s+";
	// 获取样式正则表达式
	private static final String STYLE_REG = "(^\\s*<style[^<>]*?>)(.*?)(</style>\\s*$)";
	// 获取JavaScript样式正则表达式
	private static final String SCRIPT_REG = "(^\\s*<script[^<>]*?>)(.*?)(</script>\\s*$)";
	// 更换$输出转义$,因为$会被当成正则全局对象
	private static final String $_REPLACEMENT = "\\\\\\$";
	// 获取$符的正则表达式
	private static final String $_REG = "\\$";
	// 获取转义字符正则表达式
	private static final String ESCAPE_CHARACTER = "\\\\";
	// 更换转义字符，输出转义字符
	private static final String ESCAPE_CHARACTER_REPLACEMENT = "\\\\\\\\";
	
}
