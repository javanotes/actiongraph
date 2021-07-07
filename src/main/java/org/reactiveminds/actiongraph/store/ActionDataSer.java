package org.reactiveminds.actiongraph.store;

import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

class ActionDataSer implements Serializer<ActionData>, Serializable {
    @Override
    public void serialize(DataOutput out, ActionData value) throws IOException {
        out.writeInt(value.getProps().size());
        for (ActionData.Props props : value.getProps()) {
            out.writeUTF(props.url);
            out.writeUTF(props.jsonTemplate);
        }
    }

    @Override
    public ActionData deserialize(DataInput in, int available) throws IOException {
        ActionData groupData = new ActionData();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            ActionData.Props props = new ActionData.Props();
            props.url = in.readUTF();
            props.jsonTemplate = in.readUTF();
            groupData.getProps().add(props);
        }
        return groupData;
    }

    @Override
    public int fixedSize() {
        return -1;
    }
}
