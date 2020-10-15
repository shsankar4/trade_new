package com.citi.trade.controller;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.citi.trade.model.Trade;
import com.citi.trade.model.TradeType;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;

@Controller
@CrossOrigin
public class TradeController {
	public static MongoClientURI uri = new MongoClientURI(
		    "mongodb+srv://mongoUser:cR5p1eKma8qWgIhp@cluster0.cddgx.mongodb.net/Task5?retryWrites=true&w=majority");
	
	@RequestMapping(value = "/trade/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createTrade(Trade trade, @RequestParam Map<String, String> request, Model model) {
		Stock stock = null;
		String email = request.get("email");

		String type = request.get("tradetype");
		System.out.println(type);
		if(type.equals("BUY")) {
			System.out.println(type);
			trade.setType(TradeType.BUY);
		}
		else if (type.equals("SELL")) {
			System.out.println(type);
			trade.setType(TradeType.SELL);
		} 
		System.out.println(trade.getType());
		try {
			stock = YahooFinance.get(trade.getTicker());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(stock == null) {
			
			model.addAttribute("message","Please Correct Ticker");
		    return "redirecthome";
		}
		else {
			//start of code
			MongoClient myMongo = new MongoClient(uri);
			MongoDatabase database = myMongo.getDatabase("Task5");
			MongoCollection<Document> usercollection = database.getCollection("users");
			double amoun = usercollection.find(Filters.eq("email",email)).first().getDouble("amount");
			if(type.equals("BUY")) {
				amoun -= stock.getQuote().getPrice().doubleValue()*trade.getQuantity(); 
			}
			else if (type.equals("SELL")) {
				amoun += stock.getQuote().getPrice().doubleValue()*trade.getQuantity(); 
			} 
			Document docu = new Document("amount",amoun);		
			usercollection.updateOne(Filters.eq("email",email), new Document("$set", docu));
			//end of code
			
			Document doc = new Document("created", trade.getCreated()).append("type", trade.getType().toString()).append("state",trade.getState().toString()).append("ticker", trade.getTicker()).append("quantity",trade.getQuantity()).append("price", stock.getQuote().getPrice().doubleValue()).append("balanceAfterTrade", amoun);
			MongoCollection<Document> mycollection = database.getCollection("trade");
			mycollection.insertOne(doc);
			ObjectId objectId = doc.getObjectId("_id");
			Document document = new Document("id",objectId).append("email", email);
			mycollection = database.getCollection("user_portfolio");
			mycollection.insertOne(document);
			myMongo.close(); 
			model.addAttribute("message","Trade has been created successfully. Trade ID is "+doc.getObjectId("_id").toString());
			return "createtrade";
		}
	}
	@RequestMapping(value = "/trade/view", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
//	@RequestMapping(value = "/trade/view", method = RequestMethod.GET)

	public String viewTrade(@RequestParam Map<String,String> request,Model model) {

		String email = request.get("email");

		MongoClient myMongo = new MongoClient(uri);
		MongoDatabase database = myMongo.getDatabase("Task5");
		MongoCollection<Document> usercollection = database.getCollection("user_portfolio");
		MongoCollection<Document> usr = database.getCollection("users");
		
		FindIterable<Document> iterDoc = usercollection.find(Filters.eq("email",email));
		Iterator<Document> it = iterDoc.iterator();
		String s = "";
		String y = "";
		String amoun = new String();
		while (it.hasNext()) {
			Document myDocument = it.next();
			ObjectId id = myDocument.getObjectId("id");
			MongoCollection<Document> tradeCollection = database.getCollection("trade");
			Document myDoc = tradeCollection.find(Filters.eq("_id",id)).first();
			s+=myDoc.toString()+"<br>";
			String tick = tradeCollection.find(Filters.eq("_id",id)).first().getString("ticker");
			String typ = tradeCollection.find(Filters.eq("_id",id)).first().getString("type");
			String qty = tradeCollection.find(Filters.eq("_id",id)).first().getDouble("quantity").toString();
			String stat = tradeCollection.find(Filters.eq("_id",id)).first().getString("state");
			String pric = tradeCollection.find(Filters.eq("_id",id)).first().getDouble("price").toString();
			String bal = tradeCollection.find(Filters.eq("_id",id)).first().getDouble("balanceAfterTrade").toString();
			amoun = usr.find(Filters.eq("email",email)).first().getDouble("amount").toString();
			y += "Ticker:"+tick+" Type:"+typ+" State:"+stat+" Price:"+pric+" Quantity:"+qty+" Balance After Trade:"+bal+"<br>" ; 
		}
		myMongo.close();
		
		y += "Balance Amount:"+amoun+"<br>";
		model.addAttribute("message",y);
		return "viewtrades";
		
	}
	@RequestMapping(value = "/trade/delete", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String deleteTrade(@RequestParam Map<String,String> request,Model model) {
		
		String id = request.get("id");
		ObjectId objId = new ObjectId(id);
		MongoClient myMongo = new MongoClient(uri);
		MongoDatabase database = myMongo.getDatabase("Task5");
		MongoCollection<Document> tradecollection = database.getCollection("trade");
		tradecollection.deleteOne(Filters.eq("_id",objId));
		System.out.println(objId.toString());
		MongoCollection<Document> usercollection = database.getCollection("user_portfolio");
		usercollection.deleteOne(Filters.eq("id",objId));
		model.addAttribute("message","Trade Deleted Successfully");
		myMongo.close();
		
		return "deletetrades";
	}
}
