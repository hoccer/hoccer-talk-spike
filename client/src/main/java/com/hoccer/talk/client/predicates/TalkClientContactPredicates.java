package com.hoccer.talk.client.predicates;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroupPresence;
import org.apache.commons.collections4.Predicate;

public class TalkClientContactPredicates {
    public static final Predicate<TalkClientContact> IS_ENVIRONMENT_GROUP_PREDICATE = new Predicate<TalkClientContact>() {
        @Override
        public boolean evaluate(TalkClientContact group) {
            TalkGroupPresence groupPresence = group.getGroupPresence();

            return groupPresence != null && (groupPresence.isTypeNearby() || groupPresence.isTypeWorldwide());
        }
    };

    public static final Predicate<TalkClientContact> IS_SELF_PREDICATE = new Predicate<TalkClientContact>() {
        @Override
        public boolean evaluate(TalkClientContact contact) {
            return contact.isSelf();
        }
    };
}
