package com.spidio.UserSegmenter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.util.BytesRef;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.hash.MurmurHash3;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.spidio.dataModel.DeviceObject;
import com.spidio.dataModel.LocationObject;
import com.spidio.util.GetWurflData;

public class EnhanceUserDataDailyv1 {

	// Enhances existing data points such as geo-based,device-based,Time of the
	// day patterns and update it in corresponding Elasticsearch Document.
	// These enhanced data points are later used to generate publisher based
	// report codes which gives full profile of publishers Audience.
	static Integer recordCount = 0;
	private static TransportClient client;

	public static void setUp() throws Exception {
		if (client == null) {

			Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "personaq").build();

			
	
			client = new TransportClient(settings)
					 .addTransportAddress(new InetSocketTransportAddress("192.168.106.118", 9300))
			         .addTransportAddress(new InetSocketTransportAddress("192.168.106.119", 9300))
			         .addTransportAddress(new InetSocketTransportAddress("192.168.106.120", 9300));
	
		   /*	
			client = new TransportClient(settings)
					 .addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
			*/
			
			/*
			 * client = new TransportClient();
			 * client.addTransportAddress(getTransportAddress())  
			 * NodesInfoResponse nodeInfos =
			 * (NodesInfoResponse)client.admin().cluster().prepareNodesInfo(new
			 * String[0]).get(); String clusterName = nodeInfos.getClusterName().value();
			 * System.out.println(String.format("Found cluster... cluster name: %s", new
			 * Object[] { clusterName }));
			 */
		}
		System.out.println("Finished the setup process...");
	}

	private static InetSocketTransportAddress getTransportAddress() {
		String host = null;
		String port = null;
		if (host == null) {
			host = "localhost";
			// System.out.println("ES_TEST_HOST enviroment variable does not exist. choose
			// default '192.168.101.132'");
		}
		if (port == null) {
			port = "9300";
			// System.out.println("ES_TEST_PORT enviroment variable does not exist. choose
			// default '9300'");
		}
		System.out.println(String.format("Connection details: host: %s. port:%s.", new Object[] { host, port }));
		return new InetSocketTransportAddress(host, Integer.parseInt(port));
	}

	public static String changeString(String s) {
		char[] characters = s.toCharArray();
		int rand = (int) (Math.random() * s.length());

		s = s.replace(s.charAt(rand), 'a');
		s = s.replace('_', 'b');

		return s;
	}

	public static boolean generateData(String startdate, String enddate, String channel_name, Client client, Set<String> lines)
			throws Exception {
		// TODO Auto-generated method stub

		// Client client = new TransportClient()

		// lock.lock();

		/*
		 * SearchResponse response1 = client.prepareSearch("userprofilestore")
		 * .setTypes("core2").setSearchType(SearchType.QUERY_THEN_FETCH) .setScroll(new
		 * TimeValue(600000))
		 * .setQuery(QueryBuilders.matchQuery("channel_name","wittyfeed")).setSize(
		 * 100000).execute() .actionGet();
		 */
        
		SearchResponse response1 = client.prepareSearch("enhanceduserdatabeta1").setTypes("core2")
				.setSearchType(SearchType.QUERY_THEN_FETCH).setScroll(new TimeValue(600000))
				.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchQuery("channel_name", channel_name),
						FilterBuilders.rangeFilter("request_time").from(startdate).to(enddate).cache(false)))
				.setSize(1000).execute().actionGet();
         
		/*
		SearchResponse response1 = client.prepareSearch("enhanceduserdatabeta1").setTypes("core2").setScroll(new TimeValue(600000))
			       .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchQuery("channel_name",channel_name),
					FilterBuilders.rangeFilter("request_time").from(startdate).to(enddate))).setSize(1000).execute().actionGet();
        */
		
		List<Callable<String>> lst = new ArrayList<Callable<String>>();

		try {
			System.setOut(new PrintStream(new File("DailyEnhancerLogg1.txt")));
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Scroll until no hits are returned

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

		try {

			while (true) {
				// Scroll until no hits are returned
				try {
					for (SearchHit hit : response1.getHits().getHits()) {

						// System.out.println("Records:"+response1.getHits());
						lst.add(new WorkerThread(hit, client, lines));

						// Break condition: No hits are returned
					}
					// System.out.println("Number of scrolls:"+count3);
					List<Future<String>> maps = executor.invokeAll(lst, 10, TimeUnit.MINUTES);

					System.out.println("Response Size:" + response1.getHits());

					// System.out.println("Number of scrolls:"+count3)
					response1 = client.prepareSearchScroll(response1.getScrollId()).setScroll(new TimeValue(600000))
							.execute().actionGet();
					lst.clear();
					maps.clear();

					if (response1.getHits().getHits().length == 0) {
						break;
					}

					recordCount++;
					System.out.println("RecordCount:" + startdate +":"+ enddate + recordCount);
				} catch (Exception e) {

					e.printStackTrace();
					continue;
				}

			}

		} catch (Exception e1) {

			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {

			System.out.println("FinalRecordCount" + recordCount);
			executor.shutdown();
			client.close();
		}
		return true;
	}

}