package org.reactiveminds.actiongraph.store.mapdb;

import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.store.ActionEntry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

class ActionEntrySerializer implements Serializable, Serializer<ActionEntry> {
    @Override
    public void serialize(DataOutput out, ActionEntry value) throws IOException {
        out.writeLong(value.getCreated());
        out.writeLong(value.getUpdated());
        out.writeUTF(value.getStatus());
        out.writeUTF(value.getCorrelationId());
        out.writeUTF(value.getPathMatcher());
        out.writeUTF(value.getPayload());
        out.writeUTF(value.getAddlInfo());
        out.writeUTF(value.getRoot());
    }

    @Override
    public ActionEntry deserialize(DataInput in, int available) throws IOException {
        ActionEntry entry = new ActionEntry();
        entry.setCreated(in.readLong());
        entry.setUpdated(in.readLong());
        entry.setStatus(in.readUTF());
        entry.setCorrelationId(in.readUTF());
        entry.setPathMatcher(in.readUTF());
        entry.setPayload(in.readUTF());
        entry.setAddlInfo(in.readUTF());
        entry.setRoot(in.readUTF());
        return entry;
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
