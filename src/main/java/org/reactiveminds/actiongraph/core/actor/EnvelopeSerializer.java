package org.reactiveminds.actiongraph.core.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.dispatch.Envelope;
import akka.serialization.Serialization;
import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.react.Matchers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

class EnvelopeSerializer implements Serializer<Envelope>, Serializable {
    private transient ActorRefProvider actorRefProvider;

    EnvelopeSerializer() {
        this.actorRefProvider = Actors.instance().serialization().system().provider();
    }

    @Override
    public void serialize(DataOutput out, Envelope value) throws IOException {
        if (value == null) {
            out.writeByte(0);
            return;
        }
        out.writeByte(1);
        String serializedActorPath = Serialization.serializedActorPath(value.sender());
        Object message = value.message();
        if (!(message instanceof Event))
            throw new IOException("unsupported event type " + message.getClass());
        Event event = (Event) message;
        String payload = event.payload;
        String pattern = event.predicate.pattern();
        out.writeUTF(serializedActorPath);
        out.writeInt(event.id());
        if (payload != null) {
            out.writeByte(1);
            out.writeUTF(payload);
        } else {
            out.writeByte(0);
        }
        if (pattern != null) {
            out.writeByte(1);
            out.writeUTF(pattern);
            out.writeUTF(event.predicate.getClass().getName());
        } else {
            out.writeByte(0);
        }
    }

    @Override
    public Envelope deserialize(DataInput in, int available) throws IOException {
        byte b = in.readByte();
        if (b == 0)
            return null;
        String utf = in.readUTF();
        if (actorRefProvider == null)
            actorRefProvider = Actors.instance().serialization().system().provider();
        ActorRef actorRef = actorRefProvider.resolveActorRef(utf);
        int eventId = in.readInt();
        byte has = in.readByte();
        String payload = null;
        if (has == 1)
            payload = in.readUTF();
        has = in.readByte();
        String pattern = null;
        String type = null;
        if (has == 1) {
            pattern = in.readUTF();
            type = in.readUTF();
        }
        Event event = Event.newEvent(eventId, payload, pattern != null ? "all".equals(pattern) ?
                Matchers.ALL :
                type.toUpperCase().contains("REGEX") ? Matchers.REGEX(pattern) : Matchers.PATH(pattern) : null);
        return new Envelope(event, actorRef);
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
