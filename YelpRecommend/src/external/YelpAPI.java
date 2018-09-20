package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;


public class YelpAPI {
	
	private static final String HOST = "https://api.yelp.com";
	private static final String ENDPOINT = "/v3/businesses/search";
	private static final String DEFAULT_TERM = "";
	private static final int SEARCH_LIMIT = 20;

	private static final String TOKEN_TYPE = "Bearer";
	private static final String API_KEY = "5WUzo3HaggVwNPJlF8n3dQd_PIsopqjE8RekeJw7gkYP6okd-5JUj7gfUVPldrZtGY3Gb8uUkbfIw5huSHvvO8Ngv5Hqce9Ya6VjzZloS7cdTObpsWi628dX3_2BW3Yx";

 	public List<Item> search(double lat, double lon, String term) {
 		
 		// term is the search key word / search conditions
 		if (term == null || term.isEmpty()) {
 			term = DEFAULT_TERM;
 		}
 		
 		try {
 			term = URLEncoder.encode(term, "UTF-8"); // I love Google ==> I20%love20%Google		
 		} catch(Exception e) {
 			e.printStackTrace();
 		}
 		
		String query = String.format("term=%s&latitude=%s&longitude=%s&limit=%s", term, lat, lon, SEARCH_LIMIT);
 		String url = HOST + ENDPOINT + "?" + query;
 		
 		try {
 			// Create a URLConnection instance that represents a connection
 			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
 			
 			connection.setRequestMethod("GET");
 			connection.setRequestProperty("Authorization", TOKEN_TYPE + " " + API_KEY);
 			
 			// Get the status code from an HTTP response message. It’ll send the request first and then get response code.
 			int responseCode = connection.getResponseCode();
 			
 			// print information for Debug purpose
 			System.out.println("url = " + url);
 			System.out.println("response code = " + responseCode);
 			
 			
 			if (responseCode != 200) {
 				return new ArrayList<>();
 			}
 			
 			// connection.getInputStream() - Return an inputstream that reads response data from this open connection. 
 			// Create a BufferedReader to help read text from a character-input stream. 
 			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine = "";
			StringBuilder response = new StringBuilder();
			
            // Append response data to response StringBuilder instance line by line.
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// Extract businesses array only.
			JSONObject obj = new JSONObject(response.toString());
			if (!obj.isNull("businesses")) {
				return getItemList(obj.getJSONArray("businesses"));
			}
 			
 		} catch(Exception e) {
 			e.printStackTrace();
 		}
 		
 		return new ArrayList<>();
	}
	
	/**
	 * Helper methods
	 */
	// Convert JSONArray to a list of item objects.
	private List<Item> getItemList(JSONArray restaurants) throws JSONException {
		List<Item> list = new ArrayList<>();
		
		for (int i = 0; i < restaurants.length(); ++i) {
			JSONObject restaurant = restaurants.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			if(!restaurant.isNull("id")) {
				builder.setItemId(restaurant.getString("id"));
			}
			if(!restaurant.isNull("name")) {
				builder.setName(restaurant.getString("name"));
			}
			if(!restaurant.isNull("url")) {
				builder.setUrl(restaurant.getString("url"));
			}
			if(!restaurant.isNull("image_url")) {
				builder.setImageUrl(restaurant.getString("image_url"));
			}
			if(!restaurant.isNull("rating")) {
				builder.setRating(restaurant.getDouble("rating"));
			}
			if(!restaurant.isNull("distance")) {
				builder.setDistance(restaurant.getDouble("distance"));
			}
			
			builder.setAddress(getAddress(restaurant));
			builder.setCategories(getCategories(restaurant));
			
			list.add(builder.build());
		}
		return list;
	}

	private Set<String> getCategories(JSONObject restaurant) throws JSONException {
		Set<String> categories = new HashSet<>();
		
		if (!restaurant.isNull("categories")) {
			JSONArray array = restaurant.getJSONArray("categories");
			for (int i = 0; i < array.length(); ++i) {
				JSONObject category = array.getJSONObject(i);
				if (!category.isNull("alias")) {
					categories.add(category.getString("alias"));
				}
			}
		}
		return categories;
	}


	private String getAddress(JSONObject restaurant) throws JSONException {
		String address = "";
		
		if (!restaurant.isNull("location")) {
			JSONObject location = restaurant.getJSONObject("location");
			if (!location.isNull("display_address")) {
				JSONArray array = location.getJSONArray("display_address");
				address = array.join(",");
			}
		}
		return address;
	}
	
 	// adding a print function to show JSON array returned from Yelp for debugging.
	private void queryAPI(double lat, double lon) {
		List<Item> itemList = search(lat, lon, null);
		for (Item item : itemList) {
			JSONObject jsonObject = item.toJSONObject();
			System.out.println(jsonObject);
		}
	}

	/**
	 * Main entry for sample Yelp API requests.
	 */
	public static void main(String[] args) {
		YelpAPI tmApi = new YelpAPI();
//		tmApi.queryAPI(37.38, -122.08);
		tmApi.queryAPI(43.088947, -76.154480);	// location of Syracuse, NY
	}

}
