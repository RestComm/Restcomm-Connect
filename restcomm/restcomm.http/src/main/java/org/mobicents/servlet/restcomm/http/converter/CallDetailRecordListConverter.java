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
 */
package org.mobicents.servlet.restcomm.http.converter;

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.CallDetailRecord;
import org.mobicents.servlet.restcomm.entities.CallDetailRecordList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@ThreadSafe public final class CallDetailRecordListConverter extends AbstractConverter implements JsonSerializer<CallDetailRecordList> {

	Integer page, pageSize, total;
	String pathUri;
	
	public CallDetailRecordListConverter(final Configuration configuration) {
		super(configuration);
	}

	@SuppressWarnings("rawtypes")
	@Override public boolean canConvert(final Class klass) {
		return CallDetailRecordList.class.equals(klass);
	}

	@Override public void marshal(final Object object, final HierarchicalStreamWriter writer,
			final MarshallingContext context) {
		final CallDetailRecordList list = (CallDetailRecordList)object;
		writer.startNode("Calls");
		for(final CallDetailRecord cdr : list.getCallDetailRecords()) {
			context.convertAnother(cdr);
		}
		writer.endNode();
	}

	@Override
	public JsonObject serialize(CallDetailRecordList cdrList, Type type, JsonSerializationContext context) { 

		JsonObject result = new JsonObject();
		
		JsonArray array = new JsonArray();
		for(CallDetailRecord cdr: cdrList.getCallDetailRecords()){
			array.add(context.serialize(cdr));
		}

		int totalPages = total / pageSize;
		String lastSid = (page == totalPages) ? "null" :cdrList.getCallDetailRecords().get(pageSize-1).getSid().toString();
		
		result.addProperty("page", page);
		result.addProperty("num_pages", totalPages);
		result.addProperty("page_size", pageSize);
		result.addProperty("total", total);
		result.addProperty("start", page*pageSize);
		result.addProperty("end", (page == totalPages) ? (page*pageSize)+cdrList.getCallDetailRecords().size() :(pageSize-1)+(page*pageSize));
		result.addProperty("uri", pathUri);
		result.addProperty("first_page_uri", pathUri+"?Page=0&PageSize="+pageSize);
		result.addProperty("previous_page_uri", (page == 0) ? "null" : pathUri+"?Page="+(page-1)+"&PageSize="+pageSize);
		result.addProperty("next_page_uri", (page == totalPages) ? "null" : pathUri+"?Page="+(page+1)+"&PageSize="+pageSize+"&AfterSid="+lastSid);
		result.addProperty("last_page_uri", pathUri+"?Page="+totalPages+"&PageSize="+pageSize);
		result.add("calls", array);

		return result;
	}
	
	public void setPage(Integer page) {
		this.page = page;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public void setCount(Integer count) {
		this.total = count;
	}

	public void setPathUri(String pathUri) {
		this.pathUri = pathUri;
	}


}
