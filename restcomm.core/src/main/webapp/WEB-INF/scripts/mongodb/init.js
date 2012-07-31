/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * 
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */

// Administrator Account
var account = {
	  "sid" : "ACae6e420f425248d6a26948c17a9e2acf",
	  "date_created" : "2012-04-24",
	  "date_updated" : "2012-04-24",
	  "email_address" : "administrator@company.com",
	  "friendly_name" : "Default Administrator Account",
	  "type" : "Full",
	  "auth_token" : "77f8c12cc7b8f8423e5c38b035249166",
	  "status" : "active",
	  "role" : "Administrator",
	  "uri" : "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf"
    };
// Demo Incoming Phone Number
var ipn = {
      "sid" : "PNc96bd38c7fd7413c99cab286bb73df5b",
      "date_created" : "2012-04-28",
      "date_updated" : "2012-04-28",
      "friendly_name" : "234",
      "account_sid" : "ACae6e420f425248d6a26948c17a9e2acf",
      "phone_number" : "+1234",
      "api_version" : "2012-04-24",
      "voice_caller_id_lookup" : false,
      "voice_url" : "http://127.0.0.1:8080/restcomm/demo/hello-world.xml",
      "voice_method" : "POST",
      "voice_fallback_method" : "POST",
      "status_callback_method" : "POST",
      "sms_method" : "POST",
      "sms_fallback_method" : "POST",
      "uri" : "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/IncomingPhoneNumbers/PNc96bd38c7fd7413c99cab286bb73df5b"
    };

// Insert the objects in to the database.
db.restcomm.insert(account);
db.restcomm.insert(ipn);