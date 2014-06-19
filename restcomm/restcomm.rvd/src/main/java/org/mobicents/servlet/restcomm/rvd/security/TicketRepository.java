package org.mobicents.servlet.restcomm.rvd.security;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class TicketRepository {
    static final Logger logger = Logger.getLogger(TicketRepository.class.getName());
    private static TicketRepository instance;

    private Map<String,Ticket> tickets = new HashMap<String,Ticket>();


    private TicketRepository() {
        logger.debug("TicketRepository created");

        Ticket sampleTicket = new Ticket("otsakir@gmail.com", "111");
        tickets.put(sampleTicket.getTicketId(), sampleTicket);
    }

    public static TicketRepository getInstance() {
        if ( instance == null ) {
            instance = new TicketRepository();
        }
        return instance;
    }

    public void putTicket(Ticket ticket) {
        tickets.put(ticket.getTicketId(), ticket);
    }

    /**
     * Returns the ticket for the ticketId or null if it is not found
     * @param ticketId
     * @return
     */
    public Ticket findTicket(String ticketId) {
        return tickets.get(ticketId);
    }

}
