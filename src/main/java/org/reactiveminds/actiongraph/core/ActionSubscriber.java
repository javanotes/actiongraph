package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.react.Reaction;

import java.io.Serializable;
import java.util.concurrent.Flow;

class ActionSubscriber implements Flow.Subscriber<Serializable> {
    private final Action action;
    private final Reaction reaction;

    ActionSubscriber(Action action, Reaction reaction) {
        this.action = action;
        this.reaction = reaction;
    }

    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(1);
    }

    @Override
    public void onNext(Serializable item) {
        reaction.accept(action.path(), item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {

    }
}
