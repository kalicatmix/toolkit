package toolkit.application;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import toolkit.stream.Input;
import toolkit.stream.Output;
import toolkit.stream.PipeLine;
import toolkit.stream.action.IAction;
import toolkit.stream.impl.DefaultAction;
import toolkit.stream.impl.DefaultOutput;

/**
 *         don't check null api接口 歷史上的今天
 *         https://api.66mz8.com/api/today.php?format=json 天气 -101040100
 *         -101270101
 *         https://query.asilu.com/weather/weather?action=weather/weather/&id=
 *         微博 https://v1.alapi.cn/api/new/wbtop?num= 名人名言
 *         https://v1.alapi.cn/api/mingyan?typeid= 中国银行汇率 - html静态页面
 *         https://www.boc.cn/sourcedb/whpj 指数
 *         http://58.push2.eastmoney.com/api/qt/clist/get?cb=jQuery11240978514630878339_1608883881916&pn=1&pz=21&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fs=i:1.000001,i:0.399001,i:0.399005,i:0.399006,i:1.000300,i:100.HSI,i:100.HSCEI,i:124.HSCCI,i:100.TWII,i:100.N225,i:100.KOSPI200,i:100.KS11,i:100.STI,i:100.SENSEX,i:100.KLSE,i:100.SET,i:100.PSI,i:100.KSE100,i:100.VNINDEX,i:100.JKSE,i:100.CSEALL&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f26,f22,f33,f11,f62,f128,f136,f115,f152,f124,f107&_=1608883881917
 */
public class CommandTool {
	private static class Process {
		public static final int TYPE_JSON = 0xff & 0;
		public static final int TYPE_HTML = 0xff & 1;
		public static final int TYPE_XPATH = 0xff & 2;
		public static final int TYPE_SELECTOR = 0xff & 3;
		private int type;
		private String expression; // key:type:key[output use & join] -> next ...

		public Process() {
		}

		public Process(int type, String expression) {
			this.type = type;
			this.expression = expression;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public String getExpression() {
			return expression;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}
	}

	private static class Expression {
		public static final String JSON_TYPE_OBJECT = "object";
		public static final String JSON_TYPE_ARRAY = "array";
		public static final String HTML_TYPE_LIST = "list";
		public static final String HTML_TYPE_ITEM = "item";
		public static final String TYPE_NULL = "?";
		public static final String TYPE_ALL = "#";
		private String key;
		private String type;
		private List<String> resultKey;

		public Expression() {
		}

		public Expression(String key, String type, List<String> resultKey) {
			this.key = key;
			this.type = type;
			this.resultKey = resultKey;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public List<String> getResultKey() {
			return resultKey;
		}

		public void setResultKey(List<String> resultKey) {
			this.resultKey = resultKey;
		}

	}

	private static interface Supply {
		public String supply(String args, String url);
	}

	private static final Map<String, String> APIS = new HashMap<String, String>() {
		{
			put("today", "https://api.66mz8.com/api/today.php?format=json");
			put("weather", "https://query.asilu.com/weather/weather?action=weather/weather/&id=101270101");
			put("weibo", "https://v1.alapi.cn/api/new/wbtop?num=20");
			put("say", "https://v1.alapi.cn/api/mingyan?typeid=33");
			put("rate", "https://www.boc.cn/sourcedb/whpj");
			put("point","http://58.push2.eastmoney.com/api/qt/clist/get?cb=jQuery11240978514630878339_1608883881916&pn=1&pz=21&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fs=i:1.000001,i:0.399001,i:0.399005,i:0.399006,i:1.000300,i:100.HSI,i:100.HSCEI,i:124.HSCCI,i:100.TWII,i:100.N225,i:100.KOSPI200,i:100.KS11,i:100.STI,i:100.SENSEX,i:100.KLSE,i:100.SET,i:100.PSI,i:100.KSE100,i:100.VNINDEX,i:100.JKSE,i:100.CSEALL&fields=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f12,f13,f14,f15,f16,f17,f18,f20,f21,f23,f24,f25,f26,f22,f33,f11,f62,f128,f136,f115,f152,f124,f107&_=1608883881917");
			put("lang", "http://xyz.tingroom.com/wap/");
			put("price", "http://www.xinfadi.com.cn/marketanalysis/$1/list/$2.shtml");
			put("cve", "http://www.vulhub.org.cn/vulns/$1");
			put("lastCompany", "https://aiqicha.baidu.com/index/latestClaimAjax");
			put("searchCompany", "https://aiqicha.baidu.com/s/l?q=$1&t=&p=$2&s=10&o=0&f=%7B%7D");
			put("mba", "https://wiki.mbalib.com/wiki/$1");
			put("keyword", "http://www.a-hospital.com/api.php?action=opensearch&search=$1");
			put("bot", "http://api.qingyunke.com/api.php?key=free&appid=0&msg=$1");
			put("gitee", "https://gitee.com/explore");
			put("github", "https://github.com/search?q=$1&type=");
			put("testHome", "https://testerhome.com/opensource_projects?page=$1");
		}
	};
	private static final Map<String, String> DESC = new HashMap<String, String>() {
		{
			put("today", "历史上的今天，《。。》");
			put("weather", "近期天气情况");
			put("weibo", "微博热搜榜");
			put("say", "名人名言");
			put("rate", "汇率");
			put("point", "指数");
			put("lang", "小语种");
			put("price", "价格   -type [类型：1-蔬菜 2-水果3-肉禽蛋4-水产5-粮油] -page [0...n]");
			put("cve", "cve查看[cve n(page)]");
			put("lastCompany", "最新企业信息");
			put("searchCompany", "查询企业信息[参数：-keyword 关键字   -page 页码  ]");
			put("mba", "mba智库  [mba 关键字]");
			put("keyword", "医药关键字[keyword 关键字]");
			put("bot", "对话机器人");
			put("gitee", "gitee项目推荐");
			put("github", "[github 关键字]");
			put("testHome", "[testHome 页码]");
		}
	};
	private static final Map<String, Process> PROCESS_EXPRESSION = new HashMap<String, Process>() {
		{
			put("today", new Process(Process.TYPE_JSON, "?:object:?->data:array:today&content"));
			put("weather", new Process(Process.TYPE_JSON, "?:object:date&city->list:array:date&weather&temp&wind"));
			put("weibo", new Process(Process.TYPE_JSON, "?:object:?->data:array:hot_word&hot_word_num"));
			put("say", new Process(Process.TYPE_JSON, "?:object:?->data:object:content&author"));
			put("rate", new Process(Process.TYPE_SELECTOR,
					"body > div > div.BOC_main > div.publish > div:nth-child(3) > table > tbody > tr:nth-child(1)::list::th<=>body > div > div.BOC_main > div.publish > div:nth-child(3) > table > tbody >tr::list::td"));
			put("point", new Process(Process.TYPE_JSON, "?:object:?->data:object:?->diff:array:f14&f15&f16"));
			put("lang", new Process(Process.TYPE_SELECTOR, "#menu > ul > li::list::span > a^href"));
			put("price", new Process(Process.TYPE_SELECTOR,
					"body > div.index_all.margin_auto > div.hangqing > div.hangq_left > table > tbody >tr::list::td"));
			put("cve", new Process(Process.TYPE_SELECTOR,
					"body > div > div.container.bugs.padb.bugs_list > div.col-md-12.mrg30B > div > table > tbody>tr::list::td"));
			put("lastCompany",
					new Process(Process.TYPE_JSON, "?:object:?->data:array:entName&legalPerson&startDate&regCapital"));
			put("searchCompany", new Process(Process.TYPE_JSON,
					"?:object:?->data:object:?->resultList:array:titleName&titleLegal&entType&validityFrom&regCap&scope"));
			put("mba", new Process(Process.TYPE_SELECTOR, "head::list::meta:nth-child(4)^content<=>#toc::list::ul"));
			put("keyword", new Process(Process.TYPE_JSON, "?:array:#"));
			put("bot", new Process(Process.TYPE_JSON, "?:object:content"));
			put("gitee", new Process(Process.TYPE_SELECTOR,
					"#explores-index > div.site-content > div.explore__container > div.explore-recommend > div > div > div.four.wide.column > div > div.ui.tab.active > div::list::div<=>#explores-index > div.site-content > div.explore__container > div.explore-recommend > div > div > div.four.wide.column > div > div.ui.tab.active::list::div"));
			put("github", new Process(Process.TYPE_SELECTOR,
					"#js-pjax-container > div > div.col-12.col-md-9.float-left.px-2.pt-3.pt-md-0.codesearch-results > div > ul::list::li"));
			put("testHome", new Process(Process.TYPE_SELECTOR, "#opensource_project-list > div.panel-body::list::div"));
		}
	};
	private static final List<String> EXLUDE_SAVE_LIST = new ArrayList<String>() {
		{
			add("say");
			add("weibo");
			add("price");
			add("cve");
			add("lastCompany");
			add("searchCompany");
			add("mba");
			add("keyword");
			add("bot");
			add("gitee");
			add("github");
			add("testHome");
		}
	};
	private static final List<String> HISTORY = new ArrayList<String>();
	private static final Map<String, List<IAction>> BEFOREHOOKS = new HashMap<String, List<IAction>>() {
		{
			put("point", Arrays.asList(new DefaultAction[] { new DefaultAction(new DefaultAction.Consumer() {
				@Override
				public Output accept(Input input) {
					String data = input.get().toString();
					int jsonStart = data.indexOf("(");
					return new DefaultOutput(data.substring(jsonStart + 1, data.length() - 2));
				}
			}) }));
		}
	};
	private static final Map<String, List<IAction>> AFTERHOOKS = new HashMap<String, List<IAction>>() {
		{
			put("gitee", Arrays.asList(new DefaultAction[] { new DefaultAction(new DefaultAction.Consumer() {

				@Override
				public Output accept(Input input) {
					String data = input.get().toString();
					return new DefaultOutput(data.replace("      ", "\n"));
				}
			}) }));
			put("testHome", Arrays.asList(new DefaultAction[] { new DefaultAction(new DefaultAction.Consumer() {

				@Override
				public Output accept(Input input) {
					String data = input.get().toString();
					return new DefaultOutput(data.replace("      ", "\n"));
				}
			}) }));
			put("github", Arrays.asList(new DefaultAction[] { new DefaultAction(new DefaultAction.Consumer() {

				@Override
				public Output accept(Input input) {
					String data = input.get().toString();
					return new DefaultOutput(data.replace("      ", "\n"));
				}
			}) }));
			put("lang", Arrays.asList(new DefaultAction[] { new DefaultAction(new DefaultAction.Consumer() {
				@Override
				public Output accept(Input input) {
					String data = input.get().toString();
					StringBuilder out = new StringBuilder();
					try {
						String urlAndNameLine[] = data.split("\n");
						for (String var1 : urlAndNameLine) {
							String urlAndName1[] = var1.split("      ");
							if (urlAndName1.length < 2)
								continue;
							String baseUrl = urlAndName1[0] + "/";
							Object data1 = sendRequest(urlAndName1[0]);
							String name1 = urlAndName1[1];
							Object data2 = processHtmlBySelector(data1,
									"body > div.zongbf > div:nth-child(1) > div.kcbf > div > ul > li::list::a^href");
							String urlAndNameLine1[] = data2.toString().split("\n");
							out.append("\n" + name1 + "\n");
							for (String var2 : urlAndNameLine1) {
								String urlAndName2[] = var2.trim().split("      ");
								if (urlAndName2.length < 2)
									continue;
								Thread.sleep(2 * 1000);
								Object data3 = sendRequest(baseUrl + urlAndName2[0]);
								String name2 = urlAndName2[1];
								Object data4 = processHtmlBySelector(data3,
										"body > div:nth-child(3) > div.content::list::div");
								out.append(data4 + "\n");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return new DefaultOutput(out.toString());
				}
			}) }));
		}
	};
	private static final Map<String, Supply> SUPPLY = new HashMap<String, Supply>() {
		{
			put("price", new Supply() {
				// don't check bad argument
				@Override
				public String supply(String args, String url) {
					String type = "0";
					String page = "1";
					String arg[] = args.split("-");
					String argType = "";
					String argPage = "";
					for (String var1 : arg) {
						if (var1.contains("type"))
							argType = var1.trim();
						else if (var1.contains("page"))
							argPage = var1.trim();
					}
					for (int i = argType.indexOf(" "), begin = -1; i < argType.length(); i++) {
						if (i < 0)
							break;
						if (argType.charAt(i) != ' ') {
							boolean isEnd = false;
							if (i + 1 > argType.length() - 1)
								isEnd = true;
							if (begin == -1)
								begin = i;
							if (begin != -1 && (!isEnd ? argType.charAt(i + 1) == ' ' : true)) {
								type = argType.substring(begin, i + 1);
								break;
							}
						}
					}
					for (int i = argPage.indexOf(" "), begin = -1; i < argPage.length(); i++) {
						if (i < 0)
							break;
						if (argPage.charAt(i) != ' ') {
							boolean isEnd = false;
							if (i + 1 > argPage.length() - 1)
								isEnd = true;
							if (begin == -1)
								begin = i;
							if (begin != -1 && (!isEnd ? argPage.charAt(i + 1) == ' ' : true)) {
								page = argPage.substring(begin, i + 1);
								break;
							}
						}
					}
					return url.replace("$1", type).replace("$2", page);
				}
			});
			put("cve", new Supply() {

				@Override
				public String supply(String args, String url) {
					String page = "1";
					String arg = args.trim();
					if (!"".equals(arg))
						page = arg;
					return url.replace("$1", page);
				}
			});
			put("testHome", new Supply() {

				@Override
				public String supply(String args, String url) {
					String page = "1";
					String arg = args.trim();
					if (!"".equals(arg))
						page = arg;
					return url.replace("$1", page);
				}
			});
			put("mba", new Supply() {

				@Override
				public String supply(String args, String url) {
					String search = "mba";
					String arg = args.trim();
					if (!"".equals(arg))
						search = arg;
					return url.replace("$1", search);
				}
			});
			put("keyword", new Supply() {

				@Override
				public String supply(String args, String url) {
					String search = "肺";
					String arg = args.trim();
					if (!"".equals(arg))
						search = arg;
					return url.replace("$1", search);
				}
			});
			put("github", new Supply() {

				@Override
				public String supply(String args, String url) {
					String search = "github";
					String arg = args.trim();
					if (!"".equals(arg))
						search = arg;
					return url.replace("$1", search);
				}
			});
			put("bot", new Supply() {

				@Override
				public String supply(String args, String url) {
					String msg = "hello";
					String arg = args.trim();
					if (!"".equals(arg))
						msg = arg;
					return url.replace("$1", msg);
				}
			});
			put("searchCompany", new Supply() {

				@Override
				public String supply(String args, String url) {
					String keyword = "中国石油";
					String page = "1";
					String arg[] = args.split("-");
					String argKeyWord = "";
					String argPage = "";
					for (String var1 : arg) {
						if (var1.contains("keyword"))
							argKeyWord = var1.trim();
						else if (var1.contains("page"))
							argPage = var1.trim();
					}
					for (int i = argKeyWord.indexOf(" "), begin = -1; i < argKeyWord.length(); i++) {
						if (i < 0)
							break;
						if (argKeyWord.charAt(i) != ' ') {
							boolean isEnd = false;
							if (i + 1 > argKeyWord.length() - 1)
								isEnd = true;
							if (begin == -1)
								begin = i;
							if (begin != -1 && (!isEnd ? argKeyWord.charAt(i + 1) == ' ' : true)) {
								keyword = argKeyWord.substring(begin, i + 1);
								break;
							}
						}
					}
					for (int i = argPage.indexOf(" "), begin = -1; i < argPage.length(); i++) {
						if (i < 0)
							break;
						if (argPage.charAt(i) != ' ') {
							boolean isEnd = false;
							if (i + 1 > argPage.length() - 1)
								isEnd = true;
							if (begin == -1)
								begin = i;
							if (begin != -1 && (!isEnd ? argPage.charAt(i + 1) == ' ' : true)) {
								page = argPage.substring(begin, i + 1);
								break;
							}
						}
					}
					return url.replace("$1", keyword).replace("$2", page);
				}
			});
		}
	};
	private static final Map<String, List<Header>> CUSTOM_HEADER = new HashMap<String, List<Header>>() {
		{
			put("searchCompany", Arrays.asList(new BasicHeader("Referer", "https://aiqicha.baidu.com/")));
		}
	};

	public static void main(String[] args) {
		HashMap<String, Object> commands = new HashMap<>();
		boolean repl = true;
		Scanner console = new Scanner(System.in);
		consume(System.out::println, "TOOL KIT");
		consume(System.out::println,
				"command: weather,today, etc. input more to get more info, exit can quit,history to show trace");
		while (repl) {
			consume(System.out::print, "$ ");
			String commandAndArgs = console.nextLine().trim();
			String command = commandAndArgs;
			String arg = "";
			for (int i = 0; i < commandAndArgs.length(); i++) {
				if (commandAndArgs.charAt(i) == ' ') {
					command = commandAndArgs.substring(0, i);
					arg = commandAndArgs.substring(i);
					break;
				}
			}
			Object var1 = commands.get(command);
			List<IAction> beforeHooks = BEFOREHOOKS.get(command);
			List<IAction> afterHooks = AFTERHOOKS.get(command);
			String url = APIS.get(command);
			Supply supply = SUPPLY.get(command);
			if (supply != null)
				url = supply.supply(arg, url);
			if ((var1 == null && APIS.containsKey(command)) || EXLUDE_SAVE_LIST.contains(command)) {
				commands.put(command, new String(
						sendRequestAndGet(url, command, beforeHooks != null ? beforeHooks : new ArrayList<IAction>(),
								afterHooks != null ? afterHooks : new ArrayList<IAction>()).toString().getBytes(),
						Charset.forName("utf8")));
				var1 = commands.get(command);
			}
			if (var1 != null) {
				consume(System.out::println, var1);
				continue;
			}
			switch (command) {
			case "more":
				APIS.keySet().forEach((cmd) -> {
					consume(System.out::println, cmd + "  " + DESC.get(cmd));
				});
				break;
			case "history":
				HISTORY.forEach((history -> {
					consume(System.out::println, history);
				}));
				break;
			case "exit":
				repl = false;
				break;
			default:
				consume(System.out::println, "bad command");
				break;
			}
			HISTORY.add(commandAndArgs);
		}
		consume(System.out::println, "all done");
		console.close();
	}

	private static void consume(java.util.function.Consumer<Object> consumer, Object t) {
		consumer.accept(t);
	}

	private static Object sendRequest(String url) {
		Object data = null;
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeaders(new Header[] { new BasicHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36 Edg/87.0.664.66") });
		try {
			HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
			data = new String(IOUtils.toByteArray(response.getEntity().getContent()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	}

	private static Object sendRequestAndGet(String url, String cmdKey, List<IAction> beforeHooks,
			List<IAction> afterHooks) {
		return PipeLine.in(new DefaultAction(new DefaultAction.Consumer() {
			@Override
			public Output accept(Input input) {
				Object data = null;
				HttpGet httpGet = new HttpGet(url);
				List<Header> headers = new ArrayList<>();
				if (CUSTOM_HEADER.get(cmdKey) != null)
					headers.addAll(CUSTOM_HEADER.get(cmdKey));
				headers.add(new BasicHeader("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36 Edg/87.0.664.66"));
				Header basicHeaders[] = new Header[headers.size()];
				for (int i = 0; i < basicHeaders.length; i++)
					basicHeaders[i] = headers.get(i);
				httpGet.setHeaders(basicHeaders);
				try {
					HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
					data = new String(IOUtils.toByteArray(response.getEntity().getContent()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return new DefaultOutput(data);
			}
		})).next(beforeHooks).next(new DefaultAction(new DefaultAction.Consumer() {

			@Override
			public Output accept(Input input) {
				Object data = process(input.get(), PROCESS_EXPRESSION.get(cmdKey));
				return new DefaultOutput(data);
			}
		})).next(afterHooks).out().get();

	}

	private static Object process(Object object, Process process) {
		Object data = "";
		switch (process.type) {
		case Process.TYPE_JSON:
			data = processJson(object, process.expression);
			break;
		case Process.TYPE_SELECTOR:
			data = processHtmlBySelector(object, process.expression);
		default:
			break;
		}
		return data;
	}

	private static Object processJson(Object object, String expression) {
		StringBuilder builder = new StringBuilder();
		List<String> stage = Arrays.asList(expression.split("->"));
		List<Expression> expressions = new ArrayList<CommandTool.Expression>();
		for (String var1 : stage) {
			String[] var2 = var1.split(":");
			expressions.add(new Expression(var2[0], var2[1], Arrays.asList(var2[2].split("&"))));
		}
		Object json = JSON.parse(object.toString());
		for (Expression exp : expressions) {
			if (!Expression.TYPE_NULL.equals(exp.getKey()))
				json = JSONObject.class.cast(json).get(exp.getKey());
			switch (exp.type) {
			case Expression.JSON_TYPE_OBJECT:
				for (String key : exp.getResultKey())
					if (!Expression.TYPE_NULL.equals(key)) {
						builder.append(JSONObject.class.cast(json).get(key));
						builder.append("\n");
					}
				break;
			case Expression.JSON_TYPE_ARRAY:
				JSONArray array = JSONArray.class.cast(json);
				if (exp.getResultKey().contains(Expression.TYPE_ALL)
						&& exp.getResultKey().get(0).equals(Expression.TYPE_ALL)) {
					builder.append(array);
					continue;
				}
				for (Object var1 : array) {
					JSONObject var2 = JSONObject.class.cast(var1);
					StringBuilder line = new StringBuilder();
					for (String key : exp.getResultKey()) {
						if (!Expression.TYPE_NULL.equals(key) && !Expression.TYPE_ALL.equals(key))
							line.append(var2.get(key) + "   ");
						else if (Expression.TYPE_ALL.equals(key))
							line.append(var2);
					}
					builder.append(line);
					builder.append("\n");
				}
				break;
			default:
				break;
			}
		}
		return builder.toString();
	}

	private static Object processHtmlBySelector(Object object, String expression) {
		StringBuilder builder = new StringBuilder();
		List<String> stage = Arrays.asList(expression.split("<=>"));
		List<Expression> expressions = new ArrayList<CommandTool.Expression>();
		for (String var1 : stage) {
			String[] var2 = var1.split("::");
			expressions.add(new Expression(var2[0], var2[1], Arrays.asList(var2[2].split("&"))));
		}
		Document dom = Jsoup.parse(object.toString());
		for (Expression exp : expressions) {
			Elements elements = dom.select(exp.getKey());
			for (Element element : elements) {
				StringBuilder line = new StringBuilder();
				for (String resultKey : exp.getResultKey()) {
					String[] pathAndAttr = resultKey.split("\\^");
					String path = resultKey;
					String attr = "";
					if (pathAndAttr.length > 0 && pathAndAttr.length == 2) {
						path = pathAndAttr[0];
						attr = pathAndAttr[1];
					}
					Elements var1 = element.select(path);
					for (Element var2 : var1) {
						if (!"".equals(attr))
							line.append(var2.attr(attr).trim() + "      ");
						line.append(var2.text().trim() + "      ");
					}
				}
				line.append("\n");
				builder.append(line);
			}
		}
		return builder.toString();
	}
}
