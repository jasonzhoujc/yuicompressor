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
 */
public class HtmlCompressor {
	private StringBuffer srcsb = new StringBuffer();
	private ErrorReporter reporter = null;
	// 临时替换$,因为$会被当成正则全局对象
	private String $_TEMP_MARK = "#-#";
	// $匹配
	private String $_REG = "\\$";

	public HtmlCompressor(Reader in, ErrorReporter reporter)
			throws IOException, EvaluatorException {
		int c;
		while ((c = in.read()) != -1) {
			srcsb.append((char) c);
		}
//		this.srcsb = convertStreamToString(in);
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
		Pattern cssp = Pattern.compile("(^<style[^<>]*?>)(.+?)(^</style>)", Pattern.DOTALL|Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher cssm = cssp.matcher(html);
		while (cssm.find()) {
			String css = cssm.group(2);
			StringReader cssReader = new StringReader(css);
			CssCompressor cssCompressor = new CssCompressor(cssReader);
			String replaceSb = cssCompressor.getCompressedCss(linebreak);
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
		Pattern jsp = Pattern.compile("(^<script[^<>]*?>)(.+?)(^</script>)", Pattern.DOTALL|Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
		Matcher jsm = jsp.matcher(html);
		while (jsm.find()) {
			String script = jsm.group(2);
			StringReader scriptReader = new StringReader(script);
			JavaScriptCompressor jsCompressor = new JavaScriptCompressor(
					scriptReader, this.reporter);
			String replaceSb = jsCompressor.getCompressedJavasCript(linebreak, munge,
					verbose, preserveAllSemiColons, disableOptimizations);
			replaceSb = replaceSb.replaceAll(this.$_REG, this.$_TEMP_MARK).replaceAll("\\\\", "\\\\\\\\");
			jsm.appendReplacement(htm, jsm.group(1) + replaceSb + jsm.group(3));
		}
		jsm.appendTail(htm);
		return htm.toString().replace(this.$_TEMP_MARK, "$");
	}
}
