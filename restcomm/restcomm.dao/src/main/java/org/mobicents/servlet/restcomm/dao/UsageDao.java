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
package org.mobicents.servlet.restcomm.dao;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.entities.Usage;

import java.util.List;

/**
 * @author brainslog@gmail.com (Alexandre Mendonca)
 */
public interface UsageDao {
  List<Usage> getUsage(final Sid accountSid);

  List<Usage> getUsageDaily(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  List<Usage> getUsageMonthly(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  List<Usage> getUsageYearly(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  List<Usage> getUsageAllTime(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  /*
  List<Usage> getUsageToday(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  List<Usage> getUsageYesterday(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  List<Usage> getUsageThisMonth(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);

  List<Usage> getUsageLastMonth(final Sid accountSid, Usage.Category category, DateTime startDate, DateTime endDate);
  */
}
