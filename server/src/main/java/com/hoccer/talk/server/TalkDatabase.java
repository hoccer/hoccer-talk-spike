package com.hoccer.talk.server;

import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import com.hoccer.talk.model.TalkClient;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.talk.model.TalkMessage;

public class TalkDatabase {
	
	private static Hashtable<String, TalkClient> allClientsById
		= new Hashtable<String, TalkClient>();

    private static Hashtable<String, Vector<TalkDelivery>> allDeliveriesByClientId;
	
	public static TalkClient findClient(String clientId) {
		TalkClient result = new TalkClient(clientId);
		allClientsById.put(clientId, result);
		return result;
	}
	
	
	private static Hashtable<String, TalkMessage> allMessagesById =
        new Hashtable<String, TalkMessage>();
	
	public static void saveMessage(TalkMessage m) {
		String id = UUID.randomUUID().toString();
		TalkMessage result = new TalkMessage();
		allMessagesById.put(id, result);
	}

    public static void saveDelivery(TalkDelivery delivery) {
        String clientId = delivery.getReceiverId();
        Vector<TalkDelivery> vec = allDeliveriesByClientId.get(clientId);
        if(vec == null) {
            vec = new Vector<TalkDelivery>();
            allDeliveriesByClientId.put(clientId, vec);
        }
        vec.add(delivery);
    }
	
	public static TalkMessage findMessage(String messageId) {
		return allMessagesById.get(messageId);
	}

    public static TalkDelivery findDelivery(String messageId, String clientId) {
        Vector<TalkDelivery> deliveries = allDeliveriesByClientId.get(clientId);
        for(TalkDelivery d: deliveries) {
            if(d.getMessageId().equals(messageId)) {
                return d;
            }
        }
        return null;
    }
	
}
