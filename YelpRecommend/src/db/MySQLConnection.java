package db;

import java.sql.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import entity.Item;
import entity.Item.ItemBuilder;
import external.YelpAPI;


public class MySQLConnection {
	private Connection conn;
	public MySQLConnection() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
        if (conn != null) {
        	try {
        		conn.close();
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    }

    public void setFavoriteItems(String userId, List<String> itemIds) {
        if (conn == null) {
        	System.err.println("DB Connection failed");
        	return;
        }
        try {
        	String sql = "INSERT IGNORE INTO history(user_id,item_id) VALUES(?,?)";
        	PreparedStatement pStatement = conn.prepareStatement(sql);
        	pStatement.setString(1, userId);
        	for (String itemId : itemIds) {
        		pStatement.setString(2, itemId);
        		pStatement.execute();
        	}
        }	catch (Exception e) {
        	e.printStackTrace();
        } 
    }

    public void unsetFavoriteItems(String userId, List<String> itemIds) {
    	 if (conn == null) {
             System.err.println("DB connection failed");
             return;
          }
         
          try {
             String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
             PreparedStatement ps = conn.prepareStatement(sql);
             ps.setString(1, userId);
             for (String itemId : itemIds) {
                 ps.setString(2, itemId);
                 ps.execute();
             }          
          } catch (Exception e) {
             e.printStackTrace();
          }
    }
    
    public Set<String> getFavoriteItemIds(String userId) {
    	if (conn == null) {
			return new HashSet<>();
		}	
    	Set<String> favoriteItems = new HashSet<>();
        try {
        	String sql = "SELECT item_id FROM history WHERE user_id = ?";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) { // the person may have several favorite items, thus use while(rs.next()){} here
                String itemId = rs.getString("item_id");
                favoriteItems.add(itemId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return favoriteItems;
    }

    public Set<Item> getFavoriteItems(String userId) {
    	if (conn == null) {
			return new HashSet<>();
		}
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		
		String sql = "SELECT * FROM items WHERE item_id = ?"; // "?" is to prevent SQL injection
		try {
			PreparedStatement ps = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				ps.setString(1, itemId);
				ResultSet rs = ps.executeQuery(); // right now rs point to -1 row
				
				ItemBuilder builder = new ItemBuilder();
	            // Because itemId is unique and given one item id there should have
	            // only one result returned, thus don't need to use while (rs.next()){} to read the data
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setUrl(rs.getString("url"));
					builder.setRating(rs.getDouble("rating"));
					builder.setDistance(rs.getDouble("distance"));
					builder.setCategories(getCategories(itemId));
					
					favoriteItems.add(builder.build());
				}
			}	
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteItems;
		
			
//        Set<Item> favoriteItems = new HashSet<>();
//        try {
//
//          for (String itemId : itemIds) {
//            String sql = "SELECT * FROM items WHERE user_id = ?"; // "?" is to prevent SQL injection
//            PreparedStatement statement = conn.prepareStatement(sql);
//            statement.setString(1, itemId);
//            ResultSet rs = statement.executeQuery(); // right now rs point to -1 row
//            ItemBuilder builder = new ItemBuilder();
//
//            // Because itemId is unique and given one item id there should have
//            // only one result returned, thus don't need to use while (rs.next()){} to read the data
//            if (rs.next()) {
//              builder.setItemId(rs.getString("item_id"));
//              builder.setName(rs.getString("name"));
//
//              builder.setRating(rs.getDouble("rating"));
//              builder.setAddress(rs.getString("address"));
//
//              builder.setImageUrl(rs.getString("image_url"));
//              builder.setUrl(rs.getString("url"));
//            }
//            
//            // Join categories information into builder.
//            // But why we do not join in sql? Because it'll be difficult to set it in builder.
//            sql = "SELECT * FROM categories WHERE item_id = ?";
//            statement = conn.prepareStatement(sql);
//            statement.setString(1, itemId);
//            rs = statement.executeQuery();
//            Set<String> categories = new HashSet<>();
//            while (rs.next()) {
//                categories.add(rs.getString("category"));
//            }
//            builder.setCategories(categories);
//            favoriteItems.add(builder.build());
//          }
//        } catch (SQLException e) {
//          e.printStackTrace();
//        }
//        return favoriteItems;
    }

    public Set<String> getCategories(String itemId) {
    	if (conn == null) {
			return new HashSet<>();
		}	
    	Set<String> categories = new HashSet<>();
        try {
        	//name of the row: category; name of the table: categories
            String sql = "SELECT category FROM categories WHERE item_id = ? "; 
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, itemId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return categories;
    }

    public List<Item> searchItems(double lat, double lon, String term) {
    	// Connect to external API
        YelpAPI api = new YelpAPI();
        List<Item> items = api.search(lat, lon, term);
        for (Item item : items) {
            saveItem(item);
        }
        return items;

    }

    public void saveItem(Item item) {
    	if (conn == null) {
            System.err.println("DB connection failed");
            return;
        }

        try {
            String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, item.getItemId());
            ps.setString(2, item.getName());
            ps.setDouble(3, item.getRating());
            ps.setString(4, item.getAddress());
            ps.setString(5, item.getUrl());
            ps.setString(6, item.getImageUrl());
            ps.setDouble(7, item.getDistance());
            ps.execute();

            sql = "INSERT IGNORE INTO categories VALUES(?, ?)";
            ps = conn.prepareStatement(sql);
            ps.setString(1, item.getItemId());
            for (String category : item.getCategories()) {
                ps.setString(2, category);
                ps.execute();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
