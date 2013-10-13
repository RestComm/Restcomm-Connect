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
		writer.addAttribute("page", String.valueOf(page));
		writer.addAttribute("numpages", String.valueOf(getTotalPages()));
		writer.addAttribute("pagesize", String.valueOf(pageSize));
		writer.addAttribute("total", String.valueOf(getTotalPages()));
		writer.addAttribute("start", getFirstIndex());
		writer.addAttribute("end", getLastIndex(list));
		writer.addAttribute("uri", pathUri);
		writer.addAttribute("firstpageuri", getFirstPageUri());
		writer.addAttribute("previouspageuri", getPreviousPageUri());
		writer.addAttribute("nextpageuri", getNextPageUri(list));
		writer.addAttribute("lastpageuri=", getLastPageUri());
		
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
		
		result.addProperty("page", page);
		result.addProperty("num_pages", getTotalPages());
		result.addProperty("page_size", pageSize);
		result.addProperty("total", total);
		result.addProperty("start", getFirstIndex());
		result.addProperty("end", getLastIndex(cdrList));
		result.addProperty("uri", pathUri);
		result.addProperty("first_page_uri", getFirstPageUri());
		result.addProperty("previous_page_uri", getPreviousPageUri());
		result.addProperty("next_page_uri", getNextPageUri(cdrList));
		result.addProperty("last_page_uri", getLastPageUri());
		result.add("calls", array);

		return result;
	}
	
	private int getTotalPages(){
		return total / pageSize;
	}
	
	private String getFirstIndex(){
		return String.valueOf(page*pageSize);
	}
	
	private String getLastIndex(CallDetailRecordList list){
		return String.valueOf((page == getTotalPages()) ? (page*pageSize)+list.getCallDetailRecords().size() :(pageSize-1)+(page*pageSize));
	}
	
	private String getFirstPageUri(){
		return pathUri+"?Page=0&PageSize="+pageSize;
	}
	
	private String getPreviousPageUri(){
		return ((page == 0) ? "null" : pathUri+"?Page="+(page-1)+"&PageSize="+pageSize);
	}
	
	private String getNextPageUri(CallDetailRecordList list){
		String lastSid = (page == getTotalPages()) ? "null" :list.getCallDetailRecords().get(pageSize-1).getSid().toString();
		return (page == getTotalPages()) ? "null" : pathUri+"?Page="+(page+1)+"&PageSize="+pageSize+"&AfterSid="+lastSid;
	}
	
	private String getLastPageUri(){
		return pathUri+"?Page="+getTotalPages()+"&PageSize="+pageSize;
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
