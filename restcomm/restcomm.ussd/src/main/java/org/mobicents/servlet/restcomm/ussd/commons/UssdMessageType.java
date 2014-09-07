/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.ussd.commons;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public enum UssdMessageType {

    /*
     * There are in total 6 USSD Messages
     * processUnstructuredSSRequest_Request : This request is always initiated by Mobile/User and hence is first message in PULL flow. Always PULL Case
     * processUnstructuredSSRequest_Response : This is always last response from USSD Gateway to above request received. However there can be many exchanges of below messages if Application wants tree based menu. Always PULL Case
     * unstructuredSSRequest_Request : USSD Gateway can send this message in response to received "processUnstructuredSSRequest_Request". This means that USSD Gateway is expecting some response back from Mobile/User. This can be also used in PUSH, where USSD Gateway is sending this message for first time to get user response and after receiving below response, USSD Gw can again send another  "unstructuredSSRequest_Request" or send "unstructuredSSNotify_Request" just to notify user (without expecting any input from user). Both PULL and PUSH case.
     * unstructuredSSRequest_Response : This response is always from Mobile/User. Both PULL and PUSH Case
     * unstructuredSSNotify_Request : This is always used in PUSH, where USSD Gateway is sending this message to notify User/Mobile. User doesn't get any option to send any response but just press Ok. As soon as he presses Ok below response is sent back. Always PUSH case
     * unstructuredSSNotify_Response: Response from user. Always PUSH case
     */
    /** processUnstructuredSSRequest_Request : This request is always initiated by Mobile/User and hence is first message in PULL flow. Always PULL Case */
    processUnstructuredSSRequest_Request,
    /** processUnstructuredSSRequest_Response : This is always last response from USSD Gateway to above request received. However there can be many exchanges of below messages if Application wants tree based menu. Always PULL Case */
    processUnstructuredSSRequest_Response,
    /**unstructuredSSRequest_Request : USSD Gateway can send this message in response to received "processUnstructuredSSRequest_Request". This means that USSD Gateway is expecting some response back from Mobile/User. This can be also used in PUSH, where USSD Gateway is sending this message for first time to get user response and after receiving below response, USSD Gw can again send another  "unstructuredSSRequest_Request" or send "unstructuredSSNotify_Request" just to notify user (without expecting any input from user). Both PULL and PUSH case.*/
    unstructuredSSRequest_Request,
    /**unstructuredSSRequest_Response : This response is always from Mobile/User. Both PULL and PUSH Case*/
    unstructuredSSRequest_Response,
    /**unstructuredSSNotify_Request : This is always used in PUSH, where USSD Gateway is sending this message to notify User/Mobile. User doesn't get any option to send any response but just press Ok. As soon as he presses Ok below response is sent back. Always PUSH case*/
    unstructuredSSNotify_Request,
    /**unstructuredSSNotify_Response: Response from user. Always PUSH case*/
    unstructuredSSNotify_Response;
}
