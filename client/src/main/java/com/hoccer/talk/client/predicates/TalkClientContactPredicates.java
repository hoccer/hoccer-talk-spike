package com.hoccer.talk.client.predicates;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkGroup;
import org.apache.commons.collections4.Predicate;

public class TalkClientContactPredicates {
    public static final Predicate<TalkClientContact> IS_NEARBY_GROUP_PREDICATE = new Predicate<TalkClientContact>() {
        @Override
        public boolean evaluate(TalkClientContact group) {
            TalkGroup groupPresence = group.getGroupPresence();

            if (groupPresence != null) {
                return groupPresence.isTypeNearby();
            }

            return false;
        }
    };

    public static final Predicate<TalkClientContact> IS_SELF_PREDICATE = new Predicate<TalkClientContact>() {
        @Override
        public boolean evaluate(TalkClientContact contact) {
            return contact.isSelf();
        }
    };
}
