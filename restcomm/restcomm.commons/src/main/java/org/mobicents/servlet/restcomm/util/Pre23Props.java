package org.mobicents.servlet.restcomm.util;

import akka.actor.Actor;
import akka.actor.Props;
import akka.japi.Creator;

/**
 * Created by pach on 29/07/15.
 */
public class Pre23Props {

    public static class DelegateCreator implements Creator<Actor> {
        Creator<Actor> delegate;
        public DelegateCreator(final Creator<Actor> delegate) {
            this.delegate = delegate;
        }
        @Override
        public Actor create() throws Exception {
            return delegate.create();
        }
    }

    public static Props create(final Creator<Actor> creator) {
        return Props.create(new DelegateCreator(creator));
    }

}
