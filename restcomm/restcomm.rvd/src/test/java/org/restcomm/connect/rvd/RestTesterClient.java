package org.restcomm.connect.rvd;

import com.sun.jersey.api.client.Client;

public class RestTesterClient {
    private static RestTesterClient instance;
    private Client client;
    
    private RestTesterClient() {
        client = Client.create();
    };
    
    public static RestTesterClient getInstance() {
        if ( instance == null )
            instance = new RestTesterClient();
        return instance;
    }
    
    public Client getClient() {
        return client;
    }
    
    
}


