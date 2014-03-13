/*
 * FTBUtils
 * Copyright © 2014 Ilya Dynin
 */
package com.idynin.ftbutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FTBUtils
{
	static Options opts = new Options();

	static ArrayList<ModPack> modpacks = null;

	private static Map<String, SimpleEntry<String, Integer>> chEdgeMap = new HashMap<String, SimpleEntry<String, Integer>>();

	private static boolean latenciesPopulated = false, freshEdges = false, verbose = false;

	private static final String masterServerURL = "new.creeperrepo.net";
	private static final String modpackMetaPathBase = "/FTB2/static/";
	private static String modpackMetaFile = "modpacks.xml";
	private static String modpackPath = "/FTB2/modpacks";

	private static int exitCode = 0;

	private static Thread shutdownAction = new Thread() {
		@Override
		public void run() {
			System.out.println();
			System.out.println("Terminated.");
		}
	};

	public static void main(String[] args)
	{
		addOption(oneArgOption("downloadserver", "modpack", "Download a Modpack Server"));
		addOption(oneArgOption("getversion", "modpack", "Get the recommended version of modpack"));
		addOption(oneArgOption("checkversion", "modpack", "Get the recommended version of modpack"));
		addOption(noArgOption("listmodpacks", "List all available modpacks"));
		addOption(noArgOption("help", "Show this help"));
		addOption(noArgOption("v", "Verbose mode"));
		addOption(oneArgOption("privatepack", "packcode",
				"Perform the requested action in the packcode context"));

		addOption(twoArgOption("checkversion", "modpack> <version",
				"Checks if the recommended version matches passed version"));

		Runtime.getRuntime().addShutdownHook(shutdownAction);

		initialize();

		CommandLineParser parser = new GnuParser();
		try {
			CommandLine cmd = parser.parse(opts, args);
			if (cmd.hasOption("v")) {
				verbose = true;
			}
			if (cmd.hasOption("privatepack")) {
				modpackMetaFile = cmd.getOptionValue("privatepack").trim();
				if (!modpackMetaFile.endsWith(".xml")) {
					modpackMetaFile += ".xml";
				}

				modpackPath = modpackPath.replace("modpacks", "privatepacks");
			}
			if (cmd.hasOption("help")) {
				printHelp();
			} else if (cmd.hasOption("listmodpacks")) {
				printModpacks();
			} else if (cmd.hasOption("getversion")) {
				System.out.println(getRecommendedVersion(cmd.getOptionValue("getversion")));
			} else if (cmd.hasOption("downloadserver")) {
				downloadModpackServer(cmd.getOptionValue("downloadserver"));
			} else if (cmd.hasOption("checkversion")) {
				checkVersion(cmd.getOptionValues("checkversion"));
			} else {
				printHelp();
			}
		} catch (MissingArgumentException e) {
			System.err.println(e.getLocalizedMessage());
			printHelp();
		} catch (UnrecognizedOptionException e) {
			System.err.println(e.getLocalizedMessage());
			printHelp();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		Runtime.getRuntime().removeShutdownHook(shutdownAction);

		System.exit(exitCode);
	}

	private static void addOption(Option o) {
		opts.addOption(o);
	}

	private static void checkVersion(String[] optionValues) {
		if (optionValues.length == 2) {
			String vers = getRecommendedVersion(optionValues[0]);
			if (vers != null) {
				vers = vers.split("\t")[1].trim().replace('_', '.');
				if (optionValues[1].replace('_', '.').equals(vers)) {
					exitCode = 0;
					System.out.println("Versions Match!");
					return;
				} else {
					System.err.println("Versions Do Not Match! New recommended version:\t" + vers);
					exitCode = -1;
				}
			}
		} else {
			System.err.println("Invalid number of arguments. Expected [modpack] [version]");
			exitCode = -1;
		}

	}

	private static void downloadModpackServer(String requestedModpack) {
		File outputdir = new File(".");

		if (!outputdir.canWrite()) {
			System.err.println("Cannot write to " + outputdir.getAbsolutePath());
			System.exit(-1);
		}

		if (modpacks == null) {
			populateModpacks();
		}
		requestedModpack = requestedModpack.trim();
		for (ModPack mp : modpacks) {
			if (requestedModpack.equalsIgnoreCase(mp.getName().trim())) {
				FileOutputStream fos;
				URL mpServerFullLocation;
				URLConnection conn;
				File outputFile = new File(mp.getName() + "-" + mp.getVersion()
						+ "." + FilenameUtils.getExtension(mp.getServerUrl()));
				for (String server : serversByAscendingLatency()) {
					try {
						System.out.println("Downloading modpack " + mp.getName() + " version "
								+ mp.getVersion() + " from "
								+ getServerNameFromURL(server));
						mpServerFullLocation = new URL("http://" + server + modpackPath + "/"
								+ mp.getDir() + "/" + mp.getVersion().replace('.', '_') + "/"
								+ mp.getServerUrl());
						conn = mpServerFullLocation.openConnection();
						final long filesize = conn.getContentLengthLong();
						printVerbose("From URL: " + mpServerFullLocation.toString());

						fos = new FileOutputStream(outputFile + ".part");
						final long start = System.currentTimeMillis();
						CountingOutputStream cos = new CountingOutputStream(fos) {
							String progressFormat = "Download Progress:\t%6s/%-6s\t(%s/sec)";

							String message = "";

							@Override
							protected synchronized void beforeWrite(int n) {
								super.beforeWrite(n);

								message = String.format(
										progressFormat,
										FileUtils.byteCountToDisplaySize(getCount()),
										FileUtils.byteCountToDisplaySize(filesize),
										FileUtils.byteCountToDisplaySize(getCount()
												/ (System.currentTimeMillis() - start) * 1000));

								System.out
										.print("\r                                      "
												+ "                                   \r");
								System.out.print(message);
							}
						};
						if (outputFile.exists() && !outputFile.delete()) {
							System.err.println("Unable to delete modpack file "
									+ outputFile.getAbsolutePath());
							System.exit(-1);
						}
						IOUtils.copy(conn.getInputStream(), cos);
						FileUtils.moveFile(new File(outputFile + ".part"), outputFile);
						System.out.println("\nDownloading " + outputFile.getName() + " complete!");
						return;
					} catch (IOException e) {
						System.err.println("Error downloading " + mp.getName() + " from " + server);
						continue;
					}

				}
				System.err.println("Error downloading modpack " + mp.getName());
			}
		}
		System.err.println("Modpack " + requestedModpack + " not found");
		exitCode = -1;
	}

	private static HashMap<String, SimpleEntry<String, Integer>> fetchEdgesFromServer(
			String server, int timeout) {
		JsonParser jp = new JsonParser();
		HashMap<String, SimpleEntry<String, Integer>> edgemap = new HashMap<String, SimpleEntry<String, Integer>>();

		InputStream is;
		URLConnection con;
		try {
			con = new URL("http://"
					+ server + "/edges.json").openConnection();
			con.setConnectTimeout(timeout);
			is = con.getInputStream();
			@SuppressWarnings("unchecked")
			JsonObject edges = jp.parse(
					StringUtils.join(IOUtils.readLines(is, Charset.forName("UTF-8"))))
					.getAsJsonArray().get(0).getAsJsonObject();
			for (Entry<String, JsonElement> edge : edges.entrySet()) {
				edgemap.put(edge.getKey(), new SimpleEntry<String, Integer>(edge.getValue()
						.getAsString(), Integer.MAX_VALUE));
			}

			if (edgemap.size() > 0)
				return edgemap;
		} catch (Exception e) {
		}
		return null;
	}

	private static void fetchFreshEdges() {
		HashMap<String, SimpleEntry<String, Integer>> edgemap = null;
		printVerbose("Fetching fresh edges...");
		freshEdges = false;
		if ((edgemap = fetchEdgesFromServer(masterServerURL, 600)) != null) {
			chEdgeMap = edgemap;
			latenciesPopulated = false;
			freshEdges = true;

		} else {

			edgemap = new HashMap<String, SimpleEntry<String, Integer>>();

			ArrayList<SimpleEntry<String, Integer>> servs = new ArrayList<SimpleEntry<String, Integer>>();
			servs.addAll(chEdgeMap.values());

			Collections.shuffle(servs);

			for (SimpleEntry<String, Integer> defaultedge : servs) {
				if ((edgemap = fetchEdgesFromServer(defaultedge.getKey(), 1200)) != null) {
					chEdgeMap = edgemap;
					latenciesPopulated = false;
					freshEdges = true;
					break;
				}

			}
		}
		if (freshEdges) {
			printVerbose("Edges fetched successfully");
		} else {
			printVerbose("Error fetching edges, using static edges");
		}
	}

	private static String getLowestLatencyServer() {
		return serversByAscendingLatency()[0];
	}

	private static String getRecommendedVersion(String requestedModpack) {
		if (modpacks == null) {
			populateModpacks();
		}
		requestedModpack = requestedModpack.trim();
		for (ModPack mp : modpacks) {
			if (requestedModpack.equalsIgnoreCase(mp.getName().trim()))
				return mp.getName() + " recommended version:\t" + mp.getVersion();
		}
		System.err.println("Invalid modpack: " + requestedModpack);
		exitCode = -1;
		return "";
	}

	private static String getServerNameFromURL(String url) {
		for (Entry<String, SimpleEntry<String, Integer>> e : chEdgeMap.entrySet()) {
			if (e.getValue().getKey().equalsIgnoreCase(url))
				return e.getKey();
		}
		return url;
	}

	private static void initialize() {
		try {
			InputStream is = FTBUtils.class.getResourceAsStream("edges.json");
			@SuppressWarnings("unchecked")
			JsonObject edges = new JsonParser().parse(
					StringUtils.join(IOUtils.readLines(is, Charset.forName("UTF-8"))))
					.getAsJsonArray().get(0).getAsJsonObject();
			for (Entry<String, JsonElement> edge : edges.entrySet()) {
				chEdgeMap.put(edge.getKey(), new SimpleEntry<String, Integer>(edge.getValue()
						.getAsString(), Integer.MAX_VALUE));
			}
		} catch (Exception e) {
			printVerbose("Error loading internal edges.json");
			chEdgeMap.clear();
			// @formatter:off
			chEdgeMap.put(
					"Miami",
					new SimpleEntry<String, Integer>(
							"miami1.creeperrepo.net",
							Integer.MAX_VALUE)
					);
			chEdgeMap.put(
					"Chicago",
					new SimpleEntry<String, Integer>(
							"chicago2.creeperrepo.net",
							Integer.MAX_VALUE));
			chEdgeMap.put(
					"Nottingham",
					new SimpleEntry<String, Integer>(
							"england2.creeperrepo.net",
							Integer.MAX_VALUE));
			chEdgeMap.put(
					"Grantham",
					new SimpleEntry<String, Integer>(
							"england3.creeperrepo.net",
							Integer.MAX_VALUE));
			chEdgeMap.put(
					"Los Angeles",
					new SimpleEntry<String, Integer>(
							"losangeles1.creeperrepo.net",
							Integer.MAX_VALUE));
			chEdgeMap.put(
					"Atlanta",
					new SimpleEntry<String, Integer>(
							"atlanta1.creeperrepo.net",
							Integer.MAX_VALUE));
			chEdgeMap.put(
					"Atlanta-2",
					new SimpleEntry<String, Integer>(
							"atlanta2.creeperrepo.net",
							Integer.MAX_VALUE));
			chEdgeMap.put(
					"Maidenhead",
					new SimpleEntry<String, Integer>(
							"england1.creeperrepo.net",
							Integer.MAX_VALUE));
			// @formatter:on
		}

	}

	private static void populateLatencies() {
		if (!freshEdges) {
			fetchFreshEdges();
		}

		final int latencyEarlyCuttoff = 35;

		Iterator<Entry<String, SimpleEntry<String, Integer>>> iter = chEdgeMap.entrySet()
				.iterator();

		Entry<String, SimpleEntry<String, Integer>> edge;

		class PingRequest implements Callable<Entry<String, SimpleEntry<String, Integer>>> {

			Entry<String, SimpleEntry<String, Integer>> edge;

			int timeout = 1000;
			int numpings = 3;

			public PingRequest(Entry<String, SimpleEntry<String, Integer>> target) {
				edge = target;
			}

			@Override
			public Entry<String, SimpleEntry<String, Integer>> call() throws Exception {
				InetSocketAddress addr;
				long start, stop, lat;
				int sum = 0, count = 0, pingTime = -1;
				addr = new InetSocketAddress(edge.getValue().getKey(), 80);

				printVerbose("Pinging " + addr);
				for (int i = 0; i < numpings; i++) {
					try {
						start = System.currentTimeMillis();
						new Socket().connect(addr, timeout);
						stop = System.currentTimeMillis();
						lat = stop - start;
						sum += lat;
						count++;
						if (lat < latencyEarlyCuttoff) {
							break;
						}
					} catch (IOException e) {
						continue;
					}
				}
				if (sum > 0) {
					pingTime = sum / count;
					edge.getValue().setValue(pingTime);
				}
				printVerbose("Pingtime for " + addr + " is " + pingTime);
				return edge;
			}

		}

		ExecutorService execpool = Executors.newCachedThreadPool();

		List<Future<Entry<String, SimpleEntry<String, Integer>>>> reqs = new ArrayList<Future<Entry<String, SimpleEntry<String, Integer>>>>();
		while (iter.hasNext()) {
			edge = iter.next();
			reqs.add(execpool.submit(new PingRequest(edge)));
		}

		Entry<String, SimpleEntry<String, Integer>> temp;
		while (!reqs.isEmpty()) {
			for (int i = 0; i < reqs.size(); i++) {
				if (reqs.get(i).isDone()) {
					try {
						temp = reqs.get(i).get();
						chEdgeMap.put(getServerNameFromURL(temp.getKey()), temp.getValue());
						reqs.remove(i);
						if (temp.getValue().getValue() < latencyEarlyCuttoff) {
							break;
						}
						latenciesPopulated = true;
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

				}
			}
		}
		execpool.shutdownNow();
	}

	/**
	 * Lightly modified from FTB net.ftb.workers.ModpackLoader
	 * 
	 */
	private static void populateModpacks() {
		ModPack.populateModpacks(getLowestLatencyServer(), modpackMetaPathBase + modpackMetaFile);
		modpacks = ModPack.getPackArray();

	}

	private static void printHelp() {
		HelpFormatter hf = new HelpFormatter();
		String header;
		// @formatter:off
		header = "\n88888888888 888888888888 88888888ba  88        88         88 88            \n" + 
				"88               88      88      \"8b 88        88   ,d    \"\" 88            \n" + 
				"88               88      88      ,8P 88        88   88       88            \n" + 
				"88aaaaa          88      88aaaaaa8P' 88        88 MM88MMM 88 88 ,adPPYba,  \n" + 
				"88\"\"\"\"\"          88      88\"\"\"\"\"\"8b, 88        88   88    88 88 I8[    \"\"  \n" + 
				"88               88      88      `8b 88        88   88    88 88  `\"Y8ba,   \n" + 
				"88               88      88      a8P Y8a.    .a8P   88,   88 88 aa    ]8I  \n" + 
				"88               88      88888888P\"   `\"Y8888Y\"'    \"Y888 88 88 `\"YbbdP\"'  \n\n"
				+ "FTBUtils\nCopyright © 2014 Ilya Dynin\n";
		// @formatter:on
		System.out.println(header);
		hf.printHelp("java -jar ftbutils.jar [options]", opts);
		System.out.println();
	}

	private static void printModpacks() {
		if (modpacks == null) {
			populateModpacks();
		}
		if (modpacks.size() > 0) {
			System.out.println("Pack Name\tAuthor\tMC Version\tPack Version");
			for (ModPack mp : modpacks) {
				System.out.println(mp.getName() + "\t" + mp.getAuthor() + "\t" + mp.getMcVersion()
						+ "\t" + mp.getVersion() + "\t");
			}
		} else {
			System.out.println("No modpacks found...");
		}
	}

	private static void printVerbose(Object m) {
		if (verbose) {
			System.out.println(m.toString());
		}
	}

	private static String[] serversByAscendingLatency() {
		if (!latenciesPopulated) {
			populateLatencies();
		}
		ArrayList<SimpleEntry<String, Integer>> servs = new ArrayList<SimpleEntry<String, Integer>>();
		servs.addAll(chEdgeMap.values());
		Collections.sort(servs, new Comparator<SimpleEntry<String, Integer>>() {

			@Override
			public int compare(SimpleEntry<String, Integer> o1, SimpleEntry<String, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		String[] out = new String[servs.size()];
		for (int i = 0; i < servs.size(); i++) {
			out[i] = servs.get(i).getKey();
		}
		return out;

	}

	private static Option noArgOption(String option, String description) {
		OptionBuilder
				.withDescription(description);
		return OptionBuilder
				.create(option);
	}

	private static Option oneArgOption(String option, String argName, String description) {
		OptionBuilder.hasArg();
		OptionBuilder.withArgName(argName);
		OptionBuilder
				.withDescription(description);
		return OptionBuilder
				.create(option);
	}

	private static Option twoArgOption(String option, String argName, String description) {
		OptionBuilder.hasArgs(2);
		OptionBuilder.withArgName(argName);
		OptionBuilder
				.withDescription(description);
		return OptionBuilder
				.create(option);
	}

}
