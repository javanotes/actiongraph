package org.reactiveminds.actiongraph.store.mapdb;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.dispatch.Envelope;
import akka.serialization.Serialization;
import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.core.actor.Actors;
import org.reactiveminds.actiongraph.core.actor.Command;
import org.reactiveminds.actiongraph.react.Matchers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;

class EnvelopeSerializer implements Serializer<Envelope>, Serializable {
    private transient ActorRefProvider actorRefProvider;

    EnvelopeSerializer() {
        this.actorRefProvider = Actors.instance().serializationSystemProvider();
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
        if (!(message instanceof Command))
            throw new IOException("unsupported event type " + message.getClass());
        Command command = (Command) message;
        String payload = command.payload;
        String pattern = command.predicate.pattern();
        out.writeUTF(command.correlationId);
        out.writeUTF(serializedActorPath);
        out.writeInt(command.type());
        if (payload != null) {
            out.writeByte(1);
            out.writeUTF(payload);
        } else {
            out.writeByte(0);
        }
        if (pattern != null) {
            out.writeByte(1);
            out.writeUTF(pattern);
            out.writeUTF(command.predicate.getClass().getName());
        } else {
            out.writeByte(0);
        }
        if(command.type() == 0){
            Command.ReplayCommand replayEvent = (Command.ReplayCommand) command;
            out.writeInt(replayEvent.originType);
            out.writeInt(replayEvent.retryCount);
            out.writeLong(replayEvent.originTime);
            out.writeLong(replayEvent.delay.toMillis());
        }
    }

    @Override
    public Envelope deserialize(DataInput in, int available) throws IOException {
        byte b = in.readByte();
        if (b == 0)
            return null;
        String corrId = in.readUTF();
        String utf = in.readUTF();
        if (actorRefProvider == null)
            actorRefProvider = Actors.instance().serializationSystemProvider();
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
        if(eventId == 0){
            int origType = in.readInt();
            int retry = in.readInt();
            long origTime = in.readLong();
            Duration delay = Duration.ofMillis(in.readLong());
            Command command = Command.newCommand(corrId, origType, payload, pattern != null ? "all".equalsIgnoreCase(pattern) ?
                    Matchers.ALL :
                    Matchers.REGEX(pattern) : null);
            return new Envelope(new Command.ReplayCommand(command, origTime, delay, retry), actorRef);
        }
        Command command = Command.newCommand(corrId, eventId, payload, pattern != null ? "all".equalsIgnoreCase(pattern) ?
                Matchers.ALL :
                Matchers.REGEX(pattern) : null);
        return new Envelope(command, actorRef);
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
