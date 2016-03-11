package org.mobicents.servlet.restcomm.rvd.security;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.security.exceptions.InvalidTicketCookie;

public class TicketRepository {
    static final Logger logger = Logger.getLogger(TicketRepository.class.getName());
    private static TicketRepository instance;

    private ConcurrentHashMap<String,Ticket> tickets = new ConcurrentHashMap<String,Ticket>();
    private Date lastRemovalCheckTime; // time when a check was run to invalidate stale tickets
    static final Integer STALE_REMOVAL_INTERVAL_MINUTES = 120; // every how many minutes will the stale ticket removal take place?
    static final Integer STALE_TICKET_LIFETIME_MINUTES = 120; // a ticket is considered stale if it hasn't been used for X minutes


    private TicketRepository() {
        lastRemovalCheckTime = new Date();
        logger.debug("TicketRepository created at " + lastRemovalCheckTime );
    }

    public static TicketRepository getInstance() {
        if ( instance == null ) {
            instance = new TicketRepository();
        }
        return instance;
    }

    public void putTicket(Ticket ticket) {
        //logger.debug("Adding ticket: " + ticket);
        tickets.put(ticket.getTicketId(), ticket);
        //logger.debug(this.toString());
    }

    /**
     * Returns the ticket for the ticketId or null if it is not found
     * @param ticketId
     * @return
     */
    public Ticket findTicket(String ticketId) {
        return tickets.get(ticketId);
    }

    public void invalidateTicket(String ticketCookie) {
        try {
            String ticketId = SecurityUtils.getTicketIdFromTicketCookie(ticketCookie);
            Ticket ticket = tickets.get(ticketId);
            if (ticket != null) {
                logger.debug("Invalidating ticket: " + ticket);
                tickets.remove(ticket.getTicketId());
                //logger.debug(this.toString());
                return;
            }
        } catch (InvalidTicketCookie e) {
            logger.error(e,e);
        }
        logger.warn("Could not invalidate ticket " + ticketCookie + ". Ticket does not exist");
    }

    /**
     * Runs the stale ticket removal job if at least STALE_REMOVAL_INTERVAL_HOURS have passed
     */
    public void remindStaleTicketRemoval() {
        //logger.debug("Checking if it's time to remove tickets");
        Date currentDate = new Date();
        Integer intervalMillis = STALE_REMOVAL_INTERVAL_MINUTES * 60 * 1000;
        if ( currentDate.getTime() - lastRemovalCheckTime.getTime() >= intervalMillis) {
            lastRemovalCheckTime = currentDate; // reset interval
            int removedCount = runStaleTicketRemovalJob(currentDate);
        }
    }

    /**
     * Remove tickets that haven't been used for STALE_TICKET_LIFETIME_MINUTES
     * @return how many stale tickets were removed
     */
    private int runStaleTicketRemovalJob(Date currentDate) {
        logger.debug("Running stale ticket removal job...");
        int removedCount = 0;
        for (String ticketId : tickets.keySet()) {
            Ticket ticket = tickets.get(ticketId);
            Integer staleTicketIntervalMillis = STALE_TICKET_LIFETIME_MINUTES * 60 * 1000;
            if ( currentDate.getTime() - ticket.getTimeLastAccessed().getTime() >= staleTicketIntervalMillis ) {
                logger.debug("Removing stale ticket " + ticket.toString());
                tickets.remove(ticketId);
                removedCount ++;
            }
        }
        //this.toString();
        logger.debug("" + removedCount + " tickets removed." + tickets.size() + " tickets still in TicketRepository");
        return removedCount;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Ticket repository contents:\n");
        for (String ticketId : this.tickets.keySet() ) {
            builder.append(tickets.get(ticketId).toString()).append("\n");
        }
        return builder.toString();
    }
}
