package org.restcomm.connect.extension.api;

import java.util.Map;

public class CallResponse extends ExtensionResponse{
    //expose all expected properties
//    "outbound-proxy": [
//                       {
//                         "outbound-proxy": "outbound.proxy.com:5060",
//                         "username": "myusername",
//                         "password": "mypassword",
//                         "additional-headers": [
//                           {
//                             "header-name": "Route",
//                             "header-value": "sip:10.10.10.10:5080;transport=UDP;lr"
//                           },
//                           {
//                             "header-name": "X-Custom-Header1",
//                             "header-value": "ID-X-12345"
//                           }
//                         ]
//                       }
//                     ],
//    username:{
//    },
//    password:{
//    },
//    proxy:{
//        //proxy info here
//    },
    
    Map<String,String> additionalHeaders;
    String outboundProxy;
    String outboundProxyUsername;
    String outboundProxyPassword;
}
